package com.floodin.ffmpeg_wrapper.di

import com.floodin.ffmpeg_wrapper.feature.ConcatVideosUseCase
import com.floodin.ffmpeg_wrapper.util.FfmpegCommandUtil
import com.floodin.ffmpeg_wrapper.util.FileUtil
import org.koin.dsl.module
import org.koin.dsl.single

val libModule = module {
    single<FileUtil>()
    single<FfmpegCommandUtil>()
    single<ConcatVideosUseCase>()
}