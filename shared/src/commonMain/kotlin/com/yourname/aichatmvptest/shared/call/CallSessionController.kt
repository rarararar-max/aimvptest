package com.yourname.aichatmvptest.shared.call

import com.yourname.aichatmvptest.shared.modelgateway.ChatModelRequest
import com.yourname.aichatmvptest.shared.modelgateway.ModelGateway
import com.yourname.aichatmvptest.shared.voice.TtsGateway
import com.yourname.aichatmvptest.shared.voice.TtsRequest

class CallSessionController(
    private val modelGateway: ModelGateway,
    private val ttsGateway: TtsGateway,
) {
    var state: CallState = CallState.Calling
        private set

    suspend fun connect() {
        state = CallState.Connected
    }

    suspend fun handleUserUtterance(characterId: String, text: String, voiceId: String): CallTurnResult {
        state = CallState.Thinking
        val reply = modelGateway.chat(
            ChatModelRequest(
                characterId = characterId,
                userText = text,
                recentMessages = emptyList(),
            )
        )
        val answer = reply.messages.mapNotNull { it.text }.joinToString(" ")
        state = CallState.AiSpeaking
        val tts = ttsGateway.synthesize(TtsRequest(text = answer, voiceId = voiceId))
        state = CallState.Listening
        return CallTurnResult(text = answer, audioUrl = tts.audioUrl, durationMs = tts.durationMs)
    }

    fun end() {
        state = CallState.Ended
    }
}

data class CallTurnResult(
    val text: String,
    val audioUrl: String?,
    val durationMs: Long,
)
