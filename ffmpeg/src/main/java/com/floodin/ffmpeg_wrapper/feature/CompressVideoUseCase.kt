package com.floodin.ffmpeg_wrapper.feature

import android.net.Uri
import com.floodin.ffmpeg_wrapper.util.FfmpegCommandUtil
import com.floodin.ffmpeg_wrapper.util.FileUtil
import com.floodin.ffmpeg_wrapper.util.MyLogs

class CompressVideoUseCase(
    private val fileUtil: FileUtil,
    private val cmdUtil: FfmpegCommandUtil
) {
    fun compressVideo(
        inputPath: String,
        appId: String,
        appName: String,
        onSuccessCallback: (Uri, String) -> Unit,
        onProgressCallback: (String) -> Unit,
        onErrorCallback: (String) -> Unit
    ) {
        //ffmpeg -i input.mp4 -c:a copy -c:v vp9 -b:v 1M output.mp4
        //ffmpeg -i input.mp4 -c:a copy -s hd720 output.mp4
        //ffmpeg -y -i /source-path/input.mp4 -s 480x320 -r 25 -vcodec mpeg4 -b:v 300k -b:a 48000 -ac 2 -ar 22050 /source/output.mp4
        val outputFile = fileUtil.getNewLocalCacheFile(
            appName = appName,
            COMPRESSED_DIR_NAME,
            "${System.currentTimeMillis()}.mp4"
        )
        val outputFilePath = outputFile.absolutePath
        val command = "-y -i $inputPath -s 1280x720 -r 30 -vcodec mpeg4 -b:v 300k -b:a 48000 -ac 2 -ar 22050 $outputFilePath"
        MyLogs.LOG("CompressVideoUseCase", "compressVideo", "command: $command")
        cmdUtil.executeAsync(
            command,
            outputFile,
            appId,
            onSuccessCallback,
            onProgressCallback,
            onErrorCallback,
        )
    }

    companion object {
        const val COMPRESSED_DIR_NAME = "compressed"
    }
}