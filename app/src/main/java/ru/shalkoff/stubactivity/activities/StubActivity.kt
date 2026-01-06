package ru.shalkoff.stubactivity.activities

import android.app.Activity
import android.os.Bundle

/**
 * Activity - заглушка, для запуска других активити, которые не зарегистрированы в Manifest
 */
class StubActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}
