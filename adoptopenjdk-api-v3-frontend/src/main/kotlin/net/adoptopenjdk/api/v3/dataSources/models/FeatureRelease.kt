package net.adoptopenjdk.api.v3.dataSources.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import net.adoptopenjdk.api.v3.dataSources.filters.ReleaseFilter
import net.adoptopenjdk.api.v3.models.Release

class FeatureRelease {

    val featureVersion: Int
    val releases: Releases

    constructor(featureVersion: Int, repos: List<AdoptRepo>) {
        releases = Releases(repos.flatMap { it.releases })
        this.featureVersion = featureVersion
    }

    @JsonCreator
    constructor(
            @JsonProperty("featureVersion")
            featureVersion: Int,
            @JsonProperty("releases")
            releases: Releases) {
        this.featureVersion = featureVersion
        this.releases = releases
    }

    fun getReleases(filter: ReleaseFilter): Sequence<Release> {
        val releases = releases
                .nodes
                .asSequence()
                .filter { filter.test(it) }

        return releases
    }
}