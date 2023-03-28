package com.floodin.ffmpeg_wrapper.repo

import com.floodin.ffmpeg_wrapper.data.FFmpegResult
import com.floodin.ffmpeg_wrapper.data.VideoFormat
import com.floodin.ffmpeg_wrapper.data.VideoInput
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
     * @param format - desired video resolution
     * @param duration - desired file duration to take in consideration for final compressed video
     * @param appId - application ID
     * @param appName - application Name
     * @return result of ffmpeg command
     */
    fun execute(
        inputVideo: VideoInput,
        format: VideoFormat,
        duration: Float? = null,
        appId: String,
        appName: String
    ): FFmpegResult {
        val outputFile = fileUtil.getNewLocalCacheFile(
            appName = appName,
            COMPRESSED_DIR_NAME,
            "${System.currentTimeMillis()}.mp4"
        )
//        val command = generateCommand(
//            inputPath = inputVideo.absolutePath,
//            outputPath = outputFile.absolutePath,
//            format = format,
//            duration = duration
//        )
        val command = generateCommandForSplitVideo(
            inputPath = inputVideo.absolutePath,
            outputPath = outputFile.absolutePath,
            format = format,
            duration = duration!!,
            targetDuration = 15,
            clipDuration = 5
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
        format: VideoFormat,
        duration: Float?
    ): String {
        //ffmpeg -i input.mp4 -c:a copy -c:v vp9 -b:v 1M output.mp4
        //ffmpeg -i input.mp4 -c:a copy -s hd720 output.mp4
        //ffmpeg -y -i /source-path/input.mp4 -s 480x320 -r 25 -vcodec mpeg4 -b:v 300k -b:a 48000 -ac 2 -ar 22050 /source/output.mp4

        val widthHeight = format.value.split("x")
        val currentMaxBitrate = if (format == VideoFormat.FHD) FHD_MAX_RATE else HD_MAX_RATE
        val currentCrf = if (format == VideoFormat.FHD) FHD_CRF else HD_CRF
        MyLogs.LOG(
            "generateCommand",
            "generateCommand",
            "widthHeight: $widthHeight currentMaxBitrate: $currentMaxBitrate currentCrf: $currentCrf"
        )
        return if (duration != null) {
            "-y -i '$inputPath' -f lavfi -i anullsrc -vf \"scale=w='if(gte(iw/ih,${widthHeight[0]}/${widthHeight[1]}),${widthHeight[0]},-2)':h='if(gte(iw/ih,${widthHeight[0]}/${widthHeight[1]}),-2,${widthHeight[1]})',setsar=1,setdar=a,pad=w=${widthHeight[0]}:h=${widthHeight[1]}:x=-1:y=-1\" -crf $currentCrf -maxrate ${currentMaxBitrate}M -bufsize ${currentMaxBitrate * 2}M -r 30000/1001 -c:v libx264 -c:a aac -ar 48000 -b:a 256k -movflags faststart -pix_fmt yuv420p -preset superfast -t $duration $outputPath"
        } else {
            "-y -i '$inputPath' -f lavfi -i anullsrc -vf \"scale=w='if(gte(iw/ih,${widthHeight[0]}/${widthHeight[1]}),${widthHeight[0]},-2)':h='if(gte(iw/ih,${widthHeight[0]}/${widthHeight[1]}),-2,${widthHeight[1]})',setsar=1,setdar=a,pad=w=${widthHeight[0]}:h=${widthHeight[1]}:x=-1:y=-1\" -crf $currentCrf -maxrate ${currentMaxBitrate}M -bufsize ${currentMaxBitrate * 2}M -r 30000/1001 -c:v libx264 -c:a aac -ar 48000 -b:a 256k -movflags faststart -pix_fmt yuv420p -preset superfast $outputPath"
        }
    }


    private fun generateCommandForSplitVideo(
        inputPath: String,
        outputPath: String,
        format: VideoFormat,
        duration: Float,
        targetDuration : Int,
        clipDuration : Int
    ): String {

        val widthHeight = format.value.split("x")
        val currentMaxBitrate = if (format == VideoFormat.FHD) FHD_MAX_RATE else HD_MAX_RATE
        val currentCrf = if (format == VideoFormat.FHD) FHD_CRF else HD_CRF
        MyLogs.LOG(
            "generateCommandForSplitVideo",
            "generateCommandForSplitVideo",
            "widthHeight: $widthHeight currentMaxBitrate: $currentMaxBitrate currentCrf: $currentCrf"
        )
        // duration logic
        val totalSections = (targetDuration/clipDuration).toInt()
        val interval = (duration/totalSections).toInt();
        val listOfTimestamps = ArrayList<Int>()
        val offset = interval - ((interval - clipDuration)/2).toInt()
        var intervalSum = interval;
        while(intervalSum <= duration){
            listOfTimestamps.add(intervalSum - offset)
            intervalSum += interval
        }

        var videoTrimCommand = ""
        var audioTrimCommand = ""
        var videoOutputList = ""
        var audioOutputList = ""
        var count = 1
        for(timestamp in listOfTimestamps){
            val videoOutput = "[v${count}]"
            val audioOutput = "[a${count}]"
            videoTrimCommand += "[0:v]trim=start=${timestamp}:duration=${clipDuration},setpts=PTS-STARTPTS${videoOutput},"
            audioTrimCommand += "[0:a]atrim=start=${timestamp}:duration=${clipDuration},asetpts=PTS-STARTPTS${audioOutput},"
            videoOutputList += videoOutput
            audioOutputList += audioOutput
            count++
        }
        val ffmpegCommand = "-y -i '$inputPath' -f lavfi -i anullsrc -filter_complex \"${videoTrimCommand}${videoOutputList}concat=n=${listOfTimestamps.size}:v=1:a=0,scale=w='if(gte(iw/ih,${widthHeight[0]}/${widthHeight[1]}),${widthHeight[0]},-2)':h='if(gte(iw/ih,${widthHeight[0]}/${widthHeight[1]}),-2,${widthHeight[1]})',setsar=1,setdar=a,pad=w=${widthHeight[0]}:h=${widthHeight[1]}:x=-1:y=-1[ov]\" -filter_complex \"${audioTrimCommand}${audioOutputList}concat=n=${listOfTimestamps.size}:v=0:a=1[oa]\" -map \"[ov]\" -map \"[oa]\" -crf $currentCrf -maxrate ${currentMaxBitrate}M -bufsize ${currentMaxBitrate * 2}M -r 30000/1001 -c:v libx264 -c:a aac -ar 48000 -b:a 256k -movflags faststart -pix_fmt yuv420p -preset superfast $outputPath"
        println(ffmpegCommand)
        return ffmpegCommand
    }


    companion object {
        const val COMPRESSED_DIR_NAME = "compressed"
        private const val FHD_MAX_RATE = 9
        private const val HD_MAX_RATE = 6
        private const val FHD_CRF = 22
        private const val HD_CRF = 24
    }
}