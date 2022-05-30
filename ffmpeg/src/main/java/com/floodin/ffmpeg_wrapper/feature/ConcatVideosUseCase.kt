package com.floodin.ffmpeg_wrapper.feature

import android.net.Uri
import com.floodin.ffmpeg_wrapper.util.FfmpegCommandUtil
import com.floodin.ffmpeg_wrapper.util.FileUtil
import com.floodin.ffmpeg_wrapper.util.MyLogs
import java.io.*

class ConcatVideosUseCase(
    private val fileUtil: FileUtil,
    private val cmdUtil: FfmpegCommandUtil
) {

    fun concatVideos(
        inputPaths: List<String>,
        appId: String,
        appName: String,
        onSuccessCallback: (Uri, String) -> Unit,
        onProgressCallback: (String) -> Unit,
        onErrorCallback: (String) -> Unit
    ) {
        val outputFile = fileUtil.getNewLocalCacheFile(
            appName = appName,
            customDirName = CONCAT_DIR_NAME,
            fileName = "${System.currentTimeMillis()}.mp4"
        )
        MyLogs.LOG(
            "ConcatVideosUseCase",
            "concatVideos",
            "inputPaths:$inputPaths outputFilePath: ${outputFile.absolutePath}"
        )

        val command = generateCommand(
            fileUris = inputPaths,
            outputFilePath = outputFile.absolutePath,
            appName = appName
        )
        MyLogs.LOG("ConcatVideosUseCase", "concatVideos", "command: $command")
        cmdUtil.executeAsync(
            command,
            outputFile,
            appId,
            onSuccessCallback,
            onProgressCallback,
            onErrorCallback
        )
    }

    private fun generateCommand(
        fileUris: List<String>,
        outputFilePath: String,
        appName: String
    ): String {
        val filePath = generateListFile(fileUris, appName)
        return "-f concat -safe 0 -i $filePath -c copy $outputFilePath"
    }

    /**
     * Generate an ffmpeg file list
     * @param inputs Input files for ffmpeg
     * @return File path
     */
    private fun generateListFile(
        inputs: List<String>,
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
            for (input in inputs) {
                writer.write("file '$input'\n")
                MyLogs.LOG(
                    "ConcatVideosUseCase",
                    "generateListFile",
                    "Writing to list file: $input"
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
            "ConcatVideosUseCase",
            "generateListFile",
            "Wrote list file to: ${list.absolutePath}"
        )
        return list.absolutePath
    }


    companion object {
        const val CONCAT_DIR_NAME = "concat"
    }
}
