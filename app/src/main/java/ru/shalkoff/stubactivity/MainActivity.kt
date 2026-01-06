package ru.shalkoff.stubactivity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.shalkoff.stubactivity.activities.TargetActivity
import ru.shalkoff.stubactivity.ui.theme.StubActivityTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StubActivityTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        onLaunchTarget = {
                            try {
                                val intent = Intent(this, TargetActivity::class.java)
                                startActivity(intent)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to launch TargetActivity", e)
                                Toast.makeText(
                                    this,
                                    "Launch Failed: ${e.message}",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onLaunchTarget: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onLaunchTarget) {
            Text(text = "Launch TargetActivity (Unregistered)")
        }
    }
}