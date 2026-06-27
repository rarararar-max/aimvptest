package com.yourname.aichatmvptest.shared.modelgateway

interface ModelGateway {
    suspend fun chat(input: ChatModelRequest): AiReplyEnvelope
    suspend fun voiceCall(input: VoiceCallRequest): String = input.userText
    suspend fun extractMemory(input: MemoryExtractionRequest): MemoryExtractionResult = MemoryExtractionResult()
}

data class ChatModelRequest(
    val characterId: String,
    val userText: String,
    val recentMessages: List<String>,
)

data class VoiceCallRequest(
    val characterId: String,
    val userText: String,
    val recentMessages: List<String> = emptyList(),
)

data class MemoryExtractionRequest(
    val characterId: String,
    val currentSummary: String?,
    val dialogue: List<String>,
    val mode: String = "chat",
)

class FakeModelGateway : ModelGateway {
    override suspend fun chat(input: ChatModelRequest): AiReplyEnvelope {
        val text = input.userText.trim().ifEmpty { "收到" }
        return AiReplyEnvelope(
            messages = listOf(
                AiReplySegment(type = "text", text = "通讯已接入。", delayMs = 450),
                AiReplySegment(type = "sticker", stickerId = "nod_01", alt = "点了点头", delayMs = 300),
                AiReplySegment(type = "text", text = "我收到的是：$text", delayMs = 900),
                AiReplySegment(type = "voice", text = "配置模型服务后，这里可以变成真实语音条。", voiceStyle = "soft", delayMs = 1200),
            ),
            mood = "listening",
            animation = AnimationCommand(state = "listening", durationMs = 1800),
        )
    }

    override suspend fun voiceCall(input: VoiceCallRequest): String {
        return input.userText.trim().ifBlank { "我在，慢慢说。" }
    }

    override suspend fun extractMemory(input: MemoryExtractionRequest): MemoryExtractionResult {
        return MemoryExtractionResult(rollingSummary = input.dialogue.takeLast(8).joinToString("\n").take(1000))
    }
}
