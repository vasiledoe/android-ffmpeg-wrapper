package com.floodin.ffmpeg_wrapper.usecase

import com.floodin.ffmpeg_wrapper.data.*
import com.floodin.ffmpeg_wrapper.repo.CalculateMaxDurationRepo
import com.floodin.ffmpeg_wrapper.repo.CompressVideoRepo
import com.floodin.ffmpeg_wrapper.repo.MediaInfoRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

class CompressVideoUseCase(
    private val calculateMaxDurationRepo: CalculateMaxDurationRepo,
    private val compressVideoRepo: CompressVideoRepo,
    private val mediaInfoRepo: MediaInfoRepo
) {

    /**
     * Apply compress to multiple videos
     *
     * @param inputVideo - input video file meta
     * @param resolution - desired video resolution
     * @param appId - application ID
     * @param appName - application Name
     * @return list of result compressed videos
     */
    fun executeSync(
        inputVideo: VideoInput,
        resolution: VideoResolution,
        duration: Float = DEF_MAX_COMPRESS_OUTPUT_VIDEO_DURATION.toFloat(),
        appId: String,
        appName: String
    ): FFmpegResult {
        val isPortrait = mediaInfoRepo.isVideoInPortrait(inputVideo.absolutePath)
        val inputVideoResolution = mediaInfoRepo.getVideoResolution(inputVideo.absolutePath)

        return compressVideoRepo.execute(
            inputVideo = inputVideo,
            resolution = resolution.orBetter(inputVideoResolution),
            duration = calculateMaxDurationRepo.execute(inputVideo, duration),
            isPortrait = isPortrait,
            appId = appId,
            appName = appName
        )
    }

    /**
     * Apply compress to multiple videos
     *
     * @param inputVideos - input video file metas
     * @param resolution - desired video resolution
     * @param appId - application ID
     * @param appName - application Name
     * @return list of result compressed videos
     */
    suspend fun executeSync(
        inputVideos: List<VideoInput>,
        resolution: VideoResolution,
        appId: String,
        appName: String
    ): List<FFmpegResult> = withContext(Dispatchers.IO) {
        val compressedVideoTasks = inputVideos.map { inputVideo ->
            async {
                val isPortrait = mediaInfoRepo.isVideoInPortrait(inputVideo.absolutePath)
                compressVideoRepo.execute(
                    inputVideo = inputVideo,
                    resolution = resolution,
                    isPortrait = isPortrait,
                    appId = appId,
                    appName = appName
                )
            }
        }
        return@withContext compressedVideoTasks.awaitAll()
    }


    companion object {
        const val DEF_MAX_COMPRESS_OUTPUT_VIDEO_DURATION = 5 * 60 // 5 min
    }
}