package com.floodin.ffmpeg_wrapper.data

data class VideoInputWithMaxDuration(
    val inputVideo: VideoInput,
    val inputDuration: Float,
    val targetDuration: Float
)