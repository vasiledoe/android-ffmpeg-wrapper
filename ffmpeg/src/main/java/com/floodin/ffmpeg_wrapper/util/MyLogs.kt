package com.floodin.ffmpeg_wrapper.util

import android.util.Log

class MyLogs {

    companion object {
        fun LOG(myClass: String, myFun: String, msg: String) {
            Log.d("=======> ffmpegWrapperLogs", "$myClass / $myFun / $msg")
        }
    }
}