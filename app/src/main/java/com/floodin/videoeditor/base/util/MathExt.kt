package com.floodin.videoeditor.base.util

import android.util.DisplayMetrics
import android.util.TypedValue
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
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

fun Long.toPrettyTimeElapsed(): String {
    if (this <= 0) return "0 sec"
    return String.format(
        "%02d : %02d : %02d",
        TimeUnit.MILLISECONDS.toHours(this),
        TimeUnit.MILLISECONDS.toMinutes(this) - TimeUnit.HOURS.toMinutes(
            TimeUnit.MILLISECONDS.toHours(this)
        ),
        TimeUnit.MILLISECONDS.toSeconds(this) - TimeUnit.MINUTES.toSeconds(
            TimeUnit.MILLISECONDS.toMinutes(this)
        )
    )
}

fun Float.toPx(displayMetrics: DisplayMetrics): Int = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this,
    displayMetrics
).toInt()