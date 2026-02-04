package com.guardianos.shield.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensitive_apps")
data class SensitiveAppEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val label: String = "",
    val addedAt: Long = System.currentTimeMillis()
)
