package com.yourname.aichatmvptest.shared.database

import com.yourname.aichatmvptest.shared.vector.VectorMemory
import com.yourname.aichatmvptest.shared.vector.VectorSearchRequest
import com.yourname.aichatmvptest.shared.vector.VectorStoreGateway
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.sqrt

class LocalVectorStoreGateway(
    private val database: RhodesDatabase,
) : VectorStoreGateway {
    private val queries = database.rhodesDatabaseQueries

    override suspend fun upsert(memory: VectorMemory) {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        queries.upsertMemoryItem(
            id = memory.id,
            character_id = memory.characterId,
            type = memory.metadata["type"] ?: "general",
            content = memory.content,
            importance = memory.importance,
            keywords_json = json.encodeToString(memory.metadata["keywords"]?.split(',') ?: emptyList<String>()),
            embedding_json = json.encodeToString(memory.embedding),
            remote_vector_id = null,
            source_message_id = memory.metadata["source_message_id"],
            created_at = now,
            updated_at = now,
        )
    }

    override suspend fun search(request: VectorSearchRequest): List<VectorMemory> {
        val queryEmbedding = request.queryEmbedding
        val rows = queries.selectMemoriesByCharacter(request.characterId).executeAsList()
        return rows
            .mapNotNull { row ->
                val embedding = runCatching { json.decodeFromString<List<Double>>(row.embedding_json) }.getOrDefault(emptyList())
                val score = if (queryEmbedding.isNotEmpty() && embedding.isNotEmpty()) {
                    cosineSimilarity(queryEmbedding, embedding)
                } else {
                    keywordScore(request.query, row.content) + row.importance * 0.05
                }
                VectorMemory(
                    id = row.id,
                    characterId = row.character_id,
                    content = row.content,
                    importance = row.importance,
                    embedding = embedding,
                    metadata = mapOf("score" to score.toString(), "type" to row.type),
                ) to score
            }
            .sortedByDescending { it.second }
            .take(request.limit)
            .map { it.first }
    }

    override suspend fun delete(memoryId: String) {
        queries.deleteMemoryItem(memoryId)
    }

    override suspend fun clearCharacterMemory(characterId: String) {
        queries.clearMemoriesByCharacter(characterId)
    }

    private fun keywordScore(query: String, content: String): Double {
        val queryChars = query.toSet()
        if (queryChars.isEmpty()) return 0.0
        return content.count { it in queryChars }.toDouble() / queryChars.size
    }

    private fun cosineSimilarity(a: List<Double>, b: List<Double>): Double {
        val size = minOf(a.size, b.size)
        if (size == 0) return 0.0
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        repeat(size) { index ->
            dot += a[index] * b[index]
            normA += a[index] * a[index]
            normB += b[index] * b[index]
        }
        if (normA == 0.0 || normB == 0.0) return 0.0
        return dot / (sqrt(normA) * sqrt(normB))
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}
