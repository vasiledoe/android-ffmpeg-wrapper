package com.floodin.ffmpeg_wrapper.repo

import com.arthenica.ffmpegkit.FFprobeKit
import com.floodin.ffmpeg_wrapper.data.VideoInput
import com.floodin.ffmpeg_wrapper.util.MyLogs

class CalculateMaxDurationRepo {

    /**
     * Determine max seconds from each video
     *
     * @param inputVideos - input video file metas
     * @param maxDuration - final video duration cannot exceed this limit
     * @return map<absolute path, amount of seconds>
     */
    fun execute(
        inputVideos: List<VideoInput>,
        maxDuration: Float
    ): Map<VideoInput, Float> {
        val inputDurationMeta = mutableMapOf<VideoInput, Float>()
        inputVideos.forEach { videoInput ->
            inputDurationMeta[videoInput] = videoDuration(videoInput.absolutePath)
        }
        MyLogs.LOG("CalculateMaxDurationRepo", "execute", "inputDurationMeta: $inputDurationMeta")
        val anyVideoMaxDuration = maxVideoDuration(inputDurationMeta, maxDuration)
        val inputDurationWithDurationMeta = mutableMapOf<VideoInput, Float>()
        inputDurationMeta.forEach { (videoInput, originalDuration) ->
            if (originalDuration <= anyVideoMaxDuration) {
                inputDurationWithDurationMeta[videoInput] = originalDuration
            } else {
                inputDurationWithDurationMeta[videoInput] = anyVideoMaxDuration
            }
        }
        MyLogs.LOG(
            "CalculateMaxDurationRepo",
            "execute",
            "inputDurationWithDurationMeta: $inputDurationWithDurationMeta"
        )
        return inputDurationWithDurationMeta
    }

    /**
     * Determine max seconds from each video
     *
     * @param inputVideo - input video file metas
     * @param maxDuration - final video duration cannot exceed this limit
     * @return map<absolute path, amount of seconds>
     */
    fun execute(
        inputVideo: VideoInput,
        maxDuration: Float
    ): Float? {
        val inputDuration = videoDuration(inputVideo.absolutePath)
        return if (inputDuration == 0f) {
            MyLogs.LOG(
                "CalculateMaxDurationRepo",
                "execute",
                "failed to get video duration - return null"
            )
            return null
        } else if (inputDuration > maxDuration) {
            MyLogs.LOG(
                "CalculateMaxDurationRepo",
                "execute",
                "inputDuration is too big, return maxDuration: $maxDuration"
            )
            maxDuration
        } else {
            MyLogs.LOG(
                "CalculateMaxDurationRepo",
                "execute",
                "inputDuration is shorter, return inputDuration: $inputDuration"
            )
            inputDuration
        }
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

    private fun videoDuration(inputPath: String): Float {
        val mediaInformation = FFprobeKit.getMediaInformation(inputPath)
        val duration = mediaInformation?.mediaInformation?.duration?.toFloat()
        MyLogs.LOG(
            "CalculateMaxDurationRepo",
            "videoDuration",
            "inputPath:$inputPath duration:$duration"
        )
        return duration ?: 0f
    }
}