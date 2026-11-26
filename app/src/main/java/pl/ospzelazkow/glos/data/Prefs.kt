package pl.ospzelazkow.glos.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.store by preferencesDataStore("osp_prefs")

data class Settings(
    val voteId: String = "",
    val batchLimit: Int = 150,
    val delayMinSec: Int = 25,
    val delayMaxSec: Int = 45,
    val rotateEvery: Int = 1,
    val uaSpoof: Boolean = true,
    val mailEnabled: Boolean = false,
    val mailHost: String = "s77.cyber-folks.pl",
    val mailPort: Int = 993,
    val mailUser: String = "kontakt9@ospzelazkow.pl",
    val mailPass: String = "",
    val lastUid: Long = 0L,
    val lastSweepAt: Long = 0L
)

object Prefs {
    private val VOTE_ID = stringPreferencesKey("vote_id")
    private val BATCH = intPreferencesKey("batch")
    private val D_MIN = intPreferencesKey("dmin")
    private val D_MAX = intPreferencesKey("dmax")
    private val ROT = intPreferencesKey("rot")
    private val UA = booleanPreferencesKey("ua")
    private val M_ON = booleanPreferencesKey("m_on")
    private val M_HOST = stringPreferencesKey("m_host")
    private val M_PORT = intPreferencesKey("m_port")
    private val M_USER = stringPreferencesKey("m_user")
    private val M_PASS = stringPreferencesKey("m_pass")
    private val LAST_UID = longPreferencesKey("last_uid")
    private val LAST_SWEEP = longPreferencesKey("last_sweep")

    suspend fun read(ctx: Context): Settings {
        val p = ctx.store.data.first()
        val d = Settings()
        return Settings(
            voteId = p[VOTE_ID] ?: d.voteId,
            batchLimit = p[BATCH] ?: d.batchLimit,
            delayMinSec = p[D_MIN] ?: d.delayMinSec,
            delayMaxSec = p[D_MAX] ?: d.delayMaxSec,
            rotateEvery = p[ROT] ?: d.rotateEvery,
            uaSpoof = p[UA] ?: d.uaSpoof,
            mailEnabled = p[M_ON] ?: d.mailEnabled,
            mailHost = p[M_HOST] ?: d.mailHost,
            mailPort = p[M_PORT] ?: d.mailPort,
            mailUser = p[M_USER] ?: d.mailUser,
            mailPass = p[M_PASS] ?: d.mailPass,
            lastUid = p[LAST_UID] ?: d.lastUid,
            lastSweepAt = p[LAST_SWEEP] ?: d.lastSweepAt
        )
    }

    suspend fun setVoteId(c: Context, v: String) = c.store.edit { it[VOTE_ID] = v }
    suspend fun setBatch(c: Context, v: Int) = c.store.edit { it[BATCH] = v }
    suspend fun setDelays(c: Context, a: Int, b: Int) = c.store.edit { it[D_MIN] = a; it[D_MAX] = b }
    suspend fun setRotate(c: Context, v: Int) = c.store.edit { it[ROT] = v }
    suspend fun setUaSpoof(c: Context, v: Boolean) = c.store.edit { it[UA] = v }
    suspend fun setMailEnabled(c: Context, v: Boolean) = c.store.edit { it[M_ON] = v }
    suspend fun setMailHost(c: Context, v: String) = c.store.edit { it[M_HOST] = v }
    suspend fun setMailPort(c: Context, v: Int) = c.store.edit { it[M_PORT] = v }
    suspend fun setMailUser(c: Context, v: String) = c.store.edit { it[M_USER] = v }
    suspend fun setMailPass(c: Context, v: String) = c.store.edit { it[M_PASS] = v }
    suspend fun setLastUid(c: Context, v: Long) = c.store.edit { it[LAST_UID] = v }
    suspend fun setLastSweepAt(c: Context, v: Long) = c.store.edit { it[LAST_SWEEP] = v }
}
