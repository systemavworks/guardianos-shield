// app/src/main/java/com/guardianos/shield/data/UserProfileEntity.kt
package com.guardianos.shield.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val age: Int? = null,
    val parentalPin: String? = null,
    val restrictionLevel: String, // "STRICT", "MODERATE", "MILD"
    val allowSocialMedia: Boolean = true,
    val allowedHoursStart: Int = 8,
    val allowedHoursEnd: Int = 21,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
