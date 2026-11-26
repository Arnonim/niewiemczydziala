package pl.ospzelazkow.glos.svc

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import pl.ospzelazkow.glos.OspApp
import pl.ospzelazkow.glos.R
import pl.ospzelazkow.glos.a11y.RunState
import pl.ospzelazkow.glos.data.*
import pl.ospzelazkow.glos.util.FileLog
import pl.ospzelazkow.glos.web.*
import kotlin.random.Random

class VoteService : LifecycleService() {

    private var job: Job? = null
    private var running = false
    private var done = 0
    private var budget = 0
    private var sinceRotate = 0
    private var prefs = Settings()
    private val repo get() = OspApp.instance.repo

    private val WINDOW = 12 * 3600_000L
    private val SWEEP_EVERY = 30_000L
    private val SWEEP_BATCH = 32
    private val GAP = 4_000L

    override fun onStartCommand(i: Intent?, f: Int, s: Int): Int {
        super.onStartCommand(i, f, s)
        if (i?.action == ACTION_STOP) stopRun() else startRun()
        return START_STICKY
    }

    private fun startRun() {
        if (running) return
        running = true
        RunState.running = true
        startForeground(Foreground.nextId(), Foreground.build(this, "Przygotowanie…"))
        FileLog.w("SVC start")
        job = lifecycleScope.launch {
            try {
                loop()
            } catch (t: Throwable) {
                FileLog.w("SVC crash: ${t.message}")
                notify("Błąd: ${t.message ?: t::class.java.simpleName}")
            } finally {
                stopRun()
            }
        }
    }

