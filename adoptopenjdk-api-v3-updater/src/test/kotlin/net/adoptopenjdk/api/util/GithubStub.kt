package net.adoptopenjdk.api.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import net.adoptopenjdk.api.v3.dataSources.github.GitHubApi
import net.adoptopenjdk.api.v3.dataSources.github.graphql.PageInfo
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHReleases
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.Repository

class GithubStub : GitHubApi {
    val repos: Map<String, Repository>

    init {
        val mapper = ObjectMapper()
                .registerModule(KotlinModule())
                .registerModule(JavaTimeModule())

        this.repos = (8..13).flatMap { version ->
            listOf(
                    "openjdk$version-openj9-nightly",
                    "openjdk$version-nightly",
                    "openjdk$version-binaries")
                    .map { repoName ->

                        var json = GithubStub::class.java.classLoader.getResource("serialized-$repoName.json").readText()


                        Pair(repoName, mapper.readValue(json, Repository::class.java))
                    }
        }.toMap()
    }

    override suspend fun getRepository(repoName: String): Repository {
        return repos.get(repoName) ?: Repository(GHReleases(listOf(), PageInfo(false, null)))
    }

}
