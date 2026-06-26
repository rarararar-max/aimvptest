package com.yourname.aichatmvptest.shared.voice

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MinimaxTtsGateway(
    private val endpoint: String,
    private val apiKey: String,
    private val modelName: String = "speech-2.8-hd",
    private val client: HttpClient = minimaxWsClient(),
) : TtsGateway {
    override suspend fun synthesize(request: TtsRequest): TtsResult {
        val chunks = mutableListOf<ByteArray>()

        client.webSocket(
            urlString = endpoint,
            request = { header("Authorization", "Bearer $apiKey") },
        ) {
            val connected = incoming.receiveCatching().getOrNull() as? Frame.Text
            val connectedEvent = connected?.readText().orEmpty()
            if (!connectedEvent.contains("connected_success")) return@webSocket

            send(
                Frame.Text(
                    json.encodeToString(
                        MinimaxTaskStart(
                            model = modelName,
                            voiceSetting = MinimaxVoiceSetting(
                                voiceId = request.voiceId,
                                speed = request.speed,
                            ),
                            audioSetting = MinimaxAudioSetting(format = request.format),
                        )
                    )
                )
            )
            val started = incoming.receiveCatching().getOrNull() as? Frame.Text
            val startedEvent = started?.readText().orEmpty()
            if (!startedEvent.contains("task_started")) return@webSocket

            send(Frame.Text(json.encodeToString(MinimaxTaskContinue(text = request.text))))

            while (true) {
                val frame = incoming.receiveCatching().getOrNull() ?: break
                val text = (frame as? Frame.Text)?.readText() ?: continue
                val event = runCatching { json.decodeFromString(MinimaxStreamEvent.serializer(), text) }.getOrNull()
                event?.data?.audio?.takeIf { it.isNotBlank() }?.let { hex ->
                    chunks += hexToBytes(hex)
                }
                if (event?.isFinal == true) break
            }

            send(Frame.Text(json.encodeToString(MinimaxTaskFinish())))
        }

        val audioBytes = chunks.fold(ByteArray(0)) { acc, bytes -> acc + bytes }
        return TtsResult(
            audioBytes = audioBytes,
            audioUrl = null,
            durationMs = estimateDuration(request.text),
        )
    }

    override suspend fun createAsyncTask(request: TtsRequest): TtsTask {
        // The provided Minimax reference is WebSocket streaming synthesis, not a background job API.
        return TtsTask(taskId = "minimax_streaming_only", status = "unsupported")
    }

    override suspend fun queryTask(taskId: String): TtsTask {
        return TtsTask(taskId = taskId, status = "unsupported")
    }

    private fun estimateDuration(text: String): Long = (text.length.coerceAtLeast(1) * 180L).coerceAtMost(60_000L)
}

@Serializable
private data class MinimaxTaskStart(
    val event: String = "task_start",
    val model: String,
    @SerialName("voice_setting") val voiceSetting: MinimaxVoiceSetting = MinimaxVoiceSetting(),
    @SerialName("audio_setting") val audioSetting: MinimaxAudioSetting = MinimaxAudioSetting(),
)

@Serializable
private data class MinimaxVoiceSetting(
    @SerialName("voice_id") val voiceId: String = "male-qn-qingse",
    val speed: Double = 1.0,
    val vol: Double = 1.0,
    val pitch: Int = 0,
    @SerialName("english_normalization") val englishNormalization: Boolean = false,
)

@Serializable
private data class MinimaxAudioSetting(
    @SerialName("sample_rate") val sampleRate: Int = 32000,
    val bitrate: Int = 128000,
    val format: String = "mp3",
    val channel: Int = 1,
)

@Serializable
private data class MinimaxTaskContinue(
    val event: String = "task_continue",
    val text: String,
)

@Serializable
private data class MinimaxTaskFinish(val event: String = "task_finish")

@Serializable
private data class MinimaxStreamEvent(
    val data: MinimaxStreamData? = null,
    @SerialName("is_final") val isFinal: Boolean = false,
)

@Serializable
private data class MinimaxStreamData(val audio: String? = null)

private fun hexToBytes(hex: String): ByteArray {
    val clean = hex.trim()
    return ByteArray(clean.length / 2) { index ->
        clean.substring(index * 2, index * 2 + 2).toInt(16).toByte()
    }
}

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

private fun minimaxWsClient(): HttpClient = HttpClient {
    install(WebSockets)
    install(ContentNegotiation) { json(json) }
}
