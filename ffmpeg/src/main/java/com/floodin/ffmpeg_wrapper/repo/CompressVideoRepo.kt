package com.floodin.ffmpeg_wrapper.repo

import com.floodin.ffmpeg_wrapper.data.FFmpegResult
import com.floodin.ffmpeg_wrapper.data.VideoFormat
import com.floodin.ffmpeg_wrapper.util.FfmpegCommandUtil
import com.floodin.ffmpeg_wrapper.util.FileUtil
import com.floodin.ffmpeg_wrapper.util.MyLogs


class CompressVideoRepo(
    private val fileUtil: FileUtil,
    private val cmdUtil: FfmpegCommandUtil
) {

    /**
     * Compress video and generate new one
     *
     * @param inputPath - file absolute path
     * @param format - desired video resolution
     * @param duration - desired file duration to take in consideration for final compressed video
     * @param appId - application ID
     * @param appName - application Name
     * @return result of ffmpeg command
     */
    fun execute(
        inputPath: String,
        format: VideoFormat,
        duration: Float?,
        appId: String,
        appName: String
    ): FFmpegResult {
        val outputFile = fileUtil.getNewLocalCacheFile(
            appName = appName,
            COMPRESSED_DIR_NAME,
            "${System.currentTimeMillis()}.mp4"
        )
        val command = generateCommand(
            inputPath = inputPath,
            outputPath = outputFile.absolutePath,
            format = format,
            duration = duration
        )
        MyLogs.LOG("CompressVideoRepo", "compressVideo", "command: $command")
        return cmdUtil.executeSync(
            command,
            outputFile,
            appId
        )
    }

    private fun generateCommand(
        inputPath: String,
        outputPath: String,
        format: VideoFormat,
        duration: Float?
    ): String {
        //ffmpeg -i input.mp4 -c:a copy -c:v vp9 -b:v 1M output.mp4
        //ffmpeg -i input.mp4 -c:a copy -s hd720 output.mp4
        //ffmpeg -y -i /source-path/input.mp4 -s 480x320 -r 25 -vcodec mpeg4 -b:v 300k -b:a 48000 -ac 2 -ar 22050 /source/output.mp4

        return if (duration != null) {
            "-y -i $inputPath -s ${format.value} -r 30 -vcodec mpeg4 -b:v 300k -b:a 48000 -ac 2 -ar 22050 -ss 0 -t $duration $outputPath"
        } else {
            "-y -i $inputPath -s ${format.value} -r 30 -vcodec mpeg4 -b:v 300k -b:a 48000 -ac 2 -ar 22050 $outputPath"
        }
    }


    companion object {
        const val COMPRESSED_DIR_NAME = "compressed"
    }
}