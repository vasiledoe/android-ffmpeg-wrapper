package com.floodin.videoeditor.base.view

import android.content.Context
import android.graphics.Bitmap
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
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.io.File


fun ImageView.loadFromMediaItem(media: VideoItem) {
    MyLogs.LOG("loadFromMediaItem", "onError", "media: $media")
    val thumbBitmap = media.toThumb(context)
    val thumbUri = thumbBitmap?.toUri(context)

    Picasso.get()
        .load(thumbUri)
        .placeholder(R.color.gray_dark_50)
        .error(R.color.rec_red_50)
        .resize(720, 0)
        .into(this, object : Callback {
            override fun onSuccess() {
                MyLogs.LOG("loadFromMediaItem", "onSuccess", "...")
            }

            override fun onError(e: Exception?) {
                MyLogs.LOG("loadFromMediaItem", "onError", "error: ${e?.localizedMessage}")
                e?.printStackTrace()
            }
        })
}

fun VideoItem.toThumb(context: Context) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    ThumbnailUtils.createVideoThumbnail(
        File(this.path),
        Size(
            1000f.toPx(context.resources.displayMetrics),
            1000f.toPx(context.resources.displayMetrics)
        ),
        CancellationSignal()
    )
} else {
    ThumbnailUtils.createVideoThumbnail(
        this.path,
        MediaStore.Images.Thumbnails.MINI_KIND
    )
}

fun Bitmap.toUri(context: Context): Uri {
    val bytes = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 80, bytes)
    val path = MediaStore.Images.Media.insertImage(context.contentResolver, this, "test", null)
    return Uri.parse(path)
}
