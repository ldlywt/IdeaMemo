package com.ldlywt.note.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import com.ldlywt.note.bean.Attachment
import java.io.File

/**
 * 使用 MediaRecorder 录制 AAC 音频，输出到应用私有的 Music 目录。
 * 录制完成的文件会作为 [Attachment.Type.AUDIO] 附件保存到笔记中。
 */
class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTime = 0L
    private var recording = false

    val isRecording: Boolean
        get() = recording

    /** 当前已录制时长（毫秒）。 */
    val elapsedMs: Long
        get() = if (recording) SystemClock.elapsedRealtime() - startTime else 0L

    fun start(): Boolean {
        if (recording) return true
        return try {
            val file = createOutputFile()
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            outputFile = file
            startTime = SystemClock.elapsedRealtime()
            recording = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            releaseQuietly()
            outputFile?.delete()
            outputFile = null
            recording = false
            false
        }
    }

    /**
     * 停止录音并返回附件。录音过短或录制失败时返回 null，并删除临时文件。
     */
    fun stop(): Attachment? {
        if (!recording) return null
        val file = outputFile
        val duration = elapsedMs
        var stopped = true
        try {
            recorder?.stop()
        } catch (e: Exception) {
            // 录音时间过短时 MediaRecorder.stop() 会抛出异常
            stopped = false
            e.printStackTrace()
        }
        releaseQuietly()
        recording = false
        outputFile = null

        if (!stopped || file == null || !file.exists() || file.length() == 0L || duration < MIN_DURATION_MS) {
            file?.delete()
            return null
        }
        return Attachment(
            type = Attachment.Type.AUDIO,
            path = file.absolutePath,
            fileName = file.name,
            description = file.name,
        )
    }

    /** 取消录音并删除已经写入的文件。 */
    fun cancel() {
        if (recording) {
            try {
                recorder?.stop()
            } catch (_: Exception) {
                // ignore
            }
        }
        releaseQuietly()
        recording = false
        outputFile?.delete()
        outputFile = null
    }

    private fun createOutputFile(): File {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
        val recordDir = File(baseDir, "recordings").apply { if (!exists()) mkdirs() }
        return File(recordDir, "record_${System.currentTimeMillis()}.m4a")
    }

    private fun releaseQuietly() {
        try {
            recorder?.release()
        } catch (_: Exception) {
            // ignore
        }
        recorder = null
    }

    private companion object {
        const val MIN_DURATION_MS = 600L
    }
}

/** 读取音频文件时长（毫秒），读取失败时返回 0。 */
fun getAudioDurationMs(path: String): Long {
    if (path.isBlank()) return 0L
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(path)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull() ?: 0L
    } catch (_: Exception) {
        0L
    } finally {
        try {
            retriever.release()
        } catch (_: Exception) {
            // ignore
        }
    }
}

/** 将毫秒格式化为 mm:ss（超过一小时为 h:mm:ss）。 */
fun formatDuration(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L)) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
