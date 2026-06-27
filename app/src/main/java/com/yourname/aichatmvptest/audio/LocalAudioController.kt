package com.yourname.aichatmvptest.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import java.io.File
import kotlin.concurrent.thread

class LocalAudioController(private val context: Context) {
    private var recorder: AudioRecord? = null
    private var player: MediaPlayer? = null
    private var recordingFile: File? = null
    private var startedAt: Long = 0L
    @Volatile private var isRecording = false
    private var recordingThread: Thread? = null
    private val pcmChunks = mutableListOf<ByteArray>()
    private val pcmLock = Any()
    @Volatile private var lastSpeechAt: Long = 0L
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var speakerEnabled = false

    @SuppressLint("MissingPermission")
    fun startRecording(): Boolean {
        if (isRecording) return false
        stopPlayback()
        val dir = File(context.filesDir, "voice").apply { mkdirs() }
        val file = File(dir, "voice_${System.currentTimeMillis()}.wav")
        val minBufferSize = AudioRecord.getMinBufferSize(
            RECORD_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBufferSize <= 0) return false

        return runCatching {
            val bufferSize = minBufferSize.coerceAtLeast(RECORD_SAMPLE_RATE / 2)
            val audioRecord = AudioRecord(
                android.media.MediaRecorder.AudioSource.MIC,
                RECORD_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
            )
            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord.release()
                error("AudioRecord init failed")
            }

            synchronized(pcmLock) { pcmChunks.clear() }
            audioRecord.startRecording()
            recorder = audioRecord
            recordingFile = file
            startedAt = System.currentTimeMillis()
            lastSpeechAt = startedAt
            isRecording = true
            recordingThread = thread(name = "rhodes-audio-record") {
                val buffer = ByteArray(bufferSize)
                while (isRecording) {
                    val read = audioRecord.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        val chunk = buffer.copyOf(read)
                        if (averageAbsPcm16(chunk) > RECORD_SPEECH_THRESHOLD) lastSpeechAt = System.currentTimeMillis()
                        synchronized(pcmLock) { pcmChunks += chunk }
                    }
                }
            }
            true
        }.getOrElse {
            Log.e(AUDIO_LOG_TAG, "keyword=$AUDIO_LOG_KEYWORD stage=record_start_exception", it)
            stopRecordingInternal()
            false
        }
    }

    fun stopRecording(): RecordedAudio? {
        val file = recordingFile ?: return null
        val durationMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(300L)
        stopRecordingInternal()
        val pcmBytes = synchronized(pcmLock) {
            val output = ByteArray(pcmChunks.sumOf { it.size })
            var offset = 0
            pcmChunks.forEach { chunk ->
                chunk.copyInto(output, destinationOffset = offset)
                offset += chunk.size
            }
            pcmChunks.clear()
            output
        }
        recordingFile = null
        if (pcmBytes.isEmpty()) return null
        file.writeBytes(buildWavFile(pcmBytes, RECORD_SAMPLE_RATE, 1, 16))
        return RecordedAudio(path = file.absolutePath, durationMs = durationMs)
    }

    fun readPcmFromWav(path: String): ByteArray {
        val bytes = File(path.removePrefix("file://")).readBytes()
        if (bytes.size <= WAV_HEADER_SIZE) return ByteArray(0)
        return bytes.copyOfRange(WAV_HEADER_SIZE, bytes.size)
    }

    fun play(path: String, onComplete: ((Boolean) -> Unit)? = null) {
        Log.d(AUDIO_LOG_TAG, "keyword=$AUDIO_LOG_KEYWORD stage=play_start path=$path")
        stopPlayback()
        val file = File(path.removePrefix("file://"))
        Log.d(AUDIO_LOG_TAG, "keyword=$AUDIO_LOG_KEYWORD stage=play_file_check exists=${file.exists()} size=${file.length()} absolute=${file.absolutePath}")
        Log.d(AUDIO_LOG_TAG, "keyword=$AUDIO_LOG_KEYWORD stage=audio_route mode=${audioManager.mode} musicVolume=${audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)}/${audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)}")
        runCatching {
            val focusGranted = requestPlaybackFocus()
            Log.d(AUDIO_LOG_TAG, "keyword=$AUDIO_LOG_KEYWORD stage=audio_focus granted=$focusGranted")
            player = MediaPlayer().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                } else {
                    @Suppress("DEPRECATION")
                    setAudioStreamType(AudioManager.STREAM_MUSIC)
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(AUDIO_LOG_TAG, "keyword=$AUDIO_LOG_KEYWORD stage=media_player_error what=$what extra=$extra")
                    stopPlayback()
                    onComplete?.invoke(false)
                    true
                }
                setDataSource(file.absolutePath)
                prepare()
                Log.d(AUDIO_LOG_TAG, "keyword=$AUDIO_LOG_KEYWORD stage=play_prepared duration=$duration")
                start()
                Log.d(AUDIO_LOG_TAG, "keyword=$AUDIO_LOG_KEYWORD stage=play_started")
                setOnCompletionListener {
                    Log.d(AUDIO_LOG_TAG, "keyword=$AUDIO_LOG_KEYWORD stage=play_completed")
                    stopPlayback()
                    onComplete?.invoke(true)
                }
            }
        }.getOrElse { error ->
            Log.e(AUDIO_LOG_TAG, "keyword=$AUDIO_LOG_KEYWORD stage=play_exception path=$path", error)
            stopPlayback()
            onComplete?.invoke(false)
        }
    }

    fun hasRecentSpeech(path: String, tailMs: Int = 900, threshold: Int = 550): Boolean {
        val pcm = runCatching { readPcmFromWav(path) }.getOrDefault(ByteArray(0))
        if (pcm.size < 2) return false
        val bytesPerMs = RECORD_SAMPLE_RATE * 2 / 1000
        val tailBytes = (tailMs * bytesPerMs).coerceAtMost(pcm.size)
        val start = (pcm.size - tailBytes).coerceAtLeast(0)
        var sum = 0L
        var count = 0
        var index = start
        while (index + 1 < pcm.size) {
            val sample = ((pcm[index + 1].toInt() shl 8) or (pcm[index].toInt() and 0xff)).toShort().toInt()
            sum += kotlin.math.abs(sample)
            count++
            index += 2
        }
        return count > 0 && sum / count > threshold
    }

    fun saveTtsAudio(bytes: ByteArray, extension: String = "mp3"): RecordedAudio? {
        Log.d(AUDIO_LOG_TAG, "keyword=$AUDIO_LOG_KEYWORD stage=save_tts_start bytes=${bytes.size} extension=$extension")
        if (bytes.isEmpty()) return null
        val dir = File(context.filesDir, "voice").apply { mkdirs() }
        val file = File(dir, "tts_${System.currentTimeMillis()}.$extension")
        file.writeBytes(bytes)
        Log.d(AUDIO_LOG_TAG, "keyword=$AUDIO_LOG_KEYWORD stage=save_tts_success path=${file.absolutePath} exists=${file.exists()} size=${file.length()}")
        return RecordedAudio(path = file.absolutePath, durationMs = estimateDurationMs(bytes))
    }

    fun stopPlayback() {
        runCatching { player?.stop() }
        player?.release()
        player = null
        abandonPlaybackFocus()
    }

    fun setSpeakerEnabled(enabled: Boolean) {
        speakerEnabled = enabled
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = enabled
        audioManager.mode = if (enabled) AudioManager.MODE_IN_COMMUNICATION else AudioManager.MODE_NORMAL
        Log.d(AUDIO_LOG_TAG, "keyword=$AUDIO_LOG_KEYWORD stage=speaker_toggle enabled=$enabled")
    }

    fun isSpeakerEnabled(): Boolean = speakerEnabled

    fun hasRecordingBeenSilent(silenceMs: Long = 1000L): Boolean {
        return isRecording && System.currentTimeMillis() - lastSpeechAt >= silenceMs
    }

    private fun stopRecordingInternal() {
        isRecording = false
        recordingThread?.join(500L)
        recordingThread = null
        runCatching { recorder?.stop() }
        recorder?.release()
        recorder = null
    }

    private fun requestPlaybackFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener { change ->
                    Log.d(AUDIO_LOG_TAG, "keyword=$AUDIO_LOG_KEYWORD stage=audio_focus_change change=$change")
                }
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                { change -> Log.d(AUDIO_LOG_TAG, "keyword=$AUDIO_LOG_KEYWORD stage=audio_focus_change change=$change") },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonPlaybackFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    private fun estimateDurationMs(bytes: ByteArray): Long {
        // MP3 duration requires frame parsing; this rough estimate is enough for chat bubble display.
        return (bytes.size / 16L).coerceIn(800L, 120_000L)
    }

    private fun averageAbsPcm16(bytes: ByteArray): Int {
        if (bytes.size < 2) return 0
        var sum = 0L
        var count = 0
        var index = 0
        while (index + 1 < bytes.size) {
            val sample = ((bytes[index + 1].toInt() shl 8) or (bytes[index].toInt() and 0xff)).toShort().toInt()
            sum += kotlin.math.abs(sample)
            count++
            index += 2
        }
        return if (count == 0) 0 else (sum / count).toInt()
    }

    private fun buildWavFile(pcm: ByteArray, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val output = ByteArray(WAV_HEADER_SIZE + pcm.size)
        fun writeAscii(offset: Int, value: String) {
            value.forEachIndexed { index, char -> output[offset + index] = char.code.toByte() }
        }
        fun writeIntLe(offset: Int, value: Int) {
            output[offset] = (value and 0xff).toByte()
            output[offset + 1] = ((value shr 8) and 0xff).toByte()
            output[offset + 2] = ((value shr 16) and 0xff).toByte()
            output[offset + 3] = ((value shr 24) and 0xff).toByte()
        }
        fun writeShortLe(offset: Int, value: Int) {
            output[offset] = (value and 0xff).toByte()
            output[offset + 1] = ((value shr 8) and 0xff).toByte()
        }
        writeAscii(0, "RIFF")
        writeIntLe(4, 36 + pcm.size)
        writeAscii(8, "WAVE")
        writeAscii(12, "fmt ")
        writeIntLe(16, 16)
        writeShortLe(20, 1)
        writeShortLe(22, channels)
        writeIntLe(24, sampleRate)
        writeIntLe(28, byteRate)
        writeShortLe(32, blockAlign)
        writeShortLe(34, bitsPerSample)
        writeAscii(36, "data")
        writeIntLe(40, pcm.size)
        pcm.copyInto(output, destinationOffset = WAV_HEADER_SIZE)
        return output
    }
}

private const val AUDIO_LOG_TAG = "RhodesAudio"
private const val AUDIO_LOG_KEYWORD = "RHODES_AUDIO_PLAYBACK"
private const val RECORD_SAMPLE_RATE = 16_000
private const val WAV_HEADER_SIZE = 44
private const val RECORD_SPEECH_THRESHOLD = 550

data class RecordedAudio(
    val path: String,
    val durationMs: Long,
)
