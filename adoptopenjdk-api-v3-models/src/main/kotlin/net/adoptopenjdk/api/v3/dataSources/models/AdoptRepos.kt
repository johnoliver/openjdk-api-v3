package net.adoptopenjdk.api.v3.dataSources.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import net.adoptopenjdk.api.v3.dataSources.filters.BinaryFilter
import net.adoptopenjdk.api.v3.dataSources.filters.ReleaseFilter
import net.adoptopenjdk.api.v3.models.Release

class AdoptRepos {

    val repos: Map<Int, FeatureRelease>

    @JsonIgnore
    val allReleases: Releases

    @JsonCreator
    constructor(
            @JsonProperty("repos")
            @JsonDeserialize(keyAs = Int::class)
            repos: Map<Int, FeatureRelease>) {
        this.repos = repos

        val releases = repos
                .asSequence()
                .filterNotNull()
                .map { it.value.releases }
                .flatMap { it.getReleases() }
                .toList()

        allReleases = Releases(releases)
    }

    fun getFeatureRelease(version: Int): FeatureRelease? {
        return repos.get(version)
    }

    constructor(list: List<FeatureRelease>) : this(list
            .map { Pair(it.featureVersion, it) }
            .toMap())


    fun getReleases(releaseFilter: ReleaseFilter): Sequence<Release> {
        return allReleases.getReleases(releaseFilter)
    }

    fun getFilteredReleases(version: Int, releaseFilter: ReleaseFilter, binaryFilter: BinaryFilter): Sequence<Release> {
        val featureRelease = getFeatureRelease(version)
        if (featureRelease == null) return emptySequence()

        return getFilteredReleases(featureRelease.releases.getReleases(releaseFilter), binaryFilter)
    }

    fun getFilteredReleases(releaseFilter: ReleaseFilter, binaryFilter: BinaryFilter): Sequence<Release> {
        return getFilteredReleases(allReleases.getReleases(releaseFilter), binaryFilter)
    }

    fun getFilteredReleases(releases: Sequence<Release>, binaryFilter: BinaryFilter): Sequence<Release> {
        return releases
                .map { release ->
                    release.filterBinaries(binaryFilter)
                }
                .filter { it.binaries.isNotEmpty() }
    }

    fun addRelease(i: Int, r: Release): AdoptRepos {
        return AdoptRepos(repos.plus(Pair(i, repos.get(i)!!.add(listOf(r)))))
    }

    fun removeRelease(i: Int, r: Release): AdoptRepos {
        return AdoptRepos(repos.plus(Pair(i, repos.get(i)!!.remove(r.id))))
    }

}