package com.floodin.ffmpeg_wrapper.repo

import com.floodin.ffmpeg_wrapper.data.FFmpegResult
import com.floodin.ffmpeg_wrapper.data.VideoInput
import com.floodin.ffmpeg_wrapper.data.VideoOrientationMeta
import com.floodin.ffmpeg_wrapper.data.VideoResolution
import com.floodin.ffmpeg_wrapper.data.VideoSplittingMeta
import com.floodin.ffmpeg_wrapper.data.isBetterThanHD
import com.floodin.ffmpeg_wrapper.data.toCompressedHeight
import com.floodin.ffmpeg_wrapper.data.toCompressedWidth
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
     * @param orientation - final compressed video MUST be portrait OR landscape,
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
        orientation: VideoOrientationMeta? = null,
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
                orientation = orientation,
                targetDuration = duration!!,
                splittingMeta = splittingMeta!!
            )

        } else {
            generateCommand(
                inputPath = inputVideo.absolutePath,
                outputPath = outputFile.absolutePath,
                resolution = resolution,
                orientation = orientation,
                duration = duration
            )
        }

        MyLogs.LOG(
            "CompressVideoRepo",
            "execute",
            "inputPath: ${inputVideo.absolutePath} outputPath:${outputFile.absolutePath} expected resolution:$resolution isSplittingRequired:$isSplittingRequired splittingMeta:$splittingMeta expected orientation:$orientation full ffmpeg command: $command"
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
        orientation: VideoOrientationMeta?,
        duration: Float?,
    ): String {
        val currentMaxBitrate = if (resolution.isBetterThanHD()) FHD_MAX_RATE else HD_MAX_RATE
        val currentCrf = if (resolution.isBetterThanHD()) FHD_CRF else HD_CRF
//        val rotationMeta = mediaInfoRepo.getVideoRotationMeta(inputPath)
//        val orientationMeta = rotationMeta.toOrientation()
//        val rotationTransposeCmd = rotationMeta.rotation.toRotationTransposeNewCmd()
        val compressedVideoWidth = resolution.toCompressedWidth(orientation)
        val compressedVideoHeight = resolution.toCompressedHeight(orientation)
        MyLogs.LOG(
            "CompressVideoRepo",
            "generateCommand",
            "resolution:$compressedVideoWidth x $compressedVideoHeight currentMaxBitrate: $currentMaxBitrate currentCrf: $currentCrf"
        )
        return if (duration != null) {
            //to add translation rotation we should add [,${rotationValue}] after [h=${compressedVideoHeight}:x=-1:y=-1]
            "-y -i '$inputPath' -f lavfi -i anullsrc -filter_complex \"[0:v]scale=w='if(gte(iw/ih,${compressedVideoWidth}/${compressedVideoHeight}),${compressedVideoWidth},-2)':h='if(gte(iw/ih,${compressedVideoWidth}/${compressedVideoHeight}),-2,${compressedVideoHeight})',setsar=1,setdar=a,pad=w=${compressedVideoWidth}:h=${compressedVideoHeight}:x=-1:y=-1[vout]\" -map \"[vout]\" -map \"0:a?\" -map \"1:a\" -crf $currentCrf -maxrate ${currentMaxBitrate}M -bufsize ${currentMaxBitrate * 2}M -r 30000/1001 -c:v libx264 -c:a aac -ar 48000 -b:a 256k -movflags faststart -pix_fmt yuv420p -preset superfast -t $duration $outputPath"
        } else {
            "-y -i '$inputPath' -f lavfi -i anullsrc -filter_complex \"[0:v]scale=w='if(gte(iw/ih,${compressedVideoWidth}/${compressedVideoHeight}),${compressedVideoWidth},-2)':h='if(gte(iw/ih,${compressedVideoWidth}/${compressedVideoHeight}),-2,${compressedVideoHeight})',setsar=1,setdar=a,pad=w=${compressedVideoWidth}:h=${compressedVideoHeight}:x=-1:y=-1[vout]\" -map \"[vout]\" -map \"0:a?\" -map \"1:a\" -crf $currentCrf -maxrate ${currentMaxBitrate}M -bufsize ${currentMaxBitrate * 2}M -r 30000/1001 -c:v libx264 -c:a aac -ar 48000 -b:a 256k -movflags faststart -pix_fmt yuv420p -preset superfast -shortest $outputPath"
        }
    }

    private fun generateCommandWithSplitting(
        inputPath: String,
        outputPath: String,
        resolution: VideoResolution,
        orientation: VideoOrientationMeta?,
        targetDuration: Float,
        splittingMeta: VideoSplittingMeta
    ): String {
        val currentMaxBitrate = if (resolution.isBetterThanHD()) FHD_MAX_RATE else HD_MAX_RATE
        val currentCrf = if (resolution.isBetterThanHD()) FHD_CRF else HD_CRF
        val compressedVideoWidth = resolution.toCompressedWidth(orientation)
        val compressedVideoHeight = resolution.toCompressedHeight(orientation)
        MyLogs.LOG(
            "CompressVideoRepo",
            "generateCommandWithSplitting",
            "resolution:$compressedVideoWidth x $compressedVideoHeight targetDuration:$targetDuration splittingMeta:$splittingMeta currentMaxBitrate: $currentMaxBitrate currentCrf: $currentCrf"
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

        return "-y -i '$inputPath' -f lavfi -i anullsrc -filter_complex \"${videoTrimCommand}${videoOutputList}concat=n=${sectionsAmount}:v=1:a=0,scale=w='if(gte(iw/ih,${compressedVideoWidth}/${compressedVideoHeight}),${compressedVideoWidth},-2)':h='if(gte(iw/ih,${compressedVideoWidth}/${compressedVideoHeight}),-2,${compressedVideoHeight})',setsar=1,setdar=a,pad=w=${compressedVideoWidth}:h=${compressedVideoHeight}:x=-1:y=-1[ov]\" -filter_complex \"${audioTrimCommand}${audioOutputList}concat=n=${sectionsAmount}:v=0:a=1[oa]\" -map \"[ov]\" -map \"[oa]\" -crf $currentCrf -maxrate ${currentMaxBitrate}M -bufsize ${currentMaxBitrate * 2}M -r 30000/1001 -c:v libx264 -c:a aac -ar 48000 -b:a 256k -movflags faststart -pix_fmt yuv420p -preset superfast $outputPath"
//        return "-y -i '$inputPath' -f lavfi -i anullsrc -filter_complex \"${videoTrimCommand}${videoOutputList}concat=n=${listOfTimestamps.size}:v=1:a=0,scale=w='if(gte(iw/ih,${widthHeight[0]}/${widthHeight[1]}),${widthHeight[0]},-2)':h='if(gte(iw/ih,${widthHeight[0]}/${widthHeight[1]}),-2,${widthHeight[1]})',setsar=1,setdar=a,pad=w=${widthHeight[0]}:h=${widthHeight[1]}:x=-1:y=-1[ov]\" -filter_complex \"${audioTrimCommand}${audioOutputList}concat=n=${listOfTimestamps.size}:v=0:a=1[oa]\" -map \"[ov]\" -map \"[oa]\" -crf $currentCrf -maxrate ${currentMaxBitrate}M -bufsize ${currentMaxBitrate * 2}M -r 30000/1001 -c:v libx264 -c:a aac -ar 48000 -b:a 256k -movflags faststart -pix_fmt yuv420p -preset superfast $outputPath"
    }

    companion object {
        const val COMPRESSED_DIR_NAME = "compressed"
        private const val FHD_MAX_RATE = 9
        private const val HD_MAX_RATE = 6
        private const val FHD_CRF = 22
        private const val HD_CRF = 24
        const val SECTION_DURATION = 5f
    }
}
