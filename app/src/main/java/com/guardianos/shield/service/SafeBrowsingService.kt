// app/src/main/java/com/guardianos/shield/service/SafeBrowsingService.kt
package com.guardianos.shield.service

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Servicio para integración con Google Safe Browsing API
 * Detecta sitios de malware, phishing y contenido dañino
 */
class SafeBrowsingService(private val context: Context) {
    
    // Caché de URLs verificadas para evitar consultas repetidas
    private val urlCache = ConcurrentHashMap<String, SafeBrowsingResult>()
    private val cacheExpiry = 30 * 60 * 1000L // 30 minutos
    
    companion object {
        // IMPORTANTE: Obtén tu propia API key en https://developers.google.com/safe-browsing/v4/get-started
        private const val API_KEY = "YOUR_API_KEY_HERE"
        private const val API_ENDPOINT = "https://safebrowsing.googleapis.com/v4/threatMatches:find"
        
        // Tipos de amenazas a verificar
        private val THREAT_TYPES = listOf(
            "MALWARE",
            "SOCIAL_ENGINEERING", // Phishing
            "UNWANTED_SOFTWARE",
            "POTENTIALLY_HARMFUL_APPLICATION"
        )
        
        // Plataformas a verificar
        private val PLATFORM_TYPES = listOf(
            "ANY_PLATFORM",
            "ANDROID"
        )
        
        // Tipos de amenaza a verificar
        private val THREAT_ENTRY_TYPES = listOf("URL")
    }
    
    data class SafeBrowsingResult(
        val isThreat: Boolean,
        val threatType: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Verifica si una URL es segura usando Google Safe Browsing API
     */
    suspend fun checkUrl(url: String): SafeBrowsingResult = withContext(Dispatchers.IO) {
        // Verificar caché primero
        val cached = urlCache[url]
        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < cacheExpiry) {
            return@withContext cached
        }
        
        if (API_KEY == "YOUR_API_KEY_HERE") {
            // Si no hay API key, usar verificación local básica
            return@withContext SafeBrowsingResult(
                isThreat = isLocallyBlocked(url),
                threatType = if (isLocallyBlocked(url)) "LOCAL_BLOCK" else null
            )
        }
        
        try {
            val result = performApiCheck(url)
            urlCache[url] = result
            result
        } catch (e: Exception) {
            e.printStackTrace()
            // En caso de error, asumir seguro para no bloquear falsamente
            SafeBrowsingResult(isThreat = false)
        }
    }
    
    /**
     * Realiza la verificación real con la API
     */
    private fun performApiCheck(url: String): SafeBrowsingResult {
        val requestBody = createRequestBody(listOf(url))
        
        val connection = URL("$API_ENDPOINT?key=$API_KEY").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        
        // Enviar petición
        connection.outputStream.use { os ->
            os.write(requestBody.toByteArray())
        }
        
        // Leer respuesta
        val response = connection.inputStream.bufferedReader().use { it.readText() }
        
        return parseResponse(response)
    }
    
    /**
     * Crea el cuerpo de la petición JSON
     */
    private fun createRequestBody(urls: List<String>): String {
        val json = JSONObject().apply {
            put("client", JSONObject().apply {
                put("clientId", "guardianos-shield")
                put("clientVersion", "1.0.0")
            })
            
            put("threatInfo", JSONObject().apply {
                put("threatTypes", JSONArray(THREAT_TYPES))
                put("platformTypes", JSONArray(PLATFORM_TYPES))
                put("threatEntryTypes", JSONArray(THREAT_ENTRY_TYPES))
                put("threatEntries", JSONArray().apply {
                    urls.forEach { url ->
                        put(JSONObject().apply {
                            put("url", normalizeUrl(url))
                        })
                    }
                })
            })
        }
        
        return json.toString()
    }
    
    /**
     * Parsea la respuesta de la API
     */
    private fun parseResponse(response: String): SafeBrowsingResult {
        val json = JSONObject(response)
        
        // Si hay coincidencias, es una amenaza
        if (json.has("matches")) {
            val matches = json.getJSONArray("matches")
            if (matches.length() > 0) {
                val firstMatch = matches.getJSONObject(0)
                val threatType = firstMatch.getString("threatType")
                return SafeBrowsingResult(
                    isThreat = true,
                    threatType = threatType
                )
            }
        }
        
        // No hay amenazas
        return SafeBrowsingResult(isThreat = false)
    }
    
    /**
     * Normaliza la URL para la API
     */
    private fun normalizeUrl(url: String): String {
        var normalized = url.lowercase().trim()
        
        // Agregar protocolo si falta
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "http://$normalized"
        }
        
        // Remover fragmentos
        normalized = normalized.split("#")[0]
        
