// app/src/main/java/com/guardianos/shield/service/DnsFilterService.kt
package com.guardianos.shield.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.OsConstants
import androidx.core.app.NotificationCompat
import com.guardianos.shield.MainActivity
import com.guardianos.shield.R
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

class DnsFilterService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Streams de red
    private var inputStream: FileInputStream? = null
    private var outputStream: FileOutputStream? = null

    companion object {
        const val CHANNEL_ID = "GuardianShield_DNS"
        const val NOTIFICATION_ID = 1

        // DNS Seguros - Cloudflare Family
        private const val CLOUDFLARE_IPV4_PRIMARY = "1.1.1.3"
        private const val CLOUDFLARE_IPV4_SECONDARY = "1.0.0.3"
        private const val CLOUDFLARE_IPV6_PRIMARY = "2606:4700:4700::1113"
        private const val CLOUDFLARE_IPV6_SECONDARY = "2606:4700:4700::1003"

        // Configuración VPN
        private const val VPN_LOCAL_IPv4 = "10.0.2.2"
        private const val MTU = 1500
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        sendBroadcast(Intent(MainActivity.ACTION_VPN_STARTED))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        startDnsFilter()
        return START_STICKY
    }

    private fun startDnsFilter() {
        scope.launch {
            try {
                // Configurar interfaz VPN
                vpnInterface = Builder()
                    .setSession("GuardianOS Shield")
                    // IPv4
                    .addAddress(VPN_LOCAL_IPv4, 32)
                    .addDnsServer(CLOUDFLARE_IPV4_PRIMARY)
                    .addDnsServer(CLOUDFLARE_IPV4_SECONDARY)
                    // IPv6
                    .addAddress("fd00:1:2:3::2", 128) // Local IPv6
                    .addDnsServer(CLOUDFLARE_IPV6_PRIMARY)
                    .addDnsServer(CLOUDFLARE_IPV6_SECONDARY)
                    // Rutas
                    .addRoute("0.0.0.0", 0)     // Todo IPv4
                    .addRoute("::", 0)          // Todo IPv6
                    .setMtu(MTU)
                    .setBlocking(false)         // Non-blocking I/O
                    .establish() ?: throw IllegalStateException("No se pudo establecer la VPN")

                // Inicializar streams
                inputStream = FileInputStream(vpnInterface!!.fileDescriptor)
                outputStream = FileOutputStream(vpnInterface!!.fileDescriptor)

                // Iniciar procesamiento de paquetes
                launch { processPackets() }

                // Mantener servicio activo
                while (isActive) {
                    delay(1000)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                sendBroadcast(Intent(MainActivity.ACTION_VPN_ERROR))
                stopSelf()
            }
        }
    }

    /**
     * Procesa paquetes entrantes desde la interfaz VPN.
     */
    private suspend fun processPackets() = withContext(Dispatchers.IO) {
        val buffer = ByteBuffer.allocate(MTU)
        try {
            while (isActive && inputStream != null) {
                buffer.clear()
                val length = inputStream!!.read(buffer.array())
                if (length <= 0) {
                    delay(10)
                    continue
                }
                buffer.limit(length)
                handlePacket(buffer)
            }
        } catch (e: Exception) {
            if (isActive) e.printStackTrace()
        }
    }

    /**
     * Identifica versión IP y delega al manejador correspondiente.
     */
    private fun handlePacket(packet: ByteBuffer) {
        try {
            val version = (packet.get(0).toInt() shr 4) and 0xF
            when (version) {
                4 -> handleIPv4Packet(packet)
                6 -> handleIPv6Packet(packet)
                // Ignorar versiones desconocidas
            }
        } catch (e: Exception) {
            // Paquete malformado: ignorar silenciosamente
        }
    }

    /**
     * Maneja paquetes IPv4: TCP, UDP, ICMP.
     */
    private fun handleIPv4Packet(packet: ByteBuffer) {
        val headerLength = (packet.get(0).toInt() and 0x0F) * 4
        val protocol = packet.get(9).toInt() and 0xFF
        when (protocol) {
            OsConstants.IPPROTO_UDP -> handleUDPPacket(packet, headerLength)
            OsConstants.IPPROTO_TCP,
            OsConstants.IPPROTO_ICMP -> forwardPacket(packet)
            else -> forwardPacket(packet)
        }
    }

    /**
     * Maneja paquetes IPv6: reenvío directo (sin inspección profunda).
     */
    private fun handleIPv6Packet(packet: ByteBuffer) {
        forwardPacket(packet)
    }

    /**
     * Maneja UDP: si es DNS (puerto 53), lo reenvía a Cloudflare Family.
     */
    private fun handleUDPPacket(packet: ByteBuffer, ipHeaderLen: Int) {
        try {
            val destPort = ((packet.get(ipHeaderLen + 2).toInt() and 0xFF) shl 8) or
                           (packet.get(ipHeaderLen + 3).toInt() and 0xFF)
            if (destPort == 53) {
                forwardDNSPacket(packet)
            } else {
                forwardPacket(packet)
            }
        } catch (e: Exception) {
            forwardPacket(packet)
        }
    }

    /**
     * Reenvía una consulta DNS a Cloudflare Family con timeout seguro.
     */
    private fun forwardDNSPacket(packet: ByteBuffer) {
        scope.launch(Dispatchers.IO) {
            DatagramSocket().use { socket ->
                try {
                    val query = ByteArray(packet.remaining())
                    packet.get(query)

                    // Enviar a IPv4 primario
                    val dnsReq = DatagramPacket(
                        query, query.size,
                        InetAddress.getByName(CLOUDFLARE_IPV4_PRIMARY), 53
                    )
                    socket.soTimeout = 4000 // 4 segundos
                    socket.send(dnsReq)

                    // Recibir respuesta
                    val responseBuf = ByteArray(MTU)
                    val dnsResp = DatagramPacket(responseBuf, responseBuf.size)
                    socket.receive(dnsResp)

                    // Escribir respuesta al túnel
                    outputStream?.write(dnsResp.data, 0, dnsResp.length)
                } catch (e: Exception) {
                    // Si falla IPv4, podrías intentar IPv6 aquí (opcional)
                    // Por ahora, no hacemos nada → la app recibirá timeout
                }
            }
        }
    }

    /**
     * Reenvía cualquier paquete sin modificación (passthrough).
     */
    private fun forwardPacket(packet: ByteBuffer) {
        try {
            outputStream?.write(packet.array(), 0, packet.limit())
        } catch (e: Exception) {
            // Error de escritura: ignorar (puede ser por desconexión)
        }
    }

    // === NOTIFICACIONES ===

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Protección DNS",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Servicio de filtrado DNS local y seguro"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GuardianOS Shield")
            .setContentText("Protección DNS activa • Conexión segura")
            .setSmallIcon(android.R.drawable.ic_secure)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    // === LIMPIEZA ===

    override fun onDestroy() {
        super.onDestroy()
        sendBroadcast(Intent(MainActivity.ACTION_VPN_STOPPED))
        try {
            inputStream?.close()
            outputStream?.close()
            vpnInterface?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        scope.cancel()
    }

    override fun onRevoke() {
        super.onRevoke()
        stopSelf()
    }
}
