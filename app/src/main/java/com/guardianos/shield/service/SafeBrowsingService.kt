// app/src/main/java/com/guardianos/shield/service/SafeBrowsingService.kt
package com.guardianos.shield.service

import android.content.Context
import com.google.gson.annotations.SerializedName
import com.guardianos.shield.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class SafeBrowsingService(private val context: Context) {
    
    private val api: SafeBrowsingApi by lazy { createApi() }
    private val urlCache = ConcurrentHashMap<String, CachedResult>()
    private val cacheExpiry = 30 * 60 * 1000L // 30 minutos
    
    companion object {
        private const val BASE_URL = "https://safebrowsing.googleapis.com/"
        private val API_KEY = BuildConfig.SAFE_BROWSING_API_KEY
        
        private val THREAT_TYPES = listOf(
            "MALWARE",
            "SOCIAL_ENGINEERING",
            "UNWANTED_SOFTWARE",
            "POTENTIALLY_HARMFUL_APPLICATION"
        )
        
        private val PLATFORM_TYPES = listOf("ANY_PLATFORM", "ANDROID")
        private val THREAT_ENTRY_TYPES = listOf("URL")
    }
    
    data class CachedResult(
        val result: SafeBrowsingResult,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(expiryTime: Long): Boolean =
            (System.currentTimeMillis() - timestamp) >= expiryTime
    }
    
    data class SafeBrowsingResult(
        val isThreat: Boolean,
        val threatType: String? = null,
        val platformType: String? = null,
        val threatEntryType: String? = null,
        val cacheHit: Boolean = false
    )
    
    interface SafeBrowsingApi {
        @POST("v4/threatMatches:find")
        suspend fun checkUrls(
            @Query("key") apiKey: String,
            @Body request: ThreatMatchRequest
        ): ThreatMatchResponse
    }
    
    data class ThreatMatchRequest(
        val client: ClientInfo,
        val threatInfo: ThreatInfo
    )
    
    data class ClientInfo(
        val clientId: String = "guardianos-shield",
        val clientVersion: String = "1.0.0"
    )
    
    data class ThreatInfo(
        val threatTypes: List<String>,
        val platformTypes: List<String>,
        val threatEntryTypes: List<String>,
        val threatEntries: List<ThreatEntry>
    )
    
    data class ThreatEntry(
        val url: String
    )
    
    data class ThreatMatchResponse(
        val matches: List<ThreatMatch>?
    )
    
    data class ThreatMatch(
        val threatType: String,
        val platformType: String,
        val threatEntryType: String,
        val threat: ThreatEntry,
        @SerializedName("cacheDuration") val cacheDuration: String?
    )
    
    suspend fun checkUrl(url: String): SafeBrowsingResult = withContext(Dispatchers.IO) {
        val cached = urlCache[url]
        if (cached != null && !cached.isExpired(cacheExpiry)) {
            return@withContext cached.result.copy(cacheHit = true)
        }
        
        // ✅ Solo verificar si la clave está presente y no es vacía
        if (API_KEY.isEmpty()) {
            return@withContext performLocalCheck(url)
        }
        
        try {
            val result = performApiCheck(url)
            urlCache[url] = CachedResult(result)
            if (urlCache.size > 1000) cleanExpiredCache()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            performLocalCheck(url)
        }
    }
    
    private suspend fun performApiCheck(url: String): SafeBrowsingResult {
        val request = ThreatMatchRequest(
            client = ClientInfo(),
            threatInfo = ThreatInfo(
                threatTypes = THREAT_TYPES,
                platformTypes = PLATFORM_TYPES,
                threatEntryTypes = THREAT_ENTRY_TYPES,
                threatEntries = listOf(ThreatEntry(normalizeUrl(url)))
            )
        )
        
        val response = api.checkUrls(API_KEY, request)
        
        return if (response.matches.isNullOrEmpty()) {
            SafeBrowsingResult(isThreat = false)
        } else {
            val match = response.matches.first()
            SafeBrowsingResult(
                isThreat = true,
                threatType = match.threatType,
                platformType = match.platformType,
                threatEntryType = match.threatEntryType
            )
        }
    }
    
    private fun createApi(): SafeBrowsingApi {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
        
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SafeBrowsingApi::class.java)
    }
    
    private fun normalizeUrl(url: String): String {
        var normalized = url.lowercase().trim()
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "http://$normalized"
        }
        normalized = normalized.split("#")[0]
        return normalized
    }
    
    private fun performLocalCheck(url: String): SafeBrowsingResult {
        val isBlocked = isLocallyBlocked(url)
        return SafeBrowsingResult(
            isThreat = isBlocked,
            threatType = if (isBlocked) "LOCAL_BLOCK" else null
        )
    }
    
    private fun isLocallyBlocked(url: String): Boolean {
        val suspiciousPatterns = listOf(
            "phishing", "hack", "crack", "keygen", "malware",
            "virus", "trojan", "ransomware", "scam", "warez",
            "torrent", "pirate", "xxx", "porn", "adult", "violence"
        )
        return suspiciousPatterns.any { url.lowercase().contains(it) }
    }
    
    private fun cleanExpiredCache() {
        val expired = urlCache.entries.filter { 
            it.value.isExpired(cacheExpiry) 
        }.map { it.key }
        expired.forEach { urlCache.remove(it) }
    }
}
