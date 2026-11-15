package com.preechaphukthaisong65.universalvideoplayer.browser

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private data class BrowserTabState(val id: Int, val url: String, val title: String)
private data class SavedPage(val title: String, val url: String)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    modifier: Modifier = Modifier,
    enableJavaScript: Boolean = true,
    onOpenMedia: (String, String?) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("browser-local-data", Context.MODE_PRIVATE)
    }
    val keyboard = LocalSoftwareKeyboardController.current
    val downloadHandler = remember(context) { BrowserDownloadHandler(context) }
    val fullscreenController = remember(context) { BrowserFullscreenController(context) }
    val tabs = remember { mutableStateListOf(BrowserTabState(1, "about:blank", "แท็บ 1")) }
    val webViews = remember { mutableStateMapOf<Int, WebView>() }
    val bookmarks = remember {
        mutableStateListOf<SavedPage>().apply {
            addAll(readSavedPages(preferences.getString(KEY_BOOKMARKS, null)))
        }
    }
    val history = remember {
        mutableStateListOf<SavedPage>().apply {
            addAll(readSavedPages(preferences.getString(KEY_HISTORY, null)))
        }
    }
    var nextTabId by remember { mutableIntStateOf(2) }
    var activeTabId by remember { mutableIntStateOf(1) }
    var address by remember { mutableStateOf("") }
    var pageTitle by remember { mutableStateOf("พร้อมใช้งาน") }
    var progress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf("") }
    var privacyMessage by remember { mutableStateOf("") }
    var downloadMessage by remember { mutableStateOf("") }

    fun updateTab(id: Int, transform: (BrowserTabState) -> BrowserTabState) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index >= 0) tabs[index] = transform(tabs[index])
    }

    fun saveBookmarks() {
        preferences.edit().putString(KEY_BOOKMARKS, encodeSavedPages(bookmarks)).apply()
    }

    fun saveHistory() {
        preferences.edit().putString(KEY_HISTORY, encodeSavedPages(history)).apply()
    }

    fun navigate(input: String) {
        val value = input.trim()
        if (value.isEmpty()) return
        val destination = runCatching {
            if (value.any { it.isWhitespace() } ||
                (!value.contains('.') && !value.contains("://") && !value.contains(':'))
            ) {
                "https://duckduckgo.com/?q=" +
                    URLEncoder.encode(value, StandardCharsets.UTF_8.name())
            } else {
                val candidate = if (value.contains("://")) value else "https://$value"
                val parsed = URI(candidate)
                require(parsed.scheme.equals("https", ignoreCase = true))
                require(!parsed.host.isNullOrBlank() && parsed.rawUserInfo == null)
                parsed.toASCIIString()
            }
        }.getOrNull() ?: return
        updateTab(activeTabId) { it.copy(url = destination) }
        address = destination
        keyboard?.hide()
    }

    LaunchedEffect(activeTabId) {
        fullscreenController.hide()
        val active = tabs.firstOrNull { it.id == activeTabId }
        address = active?.url?.takeUnless { it == "about:blank" }.orEmpty()
        pageTitle = active?.title ?: "แท็บใหม่"
        progress = 0
        val view = webViews[activeTabId]
        canGoBack = view?.canGoBack() == true
        canGoForward = view?.canGoForward() == true
    }

    DisposableEffect(Unit) {
        onDispose {
            fullscreenController.hide()
            webViews.values.toList().forEach { view ->
                (view.parent as? ViewGroup)?.removeView(view)
                view.stopLoading()
                view.webChromeClient = null
                view.webViewClient = WebViewClient()
                view.removeAllViews()
                view.destroy()
            }
            webViews.clear()
            CookieManager.getInstance().flush()
        }
    }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEach { tab ->
                TextButton(onClick = { activeTabId = tab.id }) {
                    Text((if (tab.id == activeTabId) "● " else "") + tab.title.take(18))
                }
                TextButton(onClick = {
                    webViews.remove(tab.id)?.let { view ->
                        (view.parent as? ViewGroup)?.removeView(view)
                        view.stopLoading()
                        view.destroy()
                    }
                    tabs.removeAll { it.id == tab.id }
                    if (tabs.isEmpty()) {
                        val id = nextTabId++
                        tabs.add(BrowserTabState(id, "about:blank", "แท็บใหม่"))
                        activeTabId = id
                    } else if (activeTabId == tab.id) {
                        activeTabId = tabs.last().id
                    }
                }) { Text("×") }
            }
            TextButton(
                onClick = {
                    if (tabs.size < MAX_TABS) {
                        val id = nextTabId++
                        tabs.add(BrowserTabState(id, "about:blank", "แท็บใหม่"))
                        activeTabId = id
                    }
                },
                enabled = tabs.size < MAX_TABS
            ) { Text("+") }
        }

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
            TextButton(onClick = { webViews[activeTabId]?.let { if (it.canGoBack()) it.goBack() } }, enabled = canGoBack) {
                Text("ย้อนกลับ")
            }
            TextButton(onClick = { webViews[activeTabId]?.let { if (it.canGoForward()) it.goForward() } }, enabled = canGoForward) {
                Text("ถัดไป")
            }
            TextButton(onClick = { webViews[activeTabId]?.reload() }, enabled = webViews.containsKey(activeTabId)) {
                Text("โหลดใหม่")
            }
            Button(onClick = { navigate(address) }) { Text("ไป") }
            TextButton(onClick = {
                val tab = tabs.firstOrNull { it.id == activeTabId }
                val storedUrl = tab?.url?.let(::safeStoredUrl)
                if (tab != null && storedUrl != null) {
                    bookmarks.removeAll { it.url == storedUrl }
                    bookmarks.add(0, SavedPage(tab.title, storedUrl))
                    while (bookmarks.size > MAX_BOOKMARKS) bookmarks.removeAt(bookmarks.lastIndex)
                    saveBookmarks()
                    privacyMessage = "บันทึกบุ๊กมาร์กในอุปกรณ์แล้ว โดยตัด query และ fragment ออกจาก URL"
                }
            }) { Text("บันทึก") }
            TextButton(onClick = { dialog = "bookmarks" }) { Text("บุ๊กมาร์ก") }
            TextButton(onClick = { dialog = "history" }) { Text("ประวัติ") }
            TextButton(onClick = { dialog = "cookies" }) { Text("ล้างคุกกี้") }
        }
        if (progress in 1..99) {
            LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
        }
        Text(pageTitle, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))

        val selectedTab = tabs.firstOrNull { it.id == activeTabId }
        if (selectedTab != null) {
            key(selectedTab.id) {
                AndroidView(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                factory = { viewContext ->
                    val view = webViews[activeTabId] ?: WebView(viewContext).apply {
                        settings.javaScriptEnabled = enableJavaScript
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.javaScriptCanOpenWindowsAutomatically = false
                        settings.mediaPlaybackRequiresUserGesture = true
                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        settings.safeBrowsingEnabled = true
                        settings.setSupportMultipleWindows(false)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                        setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                            val downloadId = downloadHandler.enqueue(
                                url,
                                userAgent,
                                contentDisposition,
                                mimeType
                            )
                            downloadMessage = if (downloadId != null) {
                                "ส่งคำขอดาวน์โหลดไปยัง Download Manager แล้ว"
                            } else {
                                "ดาวน์โหลดไม่ได้: รองรับเฉพาะลิงก์ HTTPS ที่ไม่ต้องส่ง Cookie จาก Browser"
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                if (!request.url.scheme.equals("https", ignoreCase = true) ||
                                    !request.url.userInfo.isNullOrEmpty()
                                ) return true
                                if (request.isForMainFrame && isDirectMediaUrl(request.url)) {
                                    onOpenMedia(request.url.toString(), request.url.lastPathSegment)
                                    return true
                                }
                                return false
                            }

                            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                if (url != null) {
                                    updateTab(selectedTab.id) { it.copy(url = url) }
                                    if (activeTabId == selectedTab.id) {
                                        address = url
                                        progress = 1
                                    }
                                }
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                super.onPageFinished(view, url)
                                val title = view.title?.take(120)?.ifBlank { "ไม่มีชื่อหน้าเว็บ" } ?: "ไม่มีชื่อหน้าเว็บ"
                                if (url != null) {
                                    updateTab(selectedTab.id) { it.copy(url = url, title = title) }
                                    safeStoredUrl(url)?.let { safeUrl ->
                                        history.removeAll { it.url == safeUrl }
                                        history.add(0, SavedPage(title, safeUrl))
                                        while (history.size > MAX_HISTORY) history.removeAt(history.lastIndex)
                                        saveHistory()
                                    }
                                }
                                if (activeTabId == selectedTab.id) {
                                    pageTitle = title
                                    address = url.orEmpty()
                                    progress = 100
                                    canGoBack = view.canGoBack()
                                    canGoForward = view.canGoForward()
                                }
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) {
                                if (activeTabId == selectedTab.id) progress = newProgress
                            }

                            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                                fullscreenController.show(view, callback)
                            }

                            override fun onHideCustomView() {
                                fullscreenController.hide()
                            }
                        }
                        loadUrl("about:blank")
                    }
                    (view.parent as? ViewGroup)?.removeView(view)
                    webViews[activeTabId] = view
                    view
                },
                update = { view ->
                    view.settings.javaScriptEnabled = enableJavaScript
                    if (selectedTab.url.startsWith("https://") && view.url != selectedTab.url) {
                        view.loadUrl(selectedTab.url)
                    }
                    canGoBack = view.canGoBack()
                    canGoForward = view.canGoForward()
                }
                )
            }
        }
    }

    if (dialog == "bookmarks" || dialog == "history") {
        val pages = if (dialog == "bookmarks") bookmarks else history
        AlertDialog(
            onDismissRequest = { dialog = "" },
            title = { Text(if (dialog == "bookmarks") "บุ๊กมาร์ก" else "ประวัติการเข้าชม") },
            text = {
                LazyColumn {
                    items(pages) { page ->
                        TextButton(
                            onClick = {
                                navigate(page.url)
                                dialog = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(page.title.ifBlank { page.url }, maxLines = 1) }
                    }
                    if (pages.isEmpty()) item { Text("ยังไม่มีรายการ") }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (dialog == "history") {
                        history.clear()
                        saveHistory()
                    }
                    dialog = ""
                }) { Text(if (dialog == "history") "ล้างประวัติ" else "ปิด") }
            }
        )
    }

    if (dialog == "cookies") {
        AlertDialog(
            onDismissRequest = { dialog = "" },
            title = { Text("ล้าง Cookie และ Session") },
            text = { Text("การดำเนินการนี้จะออกจากระบบเว็บไซต์ที่เคยเข้าสู่ระบบไว้") },
            confirmButton = {
                TextButton(onClick = {
                    CookieManager.getInstance().removeAllCookies {
                        CookieManager.getInstance().flush()
                        privacyMessage = "ล้าง Cookie แล้ว"
                    }
                    dialog = ""
                }) { Text("ยืนยันล้าง") }
            },
            dismissButton = { TextButton(onClick = { dialog = "" }) { Text("ยกเลิก") } }
        )
    }

    if (downloadMessage.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { downloadMessage = "" },
            text = { Text(downloadMessage) },
            confirmButton = { TextButton(onClick = { downloadMessage = "" }) { Text("ตกลง") } }
        )
    }

    if (privacyMessage.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { privacyMessage = "" },
            text = { Text(privacyMessage) },
            confirmButton = { TextButton(onClick = { privacyMessage = "" }) { Text("ตกลง") } }
        )
    }
}

