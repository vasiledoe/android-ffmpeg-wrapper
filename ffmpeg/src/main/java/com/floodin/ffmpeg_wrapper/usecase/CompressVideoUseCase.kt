package com.floodin.ffmpeg_wrapper.usecase

import com.floodin.ffmpeg_wrapper.data.FFmpegResult
import com.floodin.ffmpeg_wrapper.data.VideoFormat
import com.floodin.ffmpeg_wrapper.data.VideoInput
import com.floodin.ffmpeg_wrapper.data.VideoOutput
import com.floodin.ffmpeg_wrapper.repo.CalculateMaxDurationRepo
import com.floodin.ffmpeg_wrapper.repo.CompressVideoRepo
import com.floodin.ffmpeg_wrapper.usecase.ConcatVideosUseCase.Companion.DEF_MAX_VIDEO_DURATION

class CompressVideoUseCase(
    private val calculateMaxDurationRepo: CalculateMaxDurationRepo,
    private val compressVideoRepo: CompressVideoRepo
) {

    /**
     * Apply compress only to a video
     *
     * @param inputVideo - input video file meta
     * @param format - desired video resolution
     * @param duration - desired file duration to take in consideration for final compressed video
     * @param appId - application ID
     * @param appName - application Name
     * @param onSuccessCallback - callback to return new compressed file Uri and absolute path
     * @param onProgressCallback - todo
     * @param onErrorCallback - callback to return error
     */
    fun execute(
        inputVideo: VideoInput,
        format: VideoFormat,
        duration: Float = DEF_MAX_VIDEO_DURATION.toFloat(),
        appId: String,
        appName: String,
        onSuccessCallback: (VideoOutput) -> Unit,
        onProgressCallback: (String) -> Unit,
        onErrorCallback: (String) -> Unit
    ) {
        val pathsWithMaxDuration = calculateMaxDurationRepo.execute(listOf(inputVideo), duration)
        val compressedVideos = pathsWithMaxDuration.map {
            compressVideoRepo.execute(
                inputVideo = it.key,
                format = format,
                duration = it.value,
                appId = appId,
                appName = appName
            )
        }
        if (compressedVideos.isNotEmpty()) {
            val videoCompressed = compressedVideos.first()
            if (videoCompressed is FFmpegResult.Successful) {
                onSuccessCallback(
                    VideoOutput(
                        id = videoCompressed.inputId,
                        uri = videoCompressed.outputUri,
                        absolutePath = videoCompressed.outputPath
                    )
                )
            } else {
                onErrorCallback("Unexpected error")
            }
        } else {
            onErrorCallback("Unexpected error")
        }
    }

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
    fun executeMultipleCompressSync(
        inputVideos: List<VideoInput>,
        format: VideoFormat,
        duration: Float = DEF_MAX_VIDEO_DURATION.toFloat(),
        appId: String,
        appName: String
    ): List<VideoOutput> {
        val pathsWithMaxDuration = calculateMaxDurationRepo.execute(inputVideos, duration)
        val compressedVideos = pathsWithMaxDuration.mapNotNull {
            val videoCompressedResult = compressVideoRepo.execute(
                inputVideo = it.key,
                format = format,
                duration = it.value,
                appId = appId,
                appName = appName
            )
            if (videoCompressedResult is FFmpegResult.Successful) {
                VideoOutput(
                    id = videoCompressedResult.inputId,
                    uri = videoCompressedResult.outputUri,
                    absolutePath = videoCompressedResult.outputPath
                )
            } else {
                null
            }
        }
        return compressedVideos
    }
}