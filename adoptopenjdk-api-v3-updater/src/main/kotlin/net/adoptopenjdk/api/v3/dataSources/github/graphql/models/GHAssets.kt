package net.adoptopenjdk.api.v3.dataSources.github.graphql.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import net.adoptopenjdk.api.v3.HttpClientFactory
import net.adoptopenjdk.api.v3.dataSources.github.graphql.PageInfo
import net.adoptopenjdk.api.v3.models.*
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class GHAssets @JsonCreator constructor(@JsonProperty("nodes") val assets: List<GHAsset>,
                                             @JsonProperty("pageInfo") val pageInfo: PageInfo) {


    val BINARY_ASSET_WHITELIST: List<String> = listOf(".tar.gz", ".msi", ".pkg", ".zip", ".deb", ".rpm")
    val ARCHIVE_WHITELIST: List<String> = listOf(".tar.gz", ".zip")

    suspend fun toBinaryList(metadata: Map<GHAsset, GHMetaData>): List<Binary> {
        // probably whitelist rather than black list
        return assets
                .filter(this::isArchive)
                .map { asset -> assetToBinary(asset, metadata) }

    }

    private suspend fun assetToBinary(asset: GHAsset, metadata: Map<GHAsset, GHMetaData>): Binary {
        val download_count = asset.downloadCount
        val updated_at = getUpdatedTime(asset)

        val binaryMetadata = metadata.get(asset);

        val installer_name = ""
        val installer_link = ""
        val installer_size: Long = 0
        val installer_checksum = ""
        val installer_checksum_link = ""
        val binary_name = asset.name
        val binary_link = asset.downloadUrl
        val binary_size = asset.size
        val heap_size = getEnumFromFileName(asset.name, HeapSize.values(), HeapSize.normal)
        val binary_checksum_link = getCheckSumLink(binary_name)

        if (binaryMetadata != null) {
            return binaryFromMetadata(binaryMetadata, binary_name, binary_link, binary_size, download_count, updated_at, installer_name, installer_link, installer_size, binary_checksum_link, installer_checksum, installer_checksum_link, heap_size)
        } else {
            return binaryFromName(asset, binary_checksum_link, binary_name, binary_link, binary_size, download_count, updated_at, installer_name, installer_link, installer_size, installer_checksum, installer_checksum_link, heap_size)
        }
    }

    private fun getUpdatedTime(asset: GHAsset) =
            Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(asset.updatedAt)).atZone(ZoneId.of("UTC")).toLocalDateTime()

    private fun getCheckSumLink(binary_name: String): String? {
        return assets
                .firstOrNull { asset ->
                    asset.name.equals("${binary_name}.sha256.txt") ||
                            (binary_name.split(".")[0] + ".sha256.txt").equals(asset.name)
                }
                ?.downloadUrl
    }

    private fun isArchive(asset: GHAsset) =
            ARCHIVE_WHITELIST.filter { asset.name.endsWith(it) }.isNotEmpty()


    private suspend fun binaryFromName(asset: GHAsset, binary_checksum_link: String?, binary_name: String, binary_link: String, binary_size: Long, download_count: Long, updated_at: LocalDateTime, installer_name: String, installer_link: String, installer_size: Long, installer_checksum: String, installer_checksum_link: String, heap_size: HeapSize): Binary {
        val scm_ref = null
        val os = getEnumFromFileName(asset.name, OperatingSystem.values())
        val architecture = getEnumFromFileName(asset.name, Architecture.values())
        val binary_type = getEnumFromFileName(asset.name, ImageType.values(), ImageType.jdk)
        val jvm_impl = getEnumFromFileName(asset.name, JvmImpl.values(), JvmImpl.hotspot)

        val binary_checksum = getChecksum(binary_checksum_link)

        return Binary(binary_name,
                binary_link,
                binary_size,
                download_count,
                updated_at,
                scm_ref,
                installer_name,
                installer_link,
                installer_size,
                binary_checksum,
                binary_checksum_link,
                installer_checksum,
                installer_checksum_link,
                heap_size,
                os,
                architecture,
                binary_type,
                jvm_impl)
    }

    private fun binaryFromMetadata(binaryMetadata: GHMetaData, binary_name: String, binary_link: String,
                                   binary_size: Long, download_count: Long, updated_at: LocalDateTime,
                                   installer_name: String, installer_link: String, installer_size: Long,
                                   binary_checksum_link: String?, installer_checksum: String,
                                   installer_checksum_link: String, heap_size: HeapSize): Binary {
        return Binary(binary_name,
                binary_link,
                binary_size,
                download_count,
                updated_at,
                binaryMetadata.scmRef,
                installer_name,
                installer_link,
                installer_size,
                binaryMetadata.sha256,
                binary_checksum_link,
                installer_checksum,
                installer_checksum_link,
                heap_size,
                binaryMetadata.os,
                binaryMetadata.arch,
                binaryMetadata.binary_type,
                binaryMetadata.variant)
    }

    private suspend fun getChecksum(binary_checksum_link: String?): String? {
        var binary_checksum: String? = null
        if (binary_checksum_link != null && binary_checksum_link.isNotEmpty()) {

            val request = HttpRequest.newBuilder()
                    .uri(URI.create(binary_checksum_link))
                    .build()

            val deferrd = GlobalScope.async(Dispatchers.IO) {
                HttpClientFactory.getHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString()).get()
            }

            val checksum = deferrd.await()

            if (checksum.statusCode() == 200 && checksum.body() != null) {
                val tokens = checksum.body().split(" ");
                if (tokens.size > 1) {
                    binary_checksum = tokens[0]
                }
            }
        }
        return binary_checksum
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