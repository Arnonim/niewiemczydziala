package pl.ospzelazkow.glos.web

import android.webkit.JavascriptInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class JsBridge(private val scope: CoroutineScope) {
    @JavascriptInterface fun onFlag(t: String?) {
        if (!t.isNullOrBlank()) scope.launch { VoteBus.evts.emit(Evt.Flag(t)) }
    }
    @JavascriptInterface fun onDone(p: String?) = scope.launch { VoteBus.evts.emit(Evt.Done(p ?: "vote")) }
    @JavascriptInterface fun onQuiz(q: String?, a: String?, s: Boolean) =
        scope.launch { VoteBus.evts.emit(Evt.Quiz(q ?: "", a ?: "", s)) }
}