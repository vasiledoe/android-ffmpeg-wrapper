package com.floodin.videoeditor.base.viewmodel

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floodin.ffmpeg_wrapper.feature.CompressVideoUseCase
import com.floodin.ffmpeg_wrapper.feature.ConcatVideosUseCase
import com.floodin.ffmpeg_wrapper.util.MyLogs
import com.floodin.videoeditor.BuildConfig
import com.floodin.videoeditor.R
import com.floodin.videoeditor.base.data.VideoItem
import com.floodin.videoeditor.base.util.ResUtil
import com.floodin.videoeditor.base.util.URIPathHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

open class MainViewModel(
    private val uRIPathHelper: URIPathHelper,
    private val concatVideosUseCase: ConcatVideosUseCase,
    private val compressVideoUseCase: CompressVideoUseCase,
    private val resUtil: ResUtil
) : ViewModel(), KoinComponent {

    val successMsg = MutableLiveData<String>()
    val error = MutableLiveData<String>()
    val loadingState = MutableLiveData<Boolean>()
    val selectedVideoItems = MutableLiveData<List<VideoItem>>()


    private fun publishLoadingStateOn() {
        loadingState.value = true
    }

    private fun publishLoadingStateOff() {
        loadingState.value = false
    }

    private fun publishError(err: String) {
        publishLoadingStateOff()
        error.value = err
    }

    private fun publishSuccess(msg: String) {
        publishLoadingStateOff()
        successMsg.value = msg
    }

    fun applySelectedData(data: Intent?) {
        viewModelScope.launch {
            publishLoadingStateOn()
            if (data?.clipData != null) {
                val count = data.clipData?.itemCount ?: 0
                publishSuccess("You've selected $count videos")

                val itemsList = mutableListOf<VideoItem>().apply {
                    repeat(count) { index ->
                        data.clipData?.getItemAt(index)?.uri?.let { uri ->
                            this.add(
                                VideoItem(
                                    uri,
                                    uRIPathHelper.getPathFromUri(uri) ?: "unknown"
                                )
                            )
                        }
                    }
                }
                MyLogs.LOG("MainViewModel", "applySelectedData", "itemsList:$itemsList")
                selectedVideoItems.value = itemsList

            } else if (data?.data != null) {
                val imageUri: Uri? = data.data
                MyLogs.LOG("MainViewModel", "applySelectedData", "imageUri:$imageUri")
                publishError("You've selected only one video")
                selectedVideoItems.value = imageUri?.let {
                    listOf(VideoItem(imageUri, uRIPathHelper.getPathFromUri(imageUri) ?: "unknown"))
                } ?: emptyList()
            }
        }
    }

    fun concatVideos() {
        viewModelScope.launch {
            val paths = selectedVideoItems.value?.map { it.path }
            MyLogs.LOG("MainViewModel", "concatVideos", "paths:${paths?.size}")

            if (paths.isNullOrEmpty() || paths.size < 2) {
                publishError("Need at least 2 videos for concat")
                return@launch
            }

            publishLoadingStateOn()
            concatVideosUseCase.concatVideos(
                inputPaths = paths,
                appId = BuildConfig.APPLICATION_ID,
                appName = resUtil.getStringRes(R.string.app_name),
                onSuccessCallback = { newFileUri, newFilePath ->
                    MyLogs.LOG(
                        "MainViewModel",
                        "concatVideos",
                        "onSuccessCallback newFileUri:$newFileUri newFilePath: $newFilePath"
                    )
                    val newVideo = VideoItem(newFileUri, newFilePath)
                    val updatedList = mutableListOf(newVideo)
                    selectedVideoItems.value?.let {
                        updatedList.addAll(it)
                    }
                    viewModelScope.launch(Dispatchers.Main) {
                        publishSuccess("Concat is done successfully!")
                        selectedVideoItems.value = updatedList
                    }
                },
                onProgressCallback = {
                    MyLogs.LOG("EditEventViewModel", "concatVideos", "onProgressCallback")
                },
                onErrorCallback = {
                    MyLogs.LOG("EditEventViewModel", "concatVideos", "onErrorCallback")
                }
            )
        }
    }

    fun compressVideo() {
        viewModelScope.launch {
            val paths = selectedVideoItems.value?.map { it.path }
            MyLogs.LOG("MainViewModel", "compressVideo", "paths:${paths?.size}")

            if (paths.isNullOrEmpty() || paths.size > 1) {
                publishError("Need at least one video for encoding")
                return@launch
            }

            publishLoadingStateOn()
            compressVideoUseCase.compressVideo(
                inputPath = paths.first(),
                appId = BuildConfig.APPLICATION_ID,
                appName = resUtil.getStringRes(R.string.app_name),
                onSuccessCallback = { newFileUri, newFilePath ->
                    MyLogs.LOG(
                        "MainViewModel",
                        "compressVideo",
                        "onSuccessCallback newFileUri:$newFileUri newFilePath: $newFilePath"
                    )
                    val newVideo = VideoItem(newFileUri, newFilePath)
                    val updatedList = mutableListOf(newVideo)
                    selectedVideoItems.value?.let {
                        updatedList.addAll(it)
                    }
                    viewModelScope.launch(Dispatchers.Main) {
                        publishSuccess("Concat is done successfully!")
                        selectedVideoItems.value = updatedList
                    }
                },
                onProgressCallback = {
                    MyLogs.LOG("EditEventViewModel", "compressVideo", "onProgressCallback")
                },
                onErrorCallback = {
                    MyLogs.LOG("EditEventViewModel", "compressVideo", "onErrorCallback")
                }
            )
        }
    }
}