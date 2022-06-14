package com.floodin.ffmpeg_wrapper.usecase

import android.net.Uri
import com.floodin.ffmpeg_wrapper.data.FFmpegResult
import com.floodin.ffmpeg_wrapper.data.VideoFormat
import com.floodin.ffmpeg_wrapper.repo.CalculateMaxDurationRepo
import com.floodin.ffmpeg_wrapper.repo.CompressVideoRepo
import com.floodin.ffmpeg_wrapper.usecase.ConcatVideosUseCase.Companion.DEF_MAX_VIDEO_DURATION

class CompressVideoUseCase(
    private val calculateMaxDurationRepo: CalculateMaxDurationRepo,
    private val compressVideoRepo: CompressVideoRepo
) {

    /**
     * Apply compress only to a video
     *
     * @param inputPath - file absolute path
     * @param format - desired video resolution
     * @param duration - desired file duration to take in consideration for final compressed video
     * @param appId - application ID
     * @param appName - application Name
     * @param onSuccessCallback - callback to return new compressed file Uri and absolute path
     * @param onProgressCallback - todo
     * @param onErrorCallback - callback to return error
     */
    fun execute(
        inputPath: String,
        format: VideoFormat,
        duration: Float = DEF_MAX_VIDEO_DURATION.toFloat(),
        appId: String,
        appName: String,
        onSuccessCallback: (Uri, String) -> Unit,
        onProgressCallback: (String) -> Unit,
        onErrorCallback: (String) -> Unit
    ) {
        val pathsWithMaxDuration = calculateMaxDurationRepo.execute(listOf(inputPath), duration)
        val compressedVideos = pathsWithMaxDuration.map {
            compressVideoRepo.execute(
                inputPath = it.key,
                format = format,
                duration = it.value,
                appId = appId,
                appName = appName
            )
        }
        if (compressedVideos.isNotEmpty()) {
            val videoCompressed = compressedVideos.first()
            if (videoCompressed is FFmpegResult.Successful) {
                onSuccessCallback(videoCompressed.outputUri, videoCompressed.outputPath)
            } else {
                onErrorCallback("Unexpected error")
            }
        } else {
            onErrorCallback("Unexpected error")
        }
    }

    /**
     * Apply compress to multiple videos
     *
     * @param inputPaths - file absolute paths
     * @param format - desired video resolution
     * @param duration - desired file duration to take in consideration for final compressed video
     * @param appId - application ID
     * @param appName - application Name
     * @return list of result <media uri, absolute path>
     */
    fun executeMultipleCompressSync(
        inputPaths: List<String>,
        format: VideoFormat,
        duration: Float = DEF_MAX_VIDEO_DURATION.toFloat(),
        appId: String,
        appName: String
    ): List<Pair<Uri, String>>? {
        val pathsWithMaxDuration = calculateMaxDurationRepo.execute(inputPaths, duration)
        val compressedVideos = pathsWithMaxDuration.mapNotNull {
            val videoCompressedResult = compressVideoRepo.execute(
                inputPath = it.key,
                format = format,
                duration = it.value,
                appId = appId,
                appName = appName
            )
            if (videoCompressedResult is FFmpegResult.Successful) {
                Pair(videoCompressedResult.outputUri, videoCompressedResult.outputPath)
            } else {
                null
            }
        }
        return compressedVideos
    }
}