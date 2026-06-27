package com.yourname.aichatmvptest.shared.modelgateway

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface VisionGateway {
    suspend fun analyzeImage(request: VisionAnalyzeRequest): VisionAnalyzeResponse
}

data class VisionAnalyzeRequest(
    val imageUrlOrBase64: String,
    val prompt: String,
)

data class VisionAnalyzeResponse(
    val text: String,
    val reasoning: String = "",
)

class AliyunQwenVlGateway(
    private val endpoint: String,
    private val apiKey: String,
    private val modelName: String = "qwen3-vl-plus",
    private val client: HttpClient = dashScopeHttpClient(),
) : VisionGateway {
    override suspend fun analyzeImage(request: VisionAnalyzeRequest): VisionAnalyzeResponse {
        val responseText = client.post(endpoint) {
            bearerAuth(apiKey)
            header("X-DashScope-SSE", "enable")
            contentType(ContentType.Application.Json)
            setBody(
                NativeVisionRequest(
                    model = modelName,
                    input = NativeVisionInput(
                        messages = listOf(
                            NativeVisionMessage(
                                role = "user",
                                content = listOf(
                                    NativeVisionContent(image = request.imageUrlOrBase64),
                                    NativeVisionContent(text = request.prompt),
                                ),
                            )
                        )
                    ),
                    parameters = NativeVisionParameters(
                        enableThinking = true,
                        incrementalOutput = true,
                        thinkingBudget = 50,
                    ),
                )
            )
        }.body<String>()

        return parseVisionResponse(responseText)
    }

    private fun parseVisionResponse(raw: String): VisionAnalyzeResponse {
        runCatching { json.decodeFromString(NativeVisionResponse.serializer(), raw) }
            .getOrNull()
            ?.let { response ->
                val answer = response.output
                    ?.choices
                    ?.firstOrNull()
                    ?.message
                    ?.content
                    .orEmpty()
                    .mapNotNull { it.text }
                    .joinToString("")
                if (answer.isNotBlank()) return VisionAnalyzeResponse(text = answer)
            }

        val lines = raw.lineSequence()
            .map { it.removePrefix("data:").trim() }
            .filter { it.isNotBlank() && it != "[DONE]" }

        val answer = StringBuilder()
        val reasoning = StringBuilder()
        lines.forEach { line ->
            runCatching { json.decodeFromString(NativeVisionResponse.serializer(), line) }
                .getOrNull()
                ?.output
                ?.choices
                ?.firstOrNull()
                ?.message
                ?.let { message ->
                    message.reasoningContent?.let(reasoning::append)
                    message.content.orEmpty().forEach { content -> content.text?.let(answer::append) }
                }
        }
        if (answer.isEmpty()) answer.append(raw)
        return VisionAnalyzeResponse(text = answer.toString(), reasoning = reasoning.toString())
    }
}

class AliyunTextEmbeddingGateway(
    private val endpoint: String,
    private val apiKey: String,
    private val modelName: String = "text-embedding-v4",
    private val client: HttpClient = dashScopeHttpClient(),
) : EmbeddingGateway {
    override suspend fun embed(text: String): List<Double> {
        val response = client.post(endpoint) {
            bearerAuth(apiKey)
            contentType(ContentType.Application.Json)
            setBody(EmbeddingRequestBody(model = modelName, input = EmbeddingInput(texts = listOf(text))))
        }.body<EmbeddingResponseBody>()
        return response.output?.embeddings?.firstOrNull()?.embedding.orEmpty()
    }
}

@Serializable
private data class NativeVisionRequest(
    val model: String,
    val input: NativeVisionInput,
    val parameters: NativeVisionParameters,
)

@Serializable
private data class NativeVisionInput(val messages: List<NativeVisionMessage>)

@Serializable
private data class NativeVisionMessage(val role: String, val content: List<NativeVisionContent>)

@Serializable
private data class NativeVisionContent(val image: String? = null, val text: String? = null)

@Serializable
private data class NativeVisionParameters(
    @SerialName("enable_thinking") val enableThinking: Boolean,
    @SerialName("incremental_output") val incrementalOutput: Boolean,
    @SerialName("thinking_budget") val thinkingBudget: Int,
)

@Serializable
private data class NativeVisionResponse(val output: NativeVisionOutput? = null)

@Serializable
private data class NativeVisionOutput(val choices: List<NativeVisionChoice> = emptyList())

@Serializable
private data class NativeVisionChoice(val message: NativeVisionMessageOut)

@Serializable
private data class NativeVisionMessageOut(
    val content: List<NativeVisionContentOut> = emptyList(),
    @SerialName("reasoning_content") val reasoningContent: String? = null,
)

@Serializable
private data class NativeVisionContentOut(val text: String? = null)

@Serializable
private data class EmbeddingRequestBody(val model: String, val input: EmbeddingInput)

@Serializable
private data class EmbeddingInput(val texts: List<String>)

@Serializable
private data class EmbeddingResponseBody(val output: EmbeddingOutput? = null)

@Serializable
private data class EmbeddingOutput(val embeddings: List<EmbeddingData> = emptyList())

@Serializable
private data class EmbeddingData(val embedding: List<Double> = emptyList())

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

private fun dashScopeHttpClient(): HttpClient = HttpClient {
    install(ContentNegotiation) { json(json) }
}
