package com.floodin.ffmpeg_wrapper.data

data class VideoInput(
    val id: String,
    val absolutePath: String,
    val orientation: VideoOrientation,
    val userRotationDegrees: Int = 0
)