    private fun stopRun() {
        running = false
        RunState.running = false
        job?.cancel()
        VoteBus.cmds.tryEmit(Cmd.Kill)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun loop() {
        prefs = Prefs.read(this)
        budget = if (prefs.batchLimit > 0) prefs.batchLimit else 150
        FileLog.w("SVC loop budget=$budget rotateEvery=${prefs.rotateEvery} mail=${prefs.mailEnabled}")
        val cfg = MailChecker.Cfg(prefs.mailHost, prefs.mailPort, prefs.mailUser, prefs.mailPass)
        val mail = prefs.mailEnabled && prefs.mailHost.isNotBlank()

        startActivity(Intent(this, VoteActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        delay(1500)

        while (running && done < budget) {
            if (mail) sweep(cfg)
            var alias = repo.claimNext()
            if (alias == null) {
                FileLog.w("SVC brak NEW/RATE — reset IN_PROGRESS")
                repo.requeueStuck()
                alias = repo.claimNext()
            }
            if (alias == null) {
                notify("Brak adresów do głosowania")
                FileLog.w("SVC brak aliasów — koniec")
                break
            }

            FileLog.w("SVC alias ${alias.email} id=${alias.id}")
            val out = runOne(alias)
            FileLog.w("SVC wynik ${alias.email} = $out")
            handle(alias, out)

            if (out == Outcome.VOTED && mail) {
                if (awaitNew(cfg, alias.email)) {
                    repo.confirmed(alias.id)
                    repo.log(alias.email, "mail", "potwierdzony")
                } else {
                    repo.bumpConfirm(alias.id)
                    repo.log(alias.email, "mail", "brak — zaległość")
                }
            }

            done++; sinceRotate++
            tick("Postęp", "$done/$budget")
            if (prefs.rotateEvery > 0 && sinceRotate >= prefs.rotateEvery && done < budget) {
                sinceRotate = 0
                tick("Zmiana IP…")
                IpRotator.rotate(this)
                delay(2000)
            }
            delay(Random.nextLong(prefs.delayMinSec * 1000L, prefs.delayMaxSec * 1000L + 1))
        }
        tick("Koniec", "$done/$budget")
        FileLog.w("SVC koniec $done/$budget")
    }

    private enum class Outcome { VOTED, RATE, DEAD, SKIPPED, FAILED }

    private suspend fun runOne(a: AliasEntity): Outcome {
        tick("Start", a.email)
        VoteBus.cmds.emit(Cmd.SetEmail(a.email, (a.id % 1000).toInt()))
        VoteBus.cmds.emit(Cmd.WipeCookies)
        delay(250)
        val id = prefs.voteId
        val url = if (id.length >= 8) "https://osp-harnas.pl/?osp_wipe=1&osp_next=$id"
        else "https://osp-harnas.pl/?osp_wipe=1"
        FileLog.w("SVC load $url")
        VoteBus.cmds.emit(Cmd.Load(url))
        delay(1500)
        VoteBus.cmds.emit(Cmd.SetEmail(a.email, (a.id % 1000).toInt()))
        delay(1500)
        VoteBus.cmds.emit(Cmd.SetEmail(a.email, (a.id % 1000).toInt()))
        return awaitOutcome(a)
    }

    private suspend fun awaitOutcome(a: AliasEntity): Outcome {
        var ses = 0; var brak = 0
        val deadline = System.currentTimeMillis() + 100_000
        while (System.currentTimeMillis() < deadline && running) {
            val e = withTimeoutOrNull(3000) { nextEvent() } ?: continue
            when (e) {
                is Evt.Done -> return Outcome.VOTED
                is Evt.Failed -> return Outcome.FAILED
                is Evt.Quiz -> { if (!e.solved) repo.log(a.email, "quiz", "nieznane", e.q) }
                is Evt.Loaded -> Unit
                is Evt.Flag -> when {
                    e.title.startsWith("OSP-OK-") -> return Outcome.VOTED
                    e.title == "OSP-429" -> return Outcome.RATE
                    e.title == "OSP-MARTWY" -> return Outcome.DEAD
                    e.title == "OSP-BRAK-MAILI" -> return Outcome.SKIPPED
                    e.title.startsWith("OSP-SESIA") -> {
                        if (ses++ >= 1) return Outcome.FAILED
                        VoteBus.cmds.emit(Cmd.Submit)
                    }
                    e.title == "OSP-CZEKA-VOTE-BRAK" -> {
                        if (brak++ >= 2) return Outcome.FAILED
                        delay(1500)
                        VoteBus.cmds.emit(Cmd.Submit)
                    }
                    e.title.startsWith("OSP-CZEKA-") -> {
                        val t = e.title.removePrefix("OSP-CZEKA-")
                        if (t.length >= 8 && t != "VOTE" && t != "VOTE-BRAK") {
                            prefs = prefs.copy(voteId = t)
                            Prefs.setVoteId(this, t)
                        }
                    }
                }
            }
        }
        return Outcome.FAILED
    }

    private suspend fun nextEvent(): Evt = suspendCancellableCoroutine { c ->
        val j = lifecycleScope.launch { VoteBus.evts.collect { if (c.isActive) c.resume(it, null) } }
        c.invokeOnCancellation { j.cancel() }
    }

    private suspend fun sweep(cfg: MailChecker.Cfg) {
        if (System.currentTimeMillis() - prefs.lastSweepAt < SWEEP_EVERY) return
        prefs = prefs.copy(lastSweepAt = System.currentTimeMillis())
        Prefs.setLastSweepAt(this, prefs.lastSweepAt)
        val hi = prefs.lastUid
        if (hi <= 0) return

        val mails = MailChecker.fetch(cfg, maxOf(1, hi - 400), hi, System.currentTimeMillis() - WINDOW)
        var n = 0
        for (m in mails) {
            if (n >= SWEEP_BATCH || !running) break
            val alias = m.to.split(',', ';', ' ').map { it.trim('<', '>', ' ', '\t') }
                .firstOrNull { it.contains('@') }?.lowercase() ?: continue
            val row = repo.byEmail(alias) ?: continue
            if (row.status != AliasStatus.VOTED) continue
            val link = MailChecker.link(m) ?: continue
            if (confirmLink(link)) { repo.confirmed(row.id); n++ }
            else repo.finish(row.id, AliasStatus.EXPIRED, "404")
        }
        mails.maxOfOrNull { it.uid }?.let {
            prefs = prefs.copy(lastUid = it)
            Prefs.setLastUid(this, it)
        }
    }

    private suspend fun awaitNew(cfg: MailChecker.Cfg, alias: String): Boolean {
        var hit = false
        repeat(2) {
            val mails = MailChecker.fetch(cfg, prefs.lastUid + 1, 0, System.currentTimeMillis() - 300_000)
            if (mails.isNotEmpty()) {
                prefs = prefs.copy(lastUid = maxOf(prefs.lastUid, mails.maxOf { it.uid }))
                Prefs.setLastUid(this, prefs.lastUid)
            }
            if (!hit) {
                mails.firstOrNull { it.to.contains(alias, true) }
                    ?.let { MailChecker.link(it) }
                    ?.let { if (confirmLink(it)) hit = true }
            }
            delay(GAP)
        }
        return hit
    }

    private suspend fun confirmLink(link: String): Boolean {
        startActivity(Intent(this, ConfirmActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(ConfirmActivity.EXTRA_URL, link))
        val r = withTimeoutOrNull(45_000) {
            MailBus.results.first { it == "CONFIRM_OK" || it == "CONFIRM_404" }
        }
        return r == "CONFIRM_OK"
    }

    private suspend fun handle(a: AliasEntity, o: Outcome) = when (o) {
        Outcome.VOTED -> { repo.finish(a.id, AliasStatus.VOTED); repo.log(a.email, "vote", "ok") }
        Outcome.SKIPPED -> { repo.finish(a.id, AliasStatus.SKIPPED, "już głosował"); repo.log(a.email, "vote", "skip") }
        Outcome.DEAD -> { repo.finish(a.id, AliasStatus.DEAD, "404"); repo.log(a.email, "vote", "dead") }
        Outcome.RATE -> { repo.finish(a.id, AliasStatus.RATE, "429"); repo.log(a.email, "vote", "429") }
        Outcome.FAILED -> {
            repo.finish(a.id, if (a.attempts >= 1) AliasStatus.FAILED else AliasStatus.NEW, "timeout")
            repo.log(a.email, "vote", "fail")
        }
    }

    private fun tick(title: String, sub: String = "") {
        getSystemService(NotificationManager::class.java).notify(FG, Foreground.build(this, title, sub))
    }

    private fun notify(t: String) {
        getSystemService(NotificationManager::class.java).notify(
            Foreground.nextId(),
            NotificationCompat.Builder(this, OspApp.CH_ALERT)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("OSP Żelazków").setContentText(t).setAutoCancel(true).build()
        )
    }

    companion object {
        const val ACTION_STOP = "pl.ospzelazkow.glos.STOP"
        private const val FG = 42
        fun start(c: Context) = c.startForegroundService(Intent(c, VoteService::class.java))
        fun stop(c: Context) = c.startService(Intent(c, VoteService::class.java).setAction(ACTION_STOP))
    }
}
