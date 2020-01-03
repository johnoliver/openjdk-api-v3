package net.adoptopenjdk.api.v3

import java.net.http.HttpClient
import java.time.Duration

object HttpClientFactory {
    private var client: HttpClient = HttpClient
            .newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30))
            .build()


    private var nonRedirect: HttpClient = HttpClient
            .newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(30))
            .build()

    fun getHttpClient(): HttpClient {
        return client
    }

    fun getNonRedirectHttpClient(): HttpClient {
        return nonRedirect
    }

    fun setClient(client: HttpClient) {
        this.client = client
        this.nonRedirect = client
    }
}