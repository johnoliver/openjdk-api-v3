package net.adoptopenjdk.api.v3.dataSources.github.graphql.clients

import io.aexp.nodes.graphql.GraphQLRequestEntity
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.PageInfo
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.QueryDownloadData
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.GHDownloadsSummary
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.summary.GHRepositoryDownload
import org.slf4j.LoggerFactory


class GraphQLGitHubDownloadCountClient : GraphQLGitHubInterface() {

    companion object {
        @JvmStatic
        private val LOGGER = LoggerFactory.getLogger(this::class.java)
    }

    suspend fun getDowloadSummary(repoName: String): GHRepositoryDownload {
        val requestEntityBuilder = getDownloadCountsRequest(repoName)

        LOGGER.info("Getting repo download count $repoName")

        val releases = getAll(requestEntityBuilder,
                { request -> getDownloadCounts(request) },
                { it.repository!!.pageInfo.hasNextPage },
                { it.repository!!.pageInfo.endCursor },
                clazz = QueryDownloadData::class.java)

        LOGGER.info("Done getting download count $repoName")

        //return GHRepositoryDownload(GHDownloadsSummary(releases, PageInfo(false, null)))
        return GHRepositoryDownload(emptyList(), PageInfo(false, null))
    }

    private fun getDownloadCounts(request: QueryDownloadData): List<GHDownloadsSummary> {
        if (request.repository == null) return listOf()

        //nested releases based on how we deserialise githubs data
        return request.repository?.releases
    }

    private fun getDownloadCountsRequest(repoName: String): GraphQLRequestEntity.RequestBuilder {
        return request("""
                        query(${'$'}cursorPointer:String) { 
                            repository(owner:"$OWNER", name:"$repoName") { 
                                releases(first:50, after:${'$'}cursorPointer) {
                                    nodes {
                                        id,
                                        releaseAssets(first:50) {
                                            nodes {
                                                downloadCount,
                                                name,
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
                    """)
    }

}