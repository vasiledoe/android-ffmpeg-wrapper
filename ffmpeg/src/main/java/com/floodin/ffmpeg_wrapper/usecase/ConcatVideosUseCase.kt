package com.floodin.ffmpeg_wrapper.usecase

import com.floodin.ffmpeg_wrapper.data.*
import com.floodin.ffmpeg_wrapper.repo.CalculateMaxDurationRepo
import com.floodin.ffmpeg_wrapper.repo.CompressVideoRepo
import com.floodin.ffmpeg_wrapper.repo.ConcatVideosRepo
import com.floodin.ffmpeg_wrapper.repo.MediaInfoRepo
import com.floodin.ffmpeg_wrapper.util.MyLogs

class ConcatVideosUseCase(
    private val calculateMaxDurationRepo: CalculateMaxDurationRepo,
    private val compressVideoRepo: CompressVideoRepo,
    private val concatVideosRepo: ConcatVideosRepo,
    private val mediaInfoRepo: MediaInfoRepo
) {

    /**
     * Apply an algorithm of actions to cut, compress and concat a video set
     *
     * @param inputVideos - input video file metas
     * @param inputAudio - input audio file meta
     * @param resolution - desired video resolution
     * @param duration - desired file duration to take in consideration for final compressed video
     * @param appId - application ID
     * @param appName - application Name
     * @return result compressed concat video
     */
    fun executeSync(
        inputVideos: List<VideoInput>,
        inputAudio: AudioInput?,
        resolution: VideoResolution,
        duration: Float = DEF_MAX_CONCAT_OUTPUT_VIDEO_DURATION.toFloat(),
        appId: String,
        appName: String
    ): FFmpegResult {
        val videoInputsWithMaxDuration = calculateMaxDurationRepo.execute(inputVideos, duration)
        val isPortrait = mediaInfoRepo.isVideoInPortrait(inputVideos.first().absolutePath)
        MyLogs.LOG("ConcatVideosUseCase", "executeSync", "isPortrait:$isPortrait")

        val compressVireoResults = videoInputsWithMaxDuration.map {
            val result = compressVideoRepo.execute(
                inputVideo = it.key,
                resolution = resolution,
                duration = it.value,
                isPortrait = isPortrait,
                appId = appId,
                appName = appName
            )

            // if any video failed to compress then just return error
            if (result !is FFmpegResult.Success) {
                MyLogs.LOG(
                    "ConcatVideosUseCase",
                    "executeSync",
                    "found error while compressing - result:$result"
                )
                return result
            }

            MyLogs.LOG(
                "ConcatVideosUseCase",
                "executeSync",
                "map processed:$result"
            )
            result
        }

        val compressedVideoPaths = compressVireoResults.map {
            it.data.absolutePath
        }

        MyLogs.LOG(
            "ConcatVideosUseCase",
            "executeSync",
            "return final result"
        )
        return concatVideosRepo.execute(
            videoInputFilePaths = compressedVideoPaths,
            audioInput = inputAudio,
            appId = appId,
            appName = appName
        )
    }


    companion object {
        const val DEF_MAX_CONCAT_OUTPUT_VIDEO_DURATION = 10 * 60 // 10 min
    }
}