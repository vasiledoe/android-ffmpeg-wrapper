package com.floodin.videoeditor.base.util

import android.util.DisplayMetrics
import android.util.TypedValue
import java.text.DecimalFormat
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.pow

infix fun Int.doubleDivTo(i: Int): Double = this / i.toDouble()

infix fun Int.roundUpDivTo(i: Int) = ceil(this / i.toDouble()).toInt()

fun Long.toPrettyFileSize(): String {
    if (this <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(toDouble()) / log10(1024.0)).toInt()
    return DecimalFormat("#.##").format(this / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
}

fun Float.toPx(displayMetrics: DisplayMetrics): Int = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this,
    displayMetrics
).toInt()