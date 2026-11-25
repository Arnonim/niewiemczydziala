package pl.ospzelazkow.glos.svc

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.*

object MailChecker {

    data class Cfg(val host: String, val port: Int, val user: String, val pass: String)
    data class Mail(val uid: Long, val to: String, val date: Long, val text: String, val html: String)

    private val LINK = Regex("""https?://[^\s"'<>()\]]+""", RegexOption.IGNORE_CASE)
    private val HINT = Regex("potwierd|confirm|weryfik|verify|glos|vote", RegexOption.IGNORE_CASE)

    private fun props(c: Cfg) = Properties().apply {
        put("mail.store.protocol", "imaps")
        put("mail.imaps.host", c.host)
        put("mail.imaps.port", c.port.toString())
        put("mail.imaps.ssl.enable", (c.port == 993).toString())
        put("mail.imaps.starttls.enable", (c.port == 143).toString())
        put("mail.imaps.ssl.trust", "*")
        put("mail.imaps.connectiontimeout", "12000")
        put("mail.imaps.timeout", "12000")
    }

    private fun connect(c: Cfg): Store =
        Session.getInstance(props(c)).getStore("imaps").apply { connect(c.host, c.port, c.user, c.pass) }

    suspend fun test(c: Cfg): String = withContext(Dispatchers.IO) {
        var s: Store? = null
        try {
            s = connect(c)
            val f = s.getFolder("INBOX").apply { open(Folder.READ_ONLY) }
            val n = f.messageCount
            runCatching { f.close(false) }
            "OK — INBOX: $n"
        } catch (e: Exception) {
            "Błąd: ${e.message ?: e::class.java.simpleName}"
        } finally { runCatching { s?.close() } }
    }

    suspend fun fetch(c: Cfg, lo: Long, hi: Long, sinceMs: Long): List<Mail> = withContext(Dispatchers.IO) {
        var s: Store? = null
        try {
            s = connect(c)
            val f = s.getFolder("INBOX").apply { open(Folder.READ_WRITE) }
            try {
                val uf = f as UIDFolder
                val top = if (hi <= 0) uf.uidNext - 1 else hi
                if (top < lo) return@withContext emptyList()
                val out = ArrayList<Mail>()
                for (m in uf.getMessagesByUID(lo, top)) {
                    val d = m.receivedDate?.time ?: 0L
                    if (d < sinceMs) continue
                    val (t, h) = bodies(m)
                    out += Mail(uf.getUID(m), (m.getHeader("To") ?: emptyArray()).joinToString(" "), d, t, h)
                    runCatching { m.setFlag(Flags.Flag.SEEN, true) }
                }
                out.sortedByDescending { it.uid }
            } finally { runCatching { f.close(false) } }
        } catch (e: Exception) {
            emptyList()
        } finally { runCatching { s?.close() } }
    }

    fun link(m: Mail): String? {
        for (src in listOf(m.text, m.html)) {
            val urls = LINK.findAll(src).map { it.value.trimEnd('.', ',', ';', ')', '>') }
                .filter { it.contains("osp-harnas.pl", true) || HINT.containsMatchIn(it) }.toList()
            urls.firstOrNull { HINT.containsMatchIn(it) }?.let { return it }
            urls.firstOrNull()?.let { return it }
        }
        return null
    }

    private fun bodies(m: Message): Pair<String, String> {
        val t = StringBuilder(); val h = StringBuilder()
        fun walk(p: Part) {
            val c = runCatching { p.content }.getOrNull()
            if (c is Multipart) {
                for (i in 0 until c.count) walk(c.getBodyPart(i))
                return
            }
            val s = c?.toString().orEmpty()
            if (p.contentType?.contains("text/html", true) == true) h.append(s) else t.append(s)
        }
        runCatching { walk(m) }
        return t.toString() to h.toString()
    }
}
