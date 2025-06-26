package ru.shalkoff.stubactivity

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import java.lang.reflect.Field
import java.lang.reflect.Method

object HookHelper {
    const val EXTRA_TARGET_INTENT = "extra_target_intent"
    private const val TAG = "HookHelper"

    fun hookActivityInstrumentation(activity: Activity) {
        Log.d(TAG, "Starting Activity Instrumentation hook for activity: ${activity.javaClass.name}")
        try {
            val activityClass = Class.forName("android.app.Activity")
            Log.d(TAG, "Obtained Activity class: $activityClass")
            val instrumentationField = activityClass.getDeclaredField("mInstrumentation")
            Log.d(TAG, "Obtained mInstrumentation field: $instrumentationField")
            instrumentationField.isAccessible = true
            val originalInstrumentation = instrumentationField.get(activity) as Instrumentation
            Log.d(TAG, "Original Activity Instrumentation: $originalInstrumentation")
            val evilInstrumentation = EvilInstrumentation(originalInstrumentation)
            instrumentationField.set(activity, evilInstrumentation)
            Log.d(TAG, "Activity Instrumentation hook completed, set to: $evilInstrumentation")
        } catch (e: ClassNotFoundException) {
            Log.e(TAG, "Failed to find Activity class", e)
        } catch (e: NoSuchFieldException) {
            Log.e(TAG, "Failed to find mInstrumentation field", e)
        } catch (e: IllegalAccessException) {
            Log.e(TAG, "Failed to access mInstrumentation field", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Activity Instrumentation hook", e)
        }
    }

    // Новый метод для глобального хука Instrumentation через ActivityThread
    fun hookGlobalInstrumentation() {
        Log.d(TAG, "Starting global Instrumentation hook")
        try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread")
            currentActivityThreadMethod.isAccessible = true
            val activityThread = currentActivityThreadMethod.invoke(null)
            Log.d(TAG, "Obtained ActivityThread: $activityThread")
            val instrumentationField = activityThreadClass.getDeclaredField("mInstrumentation")
            instrumentationField.isAccessible = true
            val originalInstrumentation = instrumentationField.get(activityThread) as Instrumentation
            Log.d(TAG, "Original global Instrumentation: $originalInstrumentation")
            val evilInstrumentation = EvilInstrumentation(originalInstrumentation)
            instrumentationField.set(activityThread, evilInstrumentation)
            Log.d(TAG, "Global Instrumentation hook completed, set to: $evilInstrumentation")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hook global Instrumentation", e)
        }
    }

    // Метод для вызова startActivity через EvilInstrumentation
    fun startActivity(activity: Activity, intent: Intent, options: Bundle? = null) {
        Log.d(TAG, "HookHelper.startActivity called with intent: $intent, extras: ${intent.extras}, options: $options")
        try {
            val activityClass = Class.forName("android.app.Activity")
            val instrumentationField = activityClass.getDeclaredField("mInstrumentation")
            instrumentationField.isAccessible = true
            val instrumentation = instrumentationField.get(activity) as Instrumentation
            Log.d(TAG, "Current Instrumentation: $instrumentation")
            if (instrumentation is EvilInstrumentation) {
                Log.d(TAG, "Calling EvilInstrumentation.startActivity")
                // Получаем IApplicationThread через ActivityThread
                val contextThread = getApplicationThread()
                if (contextThread == null) {
                    Log.e(TAG, "Failed to obtain IApplicationThread, falling back to default startActivity")
                    activity.startActivity(intent, options)
                    return
                }
                instrumentation.startActivity(
                    who = activity,
                    contextThread = contextThread,
                    token = null,
                    target = activity,
                    intent = intent,
                    requestCode = -1,
                    options = options
                )
            } else {
                Log.w(TAG, "Instrumentation is not EvilInstrumentation, falling back to default startActivity")
                activity.startActivity(intent, options)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call startActivity via Instrumentation", e)
            activity.startActivity(intent, options)
        }
    }

    // Вспомогательный метод для получения IApplicationThread
    private fun getApplicationThread(): IBinder? {
        Log.d(TAG, "Attempting to get IApplicationThread")
        try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread")
            currentActivityThreadMethod.isAccessible = true
            val activityThread = currentActivityThreadMethod.invoke(null)
            Log.d(TAG, "Obtained ActivityThread: $activityThread")
            val getApplicationThreadMethod = activityThreadClass.getDeclaredMethod("getApplicationThread")
            getApplicationThreadMethod.isAccessible = true
            val applicationThread = getApplicationThreadMethod.invoke(activityThread)
            Log.d(TAG, "Obtained IApplicationThread: $applicationThread")
            return applicationThread as? IBinder
        } catch (e: Exception) {
            Log.e(TAG, "Failed to obtain IApplicationThread", e)
            return null
        }
    }
}