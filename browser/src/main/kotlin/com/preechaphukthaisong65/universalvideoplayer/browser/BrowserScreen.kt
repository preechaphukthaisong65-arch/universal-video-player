package com.preechaphukthaisong65.universalvideoplayer.browser

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(modifier: Modifier = Modifier) {
    var address by remember { mutableStateOf("") }
    var currentUrl by remember { mutableStateOf("about:blank") }
    var pageTitle by remember { mutableStateOf("พร้อมใช้งาน") }
    var progress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current

    DisposableEffect(Unit) {
        onDispose {
            webView?.let { view ->
                view.stopLoading()
                view.webChromeClient = null
                view.webViewClient = WebViewClient()
                view.removeAllViews()
                view.destroy()
            }
            webView = null
        }
    }

    fun navigate(input: String) {
        val value = input.trim()
        if (value.isEmpty()) return
        val destination = runCatching {
            if (value.any { it.isWhitespace() } || (!value.contains('.') && !value.contains("://") && !value.contains(':'))) {
                "https://duckduckgo.com/?q=" +
                    URLEncoder.encode(value, StandardCharsets.UTF_8.name())
            } else {
                val candidate = if (value.contains("://")) value else "https://$value"
                val parsed = URI(candidate)
                require(parsed.scheme.equals("https", ignoreCase = true) && !parsed.host.isNullOrBlank())
                require(parsed.rawUserInfo == null) { "Embedded URL credentials are not allowed" }
                parsed.toASCIIString()
            }
        }.getOrNull() ?: return
        address = destination
        currentUrl = destination
        keyboard?.hide()
    }

    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            singleLine = true,
            label = { Text("ค้นหาหรือป้อนที่อยู่เว็บไซต์") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { navigate(address) })
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TextButton(onClick = { if (canGoBack) webView?.goBack() }, enabled = canGoBack) { Text("ย้อนกลับ") }
            TextButton(onClick = { if (canGoForward) webView?.goForward() }, enabled = canGoForward) { Text("ถัดไป") }
            TextButton(onClick = { webView?.reload() }, enabled = webView != null) { Text("โหลดใหม่") }
            Button(onClick = { navigate(address) }) { Text("ไป") }
        }
        if (progress in 1..99) {
            LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
        }
        Text(pageTitle, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = { context ->
                WebView(context).apply {
                    webView = this
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
                            return !request.url.scheme.equals("https", ignoreCase = true)
                        }

                        override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            url?.let {
                                currentUrl = it
                                address = it
                            }
                            progress = 1
                            canGoBack = view.canGoBack()
                            canGoForward = view.canGoForward()
                        }

                        override fun onPageFinished(view: WebView, url: String?) {
                            super.onPageFinished(view, url)
                            progress = 100
                            pageTitle = view.title?.take(120)?.ifBlank { "ไม่มีชื่อหน้าเว็บ" } ?: "ไม่มีชื่อหน้าเว็บ"
                            address = url.orEmpty()
                            canGoBack = view.canGoBack()
                            canGoForward = view.canGoForward()
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView, newProgress: Int) {
                            progress = newProgress
                        }
                    }
                    loadUrl("about:blank")
                }
            },
            update = { view ->
                if (currentUrl.startsWith("https://") && view.url != currentUrl) {
                    view.loadUrl(currentUrl)
                }
                canGoBack = view.canGoBack()
                canGoForward = view.canGoForward()
            }
        )
    }
}
