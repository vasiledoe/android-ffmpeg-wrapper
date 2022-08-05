package com.floodin.videoeditor.base.viewmodel

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floodin.ffmpeg_wrapper.data.FFmpegResult
import com.floodin.ffmpeg_wrapper.data.VideoFormat
import com.floodin.ffmpeg_wrapper.data.VideoInput
import com.floodin.ffmpeg_wrapper.usecase.CompressVideoUseCase
import com.floodin.ffmpeg_wrapper.usecase.ConcatVideosUseCase
import com.floodin.ffmpeg_wrapper.util.MyLogs
import com.floodin.videoeditor.BuildConfig
import com.floodin.videoeditor.R
import com.floodin.videoeditor.base.data.VideoItem
import com.floodin.videoeditor.base.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import java.io.File

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

            val result = concatVideos.executeSync(
                inputVideos = inputVideos,
                format = VideoFormat.HD,
                duration = 15f,
                appId = BuildConfig.APPLICATION_ID,
                appName = resUtil.getStringRes(R.string.app_name)
            )
            MyLogs.LOG("MainViewModel", "concatVideosSync", "result:$result")

            when (result) {
                is FFmpegResult.Success -> {
                    val resultVideoMeta = result.data
                    val newVideo = VideoItem(resultVideoMeta.uri, resultVideoMeta.absolutePath)
                    val updatedList = mutableListOf(newVideo)
                    selectedVideoItems.value?.let {
                        updatedList.addAll(it)
                    }
                    viewModelScope.launch(Dispatchers.Main) {
                        publishSuccess("Concat is done successfully!")
                        selectedVideoItems.value = updatedList
                    }
                }
                is FFmpegResult.Error -> {
                    viewModelScope.launch(Dispatchers.Main) {
                        publishError(result.message)
                    }
                }
                is FFmpegResult.Cancel -> {
                    publishError("Seems cancelled")
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
            MyLogs.LOG(
                "MainViewModel",
                "compressVideo",
                "inputVideo:${inputVideos?.firstOrNull()}"
            )

            if (inputVideos.isNullOrEmpty()) {
                viewModelScope.launch(Dispatchers.Main) {
                    publishError("Need at least 1 video for compress")
                }
                return@launch
            }

            viewModelScope.launch(Dispatchers.Main) {
                publishLoadingStateOn()
            }

            val startTimeMillis = System.currentTimeMillis()
            val result = compressVideo.executeSync(
                inputVideo = inputVideos.first(),
                format = VideoFormat.FHD,
//                duration = 30f,
                appId = BuildConfig.APPLICATION_ID,
                appName = resUtil.getStringRes(R.string.app_name)
            )

            // some logs
            if (result is FFmpegResult.Success) {
                val timeElapsed = (System.currentTimeMillis() - startTimeMillis).toPrettyTimeElapsed()

                val inputVideoMeta = inputVideos.first()
                val inputVideoFile = File(inputVideoMeta.absolutePath)
                val inputVideoFileSize = inputVideoFile.length().toPrettyFileSize()

                val outputVideoFile = File(result.data.absolutePath)
                val outputVideoFileSize = outputVideoFile.length().toPrettyFileSize()

                MyLogs.LOG(
                    "MainViewModel",
                    "compressMultipleVideos",
                    "results \n timeElapsed: $timeElapsed \n " +
                            "input Video : ${inputVideoFile.absolutePath} $inputVideoFileSize \n " +
                            "output Video : ${outputVideoFile.absolutePath}  $outputVideoFileSize"
                )
            }


            viewModelScope.launch(Dispatchers.Main) {
                publishSuccess("Result is $result")
            }
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

            val startTimeMillis = System.currentTimeMillis()
            val results = compressVideo.executeSync(
                inputVideos = inputVideos,
                format = VideoFormat.HD,
                appId = BuildConfig.APPLICATION_ID,
                appName = resUtil.getStringRes(R.string.app_name)
            )
            val successCount = results.count { it is FFmpegResult.Success }
            val failedCount = results.count { it is FFmpegResult.Error }
            val cancelledCount = results.count { it is FFmpegResult.Cancel }
            MyLogs.LOG(
                "MainViewModel",
                "compressMultipleVideos",
                "it took:${System.currentTimeMillis() - startTimeMillis} " +
                        "successCount:$successCount, failedCount:$failedCount, cancelledCount:$cancelledCount " +
                        "all results:$results"
            )

            viewModelScope.launch(Dispatchers.Main) {
                val str = "Result: %d success, %d failed, %d cancelled"
                publishSuccess(String.format(str, successCount, failedCount, cancelledCount))
            }
        }
    }
}