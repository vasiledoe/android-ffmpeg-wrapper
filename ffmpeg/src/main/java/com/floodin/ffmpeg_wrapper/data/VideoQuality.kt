package com.floodin.ffmpeg_wrapper.data

data class VideoResolution(
    val width: Int,
    val height: Int
)

fun VideoResolution.orBetter(otherResolution: VideoResolution?): VideoResolution {
    otherResolution ?: return this
    val isMinBigger = minOf(width, height) < minOf(otherResolution.width, otherResolution.height)
    val isMaxBigger = maxOf(width, height) < maxOf(otherResolution.width, otherResolution.height)
    return if (isMinBigger && isMaxBigger) {
        otherResolution
    } else {
        this
    }
}

fun VideoResolution.isBetterThenHD(): Boolean {
    val min = minOf(height, width)
    return min > VideoQuality.HD.value
}

enum class VideoQuality(val value: Int) {
    HD(720),
    FHD(1080)
}