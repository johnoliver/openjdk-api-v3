package net.adoptopenjdk.api.v3.dataSources.github.graphql

import net.adoptopenjdk.api.v3.dataSources.github.GitHubApi
import net.adoptopenjdk.api.v3.dataSources.github.graphql.clients.GraphQLGitHubDownloadCountClient
import net.adoptopenjdk.api.v3.dataSources.github.graphql.clients.GraphQLGitHubReleaseClient
import net.adoptopenjdk.api.v3.dataSources.github.graphql.clients.GraphQLGitHubRepositoryClient
import net.adoptopenjdk.api.v3.dataSources.github.graphql.clients.GraphQLGitHubSummaryClient
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHRelease
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHRepository
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.GHRepositoryDownload
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.GHRepositorySummary


class GraphQLGitHubClient : GitHubApi {
    private val summaryClient = GraphQLGitHubSummaryClient()
    private val releaseClient = GraphQLGitHubReleaseClient()
    private val repositoryClientClient = GraphQLGitHubRepositoryClient()
    private val downloadClient = GraphQLGitHubDownloadCountClient()

    override suspend fun getRepositorySummary(repoName: String): GHRepositorySummary {
        return summaryClient.getRepositorySummary(repoName)
    }

    override suspend fun getDownloadSummary(repoName: String): GHRepositoryDownload {
        return downloadClient.getDowloadSummary(repoName)
    }

    override suspend fun getReleaseById(id: String): GHRelease? {
        return releaseClient.getReleaseById(id)
    }

    override suspend fun getRepository(repoName: String): GHRepository {
        return repositoryClientClient.getRepository(repoName)
    }


}