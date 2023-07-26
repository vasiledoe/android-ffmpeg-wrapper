package com.floodin.ffmpeg_wrapper.data

import com.floodin.ffmpeg_wrapper.repo.CompressVideoRepo.Companion.SECTION_DURATION

data class VideoSplittingMeta(
    val inputDuration: Float,
    val sectionDuration: Float = SECTION_DURATION
)