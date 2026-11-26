package pl.ospzelazkow.glos.web

import android.webkit.JavascriptInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import pl.ospzelazkow.glos.util.FileLog

class JsBridge(private val scope: CoroutineScope) {

    @JavascriptInterface
    fun onFlag(t: String?) {
        if (t.isNullOrBlank()) return
        FileLog.w("JS flag=$t")
        scope.launch { VoteBus.evts.emit(Evt.Flag(t)) }
    }

    @JavascriptInterface
    fun onDone(p: String?) {
        FileLog.w("JS done=${p ?: "vote"}")
        scope.launch { VoteBus.evts.emit(Evt.Done(p ?: "vote")) }
    }

    @JavascriptInterface
    fun onQuiz(q: String?, a: String?, s: Boolean) {
        FileLog.w("JS quiz=$q -> $a ($s)")
        scope.launch { VoteBus.evts.emit(Evt.Quiz(q ?: "", a ?: "", s)) }
    }
}
