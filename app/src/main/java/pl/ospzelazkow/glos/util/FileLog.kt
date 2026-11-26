
package pl.ospzelazkow.glos.util

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileLog {
    private var dir: File? = null
    private val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun init(ctx: Context) {
        dir = File(ctx.getExternalFilesDir(null), "logs").apply { mkdirs() }
    }

    fun w(msg: String) {
        val d = dir ?: return
        runCatching {
            val f = File(d, "app.log")
            if (f.length() > 1_500_000) {
                File(d, "app.1.log").delete()
                f.renameTo(File(d, "app.1.log"))
            }
            f.appendText("${fmt.format(Date())} [${Thread.currentThread().name}] $msg\n")
        }
    }

    fun file(): File? = dir?.let { File(it, "app.log") }

    fun clear() {
        file()?.delete()
        dir?.let { File(it, "app.1.log").delete() }
    }
}
