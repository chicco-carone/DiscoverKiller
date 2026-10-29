package com.kieronquinn.app.discoverkiller.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding

@SuppressLint("ViewConstructor", "SetJavaScriptEnabled")
class WebViewContainer(context: Context, insetForSystemBars: Boolean = false): FrameLayout(context) {

    private val progress = ProgressBar(
        context, null, android.R.attr.progressBarStyleHorizontal
    ).apply {
        max = 100
    }

    private val webView = WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        CookieManager.getInstance().setAcceptCookie(true)
        webViewClient = object: WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                CookieManager.getInstance().flush()
            }

            override fun shouldOverrideUrlLoading(
                view: WebView, request: WebResourceRequest
            ): Boolean {
                if(request.url.scheme in setOf("http", "https")) return false
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, request.url)) }
                return true
            }
        }
        webChromeClient = object: WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                this@WebViewContainer.progress.progress = newProgress
                this@WebViewContainer.progress.isVisible = newProgress < 100
            }
        }
    }

    var currentUrl: String? = null
        private set

    init {
        setBackgroundColor(android.graphics.Color.BLACK)
        if(insetForSystemBars){
            ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.updatePadding(top = bars.top, bottom = bars.bottom)
                insets
            }
        }
        addView(webView, LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
        addView(progress, LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))
    }

    fun loadUrl(url: String) {
        if(currentUrl == url) return
        currentUrl = url
        webView.loadUrl(url)
    }

    fun restoreState(state: Bundle): Boolean {
        val restored = webView.restoreState(state) != null
        if(restored) currentUrl = webView.url
        return restored
    }

    fun saveState(state: Bundle) {
        webView.saveState(state)
    }

    fun goBack(): Boolean {
        if(!webView.canGoBack()) return false
        webView.goBack()
        return true
    }

    fun onResume() {
        webView.onResume()
        webView.resumeTimers()
    }

    fun onPause() {
        CookieManager.getInstance().flush()
        webView.onPause()
        webView.pauseTimers()
    }

    fun destroy() {
        CookieManager.getInstance().flush()
        webView.stopLoading()
        webView.destroy()
        removeAllViews()
    }

}
