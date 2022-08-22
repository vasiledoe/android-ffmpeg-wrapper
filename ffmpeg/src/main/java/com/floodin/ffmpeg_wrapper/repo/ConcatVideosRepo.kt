package com.floodin.ffmpeg_wrapper.repo

import com.floodin.ffmpeg_wrapper.data.AudioInput
import com.floodin.ffmpeg_wrapper.data.FFmpegResult
import com.floodin.ffmpeg_wrapper.util.FfmpegCommandUtil
import com.floodin.ffmpeg_wrapper.util.FileUtil
import com.floodin.ffmpeg_wrapper.util.MyLogs
import java.io.*

class ConcatVideosRepo(
    private val fileUtil: FileUtil,
    private val cmdUtil: FfmpegCommandUtil
) {

    /**
     * Concat videos and generate new one
     *
     * @param videoInputFilePaths - video file absolute paths
     * @param audioInput - audio file absolute path
     * @param appId - application ID
     * @param appName - application Name
     * @return result of ffmpeg command
     */
    fun execute(
        videoInputFilePaths: List<String>,
        audioInput: AudioInput?,
        appId: String,
        appName: String
    ): FFmpegResult {
        val videoOutputFile = fileUtil.getNewLocalFile(
            appName = appName,
            customDirName = CONCAT_DIR_NAME,
            fileName = "${System.currentTimeMillis()}.mp4"
        )
        MyLogs.LOG(
            "ConcatVideosRepo",
            "execute",
            "videoInputFilePaths:$videoInputFilePaths audioInput: $audioInput videoOutputFile: ${videoOutputFile.absolutePath}"
        )
        val command = generateCommand(
            videoInputFilePaths = videoInputFilePaths,
            audioInput = audioInput,
            videoOutputFilePath = videoOutputFile.absolutePath,
            appName = appName
        )
        MyLogs.LOG("ConcatVideosRepo", "execute", "command: $command")
        return cmdUtil.executeSync(
            "concatId",
            command,
            videoOutputFile,
            appId
        )
    }

    private fun generateCommand(
        videoInputFilePaths: List<String>,
        audioInput: AudioInput?,
        videoOutputFilePath: String,
        appName: String
    ): String {
        val filePath = generateListFilePaths(videoInputFilePaths, appName)
        return if (audioInput != null) {
            "-f concat -safe 0 -i '$filePath' -stream_loop -1 -i '${audioInput.trackAbsolutePath}' -filter_complex \"[0:a]volume=${audioInput.videoLevel}/100[aorg];[1:a]volume=${audioInput.trackLevel}/100[atrk];[aorg][atrk]amix=inputs=2:duration=shortest[aout]\" -map 0:v -map \"[aout]\" -c:v copy -c:a aac '$videoOutputFilePath'"
        } else {
            "-f concat -safe 0 -i '$filePath' -c copy '$videoOutputFilePath'"
        }
    }

    /**
     * Generate ffmpeg file paths list
     *
     * @param videoInputFilePaths - input video file paths for ffmpeg
     * @return file path
     */
    private fun generateListFilePaths(
        videoInputFilePaths: List<String>,
        appName: String
    ): String {
        val list: File
        var writer: Writer? = null
        try {
            list = fileUtil.getNewLocalCacheFile(
                appName = appName,
                customDirName = CONCAT_DIR_NAME,
                fileName = "list.txt"
            )
            writer = BufferedWriter(OutputStreamWriter(FileOutputStream(list)))
            for (input in videoInputFilePaths) {
                writer.write("file '$input'\n")
                MyLogs.LOG(
                    "ConcatVideosRepo",
                    "generateListFilePaths",
                    "Writing file path to the list:$input"
                )
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return "/"
        } finally {
            try {
                writer?.close()
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        }
        MyLogs.LOG(
            "ConcatVideosRepo",
            "generateListFile",
            "Wrote list file to: ${list.absolutePath}"
        )
        return list.absolutePath
    }


    companion object {
        const val AUDIO_TRACKS_DIR_NAME = "audio_tracks"
        const val CONCAT_DIR_NAME = "concat"
    }
}
