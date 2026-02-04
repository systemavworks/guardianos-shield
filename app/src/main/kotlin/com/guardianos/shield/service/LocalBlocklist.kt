// app/src/main/java/com/guardianos/shield/service/LocalBlocklist.kt
package com.guardianos.shield.service

import android.content.Context
import com.guardianos.shield.data.GuardianRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object LocalBlocklist {

        // Devuelve un set de dominios bloqueados precargados (para filtrado síncrono)
        fun getAllBlockedDomainsSync(context: Context): Set<String> {
            val set = mutableSetOf<String>()
            set.addAll(blockedKeywords)
            try {
                val reader = BufferedReader(
                    InputStreamReader(context.assets.open("blocklist_domains.txt"))
                )
                reader.useLines { lines ->
                    lines.forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                            set.add(trimmed.lowercase())
                        }
                    }
                }
            } catch (_: Exception) {}
            return set
        }
    // Keywords bloqueadas (contenido adulto + redes sociales)
    private val blockedKeywords = listOf(
        "porn", "sex", "xxx", "adult", "casino", "gambling",
        "betting", "poker", "slots", "erotic", "nude", "hentai",
        "onlyfans", "escort", "prostitución"
    )
    
    // Dominios completos de redes sociales (bloqueo exacto)
    private val socialMediaDomains = setOf(
        "facebook.com", "www.facebook.com", "m.facebook.com", "fb.com",
        "instagram.com", "www.instagram.com", "m.instagram.com",
        "tiktok.com", "www.tiktok.com", "m.tiktok.com",
        "twitter.com", "www.twitter.com", "m.twitter.com", "x.com",
        "discord.com", "www.discord.com",
        "snapchat.com", "www.snapchat.com",
        "reddit.com", "www.reddit.com",
        "whatsapp.com", "web.whatsapp.com",
        "telegram.org", "web.telegram.org"
    )

    suspend fun isBlocked(domain: String, repository: GuardianRepository): Boolean {
        return withContext(Dispatchers.IO) {
            val lower = domain.lowercase()
            
            // 1. Bloqueo exacto de redes sociales
            if (socialMediaDomains.any { lower == it || lower.endsWith(".$it") }) {
                return@withContext true
            }
            
            // 2. Bloqueo por keywords
            if (blockedKeywords.any { lower.contains(it) }) {
                return@withContext true
            }
            
            // 3. Keywords adicionales
            lower.contains("porno") || lower.contains("sexo")
        }
    }

    // Método para cargar listas grandes desde assets (útil para futuro)
    fun loadFromAssets(context: Context): List<String> {
        val list = mutableListOf<String>()
        try {
            val reader = BufferedReader(
                InputStreamReader(context.assets.open("blocklist_domains.txt"))
            )
            reader.useLines { lines ->
                lines.forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        list.add(trimmed.lowercase())
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    // Ejemplo de uso futuro: isInLargeBlocklist(domain, context)
    suspend fun isInLargeBlocklist(domain: String, context: Context): Boolean {
        val list = loadFromAssets(context)
        return list.any { domain.lowercase().endsWith(it) || domain.lowercase().contains(it) }
    }
}
