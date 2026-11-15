package com.preechaphukthaisong65.universalvideoplayer

import android.content.Context

/** Stores only local playback/browser preferences. No preference is uploaded. */
class AppSettingsRepository(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("universal-player-settings", Context.MODE_PRIVATE)

    fun keepScreenAwake(): Boolean = preferences.getBoolean(KEY_KEEP_SCREEN_AWAKE, true)

    fun setKeepScreenAwake(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_KEEP_SCREEN_AWAKE, enabled).apply()
    }

    fun browserJavaScriptEnabled(): Boolean = preferences.getBoolean(KEY_BROWSER_JAVASCRIPT, true)

    fun setBrowserJavaScriptEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_BROWSER_JAVASCRIPT, enabled).apply()
    }

    private companion object {
        const val KEY_KEEP_SCREEN_AWAKE = "keep_screen_awake"
        const val KEY_BROWSER_JAVASCRIPT = "browser_javascript_enabled"
    }
}
