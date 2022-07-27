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
        val shorterVideoTotalDuration = inputDurationMeta.filter {
            it.value <= maxSingleVideoDuration
        }.values.sum()
        val remainingDuration = maxOutputDuration - shorterVideoTotalDuration
        val newMaxDuration = remainingDuration / longerVideoCount
        return if (longerVideoCount <= 1 || shorterVideoTotalDuration == 0f) {
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