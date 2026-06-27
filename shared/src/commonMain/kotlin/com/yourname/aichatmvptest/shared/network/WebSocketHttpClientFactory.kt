package com.yourname.aichatmvptest.shared.network

import io.ktor.client.HttpClient

expect fun webSocketHttpClient(config: HttpClientConfigBlock): HttpClient

typealias HttpClientConfigBlock = io.ktor.client.HttpClientConfig<*>.() -> Unit
