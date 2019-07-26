package net.adoptopenjdk.api.v3

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepo
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.models.FeatureRelease
import kotlin.system.exitProcess

object AdoptReposBuilder {
    suspend fun build(versions: List<Int>): AdoptRepos {
        //Fetch repos in parallel
        val reposMap = versions
                .map { version -> getDataForEachRepo(version) }
                .map { Pair(it.featureVersion, it) }
                .toMap()
        println("DONE")
        return AdoptRepos(reposMap)
    }

    private suspend fun getDataForEachRepo(version: Int): FeatureRelease {
        println("getting $version")
        val repos = listOf(
                getRepoAsync("openjdk$version-openj9-nightly"),
                getRepoAsync("openjdk$version-nightly"),
                getRepoAsync("openjdk$version-binaries"))
                .map { defered -> GlobalScope.run { defered.await() } }
                .filterNotNull()
                .requireNoNulls()

        return FeatureRelease(version, repos)
    }

    private fun getRepoAsync(repoName: String): Deferred<AdoptRepo?> {
        return GlobalScope.async {
            println("getting $repoName")
            try {
                AdoptRepo(GitHubClientFactory.getReleases(repoName))
            } catch (e: Exception) {
                e.printStackTrace()
                exitProcess(1)
            }
        }
    }
}