package com.floodin.videoeditor

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.floodin.ffmpeg_wrapper.di.libModule
import com.floodin.videoeditor.base.di.mainModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        setupKoin()
    }

    private fun setupKoin() {
        startKoin {
            androidContext(this@App)
            modules(mainModule + libModule)
        }
    }
}