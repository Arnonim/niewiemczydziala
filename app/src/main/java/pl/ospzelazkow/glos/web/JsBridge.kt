package pl.ospzelazkow.glos.web

import android.webkit.JavascriptInterface
import pl.ospzelazkow.glos.util.FileLog

class JsBridge(
    private val emailProvider: () -> String,
    private val flagCb: (String) -> Unit,
    private val doneCb: (String) -> Unit,
    private val quizCb: (String) -> Unit
) {

    @JavascriptInterface
    fun onFlag(f: String) {
        FileLog.i("JS", "flag=$f")
        flagCb(f)
    }

    @JavascriptInterface
    fun onDone(r: String) {
        FileLog.i("JS", "done=$r")
        doneCb(r)
    }

    @JavascriptInterface
    fun onQuiz(q: String) {
        FileLog.i("JS", "quiz=$q")
        quizCb(q)
    }

    @JavascriptInterface
    fun email(): String = emailProvider()

    @JavascriptInterface
    fun apk(): Boolean = true

    @JavascriptInterface
    fun index(): Int = 0
}
