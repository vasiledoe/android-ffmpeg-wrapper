package com.floodin.ffmpeg_wrapper.usecase

import com.floodin.ffmpeg_wrapper.data.FFmpegResult
import com.floodin.ffmpeg_wrapper.data.VideoFormat
import com.floodin.ffmpeg_wrapper.data.VideoInput
import com.floodin.ffmpeg_wrapper.repo.CalculateMaxDurationRepo
import com.floodin.ffmpeg_wrapper.repo.CompressVideoRepo
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
     * @param inputVideo - input video file meta
     * @param format - desired video resolution
     * @param appId - application ID
     * @param appName - application Name
     * @return list of result compressed videos
     */
    fun executeSync(
        inputVideo: VideoInput,
        format: VideoFormat,
        duration: Float = ConcatVideosUseCase.DEF_MAX_COMPRESSED_VIDEO_DURATION.toFloat(),
        appId: String,
        appName: String
    ): FFmpegResult = compressVideoRepo.execute(
        inputVideo = inputVideo,
        format = format,
        duration = calculateMaxDurationRepo.execute(inputVideo, duration),
        appId = appId,
        appName = appName
    )

    /**
     * Apply compress to multiple videos
     *
     * @param inputVideos - input video file metas
     * @param format - desired video resolution
     * @param appId - application ID
     * @param appName - application Name
     * @return list of result compressed videos
     */
    suspend fun executeSync(
        inputVideos: List<VideoInput>,
        format: VideoFormat,
        appId: String,
        appName: String
    ): List<FFmpegResult> = withContext(Dispatchers.IO) {
        val compressedVideoTasks = inputVideos.map {
            async {
                compressVideoRepo.execute(
                    inputVideo = it,
                    format = format,
                    appId = appId,
                    appName = appName
                )
            }
        }
        return@withContext compressedVideoTasks.awaitAll()
    }
}