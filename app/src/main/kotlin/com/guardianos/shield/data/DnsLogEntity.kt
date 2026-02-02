// app/src/main/java/com/guardianos/shield/data/DnsLogEntity.kt
package com.guardianos.shield.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dns_logs")
data class DnsLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val domain: String,
    val timestamp: Long = System.currentTimeMillis(),
    val blocked: Boolean
)
