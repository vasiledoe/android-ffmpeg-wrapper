package com.floodin.ffmpeg_wrapper.data

data class VideoRotation(
    val degrees: Int,
    val isHeightBiggerThenWidth: Boolean
)

fun Int.isVerticalByRotation(): Boolean {
    return when (this) {
        90, -90, 270 -> true
        else -> false
    }
}

fun VideoRotation.toOrientation(): VideoOrientation {
    return if (degrees.isVerticalByRotation() || isHeightBiggerThenWidth) {
        VideoOrientation.VERTICAL
    } else {
        VideoOrientation.HORIZONTAL
    }
}