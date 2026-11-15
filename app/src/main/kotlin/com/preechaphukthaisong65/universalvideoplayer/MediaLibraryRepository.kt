package com.preechaphukthaisong65.universalvideoplayer

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.preechaphukthaisong65.universalvideoplayer.domain.MediaSource
import java.util.ArrayDeque
import java.util.Locale

/**
 * Indexes only a directory explicitly selected by the user through SAF.
 * No broad storage permission or full-device scan is required.
 */
class MediaLibraryRepository(context: Context) {
    private val appContext = context.applicationContext

    fun scanFolder(treeUri: Uri): List<MediaSource> {
        val root = runCatching { DocumentFile.fromTreeUri(appContext, treeUri) }.getOrNull()
            ?: return emptyList()
        if (!root.isDirectory || !root.canRead()) return emptyList()

        val pending = ArrayDeque<Pair<DocumentFile, Int>>()
        val results = ArrayList<MediaSource>()
        pending.add(root to 0)

        while (pending.isNotEmpty() && results.size < MAX_FILES) {
            val (directory, depth) = pending.removeFirst()
            val children = runCatching { directory.listFiles() }.getOrDefault(emptyArray())
            for (child in children) {
                if (results.size >= MAX_FILES) break
                when {
                    child.isDirectory && depth < MAX_DEPTH && child.canRead() ->
                        pending.add(child to depth + 1)
                    child.isFile && child.canRead() && isSupportedMedia(child) ->
                        results.add(
                            MediaSource(
                                uri = child.uri.toString(),
                                title = child.name?.take(250) ?: "Media file",
                                mimeType = child.type
                            )
                        )
                }
            }
        }
        return results.sortedBy { it.title.orEmpty().lowercase(Locale.ROOT) }
    }

    private fun isSupportedMedia(file: DocumentFile): Boolean {
        val mime = file.type.orEmpty().lowercase(Locale.ROOT)
        if (mime.startsWith("video/") || mime.startsWith("audio/")) return true
        val extension = file.name.orEmpty().substringAfterLast('.', "").lowercase(Locale.ROOT)
        return extension in SUPPORTED_EXTENSIONS
    }

    companion object {
        private const val MAX_FILES = 5_000
        private const val MAX_DEPTH = 8
        private val SUPPORTED_EXTENSIONS = setOf(
            "mp4", "m4v", "mkv", "webm", "mov", "avi", "3gp", "ts", "mp3",
            "m4a", "aac", "flac", "ogg", "opus", "wav", "wma"
        )
    }
}
