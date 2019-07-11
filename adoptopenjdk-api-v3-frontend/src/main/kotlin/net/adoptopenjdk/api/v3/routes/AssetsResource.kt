package net.adoptopenjdk.api.v3.routes

import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.filters.BinaryFilter
import net.adoptopenjdk.api.v3.dataSources.filters.ReleaseFilter
import net.adoptopenjdk.api.v3.dataSources.filters.VersionRangeFilter
import net.adoptopenjdk.api.v3.dataSources.models.FeatureRelease
import net.adoptopenjdk.api.v3.models.*
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import org.jboss.resteasy.annotations.jaxrs.PathParam
import org.jboss.resteasy.annotations.jaxrs.QueryParam
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import kotlin.math.min


@Tag(name = "Assets")
@Path("assets/")
@Produces(MediaType.APPLICATION_JSON)
class AssetsResource {

    @GET
    @Path("feature_releases/{feature_version}/{release_type}")
    @Operation(summary = "Returns release information", description = "List of information about builds that match the current query")
    @APIResponses(value = [
        APIResponse(responseCode = "200", description = "search results matching criteria",
                content = [Content(schema = Schema(type = SchemaType.ARRAY, implementation = Release::class))]
        ),
        APIResponse(responseCode = "400", description = "bad input parameter",
                content = [Content(schema = Schema(implementation = Void::class))])
    ])
    fun get(
            @Parameter(name = "release_type", description = "Release type", required = true)
            @PathParam("release_type")
            release_type: ReleaseType?,

            @Parameter(name = "feature_version", description = "Feature release version e.g. 8,11,13.", required = true,
                    schema = Schema(defaultValue = "8"))
            @PathParam("feature_version")
            version: Int?,

            @Parameter(name = "os", description = "Operating System", required = false)
            @QueryParam("os")
            os: OperatingSystem?,

            @Parameter(name = "architecture", description = "Architecture", required = false)
            @QueryParam("architecture")
            arch: Architecture?,

            @Parameter(name = "image_type", description = "Image Type", required = false)
            @QueryParam("image_type")
            image_type: ImageType?,

            @Parameter(name = "jvm_impl", description = "JVM Implementation", required = false)
            @QueryParam("jvm_impl")
            jvm_impl: JvmImpl?,

            @Parameter(name = "heap_size", description = "Heap Size", required = false)
            @QueryParam("heap_size")
            heap_size: HeapSize?,

            @Parameter(name = "vendor", description = "Vendor", required = false)
            @QueryParam("vendor")
            vendor: Vendor?,

            @Parameter(name = "project", description = "Project", required = false)
            @QueryParam("project")
            project: Project?,

            @Parameter(name = "page_size", description = "Pagination page size", schema = Schema(defaultValue = "10"), required = false)
            @QueryParam("page_size")
            pageSize: Int?,

            @Parameter(name = "page", description = "Pagination page number", schema = Schema(defaultValue = "0"), required = false)
            @QueryParam("page")
            page: Int?

    ): List<Release> {
        if (release_type == null || version == null) {
            throw BadRequestException("Unrecognised type")
        }

        val releaseFilter = ReleaseFilter(release_type, version, null, vendor, null)
        val binaryFilter = BinaryFilter(os, arch, image_type, jvm_impl, heap_size)
        val repos = APIDataStore.getAdoptRepos().getFeatureRelease(version)

        if (repos == null) {
            throw NotFoundException()
        }

        val releases = APIDataStore.getAdoptRepos().getFilteredReleases(version, releaseFilter, binaryFilter)
        return getPage(pageSize, page, releases)
    }

    @GET
    @Path("version/{version}")
    @Operation(summary = "Returns release information about the specified version. Returns GA releases only.", description = "List of information about builds that match the current query ")
    @APIResponses(value = [
        APIResponse(responseCode = "200", description = "search results matching criteria",
                content = [Content(schema = Schema(type = SchemaType.ARRAY, implementation = Release::class))]
        ),
        APIResponse(responseCode = "400", description = "bad input parameter",
                content = [Content(schema = Schema(implementation = Void::class))])
    ])
    fun getReleaseVersion(
            @Parameter(name = "version", description = "Semantic version range (maven style) e.g \"11.0.4+11.1\", \"[1.0,2.0)\", \"(,1.0]\".", required = true)
            @PathParam("version")
            version: String,

            @Parameter(name = "os", description = "Operating System", required = false)
            @QueryParam("os")
            os: OperatingSystem?,

            @Parameter(name = "architecture", description = "Architecture", required = false)
            @QueryParam("architecture")
            arch: Architecture?,

            @Parameter(name = "image_type", description = "Image Type", required = false)
            @QueryParam("image_type")
            image_type: ImageType?,

            @Parameter(name = "jvm_impl", description = "JVM Implementation", required = false)
            @QueryParam("jvm_impl")
            jvm_impl: JvmImpl?,

            @Parameter(name = "heap_size", description = "Heap Size", required = false)
            @QueryParam("heap_size")
            heap_size: HeapSize?,

            @Parameter(name = "vendor", description = "Vendor", required = false)
            @QueryParam("vendor")
            vendor: Vendor?,

            @Parameter(name = "project", description = "Project", required = false)
            @QueryParam("project")
            project: Project?,

            @Parameter(name = "lts", description = "Include only LTS releases", required = false)
            @QueryParam("lts")
            lts: Boolean?,

            @Parameter(name = "release_type", description = "Release type", required = false)
            @QueryParam("release_type")
            release_type: ReleaseType?,

            @Parameter(name = "page_size", description = "Pagination page size", schema = Schema(defaultValue = "20"), required = false)
            @QueryParam("page_size")
            pageSize: Int?,

            @Parameter(name = "page", description = "Pagination page number", schema = Schema(defaultValue = "0"), required = false)
            @QueryParam("page")
            page: Int?

    ): List<Release> {

        // Require GA due to version range having no meaning for nightlies

        val range = VersionRangeFilter(version)

        val releaseFilter = ReleaseFilter(release_type, null, null, vendor, range)
        val binaryFilter = BinaryFilter(os, arch, image_type, jvm_impl, heap_size)

        val releases = APIDataStore.getAdoptRepos().getFilteredReleases(releaseFilter, binaryFilter)
        return getPage(pageSize, page, releases)
    }


    private fun getPage(pageSize: Int?, page: Int?, releases: Sequence<Release>): List<Release> {
        val pageSizeNum = min(20, (pageSize ?: 10))
        val pageNum = page ?: 0

        val chunked = releases.chunked(pageSizeNum)

        try {
            return chunked.elementAt(pageNum)
        } catch (e: IndexOutOfBoundsException) {
            throw NotFoundException("Page not available")
        }
    }

}
