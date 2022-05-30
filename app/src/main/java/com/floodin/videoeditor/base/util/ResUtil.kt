package com.floodin.videoeditor.base.util

import android.content.Context

class ResUtil(private val context: Context) {

    fun getStringRes(strId: Int): String = context.resources.getString(strId)

    fun getStringRes(
        strId: Int,
        arg: String
    ): String = context.resources.getString(strId, arg)

    fun getStringRes(
        strId: Int,
        arg1: String,
        arg2: String
    ): String = context.resources.getString(strId, arg1, arg2)

    fun getQuantityString(
        strId: Int,
        amount: Int
    ) = context.resources.getQuantityString(strId, amount, amount)
}