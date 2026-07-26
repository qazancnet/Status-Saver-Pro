package com.example.model

import android.net.Uri

data class StatusItem(
    val id: String,
    val uri: Uri,
    val fileName: String,
    val isVideo: Boolean,
    val size: Long,
    val dateModified: Long,
    val durationMs: Long = 0,
    val isSaved: Boolean = false,
    val path: String = ""
)
