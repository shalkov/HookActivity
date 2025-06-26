package ru.shalkoff.stubactivity

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import java.lang.reflect.Method

class EvilInstrumentation(private val base: Instrumentation) : Instrumentation() {
    companion object {
        private const val TAG = "EvilInstrumentation"
    }

    // Инициализация метода execStartActivity через рефлексию
    private val execStartActivityMethod: Method? by lazy {
        Log.d(TAG, "Attempting to get execStartActivity method via reflection")
        try {
            val method = Instrumentation::class.java.getDeclaredMethod(
                "execStartActivity",
                Context::class.java,
                IBinder::class.java,
                IBinder::class.java,
                Activity::class.java,
                Intent::class.java,
                Int::class.javaPrimitiveType,
                Bundle::class.java
            )
            method.isAccessible = true
            Log.d(TAG, "Successfully obtained execStartActivity method: $method")
            method
        } catch (e: NoSuchMethodException) {
            Log.e(TAG, "Failed to find execStartActivity method", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while obtaining execStartActivity method", e)
            null
        }
    }

    override fun newActivity(cl: ClassLoader, className: String, intent: Intent): Activity {
        Log.d(TAG, "newActivity called with className: $className, intent: $intent, extras: ${intent.extras}")
        val rawIntent = intent.getParcelableExtra<Intent>(HookHelper.EXTRA_TARGET_INTENT)
        if (rawIntent == null) {
            Log.d(TAG, "No rawIntent found, proceeding with original className: $className")
            try {
                return base.newActivity(cl, className, intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create Activity for className: $className", e)
                throw e
            }
        }

        val newClassName = rawIntent.component?.className
        Log.d(TAG, "Found rawIntent: $rawIntent, restoring original Activity: $newClassName")
        return try {
            if (newClassName != null && newClassName != "ru.shalkoff.stubactivity.stubs.StubActivityStandard") {
                Log.d(TAG, "Creating unregistered Activity with className: $newClassName, rawIntent: $rawIntent")
                // Создаём активность через рефлексию, чтобы обойти проверку манифеста
                val activityClass = Class.forName(newClassName, true, cl)
                val activity = activityClass.getConstructor().newInstance() as Activity
                activity.intent = rawIntent
                Log.d(TAG, "Successfully created Activity: $activity")
                activity
            } else {
                Log.d(TAG, "rawIntent points to StubActivity or no component, using original className: $className")
                base.newActivity(cl, className, intent)
            }
        } catch (e: ClassNotFoundException) {
            Log.e(TAG, "Class not found for className: $newClassName", e)
            base.newActivity(cl, className, intent) // Fallback
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create Activity for className: $newClassName", e)
            base.newActivity(cl, className, intent) // Fallback
        }
    }

    // Кастомный метод для обработки startActivity через рефлексию
    fun startActivity(
        who: Context,
        contextThread: IBinder,
        token: IBinder?,
        target: Activity?,
        intent: Intent,
        requestCode: Int,
        options: Bundle?
    ): ActivityResult? {
        Log.d(TAG, "startActivity called with intent: $intent, target: $target, requestCode: $requestCode, options: $options")
        Log.d(TAG, "Intent extras: ${intent.extras}, component: ${intent.component}")

        // Проверяем наличие метода
        if (execStartActivityMethod == null) {
            Log.e(TAG, "execStartActivityMethod is null, cannot proceed with activity start")
            return null
        }

        // Если Intent не для StubActivity, подменяем его
        val rawIntent = intent.getParcelableExtra<Intent>(HookHelper.EXTRA_TARGET_INTENT)
        if (rawIntent != null && intent.component?.className != "ru.shalkoff.stubactivity.stubs.StubActivityStandard") {
            Log.d(TAG, "Found rawIntent: $rawIntent, rawIntent component: ${rawIntent.component}")
            val stubIntent = Intent().apply {
                component = ComponentName(who.packageName, "ru.shalkoff.stubactivity.stubs.StubActivityStandard")
                putExtra(HookHelper.EXTRA_TARGET_INTENT, rawIntent)
            }
            Log.d(TAG, "Replacing intent with StubActivity intent: $stubIntent, stubIntent extras: ${stubIntent.extras}")
            return try {
                Log.d(TAG, "Invoking execStartActivity with stubIntent: $stubIntent")
                val result = execStartActivityMethod!!.invoke(
                    base,
                    who,
                    contextThread,
                    token,
                    target,
                    stubIntent,
                    requestCode,
                    options
                ) as? ActivityResult
                Log.d(TAG, "execStartActivity returned: $result")
                result
            } catch (e: Exception) {
                Log.e(TAG, "Failed to invoke execStartActivity with stubIntent", e)
                null
            }
        }

        Log.d(TAG, "No need to replace, using original intent: $intent")
        return try {
            Log.d(TAG, "Invoking execStartActivity with original intent: $intent")
            val result = execStartActivityMethod!!.invoke(
                base,
                who,
                contextThread,
                token,
                target,
                intent,
                requestCode,
                options
            ) as? ActivityResult
            Log.d(TAG, "execStartActivity returned: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to invoke execStartActivity with original intent", e)
            null
        }
    }
}