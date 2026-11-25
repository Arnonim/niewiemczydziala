package pl.ospzelazkow.glos.web

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.webkit.*
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object VoteWebView {

    private const val UA =
        "Mozilla/5.0 (Linux; Android 13; Pixel 6) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    fun prelude(email: String, index: Int) = """
        window.__OSP_APK = true;
        window.__OSP_FORCE_EMAIL = ${q(email)};
        window.__OSP_FORCE_INDEX = $index;
        try{ localStorage.setItem('current_email_index', '$index'); }catch(e){}
    """.trimIndent()

    fun applyEmail(email: String, index: Int) = prelude(email, index) + """
        ;
        (function(){
          var i=document.getElementById('vote_email');
          if(!i) return;
          i.value=${q(email)};
          i.dispatchEvent(new Event('input',{bubbles:true}));
          i.dispatchEvent(new Event('change',{bubbles:true}));
        })();
    """.trimIndent()

    fun submit() = """
        (function(){
          var f=document.getElementById('vote-form')||document.querySelector('form[action*="vote"]');
          if(!f) return;
          var b=f.querySelector('button[type=submit],input[type=submit]');
          if(b){ b.click(); return; }
          if(f.requestSubmit){ f.requestSubmit(); return; }
          f.submit();
        })();
    """.trimIndent()

    private fun q(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    @SuppressLint("SetJavaScriptEnabled")
    fun build(ctx: Context, scope: CoroutineScope, ua: Boolean, injector: String): WebView {
        val wv = WebView(ctx)
        with(wv.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            if (ua) userAgentString = UA
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(wv, true)
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(wv, injector, setOf("https://osp-harnas.pl"))
        }
        wv.addJavascriptInterface(JsBridge(scope), "OspAndroid")
        wv.webViewClient = object : WebViewClient() {
            override fun onPageStarted(v: WebView, url: String, fav: Bitmap?) {
                if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT))
                    v.evaluateJavascript(injector, null)
            }
            override fun onPageFinished(v: WebView, url: String) {
                CookieManager.getInstance().flush()
                scope.launch { VoteBus.evts.emit(Evt.Loaded(url)) }
            }
            override fun onReceivedError(v: WebView, r: WebResourceRequest, e: WebResourceError) {
                if (r.isForMainFrame) scope.launch { VoteBus.evts.emit(Evt.Failed(e.errorCode, e.description.toString())) }
            }
        }
        wv.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(m: ConsoleMessage) = true
        }
        return wv
    }
}