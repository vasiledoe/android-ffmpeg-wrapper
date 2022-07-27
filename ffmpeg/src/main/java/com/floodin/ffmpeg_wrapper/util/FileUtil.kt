package com.floodin.ffmpeg_wrapper.util

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import org.apache.commons.io.IOUtils.toByteArray
import java.io.File
import java.util.*


class FileUtil(
    private val context: Context
) {

    fun getNewLocalFile(
        appName: String,
        customDirName: String,
        fileName: String
    ): File {
        val externalDir = getOutDirPath(appName, customDirName)
        if (externalDir.exists()) {
            val dir = File(externalDir.absolutePath)
            val localFile = File(dir, fileName)
//            MyLogs.LOG("FileUtil", "getNewLocalFile", "path: ${localFile.absolutePath}")
            return localFile
        } else {
            throw IllegalStateException("Cannot create local file")
        }
    }

    fun getNewLocalCacheFile(
        appName: String,
        customDirName: String,
        fileName: String
    ): File {
        val externalDir = getOutCacheDirPath(appName, customDirName)
        if (externalDir.exists()) {
            val dir = File(externalDir.absolutePath)
            val localFile = File(dir, fileName)
//            MyLogs.LOG("FileUtil", "getNewLocalCacheFile", "path: ${localFile.absolutePath}")
            return localFile
        } else {
            throw IllegalStateException("Cannot create local cache file")
        }
    }

    @Suppress("DEPRECATION")
    fun getOutDirPath(
        appName: String,
        dirName: String? = null
    ): File {
        val customDirLocation = if (dirName != null) {
            "$appName/$dirName/"
        } else {
            "$appName/"
        }

        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            customDirLocation
        )

        //make sure directory exists
        val isDirCreated = file.mkdirs()
//        MyLogs.LOG(
//            "FileUtil",
//            "getOutDirPath",
//            "isDirCreated: $isDirCreated exists:${file.exists()}"
//        )

        return file
    }

    @Suppress("DEPRECATION")
    fun getOutCacheDirPath(
        appName: String,
        dirName: String? = null
    ): File {
        val customDirLocation = if (dirName != null) {
            "$dirName/"
        } else {
            "$appName/"
        }

        val file = File(context.cacheDir, customDirLocation)

        //make sure directory exists
        file.mkdirs()

        return file
    }

    fun getFileNameFromPath(fileAbsolutePath: String): String {
        return fileAbsolutePath.substring(fileAbsolutePath.lastIndexOf("/") + 1)
    }

    private fun deleteFileFromUri(localUri: String) {
        val fileUri: Uri = Uri.parse(localUri)
        val contentResolver: ContentResolver = context.contentResolver
        val deleted = contentResolver.delete(fileUri, null, null)
//        MyLogs.LOG("FileUtil", "deleteFileFromUri", "localUri:$localUri deleted: $deleted")
    }

    @SuppressLint("Range")
    fun getFileNameFromUri(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                cursor?.close()
            }
        }

        if (result == null) {
            val cut = uri.path?.lastIndexOf('/')
            result = if (cut != null && cut != -1) {
                uri.path?.substring(cut + 1) ?: "unknown"
            } else {
                "unknown"
            }
        }

        return result
    }

    @SuppressLint("Range")
    fun getFileSizeFromUri(uri: Uri): Long {
        var result: Long = 0
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                cursor?.close()
            }
        }

        if (result == 0L) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                result = toByteArray(inputStream).size.toLong()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return result
    }

    private fun getFileSize(file: File?): Long {
        if (file == null || !file.exists()) return 0
        if (!file.isDirectory) return file.length()
        val dirs: MutableList<File> = LinkedList()
        dirs.add(file)
        var result: Long = 0
        while (dirs.isNotEmpty()) {
            val dir = dirs.removeAt(0)
            if (!dir.exists()) continue
            val listFiles = dir.listFiles()
            if (listFiles == null || listFiles.isEmpty()) continue
            for (child in listFiles) {
                result += child.length()
                if (child.isDirectory) dirs.add(child)
            }
        }
        return result
    }

    fun getMediaFileExtension(fileName: String): String {
        return fileName.substringAfterLast(".", "unknown").lowercase()
    }
}