package pl.ospzelazkow.glos.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OspDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rows: List<AliasEntity>): List<Long>

    @Query("SELECT COUNT(*) FROM alias") fun total(): Flow<Int>
    @Query("SELECT COUNT(*) FROM alias WHERE status = :s") fun countBy(s: Int): Flow<Int>

    @Query("SELECT * FROM alias WHERE status = :s ORDER BY id LIMIT :n")
    suspend fun take(s: Int, n: Int): List<AliasEntity>

    @Query("SELECT * FROM alias WHERE email = :email LIMIT 1") suspend fun byEmail(email: String): AliasEntity?

    @Query("UPDATE alias SET status = :s, lastAttemptAt = :ts, lastNote = :note, attempts = attempts + 1 WHERE id = :id")
    suspend fun mark(id: Long, s: Int, ts: Long = System.currentTimeMillis(), note: String? = null)

    @Query("UPDATE alias SET status = 0, attempts = 0, lastNote = NULL WHERE status IN (3, 6)")
    suspend fun requeueFailed()

    @Query("UPDATE alias SET status = 0, lastNote = NULL WHERE status = 1")
    suspend fun requeueStuck()

    @Query("UPDATE alias SET status = 0, attempts = 0, confirmTries = 0, lastNote = NULL")
    suspend fun resetAll()

    @Query("SELECT email FROM alias WHERE status IN (2, 7) ORDER BY lastAttemptAt") fun votedEmails(): Flow<List<String>>

    @Query("SELECT * FROM alias WHERE status = 2 AND confirmTries < 4 ORDER BY lastAttemptAt ASC LIMIT :n")
    suspend fun toConfirm(n: Int): List<AliasEntity>

    @Query("UPDATE alias SET confirmTries = confirmTries + 1 WHERE id = :id") suspend fun bumpConfirm(id: Long)

    @Query("SELECT COUNT(*) FROM alias WHERE status = 8") fun expired(): Flow<Int>

    @Insert suspend fun log(e: VoteLogEntity)
    @Query("SELECT * FROM vote_log ORDER BY id DESC LIMIT :n") fun recentLogs(n: Int): Flow<List<VoteLogEntity>>
    @Query("DELETE FROM vote_log") suspend fun clearLogs()
}