private fun isDirectMediaUrl(uri: Uri): Boolean {
    val path = uri.path.orEmpty().lowercase()
    return DIRECT_MEDIA_EXTENSIONS.any { path.endsWith(".$it") }
}

private val DIRECT_MEDIA_EXTENSIONS = setOf(
    "mp4", "m4v", "mkv", "webm", "mov", "avi", "3gp", "ts",
    "mp3", "m4a", "aac", "flac", "ogg", "opus", "wav"
)

private fun safeStoredUrl(value: String): String? = runCatching {
    val parsed = URI(value)
    require(parsed.scheme.equals("https", ignoreCase = true))
    require(!parsed.host.isNullOrBlank() && parsed.rawUserInfo == null)
    URI(parsed.scheme, null, parsed.host, parsed.port, parsed.path, null, null).toASCIIString()
}.getOrNull()

private fun encodeSavedPages(pages: List<SavedPage>): String {
    val array = JSONArray()
    pages.forEach { page ->
        array.put(JSONObject().put("title", page.title.take(120)).put("url", page.url))
    }
    return array.toString()
}

private fun readSavedPages(serialized: String?): List<SavedPage> = runCatching {
    val array = JSONArray(serialized ?: "[]")
    buildList {
        for (index in 0 until minOf(array.length(), MAX_HISTORY)) {
            val item = array.optJSONObject(index) ?: continue
            val url = item.optString("url")
            val safeUrl = safeStoredUrl(url) ?: continue
            add(SavedPage(item.optString("title").take(120), safeUrl))
        }
    }
}.getOrDefault(emptyList())

private const val KEY_BOOKMARKS = "bookmarks"
private const val KEY_HISTORY = "history"
private const val MAX_TABS = 8
private const val MAX_BOOKMARKS = 100
private const val MAX_HISTORY = 300
