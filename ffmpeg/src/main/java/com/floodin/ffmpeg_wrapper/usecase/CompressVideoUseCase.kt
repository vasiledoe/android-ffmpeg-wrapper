package com.floodin.ffmpeg_wrapper.usecase

import com.floodin.ffmpeg_wrapper.data.FFmpegResult
import com.floodin.ffmpeg_wrapper.data.VideoFormat
import com.floodin.ffmpeg_wrapper.data.VideoInput
import com.floodin.ffmpeg_wrapper.repo.CalculateMaxDurationRepo
import com.floodin.ffmpeg_wrapper.repo.CompressVideoRepo
import com.floodin.ffmpeg_wrapper.usecase.ConcatVideosUseCase.Companion.DEF_MAX_VIDEO_DURATION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

class CompressVideoUseCase(
    private val calculateMaxDurationRepo: CalculateMaxDurationRepo,
    private val compressVideoRepo: CompressVideoRepo
) {

    /**
     * Apply compress to multiple videos
     *
     * @param inputVideos - input video file metas
     * @param format - desired video resolution
     * @param duration - desired file duration to take in consideration for final compressed video
     * @param appId - application ID
     * @param appName - application Name
     * @return list of result compressed videos
     */
    suspend fun executeSync(
        inputVideos: List<VideoInput>,
        format: VideoFormat,
        duration: Float = DEF_MAX_VIDEO_DURATION.toFloat(),
        appId: String,
        appName: String
    ): List<FFmpegResult> = withContext(Dispatchers.IO) {
        val pathsWithMaxDuration = calculateMaxDurationRepo.execute(inputVideos, duration)
        val compressedVideoTasks = pathsWithMaxDuration.map {
            async {
                compressVideoRepo.execute(
                    inputVideo = it.key,
                    format = format,
                    duration = it.value,
                    appId = appId,
                    appName = appName
                )
            }
        }
        return@withContext compressedVideoTasks.awaitAll()
    }
}