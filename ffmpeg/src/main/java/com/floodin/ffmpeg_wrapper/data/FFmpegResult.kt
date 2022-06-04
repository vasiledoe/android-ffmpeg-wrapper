package com.floodin.ffmpeg_wrapper.data

import android.net.Uri

sealed class FFmpegResult {
    data class Successful(
        var outputPath: String,
        var outputUri: Uri
    ) : FFmpegResult()

    object Cancel : FFmpegResult()

    object Error : FFmpegResult()
}