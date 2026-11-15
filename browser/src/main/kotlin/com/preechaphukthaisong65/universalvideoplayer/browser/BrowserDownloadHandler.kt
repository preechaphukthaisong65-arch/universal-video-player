package com.preechaphukthaisong65.universalvideoplayer.browser

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil

/** Enqueues only user-initiated HTTPS downloads. Cookies are deliberately not copied. */
class BrowserDownloadHandler(context: Context) {
    private val appContext = context.applicationContext

    fun enqueue(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?
    ): Long? = runCatching {
        val uri = Uri.parse(url)
        require(uri.scheme.equals("https", ignoreCase = true))
        require(!uri.host.isNullOrBlank() && uri.userInfo.isNullOrEmpty())

        val guessedName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        val safeName = guessedName
            .replace(Regex("[\\\\/:*?\"<>|\\r\\n]"), "_")
            .trim()
            .take(120)
            .ifBlank { "download" }

        val request = DownloadManager.Request(uri)
            .setTitle(safeName)
            .setDescription("กำลังดาวน์โหลดไฟล์")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, safeName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)

        mimeType?.takeIf(String::isNotBlank)?.let(request::setMimeType)
        userAgent?.replace(Regex("[\\r\\n]"), "")?.take(512)
            ?.takeIf(String::isNotBlank)
            ?.let { request.addRequestHeader("User-Agent", it) }

        val manager = appContext.getSystemService(DownloadManager::class.java)
            ?: error("Download service is unavailable")
        manager.enqueue(request)
    }.getOrNull()
}
