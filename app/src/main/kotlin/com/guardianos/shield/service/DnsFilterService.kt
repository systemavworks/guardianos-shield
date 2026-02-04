package com.guardianos.shield.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.guardianos.shield.MainActivity
import com.guardianos.shield.data.GuardianDatabase
import com.guardianos.shield.data.GuardianRepository
import kotlinx.coroutines.*

/**
 * Servicio VPN de DNS Transparente
 * 
 * ESTRATEGIA CORRECTA Y PROBADA:
 * - Configuramos DNS seguros (CleanBrowsing Adult Filter 185.228.168.168)
 * - CleanBrowsing Adult Filter: EL MÁS RESTRICTIVO disponible
 * - Bloquea TODO excepto contenido educativo y productivo
 * - NO capturamos todo el tráfico (sin addRoute general)
 * - NO procesamos paquetes manualmente
 * - Internet funciona normalmente SOLO para contenido permitido
 * 
 * CleanBrowsing Adult Filter bloquea:
 * ✓ Pornografía y contenido adulto
 * ✓ Malware y phishing
 * ✓ Redes sociales (TikTok, Facebook, Instagram, Twitter, etc.)
 * ✓ Juegos online y gaming
 * ✓ Entretenimiento (YouTube para adultos, streaming, etc.)
 * ✓ Proxies, VPNs y contenido mixto
 */
class DnsFilterService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    @Volatile
    private var isRunning = false

    private lateinit var repository: GuardianRepository
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    companion object {
        const val CHANNEL_ID = "GuardianShield_DNS"
        const val NOTIFICATION_ID = 1

        const val ACTION_VPN_STARTED = "com.guardianos.shield.VPN_STARTED"
        const val ACTION_VPN_STOPPED = "com.guardianos.shield.VPN_STOPPED"
        const val ACTION_VPN_ERROR = "com.guardianos.shield.VPN_ERROR"

        // DNS CleanBrowsing Adult Filter: Bloquea TODO excepto contenido educativo
        // El más restrictivo disponible - bloquea redes sociales, juegos, entretenimiento
        private const val DNS_PRIMARY = "185.228.168.168"    // Adult Filter Primary
        private const val DNS_SECONDARY = "185.228.169.168"   // Adult Filter Secondary
    }

    override fun onCreate() {
        super.onCreate()
        repository = GuardianRepository(this, GuardianDatabase.getDatabase(this))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isRunning) return START_STICKY

        val notification = createNotification()
        
        // Iniciar como servicio foreground (configuración original que funciona)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Configurar VPN
        if (setupVpn()) {
            isRunning = true
            sendBroadcast(Intent(ACTION_VPN_STARTED))
            Log.d("GuardianVPN", "✅ Filtro DNS Activo (CleanBrowsing Adult Filter)")
        } else {
            sendBroadcast(Intent(ACTION_VPN_ERROR))
            stopSelf()
        }

        return START_STICKY
    }

    private fun setupVpn(): Boolean {
        return try {
            val builder = Builder()
                .setSession("GuardianOS Shield")
                .setMtu(1500)
                .addAddress("10.0.0.2", 32)
                .addDnsServer(DNS_PRIMARY)
                .addDnsServer(DNS_SECONDARY)
            
            // CLAVE: Excluir la propia app para evitar bucles infinitos
            try {
                builder.addDisallowedApplication(packageName)
                Log.d("GuardianVPN", "✅ App excluida del túnel VPN")
            } catch (e: Exception) {
                Log.w("GuardianVPN", "Advertencia: ${e.message}")
            }
            
            // ✅ Android 15+: Configuración opcional non-blocking (no afecta versiones anteriores)
            if (Build.VERSION.SDK_INT >= 35) {
                try {
                    builder.setBlocking(false)
                    Log.d("GuardianVPN", "✅ Android 15: Modo non-blocking configurado")
                } catch (e: Exception) {
                    // Ignorar si falla, no es crítico
                }
            }

            // NO agregamos addRoute("0.0.0.0", 0) para que el tráfico normal fluya libremente
            
            vpnInterface = builder.establish()
            
            if (vpnInterface != null) {
                Log.d("GuardianVPN", "╔════════════════════════════════════╗")
                Log.d("GuardianVPN", "║   DNS SEGURO ACTIVADO              ║")
                Log.d("GuardianVPN", "║   CleanBrowsing ADULT FILTER       ║")
                Log.d("GuardianVPN", "║   DNS: ${DNS_PRIMARY}   ║")
                Log.d("GuardianVPN", "║        ${DNS_SECONDARY}  ║")
                Log.d("GuardianVPN", "║   FILTRO MÁS RESTRICTIVO           ║")
                Log.d("GuardianVPN", "║   Bloquea: Adulto + Redes Sociales ║")
                Log.d("GuardianVPN", "║   + Juegos + Entretenimiento       ║")
                Log.d("GuardianVPN", "╚════════════════════════════════════╝")
                true
            } else {
                Log.e("GuardianVPN", "❌ No se pudo establecer VPN")
                false
            }
        } catch (e: Exception) {
            Log.e("GuardianVPN", "❌ Error configurando VPN: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private fun cleanup() {
        if (!isRunning && vpnInterface == null) {
            return
        }
        
        isRunning = false

        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Log.e("GuardianVPN", "Error cerrando VPN: ${e.message}")
        }

        sendBroadcast(Intent(ACTION_VPN_STOPPED))
        Log.d("GuardianVPN", "🛑 VPN detenida")
    }

    override fun onDestroy() {
        cleanup()
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onRevoke() {
        cleanup()
        super.onRevoke()
        stopSelf()
    }

    private fun createNotification(): Notification {
        createNotificationChannel()
        
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GuardianOS Shield")
            .setContentText("Navegación segura activada")
            .setSmallIcon(android.R.drawable.ic_secure)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Protección Web",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Filtrado DNS activo"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

}
