package ru.shalkoff.stubactivity

import android.app.Application
import android.util.Log

class MyApplication : Application() {

    private companion object {
        private const val TAG = "MyApplication"
    }

    override fun onCreate() {
        super.onCreate()
        //Log.d(TAG, "Application onCreate started")
        //ActivityHooker.hook()
        //Log.d(TAG, "Hook applied from MyApplication")
    }
}