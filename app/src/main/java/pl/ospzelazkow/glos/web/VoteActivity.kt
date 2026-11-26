package pl.ospzelazkow.glos.web

import android.os.Bundle
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pl.ospzelazkow.glos.data.Prefs
import pl.ospzelazkow.glos.data.Settings
import pl.ospzelazkow.glos.util.FileLog

class VoteActivity : ComponentActivity() {

    private var web: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val info = TextView(this).apply {
            text = "Ładowanie…"
            setPadding(48, 200, 48, 48)
            textSize = 16f
        }
        setContentView(info)

        lifecycleScope.launch {
            val p = runCatching { Prefs.read(this@VoteActivity) }.getOrDefault(Settings())
            val js = runCatching {
                assets.open("harnas-inject.js").bufferedReader().use { it.readText() }
            }.getOrElse { e ->
                FileLog.w("VoteActivity brak injectora: ${e.message}")
                info.text = "Brak pliku harnas-inject.js: ${e.message}"
                return@launch
            }
            val wv = runCatching { VoteWebView.build(this@VoteActivity, lifecycleScope, p.uaSpoof, js) }
                .getOrElse { e ->
                    FileLog.w("VoteActivity WebView: ${e.message}")
                    info.text = "Błąd WebView: ${e.message}"
                    return@launch
                }
            web = wv
            setContentView(wv)

            launch {
                VoteBus.cmds.collect { c ->
                    val v = web ?: return@collect
                    when (c) {
                        is Cmd.Load -> v.loadUrl(c.url)
                        Cmd.WipeCookies -> CookieManager.getInstance().removeAllCookies(null)
                        is Cmd.SetEmail -> v.evaluateJavascript(VoteWebView.applyEmail(c.email, c.index), null)
                        Cmd.Submit -> v.evaluateJavascript(VoteWebView.submit(), null)
                        Cmd.Kill -> finish()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        web?.let { it.loadUrl("about:blank"); it.destroy() }
        web = null
        super.onDestroy()
    }
}
