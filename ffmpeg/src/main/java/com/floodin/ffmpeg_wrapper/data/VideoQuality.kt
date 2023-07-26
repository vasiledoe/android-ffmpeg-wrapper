package com.floodin.ffmpeg_wrapper.data

data class VideoResolution(
    val width: Int,
    val height: Int
)

enum class VideoQuality(val value: Int) {
    HD(720),
    FHD(1080)
}

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

fun VideoResolution.orWorse(otherResolution: VideoResolution?): VideoResolution {
    otherResolution ?: return this
    val isMinSmaller =
        minOf(width, height) > minOf(otherResolution.width, otherResolution.height)
    val isMaxSmaller =
        maxOf(width, height) > maxOf(otherResolution.width, otherResolution.height)
    return if (isMinSmaller && isMaxSmaller) {
        otherResolution
    } else {
        this
    }
}

fun VideoResolution?.isUndefined(): Boolean {
    return this == null || height <= 0 || width <= 0
}

fun VideoResolution.isBetterThanHD(): Boolean {
    val min = minOf(height, width)
    return min > VideoQuality.HD.value
}

fun VideoResolution.toCompressedWidth(orientation: VideoOrientationMeta?): Int {
    return if (orientation?.isVertical() == true) {
        height
    } else {
        width
    }
}

fun VideoResolution.toCompressedHeight(orientation: VideoOrientationMeta?): Int {
    return if (orientation?.isVertical() == true) {
        width
    } else {
        height
    }
}