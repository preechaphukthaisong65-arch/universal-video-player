package com.preechaphukthaisong65.universalvideoplayer.browser

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient

/** Hosts the WebView's HTML5 custom video view over the Activity decor. */
class BrowserFullscreenController(context: Context) {
    private val activity = context.findActivity()
    private var customView: View? = null
    private var callback: WebChromeClient.CustomViewCallback? = null
    private var previousSystemUiVisibility: Int = 0
    private var previousOrientation: Int? = null

    fun show(view: View, customViewCallback: WebChromeClient.CustomViewCallback) {
        val host = activity
        val decor = host?.window?.decorView as? ViewGroup
        if (host == null || decor == null) {
            customViewCallback.onCustomViewHidden()
            return
        }
        hide()
        previousSystemUiVisibility = decor.systemUiVisibility
        previousOrientation = host.requestedOrientation
        customView = view
        callback = customViewCallback
        runCatching { host.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE }
        decor.addView(
            view,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        decor.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    fun hide() {
        val view = customView ?: return
        val host = activity
        val decor = host?.window?.decorView as? ViewGroup
        (view.parent as? ViewGroup)?.removeView(view)
        customView = null
        callback?.onCustomViewHidden()
        callback = null
        if (host != null && decor != null) {
            previousOrientation?.let { orientation ->
                runCatching { host.requestedOrientation = orientation }
            }
            decor.systemUiVisibility = previousSystemUiVisibility
        }
        previousOrientation = null
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
