package com.floodin.ffmpeg_wrapper.data

enum class VideoOrientation {
    HORIZONTAL,
    VERTICAL
}

fun VideoOrientation.isVertical() = this == VideoOrientation.VERTICAL

fun VideoOrientation.isHorizontal() = this == VideoOrientation.HORIZONTAL