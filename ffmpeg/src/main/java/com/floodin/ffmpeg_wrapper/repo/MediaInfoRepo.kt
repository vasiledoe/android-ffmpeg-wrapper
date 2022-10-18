package com.floodin.ffmpeg_wrapper.repo

import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.StreamInformation
import com.floodin.ffmpeg_wrapper.data.VideoMeta
import com.floodin.ffmpeg_wrapper.util.MyLogs
import com.google.gson.Gson
import kotlin.math.abs

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
            val rotation = getDetectedVideoRotation(data)
            MyLogs.LOG(
                "MediaInfoRepo",
                "isVideoInPortrait",
                "inputPath:$inputPath width:$width height:$height rotation:$rotation"
            )
            (height > width) || abs(rotation) > 0
        } ?: run {
            MyLogs.LOG(
                "MediaInfoRepo",
                "isVideoInPortrait",
                "ERR failed to get resolution for inputPath:$inputPath"
            )
            false
        }
    }

    private fun getDetectedVideoRotation(data: StreamInformation): Int {
        val sideDataJson: String? = data.getStringProperty("side_data_list")?.toString()
        sideDataJson ?: return 0
        val sideData: VideoMeta? = Gson().fromJson(
            sideDataJson,
            Array<VideoMeta>::class.java
        )?.firstOrNull()
        return sideData?.rotation ?: 0
    }
}