package com.kieronquinn.app.discoverkiller.xposed

import android.util.Log
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Executable
import java.lang.reflect.Method

abstract class BaseXposedHooks(
    private val xposed: XposedInterface,
    private val classLoader: ClassLoader
) {

    companion object {
        internal const val TAG = "XposedHooks"
    }

    abstract val clazz: String

    open fun init() {
        try {
            setupHooks()
        }catch (e: Exception){
            Log.e(TAG, "Error setting up hooks for $clazz", e)
        }
    }

    private fun setupHooks() {
        val clazz = try {
            Class.forName(clazz, false, classLoader)
        }catch (e: ClassNotFoundException){
            Log.e(TAG, "Failed to find clazz for ${this::class.java.simpleName} hooks")
            return
        }
        this::class.java.declaredMethods.forEach { hook ->
            //We only want method hooks
            if(hook.returnType != MethodHook::class.java) return@forEach
            hook.isAccessible = true
            val replace = try {
                when {
                    hook.name.startsWith("any_") -> {
                        clazz.declaredMethods.firstOrNull {
                            it.parameterTypes.contentEquals(hook.parameterTypes)
                        } ?: run {
                            Log.e(
                                TAG, "No target found for hook with parameter types " +
                                    hook.parameterTypes.joinToString(", ") { it.name })
                            return@forEach
                        }
                    }
                    hook.name.startsWith("constructor_") -> {
                        clazz.getDeclaredConstructor(*hook.parameterTypes)
                    }
                    hook.name.startsWith("skip_") -> {
                        return@forEach
                    }
                    else -> {
                        clazz.getDeclaredMethod(hook.name, *hook.parameterTypes)
                    }
                }
            }catch (e: NoSuchMethodException){
                Log.w(TAG, "Skipping missing target ${clazz.name}.${hook.name}")
                return@forEach
            }
            hookMethod(hook, replace)
        }
    }

    private fun hookMethod(hook: Method, target: Executable) {
        xposed.hook(target).intercept { chain ->
            val param = HookParam(chain.thisObject, chain.args.toTypedArray())
            val before = (hook.invoke(this, *param.args) as MethodHook<*>)
                .beforeHookedMethod?.invoke(param, param.thisObject)
            param.result = if(before is MethodResult.Replace) {
                before.value
            } else {
                chain.proceed()
            }
            val after = (hook.invoke(this, *param.args) as MethodHook<*>)
                .afterHookedMethod?.invoke(param, param.result)
            if(after is MethodResult.Replace) after.value else param.result
        }
    }

    data class HookParam(
        val thisObject: Any?,
        val args: Array<Any?>,
        var result: Any? = null
    )

    data class MethodHook<T>(
        val afterHookedMethod: (HookParam.(result: Any?) -> MethodResult<T>)? = null,
        val beforeHookedMethod: (HookParam.(obj: Any?) -> MethodResult<T>)? = null,
    )

    sealed class MethodResult<T> {
        data class Replace<T>(val value: T?): MethodResult<T>()
        class Skip<T>: MethodResult<T>()
    }
}
