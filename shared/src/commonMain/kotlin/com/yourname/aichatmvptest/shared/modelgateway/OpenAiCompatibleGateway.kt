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
import com.yourname.aichatmvptest.shared.prompt.PromptTemplates
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class OpenAiCompatibleGateway(
    private val baseUrl: String,
    private val apiKey: String,
    private val modelName: String,
    private val client: HttpClient = defaultHttpClient(),
) : ModelGateway {
    override suspend fun chat(input: ChatModelRequest): AiReplyEnvelope {
        val response = client.post("${normalizedBaseUrl()}/chat/completions") {
            bearerAuth(apiKey)
            contentType(ContentType.Application.Json)
            header("Accept", "application/json")
            setBody(
                OpenAiChatRequest(
                    model = modelName,
                    messages = buildMessages(input),
                    temperature = 0.85,
                    responseFormat = OpenAiResponseFormat(type = "json_object"),
                )
            )
        }.body<OpenAiChatResponse>()

        val content = response.choices.firstOrNull()?.message?.content.orEmpty()
        return parseAiReply(content)
    }

    override suspend fun voiceCall(input: VoiceCallRequest): String {
        val response = client.post("${normalizedBaseUrl()}/chat/completions") {
            bearerAuth(apiKey)
            contentType(ContentType.Application.Json)
            header("Accept", "application/json")
            setBody(
                OpenAiChatRequest(
                    model = modelName,
                    messages = listOf(
                        OpenAiMessage(role = "system", content = PromptTemplates.VOICE_CALL_SYSTEM_PROMPT),
                        OpenAiMessage(
                            role = "user",
                            content = "最近上下文：\n${input.recentMessages.joinToString("\n")}\n\n用户说：${input.userText}",
                        ),
                    ),
                    temperature = 0.75,
                    responseFormat = null,
                )
            )
        }.body<OpenAiChatResponse>()
        return response.choices.firstOrNull()?.message?.content.orEmpty().trim().ifBlank { "我在。" }
    }

    override suspend fun extractMemory(input: MemoryExtractionRequest): MemoryExtractionResult {
        val response = client.post("${normalizedBaseUrl()}/chat/completions") {
            bearerAuth(apiKey)
            contentType(ContentType.Application.Json)
            header("Accept", "application/json")
            setBody(
                OpenAiChatRequest(
                    model = modelName,
                    messages = listOf(
                        OpenAiMessage(role = "system", content = PromptTemplates.MEMORY_EXTRACTION_SYSTEM_PROMPT),
                        OpenAiMessage(
                            role = "user",
                            content = "当前摘要：${input.currentSummary.orEmpty()}\n交互模式：${input.mode}\n对话：\n${input.dialogue.joinToString("\n")}",
                        ),
                    ),
                    temperature = 0.2,
                    responseFormat = OpenAiResponseFormat(type = "json_object"),
                )
            )
        }.body<OpenAiChatResponse>()
        val content = response.choices.firstOrNull()?.message?.content.orEmpty()
        return parseMemoryExtraction(content)
    }

    private fun buildMessages(input: ChatModelRequest): List<OpenAiMessage> {
        val context = input.recentMessages.joinToString(separator = "\n")
        return listOf(
            OpenAiMessage(
                role = "system",
                content = """
                    ${PromptTemplates.CHAT_SYSTEM_PROMPT}
                """.trimIndent(),
            ),
            OpenAiMessage(role = "user", content = "最近上下文、滚动摘要与相关记忆：\n$context\n\n用户新消息：${input.userText}"),
        )
    }

    private fun normalizedBaseUrl(): String {
        val trimmed = baseUrl.trim().trimEnd('/')
        return if (trimmed == "https://api.deepseek.com" || trimmed == "http://api.deepseek.com") {
            "$trimmed/v1"
        } else {
            trimmed
        }
    }

    private fun parseAiReply(raw: String): AiReplyEnvelope {
        val jsonText = extractJsonObject(raw)

        return runCatching {
            json.decodeFromString(AiReplyEnvelope.serializer(), jsonText)
        }.getOrElse {
            AiReplyEnvelope(
                messages = splitFallback(raw).mapIndexed { index, text ->
                    AiReplySegment(type = "text", text = text, delayMs = 700L + index * 450L)
                },
                mood = "neutral",
            )
        }
    }

    private fun parseMemoryExtraction(raw: String): MemoryExtractionResult {
        return runCatching {
            json.decodeFromString(MemoryExtractionResult.serializer(), extractJsonObject(raw))
        }.getOrDefault(MemoryExtractionResult())
    }

    private fun extractJsonObject(raw: String): String {
        return raw.substringAfter("```json", raw)
            .substringAfter("```", raw)
            .substringBeforeLast("```", raw)
            .let { candidate ->
                val start = candidate.indexOf('{')
                val end = candidate.lastIndexOf('}')
                if (start >= 0 && end > start) candidate.substring(start, end + 1) else candidate
            }
    }

    private fun splitFallback(text: String): List<String> {
        return text
            .replace("\n", " ")
            .split("。", "！", "？", ".", "!", "?")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(3)
            .ifEmpty { listOf("我收到了。") }
    }

    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            explicitNulls = false
        }

        fun defaultHttpClient(): HttpClient = HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
        }
    }
}

fun createOpenAiCompatibleGateway(
    baseUrl: String,
    apiKey: String,
    modelName: String,
): ModelGateway = OpenAiCompatibleGateway(
    baseUrl = baseUrl,
    apiKey = apiKey,
    modelName = modelName,
)

@Serializable
private data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double,
    @SerialName("response_format") val responseFormat: OpenAiResponseFormat? = null,
)

@Serializable
private data class OpenAiMessage(
    val role: String,
    val content: String,
)

@Serializable
private data class OpenAiResponseFormat(
    val type: String,
)

@Serializable
private data class OpenAiChatResponse(
    val choices: List<OpenAiChoice> = emptyList(),
)

@Serializable
private data class OpenAiChoice(
    val message: OpenAiMessage,
)
