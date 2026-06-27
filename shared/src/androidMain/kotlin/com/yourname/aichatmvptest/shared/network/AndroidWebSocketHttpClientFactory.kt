package com.yourname.aichatmvptest.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

actual fun webSocketHttpClient(config: HttpClientConfigBlock): HttpClient = HttpClient(OkHttp, config)
