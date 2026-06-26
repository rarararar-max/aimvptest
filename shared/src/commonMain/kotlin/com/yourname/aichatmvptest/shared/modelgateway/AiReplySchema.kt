package com.yourname.aichatmvptest.shared.modelgateway

import kotlinx.serialization.Serializable

@Serializable
data class AiReplyEnvelope(
    val messages: List<AiReplySegment>,
    val mood: String = "neutral",
    val animation: AnimationCommand? = null,
    val memoryHints: List<MemoryHint> = emptyList(),
    val proactiveSuggestion: ProactiveSuggestion? = null,
)

@Serializable
data class AiReplySegment(
    val type: String,
    val text: String? = null,
    val delayMs: Long = 800L,
    val voiceStyle: String? = null,
)

@Serializable
data class AnimationCommand(
    val state: String,
    val durationMs: Long = 2000L,
)

@Serializable
data class MemoryHint(
    val type: String,
    val content: String,
    val importance: Double,
)

@Serializable
data class ProactiveSuggestion(
    val enabled: Boolean,
    val afterMinutes: Int? = null,
    val reason: String? = null,
)
