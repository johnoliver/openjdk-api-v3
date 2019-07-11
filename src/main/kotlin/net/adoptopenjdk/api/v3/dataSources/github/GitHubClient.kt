package net.adoptopenjdk.api.v3.dataSources.github

import net.adoptopenjdk.api.v3.dataSources.github.graphql.GraphQLGitHubClient
import net.adoptopenjdk.api.v3.dataSources.github.graphql.Repository
import net.adoptopenjdk.api.v3.models.Release


interface GitHubApi {
    suspend fun getRepository(repoName: String): Repository
}

object GitHubClient {
    // Current default impl is Graphql impl
    var client: GitHubApi = GraphQLGitHubClient()

    suspend fun getReleases(repoName: String): List<Release> {
        return client.getRepository(repoName).getReleases()
    }
}