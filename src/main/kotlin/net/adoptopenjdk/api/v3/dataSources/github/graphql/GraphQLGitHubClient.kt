package net.adoptopenjdk.api.v3.dataSources.github.graphql

import io.aexp.nodes.graphql.GraphQLRequestEntity
import io.aexp.nodes.graphql.GraphQLTemplate
import io.aexp.nodes.graphql.Variable
import io.aexp.nodes.graphql.exceptions.GraphQLException
import kotlinx.coroutines.delay
import net.adoptopenjdk.api.v3.dataSources.github.GitHubApi
import java.io.File
import java.nio.file.Files
import java.util.*
import kotlin.collections.ArrayList
import kotlin.system.exitProcess


class GraphQLGitHubClient : GitHubApi {
    private val OWNER = "AdoptOpenJDK"

    private val baseUrl = "https://api.github.com/graphql"
    private val token: String = readToken()

    suspend override fun getRepository(repoName: String): Repository {
        val requestEntityBuilder = getReleasesRequest(repoName)

        println("Getting repo $repoName")

        val releases = getAll<GHRelease, QueryData>(requestEntityBuilder,
                { request -> getAllAssets(request) },
                { it.repository!!.releases.pageInfo.hasNextPage },
                { it.repository!!.releases.pageInfo.endCursor })

        return Repository(GHReleases(releases, PageInfo(false, null)))
    }


    suspend private fun getAllAssets(request: QueryData): List<GHRelease>? {
        if (request.repository == null) return null

        return request.repository.releases.releases
                .map { release ->
                    if (release.releaseAssets.pageInfo.hasNextPage) {
                        getNextPage(release)
                    } else {
                        release
                    }
                }
    }

    private suspend fun getNextPage(release: GHRelease): GHRelease {

        val getMore = getMoreReleasesQuery(release.id)
        println("Getting release assets ${release.id}")
        val moreAssets = getAll<GHAsset, ReleaseQueryData>(getMore,
                { ass ->
                    if (ass.assetNode == null) null
                    else ass.assetNode.releaseAssets.assets
                },
                { it.assetNode!!.releaseAssets.pageInfo.hasNextPage },
                { it.assetNode!!.releaseAssets.pageInfo.endCursor },
                release.releaseAssets.pageInfo.endCursor)

        val assets = mutableListOf<GHAsset>()
        assets.addAll(release.releaseAssets.assets)
        assets.addAll(moreAssets)

        return GHRelease(release.id, release.name, release.isPrerelease, release.prerelease, release.publishedAt, GHAssets(assets, PageInfo(false, null)), release.resourcePath, release.url)
    }

    private fun getMoreReleasesQuery(releaseId: String): GraphQLRequestEntity.RequestBuilder {
        return request("""query(${'$'}cursorPointer:String) { 
                              node(id:"$releaseId") {
                                ... on Release {
                                    releaseAssets(first:50, after:${'$'}cursorPointer) {
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
                    """)
    }

    private fun getReleasesRequest(repoName: String): GraphQLRequestEntity.RequestBuilder {

        return request("""
                        query(${'$'}cursorPointer:String) { 
                            repository(owner:"$OWNER", name:"$repoName") { 
                                releases(first:50, after:${'$'}cursorPointer) {
                                    nodes {
                                        id,
                                        url,
                                        name, 
                                        publishedAt,
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
                    """)
        // GH limit 500,000 nodes per request
        // total nodes:
        //  50 releases
        //  50 releases * 100 assets
        // = 50 + 50 * 100
        // = 5050
    }

    private fun request(query: String): GraphQLRequestEntity.RequestBuilder {
        return GraphQLRequestEntity.Builder()
                .url(baseUrl)
                .headers(mapOf(
                        "Authorization" to "Bearer $token"
                ))
                .request(query.trimIndent().replace("\n", ""))

    }


    suspend private inline fun <reified E, reified F : HasRateLimit> getAll(
            requestEntityBuilder: GraphQLRequestEntity.RequestBuilder,
            extract: (F) -> List<E>?,
            hasNext: (F) -> Boolean,
            getCursor: (F) -> String?,
            initialCursor: String? = null
    ): List<E> {
        var hasMore = true

        var cursor = initialCursor
        val data = ArrayList<E>()
        var retryCount = 0
        while (hasMore) {
            requestEntityBuilder.variables(Variable("cursorPointer", cursor))
            val query = requestEntityBuilder.build()
            try {
                val result = GraphQLTemplate(Int.MAX_VALUE, Int.MAX_VALUE).query(query, F::class.java)
                retryCount = 0
                if (result == null) {
                    println("Null result")
                }

                if (result.errors != null && result.errors.isNotEmpty()) {
                    if (result.errors[0].message.contains("Could not resolve to a Repository")) {
                        return data
                    }

                    result.errors.forEach {
                        println(it.message)
                    }
                }

                val rateLimitData = result.response.rateLimit

                if (rateLimitData.remaining < 1000) {
                    println("Remaining data getting low ${rateLimitData.remaining} ${rateLimitData.cost}")
                }
                println("RateLimit ${rateLimitData.remaining} ${rateLimitData.cost}")

                val newData = extract(result.response)

                if (newData == null) {
                    return data
                }

                data.addAll(newData)
                hasMore = hasNext(result.response)
                if (hasMore) {
                    cursor = getCursor(result.response)
                } else {
                    return data
                }

            } catch (e: GraphQLException) {
                if (e.status == "403" || e.status == "502") {
                    // Normally get these due to tmp ban due to rate limiting
                    println("Retrying ${e.status} ${retryCount++}")
                    if (retryCount == 0) {
                        printError(query, cursor)
                        return data
                    }
                    delay((2000 * retryCount).toLong())
                } else {
                    printError(query, cursor)
                    return data
                }
            } catch (e: Exception) {
                printError(query, cursor)
                return data
            }
        }

        return data
    }

    private fun printError(query: GraphQLRequestEntity?, cursor: String?) {
        println("Retry limit hit $query")
        println("Cursor $cursor")
    }


    private fun readToken(): String {
        var token = System.getenv("GITHUB_TOKEN")

        if (token == null) {

            val userHome = System.getProperty("user.home")

            // i.e /home/foo/.adopt_api/token.properties
            val propertiesFile = File(userHome + File.separator + ".adopt_api" + File.separator + "token.properties")

            if (propertiesFile.exists()) {

                val properties = Properties()
                properties.load(Files.newInputStream(propertiesFile.toPath()))
                token = properties.getProperty("token")
            }

        }
        if (token == null) {
            System.err.println("Could not find GITHUB_TOKEN")
            exitProcess(1)
        }
        return token
    }
}