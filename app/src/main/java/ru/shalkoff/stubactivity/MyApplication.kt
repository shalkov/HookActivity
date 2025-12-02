package ru.shalkoff.stubactivity

import android.app.Application
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass

class MyApplication : Application() {

    private companion object {
        private const val TAG = "MyApplication"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate started")

        // Инициализируем HiddenApiBypass ПЕРЕД тем, как делать что-либо с рефлексией.
        try {
            HiddenApiBypass.addHiddenApiExemptions("L")
            Log.d(TAG, "HiddenApiBypass initialized in Application")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize HiddenApiBypass", e)
        }
        
        // Применяем глобальный хук сразу при старте приложения.
        // Это гарантирует, что все последующие Activity (включая MainActivity)
        // будут созданы уже с нашим подмененным Instrumentation.
        HookManager.applyHook()
        
        Log.d(TAG, "Hook applied from MyApplication")
    }
}