import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_filters")
data class CustomFilterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val domain: String,
    val type: String, // "blacklist" o "whitelist"
    val addedAt: Long
)
