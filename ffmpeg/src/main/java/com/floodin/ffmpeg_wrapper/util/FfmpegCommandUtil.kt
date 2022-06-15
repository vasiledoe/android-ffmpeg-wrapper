package com.floodin.ffmpeg_wrapper.util

import android.content.Context
import androidx.core.content.FileProvider
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.floodin.ffmpeg_wrapper.data.FFmpegResult
import java.io.File


class FfmpegCommandUtil(
    private val context: Context
) {
    fun executeSync(
        inputFileId: String,
        command: String,
        outputFile: File,
        appId: String
    ): FFmpegResult {
        val session = FFmpegKit.execute(command)
        if (ReturnCode.isSuccess(session.returnCode)) {
            MyLogs.LOG(
                "FfmpegCommandUtil",
                "executeSync",
                "SUCCESS session: ${session.state}"
            )
            val uri = FileProvider.getUriForFile(
                context,
                "$appId.fileprovider",
                outputFile
            )
            return FFmpegResult.Successful(
                inputId = inputFileId,
                outputPath = outputFile.absolutePath,
                outputUri = uri
            )
        } else if (ReturnCode.isCancel(session.returnCode)) {
            MyLogs.LOG(
                "FfmpegCommandUtil",
                "executeSync",
                "CANCEL session: ${session.state}"
            )
            return FFmpegResult.Cancel
        } else {
            MyLogs.LOG(
                "FfmpegCommandUtil",
                "executeSync",
                "FAILURE session: ${session.state}"
            )
            return FFmpegResult.Error
        }
    }
}