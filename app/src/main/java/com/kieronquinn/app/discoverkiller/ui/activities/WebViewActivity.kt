package com.kieronquinn.app.discoverkiller.ui.activities

import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.kieronquinn.app.discoverkiller.ui.views.WebViewContainer
import com.kieronquinn.app.discoverkiller.utils.WebDestination.toWebUri

class WebViewActivity: AppCompatActivity() {

    private var webViewContainer: WebViewContainer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent.data?.toString()?.toWebUri() ?: run {
            finish()
            return
        }
        val container = WebViewContainer(this)
        webViewContainer = container
        setContentView(container)
        if(savedInstanceState == null || !container.restoreState(savedInstanceState)){
            container.loadUrl(uri.toString())
        }
        onBackPressedDispatcher.addCallback(this) {
            if(container.goBack()) return@addCallback
            else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webViewContainer?.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        webViewContainer?.destroy()
        webViewContainer = null
        super.onDestroy()
    }

}
