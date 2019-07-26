package net.adoptopenjdk.api.v3.dataSources.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

class AdoptRepos @JsonCreator constructor(
        @JsonProperty("repos")
        @JsonDeserialize(keyAs = Int::class)
        val repos: Map<Int, FeatureRelease>) {

    fun getFeatureRelease(version: Int): FeatureRelease? {
        return repos.get(version)
    }

    constructor(list: List<FeatureRelease>) : this(list
            .map { Pair(it.featureVersion, it) }
            .toMap())

}