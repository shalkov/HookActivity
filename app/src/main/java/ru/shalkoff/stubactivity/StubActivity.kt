package ru.shalkoff.stubactivity

import android.app.Activity
import android.os.Bundle

class StubActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This activity should never be visible
        finish()
    }
}
