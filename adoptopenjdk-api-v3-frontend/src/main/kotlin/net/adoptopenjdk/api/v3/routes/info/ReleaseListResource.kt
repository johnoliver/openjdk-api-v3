package net.adoptopenjdk.api.v3.routes.info

import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.filters.BinaryFilter
import net.adoptopenjdk.api.v3.dataSources.filters.ReleaseFilter
import net.adoptopenjdk.api.v3.dataSources.filters.VersionRangeFilter
import net.adoptopenjdk.api.v3.models.*
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import org.jboss.resteasy.annotations.jaxrs.PathParam
import org.jboss.resteasy.annotations.jaxrs.QueryParam
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Tag(name = "Release Info")
@Path("info")
@Produces(MediaType.APPLICATION_JSON)
class ReleaseListResource {

    @GET
    @Path("release_list")
    @Operation(summary = "Returns a list of all release names")
    fun get(
            @Parameter(name = "release_type", description = "Release type", required = false)
            @QueryParam("release_type")
            release_type: ReleaseType?,

            @Parameter(name = "version", description = "Semantic version range (maven style) e.g \"11.0.4+11.1\", \"[1.0,2.0)\", \"(,1.0]\".", required = false)
            @QueryParam("version")
            version: String?,

            @Parameter(name = "vendor", description = "Vendor", required = false)
            @QueryParam("vendor")
            vendor: Vendor?,

            @Parameter(name = "page_size", description = "Pagination page size", schema = Schema(defaultValue = "10"), required = false)
            @QueryParam("page_size")
            pageSize: Int?,

            @Parameter(name = "page", description = "Pagination page number", schema = Schema(defaultValue = "0"), required = false)
            @QueryParam("page")
            page: Int?

    ): ReleaseList {

        val filteredReleases = getReleases(release_type, vendor, version)

        val releases = filteredReleases
                .map { it.release_name }
                .toList()

        return ReleaseList(releases)
    }

    @Path("release_version_list")
    @GET
    @Operation(summary = "Returns a list of all release versions")
    fun getVersions(
            @Parameter(name = "release_type", description = "Release type", required = false)
            @QueryParam("release_type")
            release_type: ReleaseType?,

            @Parameter(name = "version", description = "Semantic version range (maven style) e.g \"11.0.4+11.1\", \"[1.0,2.0)\", \"(,1.0]\".", required = false)
            @QueryParam("version")
            version: String?,

            @Parameter(name = "vendor", description = "Vendor", required = false)
            @QueryParam("vendor")
            vendor: Vendor?,

            @Parameter(name = "page_size", description = "Pagination page size", schema = Schema(defaultValue = "10"), required = false)
            @QueryParam("page_size")
            pageSize: Int?,

            @Parameter(name = "page", description = "Pagination page number", schema = Schema(defaultValue = "0"), required = false)
            @QueryParam("page")
            page: Int?

    ): ReleaseVersionList {

        val filteredReleases = getReleases(release_type, vendor, version)

        val releases = filteredReleases
                .map { it.version_data }
                .distinct()
                .toList()

        return ReleaseVersionList(releases)
    }

    private fun getReleases(release_type: ReleaseType?, vendor: Vendor?, version: String?): Sequence<Release> {
        val range = VersionRangeFilter(version)
        val releaseFilter = ReleaseFilter(release_type, null, null, vendor, range)
        val filteredReleases = APIDataStore
                .getAdoptRepos()
                .getReleases(releaseFilter)
        return filteredReleases
    }
}
