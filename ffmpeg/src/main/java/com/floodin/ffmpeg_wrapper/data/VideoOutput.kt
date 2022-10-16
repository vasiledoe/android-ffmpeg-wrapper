package com.floodin.ffmpeg_wrapper.data

import android.net.Uri

data class VideoOutput(
    val id: String,
    val uri: Uri,
    val absolutePath: String,
    val size: Long
)