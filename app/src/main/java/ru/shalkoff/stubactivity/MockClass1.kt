package ru.shalkoff.stubactivity

import ru.shalkoff.stubactivity.stubs.StubActivityStandard

import android.content.ComponentName
import android.content.Intent
import android.util.Log
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

class MockClass1(private val base: Any) : InvocationHandler {
    companion object {
        private const val TAG = "MockClass1"
    }

    override fun invoke(proxy: Any, method: Method, args: Array<Any>?): Any? {
        Log.d(TAG, "Invoked method: ${method.name}, args: ${args?.joinToString { it?.javaClass?.simpleName ?: "null" }}")

        if (method.name == "startActivity") {
            // Находим Intent в параметрах
            var rawIntent: Intent? = null
            var index = -1
            args?.forEachIndexed { i, arg ->
                if (arg is Intent) {
                    rawIntent = arg
                    index = i
                    return@forEachIndexed
                }
            }

            if (rawIntent != null && index != -1) {
                Log.d(TAG, "Found Intent: $rawIntent, component: ${rawIntent?.component}")
                // Создаем новый Intent для StubActivity
                val newIntent = Intent()
                val stubPackage = rawIntent?.component?.packageName ?: packageName
                val componentName = ComponentName(stubPackage, StubActivityStandard::class.java.name)
                newIntent.component = componentName

                // Сохраняем оригинальный Intent
                newIntent.putExtra(HookHelper.EXTRA_TARGET_INTENT, rawIntent)
                args?.set(index, newIntent)
                Log.d(TAG, "Hooked startActivity, replaced with StubActivity Intent: $newIntent")
            } else {
                Log.w(TAG, "No Intent found in args for startActivity")
            }
        } else {
            Log.d(TAG, "Non-startActivity method: ${method.name}")
        }

        return try {
            method.invoke(base, *(args ?: emptyArray()))
        } catch (e: Exception) {
            Log.e(TAG, "Method invocation failed: ${method.name}", e)
            null
        }
    }

    private val packageName: String
        get() = "ru.shalkoff.stubactivity" // Укажите ваш package name
}