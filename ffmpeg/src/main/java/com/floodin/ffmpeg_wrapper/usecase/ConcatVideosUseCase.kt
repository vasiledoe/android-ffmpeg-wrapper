package com.floodin.ffmpeg_wrapper.usecase

import com.floodin.ffmpeg_wrapper.data.FFmpegResult
import com.floodin.ffmpeg_wrapper.data.VideoFormat
import com.floodin.ffmpeg_wrapper.data.VideoInput
import com.floodin.ffmpeg_wrapper.repo.CalculateMaxDurationRepo
import com.floodin.ffmpeg_wrapper.repo.CompressVideoRepo
import com.floodin.ffmpeg_wrapper.repo.ConcatVideosRepo
import com.floodin.ffmpeg_wrapper.util.MyLogs

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
     * @return result compressed concat video
     */
    fun executeSync(
        inputVideos: List<VideoInput>,
        format: VideoFormat,
        duration: Float = DEF_MAX_VIDEO_DURATION.toFloat(),
        appId: String,
        appName: String
    ): FFmpegResult {
        val videoInputsWithMaxDuration = calculateMaxDurationRepo.execute(inputVideos, duration)
        val compressVireoResults = videoInputsWithMaxDuration.map {
            val result = compressVideoRepo.execute(
                inputVideo = it.key,
                format = format,
                duration = it.value,
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
            inputPaths = compressedVideoPaths,
            appId = appId,
            appName = appName
        )
    }


    companion object {
        const val DEF_MAX_VIDEO_DURATION = 10 * 60 // 10 min
        const val DEF_MAX_COMPRESSED_VIDEO_DURATION = 5 * 60 // 5 min
    }
}