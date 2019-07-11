package net.adoptopenjdk.api.v3.routes

import net.adoptopenjdk.api.v3.dataSources.AdoptDataStore
import net.adoptopenjdk.api.v3.dataSources.filters.BinaryFilter
import net.adoptopenjdk.api.v3.dataSources.filters.ReleaseFilter
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


@Tag(name = "Assets")
@Path("assets/")
@Produces(MediaType.APPLICATION_JSON)
class AssetsResource {

    @GET
    @Path("{release_type}/{version}")
    @Operation(summary = "Returns release information", description = "List of information about builds that match the current query ")
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

            @Parameter(name = "version", description = "Feature release version e.g. 8,9,10,11,12,13", required = true,
                    schema = Schema(defaultValue = "8"))
            @PathParam("version")
            version: Int?,

            @Parameter(name = "os", description = "Operating System", required = false)
            @QueryParam("os")
            os: OperatingSystem?,

            @Parameter(name = "architecture", description = "Architecture", required = false)
            @QueryParam("architecture")
            arch: Architecture?,

            @Parameter(name = "release_name", description = "Release e.g latest, all, jdk8u172-b00-201807161800", required = false,
                    schema = Schema(defaultValue = "latest"))
            @QueryParam("release_name")
            release_name: String?,

            @Parameter(name = "image_type", description = "Binary Type", required = false)
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


            @Parameter(name = "page_size", description = "Pagination page size", schema = Schema(defaultValue = "20"), required = false)
            @QueryParam("page_size")
            pageSize: Int?,

            @Parameter(name = "page", description = "Pagination page number", schema = Schema(defaultValue = "0"), required = false)
            @QueryParam("page")
            page: Int?

    ): List<Release> {

        if (release_type == null || version == null) {
            throw BadRequestException("Unrecognised type")
        }

        val releases = AdoptDataStore.getAdoptRepos()
                .getFeatureRelease(version)
                .getReleases(ReleaseFilter(release_type, version, release_name, vendor))
                .filterBinaries(BinaryFilter(os, arch, image_type, jvm_impl, heap_size))
                .releases

        val pageSizeNum = (pageSize ?: 20)
        val pageNum = page ?: 0

        val chunked = releases.chunked(pageSizeNum)

        if (pageNum < chunked.size)
            return chunked[pageNum]
        else
            throw NotFoundException("Only ${chunked.size} pages are available")


    }


}
