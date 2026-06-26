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

class OpenAiCompatibleGateway(
    private val baseUrl: String,
    private val apiKey: String,
    private val modelName: String,
    private val client: HttpClient = defaultHttpClient(),
) : ModelGateway {
    override suspend fun chat(input: ChatModelRequest): AiReplyEnvelope {
        val response = client.post("${baseUrl.trimEnd('/')}/chat/completions") {
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

    private fun buildMessages(input: ChatModelRequest): List<OpenAiMessage> {
        val context = input.recentMessages.joinToString(separator = "\n")
        return listOf(
            OpenAiMessage(
                role = "system",
                content = """
                    你是一个微信式 AI 聊天对象。必须只输出 JSON，不要输出 Markdown。
                    JSON 格式：{"messages":[{"type":"text","text":"内容","delayMs":800}],"mood":"neutral"}
                    一轮回复 1 到 4 个气泡，每个气泡自然、短，不要像长文章。
                """.trimIndent(),
            ),
            OpenAiMessage(role = "user", content = "最近上下文：\n$context\n\n用户新消息：${input.userText}"),
        )
    }

    private fun parseAiReply(raw: String): AiReplyEnvelope {
        val jsonText = raw.substringAfter("```json", raw)
            .substringAfter("```", raw)
            .substringBeforeLast("```", raw)
            .let { candidate ->
                val start = candidate.indexOf('{')
                val end = candidate.lastIndexOf('}')
                if (start >= 0 && end > start) candidate.substring(start, end + 1) else candidate
            }

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
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
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
