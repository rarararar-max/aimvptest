package com.yourname.aichatmvptest.shared.voice

interface TtsGateway {
    suspend fun synthesize(request: TtsRequest): TtsResult
    suspend fun createAsyncTask(request: TtsRequest): TtsTask
    suspend fun queryTask(taskId: String): TtsTask
}

data class TtsRequest(
    val text: String,
    val voiceId: String,
    val format: String = "mp3",
    val speed: Double = 1.0,
)

data class TtsResult(
    val audioBytes: ByteArray? = null,
    val audioUrl: String? = null,
    val durationMs: Long = 0L,
)

data class TtsTask(
    val taskId: String,
    val status: String,
    val audioUrl: String? = null,
)

enum class TtsMode {
    SyncShortMessage,
    AsyncLongMessage,
    StreamingCall,
}
