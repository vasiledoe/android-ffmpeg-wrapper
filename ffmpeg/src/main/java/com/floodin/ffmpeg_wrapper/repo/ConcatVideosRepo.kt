package com.floodin.ffmpeg_wrapper.repo

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
     * @param inputPaths - file absolute paths
     * @param appId - application ID
     * @param appName - application Name
     * @return result of ffmpeg command
     */
    fun execute(
        inputPaths: List<String>,
        appId: String,
        appName: String
    ): FFmpegResult {
        val outputFile = fileUtil.getNewLocalCacheFile(
            appName = appName,
            customDirName = CONCAT_DIR_NAME,
            fileName = "${System.currentTimeMillis()}.mp4"
        )
        MyLogs.LOG(
            "ConcatVideosRepo",
            "execute",
            "inputPaths:$inputPaths outputFilePath: ${outputFile.absolutePath}"
        )
        val command = generateCommand(
            fileUris = inputPaths,
            outputFilePath = outputFile.absolutePath,
            appName = appName
        )
        MyLogs.LOG("ConcatVideosRepo", "execute", "command: $command")
        return cmdUtil.executeSync(
            command,
            outputFile,
            appId
        )
    }

    private fun generateCommand(
        fileUris: List<String>,
        outputFilePath: String,
        appName: String
    ): String {
        val filePath = generateListFilePaths(fileUris, appName)
        return "-f concat -safe 0 -i $filePath -c copy $outputFilePath"
    }

    /**
     * Generate ffmpeg file paths list
     *
     * @param inputs - input files for ffmpeg
     * @return file path
     */
    private fun generateListFilePaths(
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
        const val CONCAT_DIR_NAME = "concat"
    }
}
