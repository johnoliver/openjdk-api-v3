package net.adoptopenjdk.api.v3.dataSources.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import net.adoptopenjdk.api.v3.dataSources.filters.BinaryFilter
import net.adoptopenjdk.api.v3.models.Release

class Releases @JsonCreator constructor(
        @JsonProperty("nodes") val nodes: List<Release>) {

    fun filterBinaries(binaryFilter: BinaryFilter): Releases {
        val filtered = nodes
                .map {
                    Release(it, it.binaries.filter { binaryFilter.test(it) })
                }
        return Releases(filtered)
    }

}
