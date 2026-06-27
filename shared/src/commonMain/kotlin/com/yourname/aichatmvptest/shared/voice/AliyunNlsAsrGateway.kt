package com.yourname.aichatmvptest.shared.voice

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class AliyunNlsAsrGateway private constructor(
    private val endpoint: String,
    private val token: String,
    private val appKey: String,
    private val client: HttpClient = nlsHttpClient(),
) {
    constructor(endpoint: String, token: String, appKey: String) : this(
        endpoint = endpoint,
        token = token,
        appKey = appKey,
        client = nlsHttpClient(),
    )

    suspend fun transcribeWav(wavBytes: ByteArray): AsrResult {
        val separator = if (endpoint.contains("?")) "&" else "?"
        val response = client.post("${endpoint.trimEnd('/')}${separator}appkey=$appKey") {
            header("X-NLS-Token", token)
            contentType(ContentType.Application.OctetStream)
            setBody(wavBytes)
        }.body<NlsAsrResponse>()

        if (response.status != 20000000) {
            error("NLS ASR failed: status=${response.status}, message=${response.message.orEmpty()} taskId=${response.taskId.orEmpty()}")
        }
        return AsrResult(text = response.result.orEmpty())
    }
}

@Serializable
private data class NlsAsrResponse(
    val status: Int,
    val result: String? = null,
    val message: String? = null,
    @SerialName("task_id") val taskId: String? = null,
)

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

private fun nlsHttpClient(): HttpClient = HttpClient {
    install(ContentNegotiation) { json(json) }
}
