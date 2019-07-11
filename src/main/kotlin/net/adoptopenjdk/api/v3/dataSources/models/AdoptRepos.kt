package net.adoptopenjdk.api.v3.dataSources.models

import com.fasterxml.jackson.annotation.JsonCreator
import kotlinx.coroutines.*
import net.adoptopenjdk.api.v3.dataSources.github.GitHubClient
import kotlin.system.exitProcess


class AdoptRepos @JsonCreator constructor(val repos: Map<Int, FeatureRelease>) {

    object Builder {
        fun build(versions: List<Int>): AdoptRepos {
            //Fetch repos in parallel
            val reposMap = versions
                    .map { version ->
                        GlobalScope.async(Dispatchers.IO) {
                            println("getting $version")
                            val r = listOf(
                                    getRepoAsync("openjdk$version-openj9-nightly"),
                                    getRepoAsync("openjdk$version-nightly"),
                                    getRepoAsync("openjdk$version-binaries"))
                                    .map { defered -> GlobalScope.run { defered.await() } }
                                    .filterNotNull()
                                    .requireNoNulls()

                            FeatureRelease(version, r)
                        }
                    }
                    .map { defered -> runBlocking { defered.await() } }
                    .map { Pair(it.featureVersion, it) }
                    .toMap()
            println("DONE")
            return AdoptRepos(reposMap)
        }

        private fun getRepoAsync(repoName: String): Deferred<AdoptRepo?> {
            return GlobalScope.async {
                println("getting $repoName")
                try {
                    AdoptRepo(GitHubClient.getReleases(repoName))
                } catch (e: Exception) {
                    e.printStackTrace()
                    exitProcess(1)
                }
            }
        }
    }

    fun getFeatureRelease(version: Int): FeatureRelease {
        return repos[version] ?: FeatureRelease(version, listOf())
    }

}
