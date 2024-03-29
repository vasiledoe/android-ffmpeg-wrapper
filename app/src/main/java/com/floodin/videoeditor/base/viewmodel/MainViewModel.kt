package com.floodin.videoeditor.base.viewmodel

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floodin.ffmpeg_wrapper.data.AudioInput
import com.floodin.ffmpeg_wrapper.data.FFmpegResult
import com.floodin.ffmpeg_wrapper.data.VideoInput
import com.floodin.ffmpeg_wrapper.data.VideoResolution
import com.floodin.ffmpeg_wrapper.repo.ConcatVideosRepo
import com.floodin.ffmpeg_wrapper.repo.MediaInfoRepo
import com.floodin.ffmpeg_wrapper.usecase.CompressVideoUseCase
import com.floodin.ffmpeg_wrapper.usecase.ConcatVideosUseCase
import com.floodin.ffmpeg_wrapper.util.FileUtil
import com.floodin.ffmpeg_wrapper.util.MyLogs
import com.floodin.videoeditor.BuildConfig
import com.floodin.videoeditor.R
import com.floodin.videoeditor.base.data.VideoItem
import com.floodin.videoeditor.base.util.ResUtil
import com.floodin.videoeditor.base.util.URIPathHelper
import com.floodin.videoeditor.base.util.toPrettyFileSize
import com.floodin.videoeditor.base.util.toPrettyTimeElapsed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import java.io.File

open class MainViewModel(
    private val uRIPathHelper: URIPathHelper,
    private val concatVideos: ConcatVideosUseCase,
    private val compressVideo: CompressVideoUseCase,
    private val resUtil: ResUtil,
    private val fileUtil: FileUtil,
    private val mediaInfoRepo: MediaInfoRepo
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
                    id = "videoId:$index",
                    absolutePath = videoItem.path,
                    orientation = mediaInfoRepo.getVideoOrientation(videoItem.path),
                    userRotationDegrees = 0
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

            val externalDir = fileUtil.getOutDirPath(
                appName = resUtil.getStringRes(R.string.app_name),
                dirName = ConcatVideosRepo.AUDIO_TRACKS_DIR_NAME
            )
            val trackAbsolutePath = externalDir.absolutePath + "/music.mp3"
            val audioTrackFile = File(trackAbsolutePath)
            MyLogs.LOG(
                "MainViewModel",
                "concatVideosSync",
                "trackAbsolutePath:$trackAbsolutePath audioTrackFile exists: ${audioTrackFile.exists()}"
            )
            val audioInput = if (audioTrackFile.exists()) {
                AudioInput(
                    videoLevel = 100,
                    trackLevel = 20,
                    trackAbsolutePath = audioTrackFile.absolutePath
                )
            } else {
                null
            }

            val result = concatVideos.executeSync(
                inputVideos = inputVideos,
                inputAudio = audioInput,
                resolution = VideoResolution(1280, 720),
                duration = 50f,
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
                    publishError("Concat seems cancelled")
                }
            }
        }
    }

    fun compressVideo() {
        viewModelScope.launch(Dispatchers.IO) {
            val inputVideos = selectedVideoItems.value?.mapIndexed { index, videoItem ->
                VideoInput(
                    "videoId:$index",
                    videoItem.path,
                    orientation = mediaInfoRepo.getVideoOrientation(videoItem.path)
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

            if (inputVideos.size > 1) {
                viewModelScope.launch(Dispatchers.Main) {
                    publishError("Need only 1 video for compress")
                }
                return@launch
            }

            viewModelScope.launch(Dispatchers.Main) {
                publishLoadingStateOn()
            }

            val startTimeMillis = System.currentTimeMillis()
            val result = compressVideo.executeSync(
                inputVideo = inputVideos.first(),
                resolution = VideoResolution(1280, 720),
                duration = 25f,
//                splittingMeta = VideoSplittingMeta(inputDuration = item.inputDuration),
                appId = BuildConfig.APPLICATION_ID,
                appName = resUtil.getStringRes(R.string.app_name)
            )

            when (result) {
                is FFmpegResult.Success -> {
                    val timeElapsed = (System.currentTimeMillis() - startTimeMillis)
                    val inputVideoMeta = inputVideos.first()
                    val inputVideoFile = File(inputVideoMeta.absolutePath)
                    val inputVideoFileSize = inputVideoFile.length().toPrettyFileSize()

                    val outputVideoFile = File(result.data.absolutePath)
                    val outputVideoFileSize = outputVideoFile.length().toPrettyFileSize()
                    val videoStream = mediaInfoRepo.getVideoStreamInformation(
                        inputPath = result.data.absolutePath
                    )

                    MyLogs.LOG(
                        "MainViewModel",
                        "compressVideo",
                        "results \n timeElapsed: ${timeElapsed.toPrettyTimeElapsed()} \n " +
                                "input Video : ${inputVideoFile.absolutePath} $inputVideoFileSize \n " +
                                "output Video : ${outputVideoFile.absolutePath}  $outputVideoFileSize \n " +
                                "videoStream has value : ${videoStream != null}"
                    )

                    val resultVideoMeta = result.data
                    val newVideo = VideoItem(resultVideoMeta.uri, resultVideoMeta.absolutePath)
                    val updatedList = mutableListOf(newVideo)
                    selectedVideoItems.value?.let {
                        updatedList.addAll(it)
                    }
                    viewModelScope.launch(Dispatchers.Main) {
                        publishSuccess("Compress is done successfully!")
                        selectedVideoItems.value = updatedList
                    }
                }

                is FFmpegResult.Error -> {
                    viewModelScope.launch(Dispatchers.Main) {
                        publishError(result.message)
                    }
                }

                is FFmpegResult.Cancel -> {
                    publishError("Compress seems cancelled")
                }
            }
        }
    }
}