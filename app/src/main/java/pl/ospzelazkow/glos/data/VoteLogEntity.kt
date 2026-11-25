package pl.ospzelazkow.glos.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vote_log")
data class VoteLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long = System.currentTimeMillis(),
    val email: String,
    val phase: String,
    val result: String,
    val note: String? = null
)