package net.adoptopenjdk.api.v3

import java.net.http.HttpClient


object HttpClientFactory {
    // Current default impl is Graphql impl
    var client: HttpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build()

    fun getHttpClient(): HttpClient {
        return client
    }
}