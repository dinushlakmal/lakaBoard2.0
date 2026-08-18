package com.dinushlakmal.lakaboard

import android.app.Application

class LakaBoardApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Reserved for future global initialization (e.g. theme persistence
        // preload, crash reporting). Kept intentionally lightweight so the
        // IME process starts fast.
    }
}
