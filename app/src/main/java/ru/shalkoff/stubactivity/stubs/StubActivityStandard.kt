package ru.shalkoff.stubactivity.stubs

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import ru.shalkoff.stubactivity.HookHelper

class StubActivityStandard : ComponentActivity() {
    companion object {
        private const val TAG = "StubActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "StubActivity onCreate called, intent: $intent, extras: ${intent.extras}")

        val rawIntent = intent.getParcelableExtra<Intent>(HookHelper.EXTRA_TARGET_INTENT)
        if (rawIntent != null) {
            Log.d(TAG, "Found rawIntent: $rawIntent, component: ${rawIntent.component}")
            rawIntent.component?.let { component ->
                val newIntent = Intent().apply {
                    setComponent(ComponentName(packageName, component.className))
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                Log.d(TAG, "Starting target activity with newIntent: $newIntent, package: $packageName")
                try {
                    HookHelper.startActivity(this, newIntent)
                    Log.d(TAG, "Successfully started target activity")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start target activity with newIntent: $newIntent", e)
                }
            } ?: run {
                Log.e(TAG, "No component in rawIntent, cannot start activity")
            }
        } else {
            Log.e(TAG, "No rawIntent found in StubActivity")
        }
        Log.d(TAG, "Finishing StubActivity")
        finish()
    }
}