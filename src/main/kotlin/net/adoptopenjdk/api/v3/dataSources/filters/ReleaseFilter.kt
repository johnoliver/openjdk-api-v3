package net.adoptopenjdk.api.v3.dataSources.filters

import net.adoptopenjdk.api.v3.models.Release
import net.adoptopenjdk.api.v3.models.ReleaseType
import net.adoptopenjdk.api.v3.models.Vendor
import java.util.function.Predicate

class ReleaseFilter(private val releaseType: ReleaseType,
                    private val version: Int,
                    private val releaseName: String?,
                    private val vendor: Vendor?) : Predicate<Release> {

    override fun test(release: Release): Boolean {
        return release.release_type == releaseType &&
                release.version_data.major == version &&
                (releaseName == null || release.release_name == releaseName) &&
                (vendor == null || release.vendor == vendor)
    }

}
