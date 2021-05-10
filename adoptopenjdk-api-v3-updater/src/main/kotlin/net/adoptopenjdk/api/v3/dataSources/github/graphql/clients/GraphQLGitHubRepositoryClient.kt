package net.adoptopenjdk.api.v3.dataSources.github.graphql.clients

/* ktlint-disable no-wildcard-imports */
/* ktlint-enable no-wildcard-imports */
import com.expediagroup.graphql.client.types.GraphQLClientRequest
import net.adoptopenjdk.api.v3.dataSources.UpdaterHtmlClient
import net.adoptopenjdk.api.v3.dataSources.github.graphql.GraphQLClientApache
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHRepository
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.QueryData
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

@Singleton
class GraphQLGitHubRepositoryClient @Inject constructor(
    graphQLRequest: GraphQLRequest,
    updaterHtmlClient: UpdaterHtmlClient,
    var client: GraphQLClientApache
) : GraphQLGitHubReleaseRequest(graphQLRequest, updaterHtmlClient) {
    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    class GetRepositoryByName(repoName: String) : GraphQLClientRequest<QueryData> {
        override val query: String =
            """
                        query(${'$'}cursorPointer:String) { 
                            repository(owner:"${GraphQLGitHubInterface.OWNER}", name:"$repoName") { 
                                releases(first:50, after:${'$'}cursorPointer, orderBy: {field: CREATED_AT, direction: DESC}) {
                                    nodes {
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
                                    },
                                    pageInfo {
                                        hasNextPage,
                                        endCursor
                                    }
                                }
                            }
                            rateLimit {
                                cost,
                                remaining
                            }
                        }
                    """

        override fun responseType(): KClass<QueryData> = QueryData::class
    }

    suspend fun getRepository(repoName: String): GHRepository {
        LOGGER.info("Getting repo $repoName")
        val response = client
            .execute(GetRepositoryByName(repoName))

        if (response.errors != null && response.errors!!.isNotEmpty()) {
            // TODO handle errors
            throw RuntimeException("Errors")
        } else {
            // TODO handle pagination
            if (response.data != null && response.data!!.repository != null) {
                return response.data!!.repository!!
            } else {
                throw RuntimeException("Null data")
            }
        }
    }
}
