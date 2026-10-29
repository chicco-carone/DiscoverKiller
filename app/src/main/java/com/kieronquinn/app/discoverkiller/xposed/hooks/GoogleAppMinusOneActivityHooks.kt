package com.kieronquinn.app.discoverkiller.xposed.hooks

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kieronquinn.app.discoverkiller.providers.OverlaySettingsProvider
import com.kieronquinn.app.discoverkiller.repositories.SettingsRepository.OverlayMode
import com.kieronquinn.app.discoverkiller.ui.views.WebViewContainer
import com.kieronquinn.app.discoverkiller.utils.WebDestination.toWebUri
import com.kieronquinn.app.discoverkiller.xposed.BaseXposedHooks
import java.util.WeakHashMap

class GoogleAppMinusOneActivityHooks(
    classLoader: ClassLoader,
    private val applicationContext: Context
): BaseXposedHooks(classLoader) {

    companion object {
        const val CLASS_NAME =
            "com.google.android.apps.search.googleapp.launcher.minusone.activity.MinusOneActivity"
    }

    override val clazz = CLASS_NAME

    private val webViews = WeakHashMap<Activity, WebViewContainer>()

    fun onPostResume() = MethodHook(afterHookedMethod = {
        val activity = thisObject as Activity
        try {
            val settings = OverlaySettingsProvider.getCurrentOverlaySettings(applicationContext)
            if(!settings.enabled || settings.overlayMode == OverlayMode.OVERLAY){
                return@MethodHook MethodResult.Skip()
            }
            when(settings.overlayMode){
                OverlayMode.APP -> {
                    val component = ComponentName.unflattenFromString(settings.overlayApp)
                        ?: return@MethodHook MethodResult.Skip()
                    val intent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        this.component = component
                        if(settings.overlayAppNewTask){
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    }
                    activity.startActivity(intent)
                    activity.finish()
                    activity.overridePendingTransition(0, 0)
                    Log.d(TAG, "Launched configured APP from MinusOneActivity")
                }
                OverlayMode.WEB -> {
                    val uri = settings.webUrl.toWebUri()
                        ?: return@MethodHook MethodResult.Skip()
                    val container = webViews[activity] ?: WebViewContainer(
                        activity, insetForSystemBars = true
                    ).also {
                        webViews[activity] = it
                        activity.setContentView(it)
                    }
                    container.loadUrl(uri.toString())
                    container.onResume()
                    Log.d(TAG, "Embedded configured WEB in MinusOneActivity")
                }
                OverlayMode.OVERLAY -> return@MethodHook MethodResult.Skip()
            }
        }catch (e: Exception){
            Log.e(TAG, "Failed to launch configured content from MinusOneActivity", e)
        }
        MethodResult.Skip<Unit>()
    })

    fun onPause() = MethodHook(afterHookedMethod = {
        webViews[thisObject as Activity]?.onPause()
        MethodResult.Skip<Unit>()
    })

    fun onDestroy() = MethodHook(afterHookedMethod = {
        webViews.remove(thisObject as Activity)?.destroy()
        MethodResult.Skip<Unit>()
    })

    fun onBackPressed() = MethodHook(beforeHookedMethod = {
        val handled = webViews[thisObject as Activity]?.goBack() == true
        if(handled) MethodResult.Replace<Unit>(null) else MethodResult.Skip()
    })

}
