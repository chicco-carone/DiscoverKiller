package com.kieronquinn.app.discoverkiller.utils

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import com.kieronquinn.app.discoverkiller.BuildConfig

object WebDestination {

    private const val ACTIVITY_CLASS_NAME =
        "com.kieronquinn.app.discoverkiller.ui.activities.WebViewActivity"

    private val component = ComponentName(BuildConfig.APPLICATION_ID, ACTIVITY_CLASS_NAME)

    fun createIntent(value: String): Intent? {
        val uri = value.toWebUri() ?: return null
        return Intent(Intent.ACTION_VIEW, uri).apply {
            component = WebDestination.component
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
    }

    fun String.toWebUri(): Uri? {
        val value = trim()
        if(value.isEmpty()) return null
        val normalized = if("://" in value) value else "https://$value"
        val uri = runCatching { Uri.parse(normalized) }.getOrNull() ?: return null
        if(uri.scheme !in setOf("http", "https") || uri.host.isNullOrBlank()) return null
        return uri
    }

}
