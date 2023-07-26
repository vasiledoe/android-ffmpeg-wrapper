package com.floodin.ffmpeg_wrapper.repo

import com.floodin.ffmpeg_wrapper.data.VideoInput
import com.floodin.ffmpeg_wrapper.data.VideoInputWithMaxDuration
import com.floodin.ffmpeg_wrapper.util.MyLogs
import kotlin.math.min

class CalculateMaxDurationRepo(
    private val mediaInfoRepo: MediaInfoRepo
) {

    /**
     * Determine max seconds
     *
     * @param inputVideo - input video file meta
     * @param maxDuration - target video duration cannot exceed this limit
     * @return video input item with original and target duration or null in case we cannot get input duration
     */
    fun execute(
        inputVideo: VideoInput,
        maxDuration: Float
    ): VideoInputWithMaxDuration? {
        val inputDuration = mediaInfoRepo.getVideoDuration(inputVideo.absolutePath)
        return if (inputDuration > 0) {
            VideoInputWithMaxDuration(
                inputVideo = inputVideo,
                inputDuration = inputDuration,
                targetDuration = min(inputDuration, maxDuration)
            )
        } else {
            MyLogs.LOG(
                "CalculateMaxDurationRepo",
                "execute",
                "failed to get video duration - return null so we'll have no duration limitation for compression"
            )
            null
        }
    }

    /**
     * Determine max seconds from each video
     *
     * @param inputVideos - input video file metas
     * @param maxDuration - final video duration cannot exceed this limit
     * @return list of video input items with original and target durations ONLY for videos that has input duration
     */
    fun execute(
        inputVideos: List<VideoInput>,
        maxDuration: Float
    ): List<VideoInputWithMaxDuration> {
        val inputDurationMeta = mutableMapOf<VideoInput, Float>()
        inputVideos.forEach { videoInput ->
            inputDurationMeta[videoInput] = mediaInfoRepo.getVideoDuration(videoInput.absolutePath)
        }
        MyLogs.LOG("CalculateMaxDurationRepo", "execute", "inputDurationMeta: $inputDurationMeta")
        val list = mutableListOf<VideoInputWithMaxDuration>()
        val anyVideoMaxDuration = maxVideoDuration(inputDurationMeta, maxDuration)
        inputDurationMeta.forEach { (videoInput, inputDuration) ->
            if (inputDuration > 0) {
                list.add(
                    VideoInputWithMaxDuration(
                        inputVideo = videoInput,
                        inputDuration = inputDuration,
                        targetDuration = min(inputDuration, anyVideoMaxDuration)
                    )
                )
            } else {
                MyLogs.LOG(
                    "CalculateMaxDurationRepo",
                    "execute",
                    "Failed to get video duration - ignore this video:$videoInput"
                )
            }
        }
        MyLogs.LOG(
            "CalculateMaxDurationRepo",
            "execute",
            "list: $list"
        )
        return list
    }

    private fun maxVideoDuration(
        inputDurationMeta: Map<VideoInput, Float>,
        maxOutputDuration: Float,
        maxSingleVideoDuration: Float = maxOutputDuration / inputDurationMeta.size
    ): Float {
        val longerVideoCount = inputDurationMeta.count { it.value > maxSingleVideoDuration }
        // in case all videos are shorter then maxSingleVideoDuration
        if (longerVideoCount == 0) {
            return maxSingleVideoDuration
        }

        val shorterVideos = inputDurationMeta.filter { it.value <= maxSingleVideoDuration }
        val shorterVideoCount = shorterVideos.size
        val shorterVideoTotalDuration = shorterVideos.values.sum()

        val remainingDuration = maxOutputDuration - shorterVideoTotalDuration
        val newMaxDuration = remainingDuration / longerVideoCount
        val areAllShorterVideosReallyShorter = shorterVideos.all { it.value <= newMaxDuration }

        // all videos are longer or
        // only one video has a bit more then others or
        // all initially considered shorter videos are NO longer then new duration for long videos
        return if (shorterVideoCount == 0 || longerVideoCount == 1 || areAllShorterVideosReallyShorter) {
            MyLogs.LOG(
                "CalculateMaxDurationRepo",
                "maxVideoDuration",
                "return maxVideoDuration: $newMaxDuration"
            )
            newMaxDuration
        } else {
            MyLogs.LOG(
                "CalculateMaxDurationRepo",
                "maxVideoDuration",
                "reiterate because longerVideoCount: $longerVideoCount for newMaxDuration: $newMaxDuration"
            )
            maxVideoDuration(inputDurationMeta, maxOutputDuration, newMaxDuration)
        }
    }
}