package com.floodin.ffmpeg_wrapper.repo

import com.floodin.ffmpeg_wrapper.data.FFmpegResult
import com.floodin.ffmpeg_wrapper.data.VideoInput
import com.floodin.ffmpeg_wrapper.data.VideoResolution
import com.floodin.ffmpeg_wrapper.data.isBetterThenHD
import com.floodin.ffmpeg_wrapper.util.FfmpegCommandUtil
import com.floodin.ffmpeg_wrapper.util.FileUtil
import com.floodin.ffmpeg_wrapper.util.MyLogs


class CompressVideoRepo(
    private val fileUtil: FileUtil,
    private val cmdUtil: FfmpegCommandUtil
) {

    /**
     * Compress video and generate new one
     *
     * @param inputVideo - input video file meta
     * @param resolution - desired video resolution
     * @param duration - desired file duration to take in consideration for final compressed video
     * @param appId - application ID
     * @param appName - application Name
     * @return result of ffmpeg command
     */
    fun execute(
        inputVideo: VideoInput,
        resolution: VideoResolution,
        duration: Float? = null,
        isPortrait: Boolean,
        appId: String,
        appName: String
    ): FFmpegResult {
        val outputFile = fileUtil.getNewLocalCacheFile(
            appName = appName,
            COMPRESSED_DIR_NAME,
            "${System.currentTimeMillis()}.mp4"
        )
        val command = generateCommand(
            inputPath = inputVideo.absolutePath,
            outputPath = outputFile.absolutePath,
            resolution = resolution,
            isPortrait = isPortrait,
            duration = duration
        )
        MyLogs.LOG("CompressVideoRepo", "compressVideo", "command: $command")
        return cmdUtil.executeSync(
            inputVideo.id,
            command,
            outputFile,
            appId
        )
    }

    private fun generateCommand(
        inputPath: String,
        outputPath: String,
        resolution: VideoResolution,
        isPortrait: Boolean,
        duration: Float?
    ): String {
        //ffmpeg -i input.mp4 -c:a copy -c:v vp9 -b:v 1M output.mp4
        //ffmpeg -i input.mp4 -c:a copy -s hd720 output.mp4
        //ffmpeg -y -i /source-path/input.mp4 -s 480x320 -r 25 -vcodec mpeg4 -b:v 300k -b:a 48000 -ac 2 -ar 22050 /source/output.mp4

        val currentMaxBitrate = if (resolution.isBetterThenHD()) FHD_MAX_RATE else HD_MAX_RATE
        val currentCrf = if (resolution.isBetterThenHD()) FHD_CRF else HD_CRF
        val compressedVideoWidth = if (isPortrait) {
            resolution.height
        } else {
            resolution.width
        }
        val compressedVideoHeight = if (isPortrait) {
            resolution.width
        } else {
            resolution.height
        }
        MyLogs.LOG(
            "generateCommand",
            "generateCommand",
            "videoResolution: $resolution width:$compressedVideoWidth height:$compressedVideoHeight currentMaxBitrate: $currentMaxBitrate currentCrf: $currentCrf"
        )
        return if (duration != null) {
            "-y -i '$inputPath' -f lavfi -i anullsrc -vf \"scale=w='if(gte(iw/ih,${compressedVideoWidth}/${compressedVideoHeight}),${compressedVideoWidth},-2)':h='if(gte(iw/ih,${compressedVideoWidth}/${compressedVideoHeight}),-2,${compressedVideoHeight})',setsar=1,setdar=a,pad=w=${compressedVideoWidth}:h=${compressedVideoHeight}:x=-1:y=-1\" -crf $currentCrf -maxrate ${currentMaxBitrate}M -bufsize ${currentMaxBitrate * 2}M -r 30000/1001 -c:v libx264 -c:a aac -ar 48000 -b:a 256k -movflags faststart -pix_fmt yuv420p -preset superfast -t $duration $outputPath"
        } else {
            "-y -i '$inputPath' -f lavfi -i anullsrc -vf \"scale=w='if(gte(iw/ih,${compressedVideoWidth}/${compressedVideoHeight}),${compressedVideoWidth},-2)':h='if(gte(iw/ih,${compressedVideoWidth}/${compressedVideoHeight}),-2,${compressedVideoHeight})',setsar=1,setdar=a,pad=w=${compressedVideoWidth}:h=${compressedVideoHeight}:x=-1:y=-1\" -crf $currentCrf -maxrate ${currentMaxBitrate}M -bufsize ${currentMaxBitrate * 2}M -r 30000/1001 -c:v libx264 -c:a aac -ar 48000 -b:a 256k -movflags faststart -pix_fmt yuv420p -preset superfast $outputPath"
        }
    }


    companion object {
        const val COMPRESSED_DIR_NAME = "compressed"
        private const val FHD_MAX_RATE = 9
        private const val HD_MAX_RATE = 6
        private const val FHD_CRF = 22
        private const val HD_CRF = 24
    }
}