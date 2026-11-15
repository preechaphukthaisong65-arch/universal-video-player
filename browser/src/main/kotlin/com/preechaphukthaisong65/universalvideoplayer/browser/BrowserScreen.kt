package com.preechaphukthaisong65.universalvideoplayer.browser

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.net.URI

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(modifier: Modifier = Modifier) {
    var address by remember { mutableStateOf("") }
    var currentUrl by remember { mutableStateOf("about:blank") }
    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            singleLine = true,
            label = { Text("เว็บไซต์") }
        )
        Button(onClick = {
            val normalized = if (address.startsWith("https://", true)) address else "https://$address"
            runCatching {
                val parsed = URI(normalized)
                require(parsed.scheme == "https" && parsed.host != null)
                currentUrl = parsed.toASCIIString()
            }
        }, modifier = Modifier.padding(horizontal = 8.dp)) { Text("ไปยังเว็บไซต์") }
        AndroidView(
            modifier = Modifier.weight(1f),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    settings.mediaPlaybackRequiresUserGesture = true
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    settings.safeBrowsingEnabled = true
                    settings.setSupportMultipleWindows(false)
                    android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val uri = request.url
                            return uri.scheme != "https" && uri.scheme != "about"
                        }
                    }
                    webChromeClient = WebChromeClient()
                    loadUrl("about:blank")
                }
            },
            update = { if (currentUrl.startsWith("https://") && it.url != currentUrl) it.loadUrl(currentUrl) }
        )
    }
}
