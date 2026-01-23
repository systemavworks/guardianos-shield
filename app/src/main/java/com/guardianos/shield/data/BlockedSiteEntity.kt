import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_sites")
data class BlockedSiteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val domain: String,
    val category: String,
    val timestamp: Long,
    val threatLevel: String
)
