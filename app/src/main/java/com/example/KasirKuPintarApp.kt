package com.example

import android.app.Application
import android.content.res.Configuration

class KasirKuPintarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        lockFontScale(resources.configuration)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        lockFontScale(newConfig)
    }

    private fun lockFontScale(config: Configuration) {
        if (config.fontScale != 1.0f) {
            config.fontScale = 1.0f
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
        }
    }
}
