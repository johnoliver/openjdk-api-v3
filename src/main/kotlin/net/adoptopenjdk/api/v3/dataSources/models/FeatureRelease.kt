package net.adoptopenjdk.api.v3.dataSources.models

import com.fasterxml.jackson.annotation.JsonCreator
import net.adoptopenjdk.api.v3.dataSources.filters.ReleaseFilter

class FeatureRelease {

    val featureVersion: Int
    val releases: Releases

    constructor(featureVersion: Int, repos: List<AdoptRepo>) {
        releases = Releases(repos.flatMap { it.releases })
        this.featureVersion = featureVersion
    }

    @JsonCreator
    constructor(featureVersion: Int, releases: Releases) {
        this.featureVersion = featureVersion
        this.releases = releases
    }

    fun getReleases(filter: ReleaseFilter): Releases {
        val releases = releases.releases.filter { filter.test(it) }
        return Releases(releases)
    }
}