package net.adoptopenjdk.api.v3

import net.adoptopenjdk.api.v3.dataSources.github.GitHubApi
import net.adoptopenjdk.api.v3.dataSources.github.graphql.GraphQLGitHubClient
import net.adoptopenjdk.api.v3.models.Release


object GitHubClientFactory {
    // Current default impl is Graphql impl
    var client: GitHubApi = GraphQLGitHubClient()

    suspend fun getReleases(repoName: String): List<Release> {
        return client.getRepository(repoName).getReleases()
    }
}