package pl.ospzelazkow.glos.web

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ConfirmActivity : ComponentActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra(EXTRA_URL) ?: run { finish(); return }

        val wv = WebView(this)
        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.settings.userAgentString = wv.settings.userAgentString.replace("; wv", "")
        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(v: WebView, u: String) {
                lifecycleScope.launch {
                    delay(1500)
                    val bad = u.contains("404") || u.contains("nie-znaleziono") || u.contains("blad")
                    MailBus.results.emit(if (bad) "CONFIRM_404" else "CONFIRM_OK")
                    delay(400)
                    finish()
                }
            }
        }
        setContentView(wv)
        wv.loadUrl(url)
    }

    companion object { const val EXTRA_URL = "url" }
}