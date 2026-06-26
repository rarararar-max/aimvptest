package com.yourname.aichatmvptest.shared.modelgateway

interface ModelGateway {
    suspend fun chat(input: ChatModelRequest): AiReplyEnvelope
}

data class ChatModelRequest(
    val characterId: String,
    val userText: String,
    val recentMessages: List<String>,
)

class FakeModelGateway : ModelGateway {
    override suspend fun chat(input: ChatModelRequest): AiReplyEnvelope {
        val text = input.userText.trim().ifEmpty { "收到" }
        return AiReplyEnvelope(
            messages = listOf(
                AiReplySegment(type = "text", text = "通讯已接入。", delayMs = 450),
                AiReplySegment(type = "text", text = "我收到的是：$text", delayMs = 900),
                AiReplySegment(type = "text", text = "后面这里会替换成你配置的 OpenAI 兼容模型回复。", delayMs = 1400),
            ),
            mood = "listening",
            animation = AnimationCommand(state = "listening", durationMs = 1800),
        )
    }
}
