package com.yourname.aichatmvptest.shared.memory

import com.yourname.aichatmvptest.shared.model.ChatMessage
import com.yourname.aichatmvptest.shared.modelgateway.EmbeddingGateway
import com.yourname.aichatmvptest.shared.vector.VectorMemory
import com.yourname.aichatmvptest.shared.vector.VectorSearchRequest
import com.yourname.aichatmvptest.shared.vector.VectorStoreGateway

class MemoryService(
    private val embeddingGateway: EmbeddingGateway,
    private val vectorStoreGateway: VectorStoreGateway,
) {
    suspend fun recall(characterId: String, query: String): List<VectorMemory> {
        val queryEmbedding = embeddingGateway.embed(query)
        return vectorStoreGateway.search(
            VectorSearchRequest(
                characterId = characterId,
                query = query,
                queryEmbedding = queryEmbedding,
            )
        )
    }

    suspend fun saveHint(characterId: String, content: String, importance: Double) {
        val embedding = embeddingGateway.embed(content)
        vectorStoreGateway.upsert(
            VectorMemory(
                id = "memory_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}",
                characterId = characterId,
                content = content,
                importance = importance,
                embedding = embedding,
            )
        )
    }

    suspend fun extractFromMessage(message: ChatMessage): List<String> {
        // Later this will use LLM memoryHints or a dedicated extraction prompt.
        return emptyList()
    }
}
