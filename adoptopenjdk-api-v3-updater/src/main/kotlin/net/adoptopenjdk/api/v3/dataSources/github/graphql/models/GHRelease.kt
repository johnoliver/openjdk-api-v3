package net.adoptopenjdk.api.v3.dataSources.github.graphql.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.adoptopenjdk.api.v3.dataSources.github.VersionParser
import net.adoptopenjdk.api.v3.HttpClientFactory
import net.adoptopenjdk.api.v3.dataSources.github.graphql.PageInfo
import net.adoptopenjdk.api.v3.models.Release
import net.adoptopenjdk.api.v3.models.ReleaseType
import net.adoptopenjdk.api.v3.models.Vendor
import net.adoptopenjdk.api.v3.models.VersionData
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern


data class GHReleases @JsonCreator constructor(@JsonProperty("nodes") val releases: List<GHRelease>,
                                               @JsonProperty("pageInfo") val pageInfo: PageInfo)

data class GHRelease @JsonCreator constructor(
        @JsonProperty("id") val id: String,
        @JsonProperty("name") val name: String,
        @JsonProperty("isPrerelease") val isPrerelease: Boolean,
        @JsonProperty("prerelease") val prerelease: Boolean?,
        @JsonProperty("publishedAt") val publishedAt: String,
        @JsonProperty("releaseAssets") val releaseAssets: GHAssets,
        @JsonProperty("resourcePath") val resourcePath: String,
        @JsonProperty("url") val url: String) {


    suspend fun toAdoptRelease(): Release {
        //TODO fix me before the year 2100
        val dateMatcher = """.*(20[0-9]{2}-[0-9]{2}-[0-9]{2}|20[0-9]{6}).*"""
        val hasDate = Pattern.compile(dateMatcher).matcher(name)
        val release_type: ReleaseType = if (hasDate.matches()) ReleaseType.ea else ReleaseType.ga

        val release_link = url
        val release_name = name
        val timestamp = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(publishedAt)).atZone(ZoneId.of("UTC")).toLocalDateTime()
        val download_count = 1
        val vendor = Vendor.adoptopenjdk


        val metadata = getMetadata(releaseAssets);

        val versionData = getVersionData(metadata, release_type, release_name)

        val binaries = releaseAssets.toBinaryList(metadata)

        return Release(release_type, release_link, release_name, timestamp, binaries, download_count, vendor, versionData)
    }

    private fun getVersionData(metadata: Map<GHAsset, GHMetaData>, release_type: ReleaseType, release_name: String): VersionData {
        return metadata
                .values
                .map { it.version.toApiVersion() }
                .ifEmpty {
                    //if we have no metadata resort to parsing release names
                    parseVersionInfo(release_type, release_name)
                }
                .first()

    }

    private fun parseVersionInfo(release_type: ReleaseType, release_name: String): List<VersionData> {
        return if (release_type == ReleaseType.ga) {
            listOf(VersionParser().parse(release_name))
        } else {
            listOf(getFeatureVersion(release_name))
        }
    }

    private fun getMetadata(releaseAssets: GHAssets): Map<GHAsset, GHMetaData> {
        return releaseAssets
                .assets
                .filter { it.name.endsWith(".json") }
                .map { metadataAsset ->
                    pairUpBinaryAndMetadata(releaseAssets, metadataAsset)
                }
                .filterNotNull()
                .toMap()
    }

    private fun pairUpBinaryAndMetadata(releaseAssets: GHAssets, metadataAsset: GHAsset): Pair<GHAsset, GHMetaData>? {
        val binaryAsset = releaseAssets
                .assets
                .filter {
                    metadataAsset.name.startsWith(it.name)
                }
                .firstOrNull()

        val metadata = getMetadata(metadataAsset)
        return if (binaryAsset == null || metadata == null) {
            null
        } else {
            Pair(binaryAsset, metadata)
        }
    }

    private fun getMetadata(it: GHAsset): GHMetaData? {
        val request = HttpRequest.newBuilder()
                .uri(URI.create(it.downloadUrl))
                .build()

        val metadata = HttpClientFactory.getHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString()).get()

        if (metadata.statusCode() == 200 && metadata.body() != null) {
            try {
                return jacksonObjectMapper().readValue(metadata.body(), GHMetaData::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    private fun getFeatureVersion(release_name: String): VersionData {
        val featureVersionMatcher = """.*/adoptopenjdk/openjdk(?<feature>[0-9]+).*"""
        val matched = Pattern.compile(featureVersionMatcher).matcher(resourcePath.toLowerCase())

        if (matched.matches()) {
            val featureNumber = matched.group("feature").toInt()
            return VersionData(featureNumber, 0, 0, null, 0, "", 0, null, "")
        } else {
            //TODO: Catch this sooner
            throw IllegalStateException("Failed to find feature version for ${release_name}")
        }
    }
}