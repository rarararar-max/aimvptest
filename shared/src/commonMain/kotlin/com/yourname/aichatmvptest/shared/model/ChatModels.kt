package com.yourname.aichatmvptest.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class AiCharacter(
    val id: String,
    val name: String,
    val title: String,
    val description: String,
    val personaPrompt: String,
    val speakingStyle: String,
    val voiceStyle: String,
    val animationPack: String,
    val proactiveLevel: Int,
)

@Serializable
data class Conversation(
    val id: String,
    val characterId: String,
    val title: String,
    val lastMessage: String,
    val unreadCount: Int,
    val updatedAtText: String,
)

@Serializable
data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderType: SenderType,
    val senderId: String,
    val messageType: MessageType,
    val content: MessageContent,
    val status: MessageStatus,
    val createdAtMillis: Long,
)

@Serializable
enum class SenderType {
    User,
    Ai,
    System,
}

@Serializable
enum class MessageType {
    Text,
    Image,
    Voice,
    Sticker,
    Gift,
    Call,
    System,
}

@Serializable
enum class MessageStatus {
    Sending,
    Sent,
    Failed,
}

@Serializable
sealed interface MessageContent {
    @Serializable
    data class Text(val text: String) : MessageContent

    @Serializable
    data class Image(
        val uri: String,
        val width: Int = 0,
        val height: Int = 0,
        val prompt: String? = null,
        val caption: String? = null,
    ) : MessageContent

    @Serializable
    data class Voice(val localPath: String, val durationMs: Long, val text: String? = null) : MessageContent

    @Serializable
    data class Sticker(val stickerId: String, val alt: String? = null) : MessageContent

    @Serializable
    data class Gift(val giftType: String, val name: String) : MessageContent

    @Serializable
    data class Call(val callType: String, val status: String, val durationSeconds: Int) : MessageContent
}
