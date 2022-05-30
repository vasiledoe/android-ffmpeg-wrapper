package com.floodin.videoeditor.base.util

import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import com.floodin.ffmpeg_wrapper.util.MyLogs
import java.io.IOException
import java.io.InputStream

fun Uri.getImageRotation(ctx: Context): Int {
    var input: InputStream? = null
    try {
        input = ctx.contentResolver.openInputStream(this)
        input?.let {
            val exifInterface = ExifInterface(it)
            val orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            MyLogs.LOG("UriExt", "getImageRotation", "orientation: $orientation")
            return when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: run {
            MyLogs.LOG("UriExt", "getImageRotation", "null input")
        }
    } catch (e: IOException) {
        MyLogs.LOG("UriExt", "getImageRotation", "err1: ${e.localizedMessage}")
        e.printStackTrace()

    } finally {
        if (input != null) {
            try {
                input.close()
            } catch (e: IOException) {
                MyLogs.LOG("UriExt", "getImageRotation", "err2: ${e.localizedMessage}")
                e.printStackTrace()
            }
        }
    }
    return 0
}