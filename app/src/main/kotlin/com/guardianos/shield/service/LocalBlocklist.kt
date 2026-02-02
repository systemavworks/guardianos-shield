// app/src/main/java/com/guardianos/shield/service/LocalBlocklist.kt
package com.guardianos.shield.service

import android.content.Context
import com.guardianos.shield.data.GuardianRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object LocalBlocklist {
    private val blockedKeywords = listOf(
        "porn", "sex", "xxx", "adult", "casino", "gambling",
        "betting", "poker", "slots", "erotic", "nude", "hentai",
        "onlyfans", "escort", "prostitución"
    )

    suspend fun isBlocked(domain: String, repository: GuardianRepository): Boolean {
        return withContext(Dispatchers.IO) {
            val lower = domain.lowercase()
            blockedKeywords.any { lower.contains(it) } ||
                // Puedes añadir más lógica aquí (ej: regex si quieres)
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
