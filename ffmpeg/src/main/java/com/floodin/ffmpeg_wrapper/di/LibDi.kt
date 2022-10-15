package com.floodin.ffmpeg_wrapper.di

import com.floodin.ffmpeg_wrapper.repo.CalculateMaxDurationRepo
import com.floodin.ffmpeg_wrapper.repo.CompressVideoRepo
import com.floodin.ffmpeg_wrapper.repo.ConcatVideosRepo
import com.floodin.ffmpeg_wrapper.repo.MediaInfoRepo
import com.floodin.ffmpeg_wrapper.usecase.CompressVideoUseCase
import com.floodin.ffmpeg_wrapper.usecase.ConcatVideosUseCase
import com.floodin.ffmpeg_wrapper.util.FfmpegCommandUtil
import com.floodin.ffmpeg_wrapper.util.FileUtil
import org.koin.dsl.module
import org.koin.dsl.single

val libModule = module {
    single<FileUtil>()
    single<FfmpegCommandUtil>()
    single<MediaInfoRepo>()
    single<CalculateMaxDurationRepo>()
    single<ConcatVideosRepo>()
    single<CompressVideoRepo>()
    single<ConcatVideosUseCase>()
    single<CompressVideoUseCase>()
}