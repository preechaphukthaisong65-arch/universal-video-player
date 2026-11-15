package com.preechaphukthaisong65.universalvideoplayer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle

class ShareActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val incoming = intent
        val launch = Intent(this, MainActivity::class.java).apply {
            action = incoming.action
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            when (incoming.action) {
                Intent.ACTION_VIEW -> data = incoming.data
                Intent.ACTION_SEND -> {
                    val stream = incoming.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    putExtra(Intent.EXTRA_STREAM, stream)
                }
                Intent.ACTION_SEND_MULTIPLE -> {
                    val streams = incoming.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, streams)
                }
            }
        }
        startActivity(launch)
        finish()
    }
}
