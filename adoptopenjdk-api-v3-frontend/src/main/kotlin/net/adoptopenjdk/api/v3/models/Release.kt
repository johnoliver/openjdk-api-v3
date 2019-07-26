package net.adoptopenjdk.api.v3.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import net.adoptopenjdk.api.v3.dataSources.filters.BinaryFilter
import net.adoptopenjdk.api.v3.dataSources.models.Releases
import org.eclipse.microprofile.openapi.annotations.media.Schema
import java.time.LocalDateTime


class Release {

    @Schema(example = "https://github.com/AdoptOpenJDK/openjdk8-openj9-releases/ga/tag/jdk8u162-b12_openj9-0.8.0", required = true)
    val release_link: String

    @Schema(example = "jdk8u162-b12_openj9-0.8.0", required = true)
    val release_name: String

    @Schema(example = "2018-03-15T12:12:35.000Z", required = true)
    val timestamp: LocalDateTime

    @Schema(required = true, implementation = Binary::class)
    val binaries: List<Binary>

    @Schema(example = "7128", required = true)
    val download_count: Int

    @Schema(example = "ga", required = true)
    val release_type: ReleaseType

    @Schema(example = "adopt", required = true)
    val vendor: Vendor

    val version_data: VersionData

    @JsonCreator
    constructor(
            @JsonProperty("release_type") release_type: ReleaseType,
            @JsonProperty("release_link") release_link: String,
            @JsonProperty("release_name") release_name: String,
            @JsonProperty("timestamp") timestamp: LocalDateTime,
            @JsonProperty("binaries") binaries: List<Binary>,
            @JsonProperty("download_count") download_count: Int,
            @JsonProperty("vendor") vendor: Vendor,
            @JsonProperty("version_data") version_data: VersionData) {
        this.release_type = release_type
        this.release_link = release_link
        this.release_name = release_name
        this.timestamp = timestamp
        this.binaries = binaries
        this.download_count = download_count
        this.vendor = vendor
        this.version_data = version_data
    }

    constructor(release: Release, binaries: List<Binary>) {
        this.release_type = release.release_type
        this.release_link = release.release_link
        this.release_name = release.release_name
        this.timestamp = release.timestamp
        this.binaries = binaries
        this.download_count = release.download_count
        this.vendor = release.vendor
        this.version_data = release.version_data
    }

    fun filterBinaries(binaryFilter: BinaryFilter): Release {
        return Release(this, binaries
                .filter { binaryFilter.test(it) })

    }
}