        return normalized
    }
    
    /**
     * Verificación local básica sin API
     */
    private fun isLocallyBlocked(url: String): Boolean {
        val suspiciousPatterns = listOf(
            "phishing", "hack", "crack", "keygen", "malware",
            "virus", "trojan", "ransomware", "scam"
        )
        
        return suspiciousPatterns.any { url.lowercase().contains(it) }
    }
    
    /**
     * Verifica múltiples URLs en batch
     */
    suspend fun checkUrls(urls: List<String>): Map<String, SafeBrowsingResult> = 
        withContext(Dispatchers.IO) {
            urls.associateWith { url -> checkUrl(url) }
        }
    
    /**
     * Limpia la caché de URLs verificadas
     */
    fun clearCache() {
        urlCache.clear()
    }
    
    /**
     * Obtiene estadísticas de la caché
     */
    fun getCacheStats(): CacheStats {
        val now = System.currentTimeMillis()
        val activeEntries = urlCache.values.count { 
            (now - it.timestamp) < cacheExpiry 
        }
        val threats = urlCache.values.count { it.isThreat }
        
        return CacheStats(
            totalEntries = urlCache.size,
            activeEntries = activeEntries,
            threatsDetected = threats
        )
    }
    
    data class CacheStats(
        val totalEntries: Int,
        val activeEntries: Int,
        val threatsDetected: Int
    )
}

/**
 * Servicio mejorado de análisis de DNS con Safe Browsing
 */
class EnhancedDNSAnalyzer(private val context: Context) {
    private val safeBrowsing = SafeBrowsingService(context)
    private val dnsCache = ConcurrentHashMap<String, DnsResult>()
    
    data class DnsResult(
        val domain: String,
        val isBlocked: Boolean,
        val reason: String,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    suspend fun analyzeDomain(domain: String): DnsResult {
        // Verificar caché
        val cached = dnsCache[domain]
        if (cached != null && 
            (System.currentTimeMillis() - cached.timestamp) < 10 * 60 * 1000L) {
            return cached
        }
        
        // Verificar con Safe Browsing
        val safeBrowsingResult = safeBrowsing.checkUrl(domain)
        
        val result = DnsResult(
            domain = domain,
            isBlocked = safeBrowsingResult.isThreat,
            reason = safeBrowsingResult.threatType ?: "Safe"
        )
        
        dnsCache[domain] = result
        return result
    }
    
    /**
     * Análisis profundo de URL con múltiples capas
     */
    suspend fun deepAnalysis(url: String): ThreatAnalysis {
        val domain = extractDomain(url)
        
        // Capa 1: Safe Browsing API
        val safeBrowsingResult = safeBrowsing.checkUrl(url)
        
        // Capa 2: Análisis de patrones
        val patternScore = analyzeUrlPatterns(url)
        
        // Capa 3: Reputación del dominio
        val reputationScore = getDomainReputation(domain)
        
        return ThreatAnalysis(
            url = url,
            domain = domain,
            safeBrowsingThreat = safeBrowsingResult.isThreat,
            threatType = safeBrowsingResult.threatType,
            patternScore = patternScore,
            reputationScore = reputationScore,
            overallThreatLevel = calculateThreatLevel(
                safeBrowsingResult.isThreat,
                patternScore,
                reputationScore
            )
        )
    }
    
    private fun extractDomain(url: String): String {
        return url.replace(Regex("^https?://"), "")
            .split("/")[0]
            .split(":")[0]
    }
    
    private fun analyzeUrlPatterns(url: String): Float {
        var score = 0f
        
        // Muchos subdominios
        if (url.count { it == '.' } > 4) score += 0.3f
        
        // URL muy larga
        if (url.length > 100) score += 0.2f
        
        // Muchos números
        val digitRatio = url.count { it.isDigit() }.toFloat() / url.length
        if (digitRatio > 0.3f) score += 0.2f
        
        // Uso de IP en lugar de dominio
        if (url.matches(Regex(".*\\d+\\.\\d+\\.\\d+\\.\\d+.*"))) score += 0.4f
        
        // Palabras sospechosas
        val suspiciousWords = listOf("login", "verify", "account", "update", "secure")
        if (suspiciousWords.any { url.lowercase().contains(it) }) score += 0.3f
        
        return score.coerceIn(0f, 1f)
    }
    
    private fun getDomainReputation(domain: String): Float {
        // Aquí se podría integrar con servicios de reputación
        // Por ahora retorna una puntuación basada en análisis básico
        val knownGood = listOf(
            "google.com", "youtube.com", "wikipedia.org", "github.com",
            "stackoverflow.com", "reddit.com", "twitter.com", "facebook.com"
        )
        
        return if (knownGood.any { domain.contains(it) }) 1f else 0.5f
    }
    
    private fun calculateThreatLevel(
        isSafeBrowsingThreat: Boolean,
        patternScore: Float,
        reputationScore: Float
    ): ThreatLevel {
        return when {
            isSafeBrowsingThreat -> ThreatLevel.CRITICAL
            patternScore > 0.7f -> ThreatLevel.HIGH
            patternScore > 0.4f && reputationScore < 0.5f -> ThreatLevel.MEDIUM
            patternScore > 0.2f -> ThreatLevel.LOW
            else -> ThreatLevel.SAFE
        }
    }
    
    data class ThreatAnalysis(
        val url: String,
        val domain: String,
        val safeBrowsingThreat: Boolean,
        val threatType: String?,
        val patternScore: Float,
        val reputationScore: Float,
        val overallThreatLevel: ThreatLevel
    )
    
    enum class ThreatLevel {
        SAFE, LOW, MEDIUM, HIGH, CRITICAL
    }
}
