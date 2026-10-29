package com.kieronquinn.app.discoverkiller.xposed

import io.github.libxposed.api.XposedInterface
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

class BaseXposedHooksTest {
    class Target {
        fun call(value: String): String = value
    }

    class Hooks(api: XposedInterface) : BaseXposedHooks(api, Target::class.java.classLoader!!) {
        override val clazz = Target::class.java.name
        var before: (HookParam.() -> MethodResult<String>)? = null
        var after: (HookParam.() -> MethodResult<String>)? = null

        @Suppress("UNUSED_PARAMETER")
        fun call(value: String) = MethodHook<String>(
            beforeHookedMethod = { before?.invoke(this) ?: MethodResult.Skip() },
            afterHookedMethod = { after?.invoke(this) ?: MethodResult.Skip() }
        )
    }

    private class Fixture {
        lateinit var interceptor: XposedInterface.Hooker
        val receiver = Target()
        var originalCalls = 0
        var original: () -> Any? = { "original" }
        private val builder = proxy<XposedInterface.HookBuilder> { name, args ->
            check(name == "intercept")
            interceptor = args[0] as XposedInterface.Hooker
            proxy<XposedInterface.HookHandle> { _, _ -> null }
        }
        private val api = proxy<XposedInterface> { name, _ ->
            check(name == "hook")
            builder
        }
        val hooks = Hooks(api).also { it.init() }

        fun invoke(): Any? = interceptor.intercept(proxy<XposedInterface.Chain> { name, _ ->
            when(name) {
                "getThisObject" -> receiver
                "getArgs" -> listOf("argument")
                "proceed" -> {
                    originalCalls++
                    original()
                }
                else -> error("Unexpected chain call: $name")
            }
        })
    }

    @Test fun unchangedCallRunsOriginalOnce() {
        val fixture = Fixture()
        assertEquals("original", fixture.invoke())
        assertEquals(1, fixture.originalCalls)
    }

    @Test fun beforeReplacementSkipsOriginalAndReachesAfterHook() {
        val fixture = Fixture()
        fixture.hooks.before = { BaseXposedHooks.MethodResult.Replace("replacement") }
        fixture.hooks.after = {
            assertEquals("replacement", result)
            BaseXposedHooks.MethodResult.Replace("after")
        }
        assertEquals("after", fixture.invoke())
        assertEquals(0, fixture.originalCalls)
    }

    @Test fun afterHookReceivesReceiverArgumentsAndOriginalResult() {
        val fixture = Fixture()
        fixture.hooks.after = {
            assertSame(fixture.receiver, thisObject)
            assertArrayEquals(arrayOf("argument"), args)
            assertEquals("original", result)
            BaseXposedHooks.MethodResult.Replace("after")
        }
        assertEquals("after", fixture.invoke())
        assertEquals(1, fixture.originalCalls)
    }

    @Test fun nullReplacementSkipsOriginal() {
        val fixture = Fixture()
        fixture.hooks.before = { BaseXposedHooks.MethodResult.Replace(null) }
        assertNull(fixture.invoke())
        assertEquals(0, fixture.originalCalls)
    }

    @Test fun originalExceptionIsPropagatedWithoutRetry() {
        val fixture = Fixture()
        val failure = IllegalStateException("original failure")
        fixture.original = { throw failure }
        val thrown = assertThrows(IllegalStateException::class.java) { fixture.invoke() }
        assertSame(failure, thrown)
        assertEquals(1, fixture.originalCalls)
    }

    companion object {
        private inline fun <reified T> proxy(
            crossinline call: (String, Array<out Any?>) -> Any?
        ): T = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) {
            _, method, args -> call(method.name, args ?: emptyArray())
        } as T
    }
}
