package com.floodin.ffmpeg_wrapper.repo

import com.floodin.ffmpeg_wrapper.data.FFmpegResult
import com.floodin.ffmpeg_wrapper.data.VideoInput
import com.floodin.ffmpeg_wrapper.data.VideoOrientation
import com.floodin.ffmpeg_wrapper.data.VideoResolution
import com.floodin.ffmpeg_wrapper.data.VideoSplittingMeta
import com.floodin.ffmpeg_wrapper.data.isBetterThanHD
import com.floodin.ffmpeg_wrapper.data.toCompressedHeight
import com.floodin.ffmpeg_wrapper.data.toCompressedWidth
import com.floodin.ffmpeg_wrapper.data.toRotationTransposeCmd
import com.floodin.ffmpeg_wrapper.util.FfmpegCommandUtil
import com.floodin.ffmpeg_wrapper.util.FileUtil
import com.floodin.ffmpeg_wrapper.util.MyLogs
import kotlin.math.floor


class CompressVideoRepo(
    private val fileUtil: FileUtil,
    private val cmdUtil: FfmpegCommandUtil,
    private val mediaInfoRepo: MediaInfoRepo
) {

    /**
     * Compress video and generate new one
     *
     * @param inputVideo - input video file meta
     * @param resolution - final compressed video resolution
     * this restriction is applied only for concat video to meet overall video format.
     * For compression only, we should not care about orientation so input video's orientation
     * will be the same for final compressed video.
     * @param duration - final compressed video duration
     * @param splittingMeta - final compressed video splitting meta
     * @param appId - application ID
     * @param appName - application Name
     * @return result of ffmpeg command execution
     */
    fun execute(
        inputVideo: VideoInput,
        resolution: VideoResolution,
        duration: Float? = null,
        splittingMeta: VideoSplittingMeta? = null,
        appId: String,
        appName: String
    ): FFmpegResult {
        val outputFile = fileUtil.getNewLocalCacheFile(
            appName = appName,
            COMPRESSED_DIR_NAME,
            "${System.currentTimeMillis()}.mp4"
        )
        val isSplittingRequired = duration != null && splittingMeta != null &&
                splittingMeta.inputDuration > duration &&
                duration > splittingMeta.sectionDuration * 2

        val command = if (isSplittingRequired) {
            generateCommandWithSplitting(
                inputPath = inputVideo.absolutePath,
                outputPath = outputFile.absolutePath,
                resolution = resolution,
                orientation = inputVideo.orientation,
                userRotationDegrees = inputVideo.userRotationDegrees,
                targetDuration = duration!!,
                splittingMeta = splittingMeta!!
            )

        } else {
            generateCommand(
                inputPath = inputVideo.absolutePath,
                outputPath = outputFile.absolutePath,
                resolution = resolution,
                orientation = inputVideo.orientation,
                userRotationDegrees = inputVideo.userRotationDegrees,
                duration = duration
            )
        }

        MyLogs.LOG(
            "CompressVideoRepo",
            "execute",
            "inputVideo:$inputVideo outputPath:${outputFile.absolutePath} expected resolution:$resolution isSplittingRequired:$isSplittingRequired splittingMeta:$splittingMeta full ffmpeg command: $command"
        )
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
        orientation: VideoOrientation,
        userRotationDegrees: Int,
        duration: Float?,
    ): Array<String> {
        val videoBitrate = if (resolution.isBetterThanHD()) FHD_VIDEO_BITRATE else HD_VIDEO_BITRATE

        val rotationTransposeCmd = userRotationDegrees.toRotationTransposeCmd()
        val compressedVideoWidth = resolution.toCompressedWidth(orientation)
        val compressedVideoHeight = resolution.toCompressedHeight(orientation)

        MyLogs.LOG(
            "CompressVideoRepo",
            "generateCommand",
            "resolution:$resolution ---> final resolution:$compressedVideoWidth x $compressedVideoHeight videoBitrate: ${videoBitrate}M"
        )

        val filterComplex =
            "[0:v]scale=w='if(gte(iw/ih,${compressedVideoWidth}/${compressedVideoHeight}),${compressedVideoWidth},-2)':h='if(gte(iw/ih,${compressedVideoWidth}/${compressedVideoHeight}),-2,${compressedVideoHeight})',setsar=1,setdar=a,pad=w=${compressedVideoWidth}:h=${compressedVideoHeight}:x=-1:y=-1,${rotationTransposeCmd}[vout]"
        val common = arrayOf(
            "-y",
            "-i", inputPath,
            "-f", "lavfi",
            "-i", "anullsrc",
            "-filter_complex", filterComplex,
            "-map", "[vout]",
            "-map", "0:a?",
            "-map", "1:a",
            "-b:v", "${videoBitrate}M",
            "-maxrate", "${videoBitrate}M",
            "-bufsize", "${videoBitrate * 2}M",
            "-r", "30000/1001",
            "-c:v", "h264_mediacodec",
            "-c:a", "aac",
            "-ar", "48000",
            "-b:a", "256k",
            "-movflags", "faststart",
            "-pix_fmt", "yuv420p",
        )
        return if (duration != null) {
            common + arrayOf("-t", "$duration", outputPath)
        } else {
            common + arrayOf("-shortest", outputPath)
        }
    }

    private fun generateCommandWithSplitting(
        inputPath: String,
        outputPath: String,
        resolution: VideoResolution,
        orientation: VideoOrientation,
        userRotationDegrees: Int,
        targetDuration: Float,
        splittingMeta: VideoSplittingMeta
    ): Array<String> {
        val videoBitrate = if (resolution.isBetterThanHD()) FHD_VIDEO_BITRATE else HD_VIDEO_BITRATE

        val rotationTransposeCmd = userRotationDegrees.toRotationTransposeCmd()
        val compressedVideoWidth = resolution.toCompressedWidth(orientation)
        val compressedVideoHeight = resolution.toCompressedHeight(orientation)

        MyLogs.LOG(
            "CompressVideoRepo",
            "generateCommandWithSplitting",
            "resolution:$resolution ---> resolution:$compressedVideoWidth x $compressedVideoHeight targetDuration:$targetDuration splittingMeta:$splittingMeta videoBitrate: ${videoBitrate}M"
        )

        var videoTrimCommand = ""
        var audioTrimCommand = ""
        var videoOutputList = ""
        var audioOutputList = ""
        var count = 1

        val sectionsAmount = floor(targetDuration / splittingMeta.sectionDuration).toInt()
        var targetDurationDelta = targetDuration - (splittingMeta.sectionDuration * sectionsAmount)

        val intervalForSection = splittingMeta.inputDuration / sectionsAmount
        val freeInterval = intervalForSection - splittingMeta.sectionDuration
        var intervalSum = intervalForSection
        var startPosition = 0f

        while (intervalSum <= splittingMeta.inputDuration) {
            val optimisedSectionDuration = if (targetDurationDelta > 0) {
                if (targetDurationDelta >= freeInterval) {
                    targetDurationDelta -= freeInterval
                    splittingMeta.sectionDuration + freeInterval
                } else {
                    val targetDurationDeltaCopy = targetDurationDelta
                    targetDurationDelta -= targetDurationDeltaCopy
                    splittingMeta.sectionDuration + targetDurationDeltaCopy
                }

            } else {
                splittingMeta.sectionDuration
            }

            val videoOutput = "[v${count}]"
            val audioOutput = "[a${count}]"
            videoTrimCommand += "[0:v]trim=start=${startPosition}:duration=${optimisedSectionDuration},setpts=PTS-STARTPTS${videoOutput},"
            audioTrimCommand += "[0:a]atrim=start=${startPosition}:duration=${optimisedSectionDuration},asetpts=PTS-STARTPTS${audioOutput},"
            videoOutputList += videoOutput
            audioOutputList += audioOutput

            startPosition += intervalForSection
            intervalSum += intervalForSection
            count++
        }

        val videoFilterComplex =
            "${videoTrimCommand}${videoOutputList}concat=n=${sectionsAmount}:v=1:a=0,scale=w='if(gte(iw/ih,${compressedVideoWidth}/${compressedVideoHeight}),${compressedVideoWidth},-2)':h='if(gte(iw/ih,${compressedVideoWidth}/${compressedVideoHeight}),-2,${compressedVideoHeight})',setsar=1,setdar=a,pad=w=${compressedVideoWidth}:h=${compressedVideoHeight}:x=-1:y=-1,${rotationTransposeCmd}[ov]"
        val audioFilterComplex =
            "${audioTrimCommand}${audioOutputList}concat=n=${sectionsAmount}:v=0:a=1[oa]"
        return arrayOf(
            "-y",
            "-i", inputPath,
            "-f", "lavfi",
            "-i", "anullsrc",
            "-filter_complex", "${videoFilterComplex};${audioFilterComplex}",
            "-map", "[ov]",
            "-map", "[oa]",
            "-b:v", "${videoBitrate}M",
            "-maxrate", "${videoBitrate}M",
            "-bufsize", "${videoBitrate * 2}M",
            "-r", "30000/1001",
            "-c:v", "h264_mediacodec",
            "-c:a", "aac",
            "-ar", "48000",
            "-b:a", "256k",
            "-movflags", "faststart",
            "-pix_fmt", "yuv420p",
            outputPath
        )
    }

    companion object {
        const val COMPRESSED_DIR_NAME = "compressed"
        private const val FHD_VIDEO_BITRATE = 9
        private const val HD_VIDEO_BITRATE = 6
        const val SECTION_DURATION = 5f
    }
}