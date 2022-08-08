package com.floodin.videoeditor.base.view

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.provider.MediaStore
import android.util.Size
import android.widget.ImageView
import com.floodin.ffmpeg_wrapper.util.MyLogs
import com.floodin.videoeditor.R
import com.floodin.videoeditor.base.data.VideoItem
import com.floodin.videoeditor.base.util.toPx
import java.io.File


fun ImageView.loadFromMediaItem(media: VideoItem) {
    val thumbBitmap = try {
        media.toThumb(context)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
    thumbBitmap?.let {
        setImageBitmap(it)
    } ?: run {
        setImageResource(R.color.rec_red_50)
    }
}

@Throws(Exception::class)
fun VideoItem.toThumb(context: Context) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    MyLogs.LOG("ImageViewExt", "toThumb", "Build.VERSION_CODES.R")
    ThumbnailUtils.createVideoThumbnail(
        File(this.path),
        Size(
            1000f.toPx(context.resources.displayMetrics),
            1000f.toPx(context.resources.displayMetrics)
        ),
        CancellationSignal()
    )
} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    MyLogs.LOG("ImageViewExt", "toThumb", "Build.VERSION_CODES.Q")
    createThumbnail(context, uri)
} else {
    MyLogs.LOG("ImageViewExt", "toThumb", "old")
    ThumbnailUtils.createVideoThumbnail(
        this.path,
        MediaStore.Images.Thumbnails.MINI_KIND
    )
}

fun createThumbnail(context: Context, uri: Uri): Bitmap? {
    var mediaMetadataRetriever: MediaMetadataRetriever? = null
    var bitmap: Bitmap? = null
    try {
        mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(context, uri)
        bitmap = mediaMetadataRetriever.getFrameAtTime(
            1000,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
        )
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        mediaMetadataRetriever?.release()
    }
    return bitmap
}