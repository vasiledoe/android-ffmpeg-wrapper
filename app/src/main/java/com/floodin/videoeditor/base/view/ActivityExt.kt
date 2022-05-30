package com.floodin.videoeditor.base.view

import android.content.pm.PackageManager
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

fun AppCompatActivity.showToast(message: String, toastLen: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, toastLen).show()
}

fun AppCompatActivity.hasPermissions(permissions: Array<String>) = permissions.all {
    checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
}

fun AppCompatActivity.showPermissionSnackAction(
    messageStrId: Int,
    actionStrId: Int,
    listener: View.OnClickListener? = null
) {
    val snackbar = Snackbar.make(
        findViewById(android.R.id.content), getString(messageStrId),
        BaseTransientBottomBar.LENGTH_INDEFINITE
    )
    if (actionStrId != 0 && listener != null) {
        snackbar.setAction(getString(actionStrId), listener)
    }
    snackbar.show()
}