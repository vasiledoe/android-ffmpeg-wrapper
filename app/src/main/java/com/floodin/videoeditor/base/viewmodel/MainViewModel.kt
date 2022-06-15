package com.floodin.videoeditor.base.viewmodel

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floodin.ffmpeg_wrapper.data.VideoFormat
import com.floodin.ffmpeg_wrapper.data.VideoInput
import com.floodin.ffmpeg_wrapper.usecase.CompressVideoUseCase
import com.floodin.ffmpeg_wrapper.usecase.ConcatVideosUseCase
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
    private val concatVideos: ConcatVideosUseCase,
    private val compressVideo: CompressVideoUseCase,
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
        viewModelScope.launch(Dispatchers.IO) {
            val inputVideos = selectedVideoItems.value?.mapIndexed { index, videoItem ->
                VideoInput(
                    "videoId:$index",
                    videoItem.path
                )
            }
            MyLogs.LOG("MainViewModel", "concatVideos", "inputVideos:${inputVideos?.size}")

            if (inputVideos.isNullOrEmpty() || inputVideos.size < 2) {
                viewModelScope.launch(Dispatchers.Main) {
                    publishError("Need at least 2 videos for concat")
                }
                return@launch
            }

            viewModelScope.launch(Dispatchers.Main) {
                publishLoadingStateOn()
            }

            concatVideos.executeAsync(
                inputVideos = inputVideos,
                format = VideoFormat.HD,
                duration = 15f,
                appId = BuildConfig.APPLICATION_ID,
                appName = resUtil.getStringRes(R.string.app_name),
                onSuccessCallback = { videoOutput ->
                    MyLogs.LOG(
                        "MainViewModel",
                        "concatVideos",
                        "onSuccessCallback videoOutput:$videoOutput"
                    )
                    val newVideo = VideoItem(videoOutput.uri, videoOutput.absolutePath)
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
                    MyLogs.LOG("MainViewModel", "concatVideos", "onProgressCallback")
                },
                onErrorCallback = {
                    MyLogs.LOG("MainViewModel", "concatVideos", "onErrorCallback")
                }
            )
        }
    }

    fun concatVideosSync() {
        viewModelScope.launch(Dispatchers.IO) {
            val inputVideos = selectedVideoItems.value?.mapIndexed { index, videoItem ->
                VideoInput(
                    "videoId:$index",
                    videoItem.path
                )
            }
            MyLogs.LOG("MainViewModel", "concatVideosSync", "inputVideos:${inputVideos?.size}")

            if (inputVideos.isNullOrEmpty() || inputVideos.size < 2) {
                viewModelScope.launch(Dispatchers.Main) {
                    publishError("Need at least 2 videos for concat")
                }
                return@launch
            }

            viewModelScope.launch(Dispatchers.Main) {
                publishLoadingStateOn()
            }

            val videoOutput = concatVideos.executeSync(
                inputVideos = inputVideos,
                format = VideoFormat.HD,
                duration = 15f,
                appId = BuildConfig.APPLICATION_ID,
                appName = resUtil.getStringRes(R.string.app_name)
            )

            MyLogs.LOG(
                "MainViewModel",
                "concatVideosSync",
                "onSuccessCallback videoOutput:$videoOutput"
            )

            videoOutput?.let { resultVideoMeta ->
                val newVideo = VideoItem(resultVideoMeta.uri, resultVideoMeta.absolutePath)
                val updatedList = mutableListOf(newVideo)
                selectedVideoItems.value?.let {
                    updatedList.addAll(it)
                }
                viewModelScope.launch(Dispatchers.Main) {
                    publishSuccess("Concat is done successfully!")
                    selectedVideoItems.value = updatedList
                }
            } ?: run {
                viewModelScope.launch(Dispatchers.Main) {
                    publishError("Failed to concat videos!")
                }
            }
        }
    }

    fun compressVideo() {
        viewModelScope.launch(Dispatchers.IO) {
            val inputVideos = selectedVideoItems.value?.mapIndexed { index, videoItem ->
                VideoInput(
                    "videoId:$index",
                    videoItem.path
                )
            }
            MyLogs.LOG("MainViewModel", "compressVideo", "inputVideos:${inputVideos?.size}")

            if (inputVideos.isNullOrEmpty() || inputVideos.size > 1) {
                viewModelScope.launch(Dispatchers.Main) {
                    publishError("Need only one video for compressing")
                }
                return@launch
            }

            viewModelScope.launch(Dispatchers.Main) {
                publishLoadingStateOn()
            }
            compressVideo.executeAsync(
                inputVideo = inputVideos.first(),
                format = VideoFormat.HD,
                appId = BuildConfig.APPLICATION_ID,
                appName = resUtil.getStringRes(R.string.app_name),
                onSuccessCallback = { videoOutput ->
                    MyLogs.LOG(
                        "MainViewModel",
                        "compressVideo",
                        "onSuccessCallback videoOutput:$videoOutput"
                    )
                    val newVideo = VideoItem(videoOutput.uri, videoOutput.absolutePath)
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
                    MyLogs.LOG("MainViewModel", "compressVideo", "onProgressCallback")
                },
                onErrorCallback = {
                    MyLogs.LOG("MainViewModel", "compressVideo", "onErrorCallback")
                }
            )
        }
    }

    fun compressMultipleVideos() {
        viewModelScope.launch(Dispatchers.IO) {
            val inputVideos = selectedVideoItems.value?.mapIndexed { index, videoItem ->
                VideoInput(
                    "videoId:$index",
                    videoItem.path
                )
            }
            MyLogs.LOG(
                "MainViewModel",
                "compressMultipleVideos",
                "inputVideos:${inputVideos?.size}"
            )

            if (inputVideos.isNullOrEmpty() || inputVideos.size < 2) {
                viewModelScope.launch(Dispatchers.Main) {
                    publishError("Need at least 2 videos for multiple compress")
                }
                return@launch
            }

            viewModelScope.launch(Dispatchers.Main) {
                publishLoadingStateOn()
            }

            val result = compressVideo.executeMultipleCompressSync(
                inputVideos = inputVideos,
                format = VideoFormat.HD,
                appId = BuildConfig.APPLICATION_ID,
                appName = resUtil.getStringRes(R.string.app_name)
            )

            viewModelScope.launch(Dispatchers.Main) {
                publishLoadingStateOff()
            }
            MyLogs.LOG("MainViewModel", "compressMultipleVideos", "result:$result")
        }
    }
}