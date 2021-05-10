package net.adoptopenjdk.api.v3.dataSources.github.graphql.clients

import com.expediagroup.graphql.client.types.GraphQLClientRequest
import net.adoptopenjdk.api.v3.dataSources.UpdaterHtmlClient
import net.adoptopenjdk.api.v3.dataSources.github.graphql.GraphQLClientApache
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHRelease
import net.adoptopenjdk.api.v3.dataSources.models.GitHubId
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

@Singleton
class GraphQLGitHubReleaseClient @Inject constructor(
    graphQLRequest: GraphQLRequest,
    updaterHtmlClient: UpdaterHtmlClient,
    var client: GraphQLClientApache
) : GraphQLGitHubReleaseRequest(graphQLRequest, updaterHtmlClient) {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    class GetReleaseByIdRequest(releaseId: GitHubId) : GraphQLClientRequest<GHRelease> {
        override val query: String =
            """query { 
                              node(id:"${releaseId.id}") {
                                ... on Release {
                                        id,
                                        url,
                                        name, 
                                        publishedAt,
                                        updatedAt,
                                        isPrerelease,
                                        resourcePath,
                                        releaseAssets(first:50) {
                                            nodes {
                                                downloadCount,
                                                updatedAt,
                                                name,
                                                downloadUrl,
                                                size
                                            },
                                            pageInfo {
                                                hasNextPage,
                                                endCursor
                                            }
                                        }
                                    }
                            }
                            rateLimit {
                                cost,
                                remaining
                            }
                        }
                    """

        override fun responseType(): KClass<GHRelease> = GHRelease::class
    }

    suspend fun getReleaseById(id: GitHubId): GHRelease {
        LOGGER.info("Getting id $id")
        val response = client
            .execute(GetReleaseByIdRequest(id))

        if (response.errors != null && response.errors!!.isNotEmpty()) {
            // TODO handle errors
            throw RuntimeException("Errors")
        } else {
            // TODO handle pagination
            if (response.data != null) {
                return response.data!!
            } else {
                throw RuntimeException("Null data")
            }
        }
    }
}
