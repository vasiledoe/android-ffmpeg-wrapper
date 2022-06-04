package com.floodin.ffmpeg_wrapper.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.floodin.ffmpeg_wrapper.data.FFmpegResult
import java.io.File


class FfmpegCommandUtil(
    private val context: Context
) {
    fun executeAsync(
        command: String,
        outputFile: File,
        appId: String,
        onSuccessCallback: (Uri, String) -> Unit,
        onProgressCallback: (String) -> Unit,
        onErrorCallback: (String) -> Unit
    ) {
        FFmpegKit.executeAsync(command,
            { session ->
                val state = session.state
                val returnCode = session.returnCode

                // CALLED WHEN SESSION IS EXECUTED
                MyLogs.LOG(
                    "FfmpegCommandUtil",
                    "executeAsync",
                    String.format(
                        "FFmpeg process exited with state %s and rc %s.%s",
                        state,
                        returnCode,
                        session.failStackTrace
                    )
                )

                val uri = FileProvider.getUriForFile(
                    context,
                    "$appId.fileprovider",
                    outputFile
                )
                onSuccessCallback.invoke(uri, outputFile.absolutePath)
            },
            {
                // CALLED WHEN SESSION PRINTS LOGS
                MyLogs.LOG(
                    "FfmpegCommandUtil",
                    "executeAsync",
                    "logs: ${it.message}"
                )
//                onProgressCallback.invoke(it.message)
            })
        {
            // CALLED WHEN SESSION GENERATES STATISTICS
            MyLogs.LOG(
                "FfmpegCommandUtil",
                "executeAsync",
                "statistics: $it"
            )
//            onProgressCallback.invoke(it.toString())
        }
    }

    fun executeSync(
        command: String,
        outputFile: File,
        appId: String,
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