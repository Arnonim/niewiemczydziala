package pl.ospzelazkow.glos.data

import kotlinx.coroutines.flow.Flow

class OspRepository(private val dao: OspDao) {
    val total: Flow<Int> = dao.total()
    val voted: Flow<Int> = dao.countBy(AliasStatus.CONFIRMED)
    val failed: Flow<Int> = dao.countBy(AliasStatus.FAILED)
    val rate: Flow<Int> = dao.countBy(AliasStatus.RATE)
    val remaining: Flow<Int> = dao.countBy(AliasStatus.NEW)
    val expired: Flow<Int> = dao.expired()
    val logs: Flow<List<VoteLogEntity>> = dao.recentLogs(300)
    val votedEmails: Flow<List<String>> = dao.votedEmails()

    suspend fun claimNext(): AliasEntity? {
        val row = dao.take(AliasStatus.NEW, 1).firstOrNull()
            ?: dao.take(AliasStatus.RATE, 1).firstOrNull() ?: return null
        dao.mark(row.id, AliasStatus.IN_PROGRESS)
        return row
    }

    suspend fun finish(id: Long, s: Int, note: String? = null) = dao.mark(id, s, note = note)
    suspend fun byEmail(e: String) = dao.byEmail(e)
    suspend fun toConfirm(n: Int) = dao.toConfirm(n)
    suspend fun confirmed(id: Long) = dao.mark(id, AliasStatus.CONFIRMED)
    suspend fun bumpConfirm(id: Long) = dao.bumpConfirm(id)
    suspend fun requeueFailed() = dao.requeueFailed()
    suspend fun resetAll() = dao.resetAll()
    suspend fun import(rows: List<AliasEntity>) = dao.insertAll(rows)
    suspend fun log(e: String, p: String, r: String, n: String? = null) =
        dao.log(VoteLogEntity(email = e, phase = p, result = r, note = n))
}