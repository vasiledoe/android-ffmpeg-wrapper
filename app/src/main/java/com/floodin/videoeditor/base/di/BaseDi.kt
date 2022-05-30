package com.floodin.videoeditor.base.di

import com.floodin.videoeditor.base.util.ResUtil
import com.floodin.videoeditor.base.util.URIPathHelper
import com.floodin.videoeditor.base.viewmodel.MainViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koin.dsl.single

val mainModule = module {
    viewModel<MainViewModel>()
    single<URIPathHelper>()
    single<ResUtil>()
}