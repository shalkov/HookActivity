package ru.shalkoff.stubactivity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.lsposed.hiddenapibypass.HiddenApiBypass
import ru.shalkoff.stubactivity.stubs.StubActivityStandard
import ru.shalkoff.stubactivity.ui.theme.StubActivityTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Обход ограничений скрытых API
        Log.d(TAG, "Initializing HiddenApiBypass")
        try {
            HiddenApiBypass.addHiddenApiExemptions("android.app.IActivityManager")
            HiddenApiBypass.addHiddenApiExemptions("android.app.ActivityManager")
            HiddenApiBypass.addHiddenApiExemptions("android.util.Singleton")
            HiddenApiBypass.addHiddenApiExemptions("android.app.ActivityThread")
            HiddenApiBypass.addHiddenApiExemptions("android.app.Activity")
            Log.d(TAG, "HiddenApiBypass initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize HiddenApiBypass", e)
        }

        // Устанавливаем глобальный хук
        Log.d(TAG, "Installing global Instrumentation hook")
        try {
            HookHelper.hookGlobalInstrumentation()
            Log.d(TAG, "Global hooks installed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install global hooks", e)
        }
        enableEdgeToEdge()
        setContent {
            StubActivityTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier
                            .clickable {
                                Log.d(TAG, "button_second clicked, launching SecondActivity")
                                val targetIntent = Intent().apply {
                                    setClassName(packageName, "ru.shalkoff.stubactivity.SecondActivity")
                                }
                                Log.d(TAG, "Created targetIntent: $targetIntent")
                                val stubIntent = Intent(this, StubActivityStandard::class.java).apply {
                                    putExtra(HookHelper.EXTRA_TARGET_INTENT, targetIntent)
                                }
                                Log.d(TAG, "Created stubIntent: $stubIntent, extras: ${stubIntent.extras}")
                                HookHelper.startActivity(this, stubIntent)
                            }
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    StubActivityTheme {
        Greeting("Android")
    }
}