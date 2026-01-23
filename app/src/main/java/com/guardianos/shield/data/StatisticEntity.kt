import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "statistics")
data class StatisticEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: String,
    val totalBlocked: Int = 0,
    val malwareBlocked: Int = 0,
    val adultContentBlocked: Int = 0,
    val violenceBlocked: Int = 0,
    val socialMediaBlocked: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
