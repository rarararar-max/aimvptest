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
    val stickerId: String? = null,
    val alt: String? = null,
    val prompt: String? = null,
    val caption: String? = null,
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
    val subject: String = "unknown",
    val confidence: Double = 0.8,
    val target: String? = null,
    val change: String? = null,
)

@Serializable
data class ProactiveSuggestion(
    val enabled: Boolean,
    val afterMinutes: Int? = null,
    val reason: String? = null,
)

@Serializable
data class MemoryExtractionResult(
    val rollingSummary: String = "",
    val memories: List<MemoryHint> = emptyList(),
)

@Serializable
data class VideoCallReply(
    val text: String,
    val action: VideoCallAction = VideoCallAction(),
    val mood: String = "neutral",
)

@Serializable
data class VideoCallAction(
    val name: String = "idle",
    val intensity: Double = 0.5,
    val durationMs: Long = 1000L,
    val loop: Boolean = false,
)
