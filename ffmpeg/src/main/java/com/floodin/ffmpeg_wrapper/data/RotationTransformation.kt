package com.floodin.ffmpeg_wrapper.data

enum class RotationTransformation(val value: String) {
    NO_ROTATION("null"),
    ROTATION_90("transpose=1"),
    ROTATION_MINUS_90("transpose=2"),
    ROTATION_180("transpose=1,transpose=1")
}

fun Int.toRotationTransposeCmd(): String {
    return when (this) {
        90 -> RotationTransformation.ROTATION_90.value
        -90 -> RotationTransformation.ROTATION_MINUS_90.value
        180, -180 -> RotationTransformation.ROTATION_180.value
        else -> RotationTransformation.NO_ROTATION.value
    }
}