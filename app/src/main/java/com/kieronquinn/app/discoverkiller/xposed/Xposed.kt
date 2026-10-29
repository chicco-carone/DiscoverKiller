package com.kieronquinn.app.discoverkiller.xposed

import android.app.Application
import android.content.Context
import android.util.Log
import com.kieronquinn.app.discoverkiller.xposed.hooks.*
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

class Xposed: XposedModule() {

    companion object {
        const val GOOGLE_APP_PACKAGE_NAME = "com.google.android.googlequicksearchbox"
        const val ENTERTAINMENT_SPACE_PACKAGE_NAME = "com.google.android.apps.mediahome.launcher"

        private val PACKAGES = mapOf(
            GOOGLE_APP_PACKAGE_NAME to Xposed::gsaHooks,
            ENTERTAINMENT_SPACE_PACKAGE_NAME to Xposed::entertainmentSpaceHooks
        )
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if(!param.isFirstPackage) return
        val methods = PACKAGES[param.packageName] ?: return
        // attach runs even when an Application subclass does not call super.onCreate().
        hook(Application::class.java.getDeclaredMethod("attach", Context::class.java))
            .intercept { chain ->
                val result = chain.proceed()
                val application = chain.thisObject as Application
                if(application.packageName == param.packageName) {
                    Log.d("DOS", "Application attached: ${param.packageName}")
                    methods(this, param.classLoader, application).forEach { it.init() }
                }
                result
            }
    }

    private fun gsaHooks(classLoader: ClassLoader, context: Context): Array<BaseXposedHooks> {
        val hooks = mutableListOf<BaseXposedHooks>(
            GoogleAppDrawerOverlayServiceHooks(this, classLoader, context),
            GoogleAppPublicSearchHooks(this, classLoader)
        )
        if(runCatching { classLoader.loadClass(GoogleAppMinusOneActivityHooks.CLASS_NAME) }.isSuccess){
            hooks.add(GoogleAppMinusOneActivityHooks(this, classLoader, context))
        }
        return hooks.toTypedArray()
    }

    private fun entertainmentSpaceHooks(
        classLoader: ClassLoader, context: Context
    ): Array<BaseXposedHooks> {
        return arrayOf(
            EntertainmentSpaceLayoutHooks(this, classLoader, context),
            EntertainmentSpaceDetailsActivityHooks(this, classLoader)
        )
    }

}
