package com.yourname.aichatmvptest.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class LocalAudioController(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var recordingFile: File? = null
    private var startedAt: Long = 0L

    fun startRecording(): Boolean {
        stopPlayback()
        val dir = File(context.filesDir, "voice").apply { mkdirs() }
        val file = File(dir, "voice_${System.currentTimeMillis()}.m4a")
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        return runCatching {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.setAudioEncodingBitRate(96_000)
            mediaRecorder.setAudioSamplingRate(44_100)
            mediaRecorder.setOutputFile(file.absolutePath)
            mediaRecorder.prepare()
            mediaRecorder.start()
            recorder = mediaRecorder
            recordingFile = file
            startedAt = System.currentTimeMillis()
            true
        }.getOrElse {
            mediaRecorder.release()
            false
        }
    }

    fun stopRecording(): RecordedAudio? {
        val file = recordingFile ?: return null
        val durationMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(300L)
        runCatching { recorder?.stop() }
        recorder?.release()
        recorder = null
        recordingFile = null
        return RecordedAudio(path = file.absolutePath, durationMs = durationMs)
    }

    fun play(path: String) {
        stopPlayback()
        player = MediaPlayer().apply {
            setDataSource(path.removePrefix("file://"))
            prepare()
            start()
            setOnCompletionListener { stopPlayback() }
        }
    }

    fun saveTtsAudio(bytes: ByteArray, extension: String = "mp3"): RecordedAudio? {
        if (bytes.isEmpty()) return null
        val dir = File(context.filesDir, "voice").apply { mkdirs() }
        val file = File(dir, "tts_${System.currentTimeMillis()}.$extension")
        file.writeBytes(bytes)
        return RecordedAudio(path = file.absolutePath, durationMs = estimateDurationMs(bytes))
    }

    fun stopPlayback() {
        runCatching { player?.stop() }
        player?.release()
        player = null
    }

    private fun estimateDurationMs(bytes: ByteArray): Long {
        // MP3 duration requires frame parsing; this rough estimate is enough for chat bubble display.
        return (bytes.size / 16L).coerceIn(800L, 120_000L)
    }
}

data class RecordedAudio(
    val path: String,
    val durationMs: Long,
)
