package com.yourname.aichatmvptest.shared.vector

data class VectorMemory(
    val id: String,
    val characterId: String,
    val content: String,
    val importance: Double,
    val embedding: List<Double> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
)

data class VectorSearchRequest(
    val characterId: String,
    val query: String,
    val queryEmbedding: List<Double> = emptyList(),
    val limit: Int = 6,
)

interface VectorStoreGateway {
    suspend fun upsert(memory: VectorMemory)
    suspend fun search(request: VectorSearchRequest): List<VectorMemory>
    suspend fun delete(memoryId: String)
    suspend fun clearCharacterMemory(characterId: String)
}

class DisabledVectorStoreGateway : VectorStoreGateway {
    override suspend fun upsert(memory: VectorMemory) = Unit
    override suspend fun search(request: VectorSearchRequest): List<VectorMemory> = emptyList()
    override suspend fun delete(memoryId: String) = Unit
    override suspend fun clearCharacterMemory(characterId: String) = Unit
}
