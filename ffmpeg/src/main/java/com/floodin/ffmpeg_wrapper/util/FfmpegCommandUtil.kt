package com.floodin.ffmpeg_wrapper.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
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
        appId: String
    ) {
        val session = FFmpegKit.execute(command)
        if (ReturnCode.isSuccess(session.returnCode)) {
            MyLogs.LOG(
                "FfmpegCommandUtil",
                "executeSync",
                "SUCCESS session: ${session.state}"
            )
        } else if (ReturnCode.isCancel(session.returnCode)) {
            MyLogs.LOG(
                "FfmpegCommandUtil",
                "executeSync",
                "CANCEL session: ${session.state}"
            )
        } else {
            MyLogs.LOG(
                "FfmpegCommandUtil",
                "executeSync",
                "FAILURE session: ${session.state}"
            )
        }
    }


//    protected void execSync(String ffmpegCommand) {
//        FFmpegSession session = FFmpegKit.execute(ffmpegCommand);
//        if (ReturnCode.isSuccess(session.getReturnCode())) {
//            Log.d(TAG, "execSync - SUCCESS");
//            // SUCCESS
//
//        } else if (ReturnCode.isCancel(session.getReturnCode())) {
//            Log.d(TAG, "CANCEL");
//            // CANCEL
//
//        } else {
//            Log.d(TAG, String.format("execSync - Command failed with state %s and rc %s.%s", session.getState(), session.getReturnCode(), session.getFailStackTrace()));
//            // FAILURE
//
//        }
//    }
//
//    protected void execAsync(String ffmpegCommand) {
//        FFmpegKit.executeAsync(
//                ffmpegCommand,
//                session -> {
//                    SessionState state = session.getState();
//                    ReturnCode returnCode = session.getReturnCode();
//                    Log.d(TAG, String.format("FFmpeg process exited with state %s and rc %s.%s", state, returnCode, session.getFailStackTrace()));
//                    // CALLED WHEN SESSION IS EXECUTED
//
//                },
//                log -> {
//                    Log.d(TAG, "logs:" + log);
//                    // CALLED WHEN SESSION PRINTS LOGS
//
//                }, statistics -> {
//                    // CALLED WHEN SESSION GENERATES STATISTICS
//                    Log.d(TAG, "statistics:" + statistics);
//
//                });
//    }

}