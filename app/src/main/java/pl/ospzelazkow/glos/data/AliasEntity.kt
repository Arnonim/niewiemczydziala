package pl.ospzelazkow.glos.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "alias", indices = [Index(value = ["email"], unique = true)])
data class AliasEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val status: Int = AliasStatus.NEW,
    val attempts: Int = 0,
    val confirmTries: Int = 0,
    val lastAttemptAt: Long = 0L,
    val lastNote: String? = null
)