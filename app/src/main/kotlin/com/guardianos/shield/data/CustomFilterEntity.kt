package com.guardianos.shield.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_filters")
data class CustomFilterEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val domain: String,            // ← Campo usado en la UI
    val pattern: String = "",      // ← Campo crítico para SafeBrowser (derivado de domain)
    val isEnabled: Boolean = true, // ← Campo crítico para SafeBrowser
    val isActive: Boolean = true,  // ← Usado en GuardianRepository
    val addedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = addedAt
)
