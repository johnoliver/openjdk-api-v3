package net.adoptopenjdk.api.v3.dataSources.github

import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.Repository

interface GitHubApi {
    suspend fun getRepository(repoName: String): Repository
}
