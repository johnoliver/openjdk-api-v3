package net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.PageInfo


data class GHRepositoryDownload @JsonCreator constructor(
        @JsonProperty("releases") val releases: List<GHDownloadsSummary>,
        @JsonProperty("pageInfo") val pageInfo: PageInfo)

data class GHDownloadsSummary @JsonCreator constructor(
        @JsonProperty("id") val id: String,
        @JsonProperty("nodes") val nodes: List<GHDownloadSummary>)

data class GHDownloadSummary @JsonCreator constructor(
        @JsonProperty("releaseAssets") val releaseAssets: List<ReleaseAssets>)


data class ReleaseAssets @JsonCreator constructor(
        @JsonProperty("name") val name: String,
        @JsonProperty("downloads") val downloads: Int)