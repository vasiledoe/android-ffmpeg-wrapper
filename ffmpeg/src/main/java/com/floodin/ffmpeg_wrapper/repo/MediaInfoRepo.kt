package com.floodin.ffmpeg_wrapper.repo

import com.arthenica.ffmpegkit.FFprobeKit
import com.floodin.ffmpeg_wrapper.util.MyLogs

class MediaInfoRepo {
    fun videoDuration(inputPath: String): Float {
        val mediaInformation = FFprobeKit.getMediaInformation(inputPath)
        val duration = mediaInformation?.mediaInformation?.duration?.toFloat()
        MyLogs.LOG("MediaInfoRepo", "videoDuration", "inputPath:$inputPath duration:$duration")
        return duration ?: 0f
    }

    fun isVideoInPortrait(inputPath: String): Boolean {
        val mediaInformation = FFprobeKit.getMediaInformation(inputPath)
        val videoStreamData = mediaInformation?.mediaInformation?.streams?.first {
            it.type == "video"
        }
        return videoStreamData?.let { data ->
            val width = data.width
            val height = data.height
            MyLogs.LOG(
                "MediaInfoRepo",
                "isVideoInPortrait",
                "inputPath:$inputPath width:$width height:$height"
            )
            height > width
        } ?: run {
            MyLogs.LOG(
                "MediaInfoRepo",
                "isVideoInPortrait",
                "ERR failed to get resolution for inputPath:$inputPath"
            )
            false
        }
    }
}