package com.floodin.ffmpeg_wrapper.usecase

import com.floodin.ffmpeg_wrapper.data.FFmpegResult
import com.floodin.ffmpeg_wrapper.data.VideoFormat
import com.floodin.ffmpeg_wrapper.data.VideoInput
import com.floodin.ffmpeg_wrapper.data.VideoOutput
import com.floodin.ffmpeg_wrapper.repo.CalculateMaxDurationRepo
import com.floodin.ffmpeg_wrapper.repo.CompressVideoRepo
import com.floodin.ffmpeg_wrapper.repo.ConcatVideosRepo

class ConcatVideosUseCase(
    private val calculateMaxDurationRepo: CalculateMaxDurationRepo,
    private val compressVideoRepo: CompressVideoRepo,
    private val concatVideosRepo: ConcatVideosRepo,
) {

    /**
     * Apply an algorithm of actions to cut, compress and concat a video set
     *
     * @param inputVideos - input video file metas
     * @param format - desired video resolution
     * @param duration - desired file duration to take in consideration for final compressed video
     * @param appId - application ID
     * @param appName - application Name
     * @param onSuccessCallback - callback to return new compressed file Uri and absolute path
     * @param onProgressCallback - todo
     * @param onErrorCallback - callback to return error
     */
    fun executeAsync(
        inputVideos: List<VideoInput>,
        format: VideoFormat,
        duration: Float = DEF_MAX_VIDEO_DURATION.toFloat(),
        appId: String,
        appName: String,
        onSuccessCallback: (VideoOutput) -> Unit,
        onProgressCallback: (String) -> Unit,
        onErrorCallback: (String) -> Unit
    ) {
        val videoInputsWithMaxDuration = calculateMaxDurationRepo.execute(inputVideos, duration)
        val compressedVideos = videoInputsWithMaxDuration.map {
            compressVideoRepo.execute(
                inputVideo = it.key,
                format = format,
                duration = it.value,
                appId = appId,
                appName = appName
            )
        }.filterIsInstance<FFmpegResult.Successful>().map {
            it.outputPath
        }
        if (compressedVideos.isNotEmpty()) {
            val concatVideo = concatVideosRepo.execute(
                inputPaths = compressedVideos,
                appId = appId,
                appName = appName
            )
            if (concatVideo is FFmpegResult.Successful) {
                onSuccessCallback(
                    VideoOutput(
                        id = concatVideo.inputId,
                        uri = concatVideo.outputUri,
                        absolutePath = concatVideo.outputPath
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
     * Apply an algorithm of actions to cut, compress and concat a video set
     *
     * @param inputVideos - input video file metas
     * @param format - desired video resolution
     * @param duration - desired file duration to take in consideration for final compressed video
     * @param appId - application ID
     * @param appName - application Name
     * @return result compressed concat video
     */
    fun executeSync(
        inputVideos: List<VideoInput>,
        format: VideoFormat,
        duration: Float = DEF_MAX_VIDEO_DURATION.toFloat(),
        appId: String,
        appName: String
    ): VideoOutput? {
        val videoInputsWithMaxDuration = calculateMaxDurationRepo.execute(inputVideos, duration)
        val compressedVideos = videoInputsWithMaxDuration.map {
            compressVideoRepo.execute(
                inputVideo = it.key,
                format = format,
                duration = it.value,
                appId = appId,
                appName = appName
            )
        }.filterIsInstance<FFmpegResult.Successful>().map {
            it.outputPath
        }
        if (compressedVideos.isNotEmpty()) {
            val concatVideo = concatVideosRepo.execute(
                inputPaths = compressedVideos,
                appId = appId,
                appName = appName
            )
            if (concatVideo is FFmpegResult.Successful) {
                return VideoOutput(
                    id = concatVideo.inputId,
                    uri = concatVideo.outputUri,
                    absolutePath = concatVideo.outputPath
                )
            }
        }
        return null
    }


    companion object {
        const val DEF_MAX_VIDEO_DURATION = 10 * 60 // 10 min
    }
}