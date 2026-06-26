package com.yourname.aichatmvptest.shared.modelgateway

import com.yourname.aichatmvptest.shared.voice.AliyunQwenAsrGateway
import com.yourname.aichatmvptest.shared.voice.AsrGateway
import com.yourname.aichatmvptest.shared.voice.MinimaxTtsGateway
import com.yourname.aichatmvptest.shared.voice.TtsGateway

fun createAliyunQwenVlGateway(
    endpoint: String,
    apiKey: String,
    modelName: String = "qwen3-vl-plus",
): VisionGateway = AliyunQwenVlGateway(
    endpoint = endpoint,
    apiKey = apiKey,
    modelName = modelName,
)

fun createAliyunTextEmbeddingGateway(
    endpoint: String,
    apiKey: String,
    modelName: String = "text-embedding-v4",
): EmbeddingGateway = AliyunTextEmbeddingGateway(
    endpoint = endpoint,
    apiKey = apiKey,
    modelName = modelName,
)

fun createMinimaxTtsGateway(
    endpoint: String,
    apiKey: String,
    modelName: String = "speech-2.8-hd",
): TtsGateway = MinimaxTtsGateway(
    endpoint = endpoint,
    apiKey = apiKey,
    modelName = modelName,
)

fun createAliyunQwenAsrGateway(
    endpoint: String,
    apiKey: String,
    modelName: String = "qwen3-asr-flash-realtime",
): AsrGateway = AliyunQwenAsrGateway(
    endpoint = endpoint,
    apiKey = apiKey,
    modelName = modelName,
)
