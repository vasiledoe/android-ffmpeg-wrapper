package com.floodin.ffmpeg_wrapper.data

enum class VideoOrientationMeta {
    HORIZONTAL,
    VERTICAL
}

fun VideoOrientationMeta.isVertical() = this == VideoOrientationMeta.VERTICAL

fun VideoOrientationMeta.isHorizontal() = this == VideoOrientationMeta.HORIZONTAL