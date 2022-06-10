package com.floodin.ffmpeg_wrapper.repo

import com.arthenica.ffmpegkit.FFprobeKit
import com.floodin.ffmpeg_wrapper.util.MyLogs

class CalculateMaxDurationRepo {

    /**
     * Determine max seconds from each video
     *
     * @param inputPaths - video file absolute paths
     * @param maxDuration - final video duration cannot exceed this limit
     * @return map<absolute path, amount of seconds>
     */
    fun execute(
        inputPaths: List<String>,
        maxDuration: Float
    ): Map<String, Float> {
        val inputDurationMeta = mutableMapOf<String, Float>()
        inputPaths.forEach { filePath ->
            inputDurationMeta[filePath] = videoDuration(filePath)
        }
        MyLogs.LOG("CalculateMaxDurationRepo", "execute", "inputDurationMeta: $inputDurationMeta")
        val anyVideoMaxDuration = maxVideoDuration(inputDurationMeta, maxDuration)
        val inputDurationWithDurationMeta = mutableMapOf<String, Float>()
        inputDurationMeta.forEach { (path, originalDuration) ->
            if (originalDuration <= anyVideoMaxDuration) {
                inputDurationWithDurationMeta[path] = originalDuration
            } else {
                inputDurationWithDurationMeta[path] = anyVideoMaxDuration
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
        inputDurationMeta: Map<String, Float>,
        maxOutputDuration: Float,
        maxSingleVideoDuration: Float = maxOutputDuration / inputDurationMeta.size
    ): Float {
        val longerVideoCount = inputDurationMeta.count { it.value > maxSingleVideoDuration }
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
        val info = mediaInformation.mediaInformation
        val duration = info.duration?.toFloat()
        MyLogs.LOG(
            "CalculateMaxDurationRepo",
            "videoDuration",
            "inputPath:$inputPath duration:$duration"
        )
        return duration ?: 0f
    }
}