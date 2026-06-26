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
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class AliyunQwenAsrGateway(
    private val endpoint: String,
    private val apiKey: String,
    private val modelName: String = "qwen3-asr-flash-realtime",
    private val client: HttpClient = asrWsClient(),
) : AsrGateway {
    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun transcribe(request: AsrRequest): AsrResult {
        var finalText = ""
        var emotion: String? = null

        client.webSocket(
            urlString = endpoint,
            request = { header("Authorization", "Bearer $apiKey") },
        ) {
            send(Frame.Text(json.encodeToString(SessionUpdateEvent.manual(request, modelName))))

            request.pcm16kMonoAudio.asList().chunked(1024).forEach { chunk ->
                val bytes = chunk.toByteArray()
                send(Frame.Text(json.encodeToString(AppendAudioEvent(audio = Base64.encode(bytes)))))
            }
            send(Frame.Text(json.encodeToString(CommitAudioEvent())))
            send(Frame.Text(json.encodeToString(FinishSessionEvent())))

            while (true) {
                val frame = incoming.receiveCatching().getOrNull() ?: break
                val text = (frame as? Frame.Text)?.readText() ?: continue
                val event = runCatching { json.decodeFromString(AsrEvent.serializer(), text) }.getOrNull() ?: continue
                when (event.type) {
                    "conversation.item.input_audio_transcription.text" -> {
                        finalText = event.text.orEmpty() + event.stash.orEmpty()
                        emotion = event.emotion ?: emotion
                    }
                    "conversation.item.input_audio_transcription.completed" -> {
                        finalText = event.transcript ?: finalText
                        emotion = event.emotion ?: emotion
                        break
                    }
                }
            }
        }

        return AsrResult(text = finalText, emotion = emotion)
    }
}

@Serializable
private data class SessionUpdateEvent(
    val type: String = "session.update",
    val session: SessionConfig,
) {
    companion object {
        fun manual(request: AsrRequest, model: String): SessionUpdateEvent = SessionUpdateEvent(
            session = SessionConfig(
                model = model,
                turnDetection = null,
                inputAudioTranscription = TranscriptionConfig(
                    language = request.language,
                    inputAudioFormat = request.inputAudioFormat,
                    inputSampleRate = request.sampleRate,
                ),
            )
        )
    }
}

@Serializable
private data class SessionConfig(
    val model: String,
    @SerialName("turn_detection") val turnDetection: String? = null,
    @SerialName("input_audio_transcription") val inputAudioTranscription: TranscriptionConfig,
)

@Serializable
private data class TranscriptionConfig(
    val language: String,
    @SerialName("input_audio_format") val inputAudioFormat: String,
    @SerialName("input_sample_rate") val inputSampleRate: Int,
)

@Serializable
private data class AppendAudioEvent(
    val type: String = "input_audio_buffer.append",
    val audio: String,
)

@Serializable
private data class CommitAudioEvent(val type: String = "input_audio_buffer.commit")

@Serializable
private data class FinishSessionEvent(val type: String = "session.finish")

@Serializable
private data class AsrEvent(
    val type: String,
    val text: String? = null,
    val stash: String? = null,
    val transcript: String? = null,
    val emotion: String? = null,
)

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

private fun asrWsClient(): HttpClient = HttpClient {
    install(WebSockets)
    install(ContentNegotiation) { json(json) }
}
