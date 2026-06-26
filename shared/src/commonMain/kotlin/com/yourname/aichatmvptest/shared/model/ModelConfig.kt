package com.yourname.aichatmvptest.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class ModelConfig(
    val id: String,
    val provider: String,
    val modelType: ModelType,
    val baseUrl: String,
    val apiKeyMasked: String,
    val modelName: String,
    val enabled: Boolean,
)

@Serializable
enum class ModelType {
    Llm,
    Vision,
    Embedding,
    VectorStore,
    Asr,
    Tts,
}

@Serializable
data class VectorStoreConfig(
    val provider: String,
    val baseUrl: String,
    val apiKeyMasked: String,
    val collectionName: String,
    val namespace: String? = null,
)
