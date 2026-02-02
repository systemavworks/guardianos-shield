// app/src/main/java/com/guardianos/shield/data/StatisticEntity.kt
package com.guardianos.shield.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "statistics")
data class StatisticEntity(
    @PrimaryKey val dateKey: String, // YYYY-MM-DD
    val totalBlocked: Int,
    val uniqueDomains: Int,
    val malwareBlocked: Int,
    val adultContentBlocked: Int,
    val violenceBlocked: Int,
    val socialMediaBlocked: Int,
    val lastUpdated: Long
)
