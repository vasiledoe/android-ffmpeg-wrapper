package com.floodin.ffmpeg_wrapper.data

sealed class FFmpegResult {
    data class Success(val data: VideoOutput) : FFmpegResult()
    data class Error(val message: String) : FFmpegResult()
    object Cancel : FFmpegResult()
}