package net.adoptopenjdk.api.v3

import net.adoptopenjdk.api.v3.routes.AssetsResource
import net.adoptopenjdk.api.v3.routes.BinaryResource
import net.adoptopenjdk.api.v3.routes.info.AvailableReleasesResource
import net.adoptopenjdk.api.v3.routes.info.PlatformsResource
import net.adoptopenjdk.api.v3.routes.info.VariantsResource
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition
import org.eclipse.microprofile.openapi.annotations.info.Info
import org.eclipse.microprofile.openapi.annotations.servers.Server
import org.jboss.resteasy.plugins.interceptors.CorsFilter
import javax.ws.rs.ApplicationPath
import javax.ws.rs.core.Application


@OpenAPIDefinition(
        servers = [
            Server(url = "https://api.adoptopenjdk.net")
        ],
        info = Info(title = "v3", version = "3.0.0-beta"))
@ApplicationPath("v3")
class V3 : Application() {

    private val resourceClasses: Set<Class<out Any>>
    private val cors: Set<Any>

    init {
        val corsFilter = CorsFilter()
        corsFilter.allowedOrigins.add("*")

        cors = setOf(corsFilter)

        resourceClasses = setOf(
                AssetsResource::class.java,
                BinaryResource::class.java,
                AvailableReleasesResource::class.java,
                PlatformsResource::class.java,
                VariantsResource::class.java)
    }

    override fun getSingletons(): Set<Any> {
        return cors
    }

    override fun getClasses(): Set<Class<*>> {
        return resourceClasses
    }
}
