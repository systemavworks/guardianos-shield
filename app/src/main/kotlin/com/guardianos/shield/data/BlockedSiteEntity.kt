package com.guardianos.shield.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_sites")
data class BlockedSiteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val domain: String,
    val category: String,          // ← String (no Int)
    val timestamp: Long,
    val threatLevel: Int = 1       // ← Int (no String) - 1=LOW, 2=MEDIUM, 3=HIGH
)
