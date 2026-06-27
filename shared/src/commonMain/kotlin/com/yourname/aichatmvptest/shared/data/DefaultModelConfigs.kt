package com.yourname.aichatmvptest.shared.data

import com.yourname.aichatmvptest.shared.model.ModelConfig
import com.yourname.aichatmvptest.shared.model.ModelType
import com.yourname.aichatmvptest.shared.model.VectorStoreConfig

object DefaultModelConfigs {
    val llm = ModelConfig(
        id = "llm_openai_compatible",
        provider = "DeepSeek / OpenAI兼容",
        modelType = ModelType.Llm,
        baseUrl = "https://api.deepseek.com/v1",
        apiKeyMasked = "",
        modelName = "deepseek-v4-flash",
        enabled = false,
    )

    val vision = ModelConfig(
        id = "vision_aliyun_qwen3_vl_plus",
        provider = "阿里百炼",
        modelType = ModelType.Vision,
        baseUrl = "https://llm-imxtee9l3et45y6z.cn-beijing.maas.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation",
        apiKeyMasked = "",
        modelName = "qwen3-vl-plus",
        enabled = false,
    )

    val embedding = ModelConfig(
        id = "embedding_aliyun_text_embedding_v4",
        provider = "阿里百炼",
        modelType = ModelType.Embedding,
        baseUrl = "https://llm-imxtee9l3et45y6z.cn-beijing.maas.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding",
        apiKeyMasked = "",
        modelName = "text-embedding-v4",
        enabled = false,
    )

    val tts = ModelConfig(
        id = "tts_minimax",
        provider = "Minimax",
        modelType = ModelType.Tts,
        baseUrl = "wss://api.minimaxi.com/ws/v1/t2a_v2",
        apiKeyMasked = "",
        modelName = "speech-2.8-hd|male-qn-qingse",
        enabled = false,
    )

    val asr = ModelConfig(
        id = "asr_aliyun_dashscope",
        provider = "阿里百炼",
        modelType = ModelType.Asr,
        baseUrl = "",
        apiKeyMasked = "",
        modelName = "qwen3.5-omni-flash-realtime|qwen3-asr-flash-realtime",
        enabled = false,
    )

    val vectorStore = VectorStoreConfig(
        provider = "自定义向量库",
        baseUrl = "",
        apiKeyMasked = "",
        collectionName = "rhodes_memories",
        namespace = "default",
    )

    val allModelConfigs = listOf(llm, vision, embedding, tts, asr)
}
