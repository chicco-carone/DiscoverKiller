package com.kieronquinn.app.discoverkiller.xposed.hooks

import android.content.Intent
import android.util.Log
import com.kieronquinn.app.discoverkiller.service.IDiscoverKillerClient
import com.kieronquinn.app.discoverkiller.utils.extensions.hasDiscoverKillerToken
import io.github.libxposed.api.XposedInterface
import com.kieronquinn.app.discoverkiller.xposed.BaseXposedHooks

/**
 *  Hooks PublicSearchService to return the validation service when hooking is working. In the
 *  event hooks are not working, this will return a normal IPublicService binder and we can
 *  therefore tell that the module is not installed correctly.
 */
class GoogleAppPublicSearchHooks(
    xposed: XposedInterface,
    classLoader: ClassLoader
): BaseXposedHooks(xposed, classLoader) {

    companion object {
        private const val CLASSIC_SERVICE =
            "com.google.android.apps.gsa.publicsearch.PublicSearchService"
        private const val CLASSICLESS_SERVICE =
            "com.google.android.apps.gsa.publicsearch.classicless.ClassicLessPublicSearchService"
    }

    override val clazz = if(runCatching { classLoader.loadClass(CLASSICLESS_SERVICE) }.isSuccess){
        CLASSICLESS_SERVICE
    }else{
        CLASSIC_SERVICE
    }

    private val discoverKillerClientService = DiscoverKillerClientService()

    fun onBind(intent: Intent) = MethodHook {
        if (intent.hasDiscoverKillerToken()) {
            return@MethodHook MethodResult.Replace(discoverKillerClientService.asBinder())
        }
        MethodResult.Skip()
    }

    private inner class DiscoverKillerClientService: IDiscoverKillerClient.Stub() {
        override fun areHooksWorking(): Boolean {
            return true
        }
    }

}
