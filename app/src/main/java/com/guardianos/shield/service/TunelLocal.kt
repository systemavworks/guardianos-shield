// app/src/main/java/com/guardianos/shield/service/TunelLocal.kt
package com.guardianos.shield.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.guardianos.shield.MainActivity
import com.guardianos.shield.R
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

class TunelLocal : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var serviceJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var blockedToday = 0
    private val contentFilter = ContentFilter()
    
    companion object {
        private const val CHANNEL_ID = "GuardianShield_VPN"
        private const val NOTIFICATION_ID = 1
        private const val VPN_MTU = 1500
        private const val VPN_ADDRESS = "10.0.0.2"
        private const val VPN_ROUTE = "0.0.0.0"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        startVpnService()
        return START_STICKY
    }

    private fun startVpnService() {
        serviceJob = scope.launch {
            try {
                vpnInterface = createVpnInterface()
                vpnInterface?.let { tunnel ->
                    handlePackets(tunnel)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                stopSelf()
            }
        }
    }

    private fun createVpnInterface(): ParcelFileDescriptor {
        return Builder()
            .setSession("GuardianOS Shield")
            .addAddress(VPN_ADDRESS, 24)
            .addRoute(VPN_ROUTE, 0)
            .addDnsServer("1.1.1.3") // Cloudflare for Families (bloquea malware y adultos)
            .addDnsServer("1.0.0.3")
            .setBlocking(false)
            .setMtu(VPN_MTU)
            .establish() ?: throw IllegalStateException("No se pudo establecer la VPN")
    }

    private suspend fun handlePackets(tunnel: ParcelFileDescriptor) {
        withContext(Dispatchers.IO) {
            val inputStream = FileInputStream(tunnel.fileDescriptor)
            val outputStream = FileOutputStream(tunnel.fileDescriptor)
            val buffer = ByteBuffer.allocate(VPN_MTU)

            while (isActive) {
                try {
                    val length = inputStream.channel.read(buffer)
                    if (length > 0) {
                        buffer.flip()
                        
                        // Analizar el paquete
                        val packet = PacketAnalyzer.analyze(buffer)
                        
                        // Verificar si debe bloquearse
                        if (shouldBlock(packet)) {
                            val category = contentFilter.getCategoryForDomain(packet.destination)
                            blockedToday++
                            
                            // Guardar en base de datos
                            launch {
                                val repository = GuardianRepository(
                                    GuardianDatabase.getDatabase(applicationContext)
                                )
                                repository.addBlockedSite(packet.destination, category)
                            }
                            
                            updateNotification()
                            sendBlockNotification(packet.destination, category)
                            buffer.clear()
                            continue
                        }
                        
                        // Reenviar paquete permitido
                        buffer.flip()
                        outputStream.channel.write(buffer)
                        buffer.clear()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    break
                }
            }
        }
    }

    private fun shouldBlock(packet: NetworkPacket): Boolean {
        val domain = packet.destination
        
        // Verificar contra listas de bloqueo
        return contentFilter.isBlocked(domain)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Protección GuardianOS",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Servicio de filtrado web activo"
                setShowBadge(false)
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛡️ GuardianOS Shield")
            .setContentText("Protección activa • $blockedToday bloqueados hoy")
            .setSmallIcon(android.R.drawable.ic_secure)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        val notification = createNotification()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun sendBlockNotification(domain: String, category: String) {
        // Enviar notificación de bloqueo si está configurado
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛡️ Sitio bloqueado")
            .setContentText("$category: $domain")
            .setSmallIcon(android.R.drawable.ic_delete)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob?.cancel()
        vpnInterface?.close()
        scope.cancel()
    }

    override fun onRevoke() {
        super.onRevoke()
        stopSelf()
    }
}

// Analizador de paquetes de red
object PacketAnalyzer {
    fun analyze(buffer: ByteBuffer): NetworkPacket {
        buffer.rewind()
        
        // Leer cabecera IP (simplificado)
        val versionAndIHL = buffer.get().toInt() and 0xFF
        val version = versionAndIHL shr 4
        
        // Saltar al destino (simplificado para este ejemplo)
        buffer.position(16)
        val destBytes = ByteArray(4)
        buffer.get(destBytes)
        
        val destination = destBytes.joinToString(".") { (it.toInt() and 0xFF).toString() }
        
        return NetworkPacket(
            destination = destination,
            protocol = "IP",
            version = version
        )
    }
}

data class NetworkPacket(
    val destination: String,
    val protocol: String,
    val version: Int
)

// Filtro de contenido mejorado
class ContentFilter(private val context: android.content.Context) {
    private val repository = GuardianRepository(GuardianDatabase.getDatabase(context))
    private var customBlacklist = setOf<String>()
    private var customWhitelist = setOf<String>()
    
    init {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            repository.blacklist.collect { list ->
                customBlacklist = list.map { it.domain }.toSet()
            }
        }
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            repository.whitelist.collect { list ->
                customWhitelist = list.map { it.domain }.toSet()
            }
        }
    }
    
    // Listas de dominios bloqueados por categoría (expandidas)
    private val adultContent = setOf(
        "pornhub.com", "xvideos.com", "xnxx.com", "redtube.com",
        "youporn.com", "tube8.com", "spankbang.com", "xhamster.com",
        "chaturbate.com", "stripchat.com", "livejasmin.com", "onlyfans.com",
        "pornhd.com", "beeg.com", "sex.com", "eporner.com", "tnaflix.com",
        "drtuber.com", "txxx.com", "motherless.com", "4chan.org"
    )
    
    private val violentContent = setOf(
        "bestgore.com", "documenting reality.com", "goregrish.com",
        "theync.com", "kaotic.com"
    )
    
    private val malwareAndPhishing = setOf(
        "free-minecraft.com", "free-robux.com", "download-fortnite-free.com",
        "win-iphone-now.com", "your-prize-here.com"
    )
    
    private val socialMediaRestricted = setOf(
        // Opcional: para control parental estricto
        // "tiktok.com", "snapchat.com", "instagram.com"
    )
    
    // Palabras clave sospechosas en URLs
    private val suspiciousKeywords = listOf(
        "xxx", "sex", "porn", "adult", "nude", "naked", "nsfw",
        "18+", "gore", "death", "kill", "hack", "crack", "keygen",
        "torrent", "pirate", "warez"
    )
    
    fun isBlocked(domain: String): Boolean {
        val cleanDomain = domain.lowercase().trim()
        
        // 1. Verificar lista blanca primero (máxima prioridad)
        if (customWhitelist.any { cleanDomain.contains(it) || matchesWildcard(cleanDomain, it) }) {
            return false
        }
        
        // 2. Verificar lista negra personalizada
        if (customBlacklist.any { cleanDomain.contains(it) || matchesWildcard(cleanDomain, it) }) {
            return true
        }
        
        // 3. Verificar listas de bloqueo directas
        if (adultContent.any { cleanDomain.contains(it) }) return true
        if (violentContent.any { cleanDomain.contains(it) }) return true
        if (malwareAndPhishing.any { cleanDomain.contains(it) }) return true
        
        // 4. Verificar palabras clave sospechosas
        if (suspiciousKeywords.any { cleanDomain.contains(it) }) return true
        
        // 5. Verificar patrones de phishing
        if (isPotentialPhishing(cleanDomain)) return true
        
        // 6. Verificar contra Google Safe Browsing (si está disponible)
        if (checkSafeBrowsing(cleanDomain)) return true
        
        return false
    }
    
    private fun matchesWildcard(domain: String, pattern: String): Boolean {
        if (!pattern.startsWith("*.")) return domain == pattern
        val suffix = pattern.substring(2)
        return domain.endsWith(suffix) || domain == suffix
    }
    
    private fun checkSafeBrowsing(domain: String): Boolean {
        // Aquí se implementaría la integración con Google Safe Browsing API
        // Por ahora retorna false para no bloquear sin verificación
        // Implementación real requiere API key y peticiones HTTP
        return false
    }
    
    fun getCategoryForDomain(domain: String): String {
        val cleanDomain = domain.lowercase().trim()
        return when {
            adultContent.any { cleanDomain.contains(it) } -> "Contenido adulto"
            violentContent.any { cleanDomain.contains(it) } -> "Violencia"
            malwareAndPhishing.any { cleanDomain.contains(it) } -> "Malware/Phishing"
            socialMediaRestricted.any { cleanDomain.contains(it) } -> "Redes sociales"
            else -> "Otro"
        }
    }
    
    private fun isPotentialPhishing(domain: String): Boolean {
        // Detectar dominios sospechosos con muchos guiones
        if (domain.count { it == '-' } > 3) return true
        
        // Detectar subdominios excesivos
        if (domain.count { it == '.' } > 4) return true
        
        // Detectar URLs con números sospechosos
        if (domain.matches(Regex(".*\\d{5,}.*"))) return true
        
        return false
    }
    
    // Método para agregar dominios personalizados
    fun addCustomBlock(domain: String) {
        // Implementar persistencia con SharedPreferences o Room
    }
    
    // Método para permitir dominios temporalmente
    fun addWhitelist(domain: String) {
        // Implementar lista blanca
    }
}

// Extensión para análisis DNS
object DNSAnalyzer {
    private val dnsCache = mutableMapOf<String, String>()
    
    fun resolveDomain(ip: String): String? {
        return dnsCache[ip]
    }
    
    fun cacheDomain(ip: String, domain: String) {
        dnsCache[ip] = domain
    }
}
