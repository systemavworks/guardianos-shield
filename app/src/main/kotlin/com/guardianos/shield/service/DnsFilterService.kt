package com.guardianos.shield.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.guardianos.shield.data.GuardianDatabase
import com.guardianos.shield.data.GuardianRepository
import com.guardianos.shield.data.UserProfileEntity
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

class DnsFilterService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    @Volatile
    private var isRunning = false

    private lateinit var repository: GuardianRepository
    private val dnsCache = mutableMapOf<Int, Pair<InetAddress, Int>>()

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    // Serializa las escrituras al descriptor TUN para evitar corrupción de paquetes
    private val outputMutex = kotlinx.coroutines.sync.Mutex()
    // Proceso tun2socks (experimental)
    private var tun2SocksProcess: Process? = null

    companion object {
        const val CHANNEL_ID = "GuardianShield_DNS"
        const val CHANNEL_ID_BLOCKS = "GuardianShield_Blocks"
        const val NOTIFICATION_ID = 1

        const val ACTION_VPN_STARTED = "com.guardianos.shield.VPN_STARTED"
        const val ACTION_VPN_STOPPED = "com.guardianos.shield.VPN_STOPPED"
        const val ACTION_VPN_ERROR = "com.guardianos.shield.VPN_ERROR"

        private const val VPN_ADDRESS = "10.111.222.3"
        private const val VPN_ROUTE = "10.111.222.0"
        private const val MTU = 1500
        private const val DNS_PORT = 53
        private const val CLOUDFLARE_FAMILY_DNS = "1.1.1.3"
        private const val CLOUDFLARE_FAMILY_DNS_V6 = "2606:4700:4700::1113"
        private const val GOOGLE_DNS = "8.8.8.8"
        private const val GOOGLE_DNS_V6 = "2001:4860:4860::8888"

        // Experimental: intentar usar tun2socks para reenviar todo el tráfico en userspace
        private const val EXPERIMENTAL_TUN2SOCKS_ENABLED = false
        private const val TUN2SOCKS_BINARY = "tun2socks"
    }

    override fun onCreate() {
        super.onCreate()
        repository = GuardianRepository(this, GuardianDatabase.getDatabase(this))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isRunning) return START_STICKY

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }

        if (setupVpn()) {
            isRunning = true
            // Iniciar loop de TUN para interceptar consultas DNS y procesarlas
            serviceScope.launch {
                runVpnLoop()
            }
            // Modo experimental: tun2socks para reenviar todo el tráfico en userspace
            if (EXPERIMENTAL_TUN2SOCKS_ENABLED) {
                startTun2Socks()
            }
            sendBroadcast(Intent(ACTION_VPN_STARTED))
            Log.d("GuardianVPN", "✅ VPN DNS iniciado correctamente (loop TUN activo)")
        } else {
            sendBroadcast(Intent(ACTION_VPN_ERROR))
            stopSelf()
        }

        return START_STICKY
    }

    private fun setupVpn(): Boolean {
        return try {
            val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = cm.activeNetwork

            val builder = Builder()
                .setSession("GuardianOS Shield")
                .setMtu(MTU)
                .addAddress(VPN_ADDRESS, 32)
                // Rutas selectivas: solo enrutar hacia servidores DNS upstream para interceptar consultas
                // Esto evita capturar todo el tráfico y mantiene Internet funcionando normalmente.
                .addDnsServer(CLOUDFLARE_FAMILY_DNS)
                .addDnsServer("1.0.0.3")
                .apply {
                    // Añadir rutas a cada servidor DNS (IPv4 /32, IPv6 /128)
                    try {
                        val dnsServers = listOf(
                            CLOUDFLARE_FAMILY_DNS, "1.0.0.3", CLOUDFLARE_FAMILY_DNS_V6,
                            GOOGLE_DNS, GOOGLE_DNS_V6
                        )
                        for (srv in dnsServers) {
                            try {
                                val addr = InetAddress.getByName(srv)
                                Log.d("GuardianVPN", "Añadiendo ruta a DNS $srv -> ${'$'}{addr.hostAddress}")
                                if (addr.address.size == 4) {
                                    addRoute(addr.hostAddress, 32)
                                } else {
                                    addRoute(addr.hostAddress, 128)
                                }
                            } catch (inner: Exception) {
                                Log.w("GuardianVPN", "No se pudo resolver/añadir ruta para $srv", inner)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("GuardianVPN", "No se pudieron añadir rutas a servidores DNS (general)", e)
                    }
                }
                // Evitar que la propia aplicación sea afectada por el VPN
                .addDisallowedApplication(packageName)
                // Mantener modo blocking para poder interceptar consultas DNS
                .setBlocking(true)

            // Logs de diagnóstico: registrar activeNetwork y LinkProperties (DNS, rutas, interfaz)
            Log.d("GuardianVPN", "🔧 setupVpn: activeNetwork=$activeNetwork")
            try {
                val lp = cm.getLinkProperties(activeNetwork)
                Log.d("GuardianVPN", "🔧 LinkProperties: iface=${'$'}{lp?.interfaceName}, dns=${'$'}{lp?.dnsServers}, routes=${'$'}{lp?.routes}")
            } catch (e: Exception) {
                Log.d("GuardianVPN", "🔧 Could not get LinkProperties", e)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && activeNetwork != null) {
                builder.setUnderlyingNetworks(arrayOf(activeNetwork))
                Log.d("GuardianVPN", "🔧 setUnderlyingNetworks applied")
            }

            vpnInterface = builder.establish()
            if (vpnInterface == null) {
                Log.e("GuardianVPN", "❌ Failed to establish VPN interface")
                return false
            }

            // Log details about the established interface for debugging
            Log.d("GuardianVPN", "✅ VPN interface establecida: vpnInterface=$vpnInterface")
            try {
                Log.d("GuardianVPN", "🔧 parcelFileDescriptor: ${'$'}{vpnInterface?.fileDescriptor}")
            } catch (e: Exception) {
                Log.d("GuardianVPN", "🔧 parcelFileDescriptor not available", e)
            }

            true
        } catch (e: Exception) {
            Log.e("GuardianVPN", "❌ Error configurando VPN", e)
            false
        }
    }

    private suspend fun runVpnLoop() = withContext(Dispatchers.IO) {
        val input = FileInputStream(vpnInterface?.fileDescriptor)
        val output = FileOutputStream(vpnInterface?.fileDescriptor)
        val buffer = ByteArray(32767)

        Log.d("GuardianVPN", "📡 Loop VPN iniciado")

        while (isRunning) {
            try {
                val length = input.read(buffer)
                if (length <= 0) continue

                val packet = buffer.copyOf(length)
                Log.d("GuardianVPN", "📦 Paquete recibido: $length bytes")

                if (isDnsPacket(packet)) {
                    Log.d("GuardianVPN", "🔍 Paquete DNS detectado")
                    launch { handleDnsPacket(packet, output) }
                } else {
                    // Agregar diagnóstico útil para paquetes que no son DNS
                    try {
                        val proto = if (packet.isNotEmpty()) packet[9].toInt() and 0xFF else -1
                        Log.d("GuardianVPN", "⚠️ Paquete NO-DNS ignorado (proto=$proto). Primeros bytes: ${hexdump(packet, 0, minOf(28, packet.size))}")
                    } catch (e: Exception) {
                        Log.d("GuardianVPN", "⚠️ Paquete NO-DNS ignorado (no se pudo diagnosticar)")
                    }
                }
            } catch (e: Exception) {
                if (isRunning) {
                    Log.e("GuardianVPN", "❌ Error en loop VPN", e)
                }
                break
            }
        }
        Log.d("GuardianVPN", "🛑 Loop VPN terminado")
    }

    private fun isDnsPacket(packet: ByteArray): Boolean {
        try {
            if (packet.size < 28) {
                Log.d("GuardianVPN", "isDnsPacket: paquete demasiado corto: ${packet.size}")
                return false
            }

            val version = (packet[0].toInt() ushr 4)

            // IPv4
            if (version == 4) {
                val protocol = packet[9].toInt() and 0xFF
                if (protocol != 17) {
                    Log.d("GuardianVPN", "isDnsPacket: IPv4 pero no UDP (protocol=$protocol)")
                    return false
                }
                val ipHeaderLen = (packet[0].toInt() and 0x0F) * 4
                if (packet.size < ipHeaderLen + 8) {
                    Log.d("GuardianVPN", "isDnsPacket: paquete no contiene cabecera UDP completa (size=${packet.size}, ipHeaderLen=$ipHeaderLen)")
                    return false
                }
                val destPort = ((packet[ipHeaderLen + 2].toInt() and 0xFF) shl 8) or 
                              (packet[ipHeaderLen + 3].toInt() and 0xFF)
                return destPort == DNS_PORT
            }

            // IPv6
            if (version == 6) {
                // IPv6 header is 40 bytes; next header at offset 6
                if (packet.size < 48) {
                    Log.d("GuardianVPN", "isDnsPacket: IPv6 pero paquete corto (size=${packet.size})")
                    return false
                }
                val nextHeader = packet[6].toInt() and 0xFF
                if (nextHeader != 17) {
                    Log.d("GuardianVPN", "isDnsPacket: IPv6 pero nextHeader != UDP (nextHeader=$nextHeader)")
                    return false
                }
                // UDP header starts at byte 40
                val destPort = ((packet[42].toInt() and 0xFF) shl 8) or (packet[43].toInt() and 0xFF)
                return destPort == DNS_PORT
            }

            Log.d("GuardianVPN", "isDnsPacket: versión IP desconocida (version=$version)")
            return false
        } catch (e: Exception) {
            Log.e("GuardianVPN", "isDnsPacket: excepción al analizar paquete", e)
            return false
        }
    }

    private suspend fun handleDnsPacket(packet: ByteArray, output: FileOutputStream) = withContext(Dispatchers.IO) {
        try {
            val isIpv6 = ((packet[0].toInt() ushr 4) == 6)

            val dnsQuery: ByteArray
            val srcAddr: ByteArray
            val dstAddr: ByteArray
            val srcPort: Int

            if (!isIpv6) {
                // IPv4
                val ipHeaderLen = (packet[0].toInt() and 0x0F) * 4
                dnsQuery = packet.copyOfRange(ipHeaderLen + 8, packet.size)
                srcAddr = packet.copyOfRange(12, 16)
                dstAddr = packet.copyOfRange(16, 20)
                srcPort = ((packet[ipHeaderLen].toInt() and 0xFF) shl 8) or 
                          (packet[ipHeaderLen + 1].toInt() and 0xFF)
            } else {
                // IPv6
                // IPv6 header fixed 40 bytes; UDP header starts at 40
                if (packet.size < 48) {
                    Log.e("GuardianVPN", "Paquete IPv6 demasiado corto para UDP")
                    return@withContext
                }
                dnsQuery = packet.copyOfRange(48, packet.size)
                srcAddr = packet.copyOfRange(8, 24)
                dstAddr = packet.copyOfRange(24, 40)
                srcPort = ((packet[40].toInt() and 0xFF) shl 8) or (packet[41].toInt() and 0xFF)
            }

            val domain = extractDomainFromDnsQuery(dnsQuery)
            if (domain == null) {
                Log.e("GuardianVPN", "❌ No se pudo extraer dominio")
                return@withContext
            }

            Log.d("GuardianVPN", "🌐 Consulta DNS: $domain")

            val transactionId = ((dnsQuery[0].toInt() and 0xFF) shl 8) or (dnsQuery[1].toInt() and 0xFF)

            dnsCache[transactionId] = Pair(
                InetAddress.getByAddress(srcAddr),
                srcPort
            )

            serviceScope.launch {
                try {
                    repository.logDnsQuery(domain)
                } catch (e: Exception) {
                    Log.e("GuardianVPN", "Error logging DNS", e)
                }
            }

            val shouldBlock = shouldBlockDomain(domain)

            val dnsResponse = if (shouldBlock) {
                serviceScope.launch {
                    try {
                        repository.addBlockedSite(domain, "BLOCKED_BY_DNS", 2)
                        showBlockNotification(domain)
                    } catch (e: Exception) {
                        Log.e("GuardianVPN", "Error adding blocked site", e)
                    }
                }
                Log.d("GuardianVPN", "🚫 Bloqueado: $domain")
                createNxdomainResponse(dnsQuery)
            } else {
                Log.d("GuardianVPN", "✅ Permitido: $domain - Reenviando...")
                forwardDnsQuery(dnsQuery) ?: createNxdomainResponse(dnsQuery)
            }

            val responsePacket = if (!isIpv6) {
                createIpUdpPacket(
                    dnsResponse,
                    InetAddress.getByName(VPN_ADDRESS),
                    DNS_PORT,
                    InetAddress.getByAddress(srcAddr),
                    srcPort
                )
            } else {
                // En IPv6, usar la dirección destino del paquete original como nuestra IP de origen
                createIpv6UdpPacket(
                    dnsResponse,
                    InetAddress.getByAddress(dstAddr),
                    DNS_PORT,
                    InetAddress.getByAddress(srcAddr),
                    srcPort
                )
            }

            Log.d("GuardianVPN", "📤 Enviando respuesta DNS: ${dnsResponse.size} bytes")
            outputMutex.withLock {
                output.write(responsePacket)
                output.flush()
            }
            dnsCache.remove(transactionId)
        } catch (e: Exception) {
            Log.e("GuardianVPN", "❌ Error handling DNS", e)
        }
    }

    private suspend fun forwardDnsQuery(query: ByteArray): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val socket = DatagramSocket()
            socket.soTimeout = 2000

            val protectedOk = protect(socket)
            if (!protectedOk) {
                Log.e("GuardianVPN", "protect(socket) returned false — outgoing socket may still be routed by VPN or blocked")
            }

            val servers = listOf(CLOUDFLARE_FAMILY_DNS, GOOGLE_DNS)
            for (srv in servers) {
                try {
                    val address = InetAddress.getByName(srv)
                    val packet = DatagramPacket(query, query.size, address, DNS_PORT)
                    socket.send(packet)

                    val receiveBuffer = ByteArray(512)
                    val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                    socket.receive(receivePacket)

                    socket.close()
                    Log.d("GuardianVPN", "Reenvío DNS exitoso a $srv, respuesta ${receivePacket.length} bytes")
                    return@withContext receiveBuffer.copyOf(receivePacket.length)
                } catch (e: Exception) {
                    Log.w("GuardianVPN", "Reenvío falló a $srv", e)
                    // intentar siguiente servidor
                }
            }

            socket.close()
            null
        } catch (e: Exception) {
            Log.e("GuardianVPN", "Error forwarding DNS (general)", e)
            null
        }
    }

    private fun createIpUdpPacket(
        payload: ByteArray,
        srcIp: InetAddress,
        srcPort: Int,
        dstIp: InetAddress,
        dstPort: Int
    ): ByteArray {
        val ipHeaderLen = 20
        val udpHeaderLen = 8
        val totalLen = ipHeaderLen + udpHeaderLen + payload.size

        val packet = ByteArray(totalLen)

        packet[0] = 0x45.toByte()
        packet[1] = 0x00.toByte()
        packet[2] = (totalLen shr 8).toByte()
        packet[3] = (totalLen and 0xFF).toByte()
        // IP ID: usar un valor no-nulo aleatorio para reducir la probabilidad de que la pila ignore el paquete
        val id = (Math.random() * 0xFFFF).toInt() and 0xFFFF
        packet[4] = (id shr 8).toByte()
        packet[5] = (id and 0xFF).toByte()
        packet[6] = 0x40.toByte()
        packet[7] = 0x00.toByte()
        packet[8] = 0x40.toByte()
        packet[9] = 0x11.toByte()
        packet[10] = 0x00.toByte()
        packet[11] = 0x00.toByte()

        val srcBytes = srcIp.address
        val dstBytes = dstIp.address
        System.arraycopy(srcBytes, 0, packet, 12, 4)
        System.arraycopy(dstBytes, 0, packet, 16, 4)

        // IP header checksum
        var checksum = 0L
        for (i in 0 until ipHeaderLen step 2) {
            checksum += ((packet[i].toInt() and 0xFF) shl 8) or (packet[i + 1].toInt() and 0xFF)
        }
        checksum = (checksum shr 16) + (checksum and 0xFFFF)
        checksum += (checksum shr 16)
        val checksumVal = (checksum.inv() and 0xFFFF).toInt()
        packet[10] = (checksumVal shr 8).toByte()
        packet[11] = (checksumVal and 0xFF).toByte()

        val udpLen = udpHeaderLen + payload.size
        packet[ipHeaderLen] = (srcPort shr 8).toByte()
        packet[ipHeaderLen + 1] = (srcPort and 0xFF).toByte()
        packet[ipHeaderLen + 2] = (dstPort shr 8).toByte()
        packet[ipHeaderLen + 3] = (dstPort and 0xFF).toByte()
        packet[ipHeaderLen + 4] = (udpLen shr 8).toByte()
        packet[ipHeaderLen + 5] = (udpLen and 0xFF).toByte()
        packet[ipHeaderLen + 6] = 0x00.toByte()
        packet[ipHeaderLen + 7] = 0x00.toByte() // checksum placeholder

        System.arraycopy(payload, 0, packet, ipHeaderLen + udpHeaderLen, payload.size)

        // Calcular checksum UDP (IPv4) y escribirlo
        try {
            val udpChecksum = computeUdpChecksumIPv4(srcBytes, dstBytes, packet.copyOfRange(ipHeaderLen, totalLen))
            packet[ipHeaderLen + 6] = (udpChecksum shr 8).toByte()
            packet[ipHeaderLen + 7] = (udpChecksum and 0xFF).toByte()
        } catch (e: Exception) {
            Log.w("GuardianVPN", "Error calculando checksum UDP IPv4", e)
        }

        return packet
    }

    private fun createIpv6UdpPacket(
        payload: ByteArray,
        srcIp: InetAddress,
        srcPort: Int,
        dstIp: InetAddress,
        dstPort: Int
    ): ByteArray {
        val ipHeaderLen = 40
        val udpHeaderLen = 8
        val udpLen = udpHeaderLen + payload.size
        val totalLen = ipHeaderLen + udpLen

        val packet = ByteArray(totalLen)

        // IPv6 header
        packet[0] = 0x60.toByte() // Version 6
        packet[1] = 0x00.toByte()
        packet[2] = 0x00.toByte()
        packet[3] = 0x00.toByte()

        // Payload length (UDP header + payload)
        packet[4] = ((udpLen shr 8) and 0xFF).toByte()
        packet[5] = (udpLen and 0xFF).toByte()

        // Next header = UDP (17)
        packet[6] = 0x11.toByte()
        // Hop limit
        packet[7] = 64.toByte()

        // Addresses
        val srcBytes = srcIp.address
        val dstBytes = dstIp.address
        System.arraycopy(srcBytes, 0, packet, 8, 16)
        System.arraycopy(dstBytes, 0, packet, 24, 16)

        // UDP header
        packet[ipHeaderLen + 0] = (srcPort shr 8).toByte()
        packet[ipHeaderLen + 1] = (srcPort and 0xFF).toByte()
        packet[ipHeaderLen + 2] = (dstPort shr 8).toByte()
        packet[ipHeaderLen + 3] = (dstPort and 0xFF).toByte()
        packet[ipHeaderLen + 4] = ((udpLen shr 8) and 0xFF).toByte()
        packet[ipHeaderLen + 5] = (udpLen and 0xFF).toByte()
        packet[ipHeaderLen + 6] = 0x00.toByte()
        packet[ipHeaderLen + 7] = 0x00.toByte() // checksum placeholder

        System.arraycopy(payload, 0, packet, ipHeaderLen + udpHeaderLen, payload.size)

        // Compute UDP checksum for IPv6
        try {
            val checksum = computeUdpChecksumIPv6(srcBytes, dstBytes, packet.copyOfRange(ipHeaderLen, totalLen))
            packet[ipHeaderLen + 6] = (checksum shr 8).toByte()
            packet[ipHeaderLen + 7] = (checksum and 0xFF).toByte()
        } catch (e: Exception) {
            Log.w("GuardianVPN", "Error calculando checksum UDP IPv6", e)
        }

        return packet
    }

    private fun computeUdpChecksumIPv6(src: ByteArray, dst: ByteArray, udpSegment: ByteArray): Int {
        val pseudo = ByteArray(16 + 16 + 4 + 1 + udpSegment.size)
        var pos = 0
        System.arraycopy(src, 0, pseudo, pos, 16); pos += 16
        System.arraycopy(dst, 0, pseudo, pos, 16); pos += 16
        // UDP length (32-bit)
        val udpLen = udpSegment.size
        pseudo[pos++] = ((udpLen shr 24) and 0xFF).toByte()
        pseudo[pos++] = ((udpLen shr 16) and 0xFF).toByte()
        pseudo[pos++] = ((udpLen shr 8) and 0xFF).toByte()
        pseudo[pos++] = (udpLen and 0xFF).toByte()
        // Next header
        pseudo[pos++] = 0x00.toByte()
        // add UDP segment
        System.arraycopy(udpSegment, 0, pseudo, pos, udpSegment.size)

        // compute checksum over pseudo
        var sum = 0L
        var i = 0
        while (i < pseudo.size - 1) {
            val word = ((pseudo[i].toInt() and 0xFF) shl 8) or (pseudo[i + 1].toInt() and 0xFF)
            sum += word.toLong()
            i += 2
        }
        if (pseudo.size % 2 == 1) {
            val last = (pseudo[pseudo.size - 1].toInt() and 0xFF) shl 8
            sum += last.toLong()
        }
        while (sum ushr 16 != 0L) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        val checksum = sum.inv().toInt() and 0xFFFF
        return checksum
    }

    private fun computeUdpChecksumIPv4(src: ByteArray, dst: ByteArray, udpSegment: ByteArray): Int {
        // pseudo header: src(4) + dst(4) + zero(1) + protocol(1) + udpLen(2) + UDP segment
        val pseudo = ByteArray(4 + 4 + 1 + 1 + 2 + udpSegment.size)
        var pos = 0
        System.arraycopy(src, 0, pseudo, pos, 4); pos += 4
        System.arraycopy(dst, 0, pseudo, pos, 4); pos += 4
        pseudo[pos++] = 0x00.toByte()
        pseudo[pos++] = 0x11.toByte() // UDP protocol
        val udpLen = udpSegment.size
        pseudo[pos++] = ((udpLen shr 8) and 0xFF).toByte()
        pseudo[pos++] = (udpLen and 0xFF).toByte()
        System.arraycopy(udpSegment, 0, pseudo, pos, udpSegment.size)

        var sum = 0L
        var i = 0
        while (i < pseudo.size - 1) {
            val word = ((pseudo[i].toInt() and 0xFF) shl 8) or (pseudo[i + 1].toInt() and 0xFF)
            sum += word.toLong()
            i += 2
        }
        if (pseudo.size % 2 == 1) {
            val last = (pseudo[pseudo.size - 1].toInt() and 0xFF) shl 8
            sum += last.toLong()
        }
        while (sum ushr 16 != 0L) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        val checksum = sum.inv().toInt() and 0xFFFF
        return if (checksum == 0) 0xFFFF else checksum
    }

    private suspend fun shouldBlockDomain(domain: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val lowerDomain = domain.lowercase().trim()

                if (LocalBlocklist.isBlocked(lowerDomain, repository)) return@withContext true
                if (repository.isInBlacklist(lowerDomain)) return@withContext true
                if (repository.isInWhitelist(lowerDomain)) return@withContext false

                val profile: UserProfileEntity? = repository.getActiveProfile()
                if (profile == null) return@withContext false

                if (profile.scheduleEnabled && !isWithinAllowedTime(profile)) {
                    if (profile.blockSocialMedia == true && isSocialMediaDomain(lowerDomain)) return@withContext true
                    if (profile.blockAdultContent == true && isAdultDomain(lowerDomain)) return@withContext true
                    if (profile.blockGambling == true && isGamblingDomain(lowerDomain)) return@withContext true
                    if (profile.blockGaming == true && isGamingDomain(lowerDomain)) return@withContext true
                    if (profile.blockStreaming == true && isStreamingDomain(lowerDomain)) return@withContext true
                }

                if (profile.restrictionLevel == "HIGH") {
                    if (isSocialMediaDomain(lowerDomain) ||
                        isAdultDomain(lowerDomain) ||
                        isGamblingDomain(lowerDomain)) {
                        return@withContext true
                    }
                }

                if (profile.blockAdultContent == true && isAdultDomain(lowerDomain)) {
                    return@withContext true
                }

                if (profile.blockGambling == true && isGamblingDomain(lowerDomain)) {
                    return@withContext true
                }

                false
            } catch (e: Exception) {
                Log.e("GuardianVPN", "Error checking block", e)
                false
            }
        }
    }

    private fun isWithinAllowedTime(profile: UserProfileEntity): Boolean {
        val calendar = java.util.Calendar.getInstance()
        val currentMinutes = calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 + 
                            calendar.get(java.util.Calendar.MINUTE)
        
        val startMinutes = profile.startTimeMinutes ?: 0
        val endMinutes = profile.endTimeMinutes ?: 1440
        
        return currentMinutes in startMinutes..endMinutes
    }

    private fun isSocialMediaDomain(domain: String): Boolean =
        domain.contains("tiktok") || domain.contains("instagram") ||
                domain.contains("facebook") || domain.contains("twitter") ||
                domain.contains("snapchat") || domain.contains("youtube") ||
                domain.contains("twitch") || domain.contains("discord")

    private fun isAdultDomain(domain: String): Boolean =
        domain.contains("porn") || domain.contains("sex") || domain.contains("xxx") ||
                domain.contains("adult") || domain.contains("erotic") || domain.contains("hentai") ||
                domain.contains("onlyfans") || domain.contains("xvideos") || domain.contains("xhamster") ||
                domain.contains("pornhub")

    private fun isGamblingDomain(domain: String): Boolean =
        domain.contains("casino") || domain.contains("gambling") ||
                domain.contains("betting") || domain.contains("poker") ||
                domain.contains("slots") || domain.contains("apuestas") ||
                domain.contains("bet365") || domain.contains("bwin")

    private fun isGamingDomain(domain: String): Boolean =
        domain.contains("game") || domain.contains("gaming") ||
                domain.contains("steam") || domain.contains("epicgames") ||
                domain.contains("roblox") || domain.contains("fortnite") ||
                domain.contains("minecraft")

    private fun isStreamingDomain(domain: String): Boolean =
        domain.contains("netflix") || domain.contains("disneyplus") ||
                domain.contains("hbomax") || domain.contains("primevideo") ||
                domain.contains("twitch.tv") || domain.contains("hulu")

    private fun extractDomainFromDnsQuery(packet: ByteArray): String? {
        try {
            var pos = 12
            if (pos >= packet.size) return null

            val domain = StringBuilder()
            var jumped = false
            var jumps = 0

            while (pos < packet.size && jumps < 5) {
                val len = packet[pos].toInt() and 0xFF

                if (len == 0) break

                if ((len and 0xC0) == 0xC0) {
                    if (pos + 1 >= packet.size) break
                    val pointer = ((len and 0x3F) shl 8) or (packet[pos + 1].toInt() and 0xFF)
                    if (!jumped) pos += 2
                    pos = pointer
                    jumped = true
                    jumps++
                    continue
                }

                pos++
                if (pos + len > packet.size) return null

                if (domain.isNotEmpty()) domain.append('.')
                for (i in 0 until len) {
                    if (pos >= packet.size) return null
                    val char = packet[pos].toInt() and 0xFF
                    domain.append(char.toChar())
                    pos++
                }

                if (jumped) break
            }

            return domain.toString().trim().lowercase()
        } catch (e: Exception) {
            Log.e("GuardianVPN", "Error extrayendo dominio", e)
            return null
        }
    }

    private fun createNxdomainResponse(query: ByteArray): ByteArray {
        val response = query.copyOf()
        response[2] = (response[2].toInt() or 0x80).toByte()
        response[3] = 0x83.toByte()
        response[6] = 0
        response[7] = 0
        response[8] = 0
        response[9] = 0
        response[10] = 0
        response[11] = 0
        return response
    }

    // Helper para debug: hex dump de un rango de bytes (pocas longitudes)
    private fun hexdump(bytes: ByteArray, start: Int = 0, length: Int = 32): String {
        val end = minOf(bytes.size, start + length)
        val sb = StringBuilder()
        for (i in start until end) {
            sb.append(String.format("%02X ", bytes[i]))
        }
        return sb.toString().trim()
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Guardian Shield DNS",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
            
            val channelBlocks = NotificationChannel(
                CHANNEL_ID_BLOCKS,
                "Sitios Bloqueados",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channelBlocks)
        }

        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GuardianOS Shield Activo")
            .setContentText("Filtrando DNS")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun showBlockNotification(domain: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val notificationManager = getSystemService(NotificationManager::class.java)
        val notificationId = System.currentTimeMillis().toInt()

        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID_BLOCKS)
            .setContentTitle("🚫 Sitio Bloqueado")
            .setContentText(domain)
            .setSmallIcon(android.R.drawable.ic_delete)
            .setAutoCancel(true)
            .setTimeoutAfter(5000)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    // Experimental: intentar arrancar tun2socks binario si está disponible en el sistema.
    private fun startTun2Socks() {
        if (!EXPERIMENTAL_TUN2SOCKS_ENABLED) return
        try {
            Log.d("GuardianVPN", "🔧 Intentando iniciar tun2socks (experimental)")
            val proc = Runtime.getRuntime().exec(TUN2SOCKS_BINARY)
            tun2SocksProcess = proc
            // Leer salida en background para depuración
            serviceScope.launch {
                try {
                    proc.inputStream.bufferedReader().forEachLine {
                        Log.d("GuardianVPN", "tun2socks: $it")
                    }
                } catch (e: Exception) {
                    Log.w("GuardianVPN", "Error leyendo la salida de tun2socks", e)
                }
            }
            Log.d("GuardianVPN", "✅ tun2socks iniciado (experimental)")
        } catch (e: Exception) {
            Log.w("GuardianVPN", "tun2socks no está disponible o falló al iniciar (experimental)", e)
        }
    }

    private fun stopTun2Socks() {
        try {
            tun2SocksProcess?.destroy()
            tun2SocksProcess = null
            Log.d("GuardianVPN", "🔧 tun2socks detenido")
        } catch (e: Exception) {
            Log.w("GuardianVPN", "Error deteniendo tun2socks", e)
        }
    }

    private fun cleanup() {
        if (!isRunning) return
        isRunning = false
        try {
            stopTun2Socks()
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Log.e("GuardianVPN", "Error cerrando VPN", e)
        }
        sendBroadcast(Intent(ACTION_VPN_STOPPED))
        Log.d("GuardianVPN", "VPN detenida")
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
}
