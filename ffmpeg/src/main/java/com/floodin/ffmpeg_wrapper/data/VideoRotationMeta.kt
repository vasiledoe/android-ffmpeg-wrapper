package com.floodin.ffmpeg_wrapper.data

data class VideoRotationMeta(
    val rotation: Int,
    val isVerticalByRotation: Boolean,
    val isHeightBiggerThenWidth: Boolean
)

fun Int.isVerticalByRotation(): Boolean {
    return when (this) {
        90, 270 -> true
        else -> false
    }
}

fun VideoRotationMeta.toOrientation(): VideoOrientationMeta {
    return if (isVerticalByRotation || isHeightBiggerThenWidth) {
        VideoOrientationMeta.VERTICAL
    } else {
        VideoOrientationMeta.HORIZONTAL
    }
}