package net.adoptopenjdk.api.v3.dataSources.github.graphql

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import net.adoptopenjdk.api.v3.dataSources.github.VersionParser
import net.adoptopenjdk.api.v3.models.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

/*
    Models that encapsulate how github represents its release data
 */


class RateLimit @JsonCreator constructor(@JsonProperty("cost") val cost: Int,
                                         @JsonProperty("remaining") val remaining: Int)


class Repository @JsonCreator constructor(@JsonProperty("releases") val releases: GHReleases) {
    fun getReleases(): List<Release> {
        return releases.releases.map({ it.toAdoptRelease() })
    }
}

class GHReleases @JsonCreator constructor(@JsonProperty("nodes") val releases: List<GHRelease>,
                                          @JsonProperty("pageInfo") val pageInfo: PageInfo)

class PageInfo @JsonCreator constructor(@JsonProperty("hasNextPage") val hasNextPage: Boolean,
                                        @JsonProperty("endCursor") val endCursor: String?)

class GHRelease @JsonCreator constructor(
        @JsonProperty("id") val id: String,
        @JsonProperty("name") val name: String,
        @JsonProperty("isPrerelease") val isPrerelease: Boolean,
        @JsonProperty("prerelease") val prerelease: Boolean?,
        @JsonProperty("publishedAt") val publishedAt: String,
        @JsonProperty("releaseAssets") val releaseAssets: GHAssets,
        @JsonProperty("resourcePath") val resourcePath: String,
        @JsonProperty("url") val url: String) {

    fun toAdoptRelease(): Release {
        //TODO fix me before the year 2100
        val dateMatcher = """.*(20[0-9]{2}-[0-9]{2}-[0-9]{2}|20[0-9]{6}).*"""
        val hasDate = Pattern.compile(dateMatcher).matcher(name)
        val release_type: ReleaseType = if (hasDate.matches()) ReleaseType.ea else ReleaseType.ga

        val release_link = url
        val release_name = name
        val timestamp = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(publishedAt)).atZone(ZoneId.of("UTC")).toLocalDateTime()
        val download_count = 1
        val vendor = Vendor.adoptopenjdk

        val versionData: VersionData
        if (release_type == ReleaseType.ga) {
            versionData = VersionParser().parse(release_name)
        } else {
            versionData = getFeatureVersion(release_name)
        }
        val binaries = releaseAssets.toBinaryList()

        return Release(release_type, release_link, release_name, timestamp, binaries, download_count, vendor, versionData)
    }

    private fun getFeatureVersion(release_name: String): VersionData {
        val featureVersionMatcher = """.*/adoptopenjdk/openjdk(?<feature>[0-9]+).*"""
        val matched = Pattern.compile(featureVersionMatcher).matcher(resourcePath.toLowerCase())

        if (matched.matches()) {
            val featureNumber = matched.group("feature").toInt()
            return VersionData(featureNumber, 0, 0, null, 0, "", 0, null, "")
        } else {
            System.err.println()
            throw IllegalStateException("Failed to find feature version for ${release_name}")
        }
    }
}

class GHAssets @JsonCreator constructor(@JsonProperty("nodes") val assets: List<GHAsset>,
                                        @JsonProperty("pageInfo") val pageInfo: PageInfo) {
    fun toBinaryList(): List<Binary> {
        return assets
                .filter { !it.name.endsWith(".txt") }
                .map { asset ->

                    val download_count = asset.downloadCount
                    val updated_at = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(asset.updatedAt)).atZone(ZoneId.of("UTC")).toLocalDateTime()
                    val scm_ref = ""

                    val installer_name = ""
                    val installer_link = ""
                    val installer_size: Long = 0
                    val installer_checksum = ""
                    val installer_checksum_link = ""

                    val binary_name = asset.name
                    val binary_link = asset.downloadUrl
                    val binary_size = asset.size
                    val binary_checksum = ""
                    val binary_checksum_link = assets
                            .firstOrNull { it.name.equals("${asset.name}.sha256.txt") }
                            ?.downloadUrl

                    try {

                        val heap_size = getEnumFromFileName(asset.name, HeapSize.values(), HeapSize.normal)
                        val os = getEnumFromFileName(asset.name, OperatingSystem.values())
                        val architecture = getEnumFromFileName(asset.name, Architecture.values())
                        val binary_type = getEnumFromFileName(asset.name, ImageType.values(), ImageType.jdk)
                        val jvm_impl = getEnumFromFileName(asset.name, JvmImpl.values(), JvmImpl.hotspot)

                        Binary(binary_name,
                                binary_link,
                                binary_size,
                                download_count,
                                updated_at,
                                scm_ref,
                                installer_name,
                                installer_link,
                                installer_size,
                                binary_checksum,
                                binary_checksum_link ?: "",
                                installer_checksum,
                                installer_checksum_link,
                                heap_size,
                                os,
                                architecture,
                                binary_type,
                                jvm_impl)
                    } catch (e: IllegalArgumentException) {
                        System.err.println(e.message)
                        null
                    }
                }
                .filterNotNull()

    }


    private fun <T : FileNameMatcher> getEnumFromFileName(fileName: String, values: Array<T>, default: T? = null): T {

        val matched = values
                .filter { it.matchesFile(fileName) }
                .toList()

        if (matched.size != 1) {
            if (default != null) {
                return default
            }

            throw IllegalArgumentException("cannot determine ${values.get(0).javaClass.name} of asset $fileName")
        } else {
            return matched.get(0)
        }
    }
}

class GHAsset @JsonCreator constructor(
        @JsonProperty("name") val name: String,
        @JsonProperty("size") val size: Long,
        @JsonProperty("downloadUrl") val downloadUrl: String,
        @JsonProperty("downloadCount") val downloadCount: Long,
        @JsonProperty("updatedAt") val updatedAt: String
)

class AssetNode @JsonCreator constructor(@JsonProperty("releaseAssets") val releaseAssets: GHAssets)

abstract class HasRateLimit(@JsonProperty("rateLimit") val rateLimit: RateLimit) {
}

class QueryData @JsonCreator constructor(@JsonProperty("repository") val repository: Repository?,
                                         @JsonProperty("rateLimit") rateLimit: RateLimit) : HasRateLimit(rateLimit)

class ReleaseQueryData @JsonCreator constructor(@JsonProperty("node") val assetNode: AssetNode?,
                                                @JsonProperty("rateLimit") rateLimit: RateLimit) : HasRateLimit(rateLimit)

