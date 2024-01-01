package com.floodin.ffmpeg_wrapper.repo

import android.media.MediaMetadataRetriever
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.StreamInformation
import com.floodin.ffmpeg_wrapper.data.VideoMeta
import com.floodin.ffmpeg_wrapper.data.VideoResolution
import com.floodin.ffmpeg_wrapper.data.VideoRotation
import com.floodin.ffmpeg_wrapper.data.toOrientation
import com.floodin.ffmpeg_wrapper.util.MyLogs
import com.google.gson.Gson
import kotlin.math.abs


class MediaInfoRepo {

    fun getVideoDuration(inputPath: String): Float {
        val mediaInformation = FFprobeKit.getMediaInformation(inputPath)
        val duration = mediaInformation?.mediaInformation?.duration?.toFloat()
        MyLogs.LOG("MediaInfoRepo", "getVideoDuration", "inputPath:$inputPath duration:$duration")
        return duration ?: 0f
    }

    fun getVideoOrientation(inputPath: String) = getVideoRotationMeta(inputPath).toOrientation()

    fun getVideoRotationMeta(inputPath: String): VideoRotation {
        return getVideoStreamInformation(inputPath)?.let { data ->
            val width = data.width
            val height = data.height
//            val rotation = getVideoRotation(data)
            val mediaRotation = getMediaMetaRotation(inputPath)
            val meta = VideoRotation(
                degrees = mediaRotation,
                isHeightBiggerThenWidth = height > width
            )
            MyLogs.LOG(
                "MediaInfoRepo",
                "getVideoRotationMeta",
                "inputPath:$inputPath resolution:$width x $height mediaRotation:$mediaRotation meta:$meta"
            )
            meta
        } ?: run {
            MyLogs.LOG(
                "MediaInfoRepo",
                "getVideoRotationMeta",
                "ERR failed to get metadata for inputPath:$inputPath"
            )
            VideoRotation(
                degrees = 0,
                isHeightBiggerThenWidth = false
            )
        }
    }

    fun isVideoInPortrait(inputPath: String): Boolean {
        return getVideoStreamInformation(inputPath)?.let { data ->
            val width = data.width
            val height = data.height
            val rotation = getVideoRotation(data)
            MyLogs.LOG(
                "MediaInfoRepo",
                "isVideoInPortrait",
                "inputPath:$inputPath width:$width height:$height rotation:$rotation"
            )
            (height > width) || abs(rotation) == 90
        } ?: run {
            MyLogs.LOG(
                "MediaInfoRepo",
                "isVideoInPortrait",
                "ERR failed to get metadata for inputPath:$inputPath"
            )
            false
        }
    }

    fun getVideoRotation(inputPath: String): Int {
        return getVideoStreamInformation(inputPath)?.let { data ->
            getVideoRotation(data)
        } ?: 0
    }

    fun getVideoResolution(inputPath: String): VideoResolution? {
        return getVideoStreamInformation(inputPath)?.let { data ->
            val width = data.width.toInt()
            val height = data.height.toInt()
            MyLogs.LOG(
                "MediaInfoRepo",
                "getVideoResolution",
                "inputPath:$inputPath width:$width height:$height"
            )
            VideoResolution(width, height)
        } ?: run {
            MyLogs.LOG(
                "MediaInfoRepo",
                "getVideoResolution",
                "ERR failed to get metadata for inputPath:$inputPath"
            )
            null
        }
    }

    fun getVideoStreamInformation(inputPath: String): StreamInformation? {
        val mediaInformation = FFprobeKit.getMediaInformation(inputPath)
        return mediaInformation?.mediaInformation?.streams?.firstOrNull {
            it.type == "video"
        }
    }

    private fun getVideoRotation(data: StreamInformation): Int {
        val sideDataJson: String? = data.getStringProperty("side_data_list")?.toString()
        sideDataJson ?: return 0
        val sideData: VideoMeta? = Gson().fromJson(
            sideDataJson,
            Array<VideoMeta>::class.java
        )?.firstOrNull()
        return sideData?.rotation ?: 0
    }

    private fun getMediaMetaRotation(path: String): Int {
        val m = MediaMetadataRetriever()
        m.setDataSource(path)
        val rotationMeta = m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
        return rotationMeta?.toIntOrNull() ?: 0
    }
}