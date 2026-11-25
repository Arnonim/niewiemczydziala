package pl.ospzelazkow.glos.web

import android.os.Bundle
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pl.ospzelazkow.glos.data.Prefs

class VoteActivity : ComponentActivity() {

    private var web: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        lifecycleScope.launch {
            val p = Prefs.read(this@VoteActivity)
            val js = assets.open("harnas-inject.js").bufferedReader().use { it.readText() }
            val wv = VoteWebView.build(this@VoteActivity, lifecycleScope, p.uaSpoof, js)
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