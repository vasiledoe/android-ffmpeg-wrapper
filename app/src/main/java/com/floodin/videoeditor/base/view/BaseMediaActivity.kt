package com.floodin.videoeditor.base.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.floodin.ffmpeg_wrapper.util.MyLogs
import com.floodin.videoeditor.BuildConfig
import com.floodin.videoeditor.R

abstract class BaseMediaActivity : AppCompatActivity() {

    /**
     * Called in child activities when permission is granted
     */
    protected abstract fun onStoragePermissionGranted()

    protected fun hasStoragePermission() = hasPermissions(getStoragePermissions())

    protected fun requestStoragePermission() {
        requestPermissions(
            getStoragePermissions(),
            STORAGE_PERMISSIONS_REQUEST_CODE
        )
    }

    private fun showPermissionDeniedWithSnackAction() {
        this.showPermissionSnackAction(
            R.string.permission_denied_explanation,
            R.string.settings
        ) {
            val intent = Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        }
    }


    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == STORAGE_PERMISSIONS_REQUEST_CODE) {
            when {
                // if user interaction was interrupted, the permission request is cancelled and we receive empty arrays
                grantResults.isEmpty() -> MyLogs.LOG(
                    "BaseMediaActivity",
                    "onRequestPermissionsResult",
                    "user interaction was cancelled."
                )

                // Permission granted.
                (grantResults[0] == PackageManager.PERMISSION_GRANTED) -> onStoragePermissionGranted()

                // Permission denied. Show snack with option to displays the App settings screen.
                else -> {
                    showPermissionDeniedWithSnackAction()
                }
            }
        }
    }

    private fun getStoragePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    companion object {
        const val STORAGE_PERMISSIONS_REQUEST_CODE = 10
    }
}