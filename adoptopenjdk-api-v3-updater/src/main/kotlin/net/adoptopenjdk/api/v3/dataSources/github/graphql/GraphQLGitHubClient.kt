package net.adoptopenjdk.api.v3.dataSources.github.graphql

import io.aexp.nodes.graphql.GraphQLRequestEntity
import io.aexp.nodes.graphql.GraphQLResponseEntity
import io.aexp.nodes.graphql.GraphQLTemplate
import io.aexp.nodes.graphql.Variable
import io.aexp.nodes.graphql.exceptions.GraphQLException
import kotlinx.coroutines.delay
import net.adoptopenjdk.api.v3.dataSources.github.GitHubApi
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.*
import java.io.File
import java.nio.file.Files
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess


class GraphQLGitHubClient : GitHubApi {

    // GH limit 500,000 nodes per request
    // total nodes:
    //  50 releases
    //  50 releases * 100 assets
    // = 50 + 50 * 100
    // = 5050

    private val OWNER = "AdoptOpenJDK"

    private val BASE_URL = "https://api.github.com/graphql"
    private val TOKEN: String = readToken()

    suspend override fun getRepository(repoName: String): Repository {
        val requestEntityBuilder = getReleasesRequest(repoName)

        println("Getting repo $repoName")

        val releases = getAll(requestEntityBuilder,
                { request -> getAllAssets(request) },
                { it.repository!!.releases.pageInfo.hasNextPage },
                { it.repository!!.releases.pageInfo.endCursor },
                clazz = QueryData::class.java)

        return Repository(GHReleases(releases, PageInfo(false, null)))
    }


    suspend private fun getAllAssets(request: QueryData): List<GHRelease> {
        if (request.repository == null) return listOf()

        //nested releases based on how we deserialise githubs data
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
                { asset ->
                    if (asset.assetNode == null) listOf()
                    else asset.assetNode.releaseAssets.assets
                },
                { it.assetNode!!.releaseAssets.pageInfo.hasNextPage },
                { it.assetNode!!.releaseAssets.pageInfo.endCursor },
                release.releaseAssets.pageInfo.endCursor,
                null, ReleaseQueryData::class.java)

        val assets = release.releaseAssets.assets.union(moreAssets)

        return GHRelease(release.id, release.name, release.isPrerelease, release.prerelease, release.publishedAt, GHAssets(assets.toList(), PageInfo(false, null)), release.resourcePath, release.url)
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
    }

    private fun request(query: String): GraphQLRequestEntity.RequestBuilder {
        return GraphQLRequestEntity.Builder()
                .url(BASE_URL)
                .headers(mapOf(
                        "Authorization" to "Bearer $TOKEN"
                ))
                .request(query.trimIndent().replace("\n", ""))

    }

    suspend private fun <E, F : HasRateLimit> getAll(
            requestEntityBuilder: GraphQLRequestEntity.RequestBuilder,

            extract: suspend (F) -> List<E>,
            hasNext: (F) -> Boolean,
            getCursor: (F) -> String?,

            initialCursor: String? = null,
            response: F? = null,
            clazz: Class<F>
    ): List<E> {
        var cursor = initialCursor

        if (response != null) {
            if (!hasNext(response)) {
                return listOf()
            } else {
                cursor = getCursor(response)
            }
        }

        val result: GraphQLResponseEntity<F>? = queryApi(requestEntityBuilder, cursor, clazz)

        if (result == null || repoDoesNotExist(result)) return listOf()

        printRateLimit(result)

        val newData = extract(result.response)

        val more = getAll(requestEntityBuilder, extract, hasNext, getCursor, initialCursor, result.response, clazz)

        return newData.plus(more)
    }

    private fun <F : HasRateLimit> repoDoesNotExist(result: GraphQLResponseEntity<F>): Boolean {
        if (result.errors != null && result.errors.isNotEmpty()) {
            if (result.errors[0].message.contains("Could not resolve to a Repository")) {
                return true
            }

            result.errors.forEach {
                println(it.message)
            }
        }
        return false
    }

    private fun <F : HasRateLimit> printRateLimit(result: GraphQLResponseEntity<F>) {
        val rateLimitData = result.response.rateLimit

        if (rateLimitData.remaining < 1000) {
            println("Remaining data getting low ${rateLimitData.remaining} ${rateLimitData.cost}")
        }
        println("RateLimit ${rateLimitData.remaining} ${rateLimitData.cost}")
    }

    private suspend fun <F : HasRateLimit> queryApi(requestEntityBuilder: GraphQLRequestEntity.RequestBuilder, cursor: String?, clazz: Class<F>): GraphQLResponseEntity<F>? {

        requestEntityBuilder.variables(Variable("cursorPointer", cursor))
        val query = requestEntityBuilder.build()

        var result: GraphQLResponseEntity<F>? = null
        var retryCount = 0
        while (result == null) {
            try {
                result = GraphQLTemplate(Int.MAX_VALUE, Int.MAX_VALUE).query(query, clazz)
            } catch (e: GraphQLException) {
                if (e.status == "403" || e.status == "502") {
                    // Normally get these due to tmp ban due to rate limiting
                    println("Retrying ${e.status} ${retryCount++}")
                    if (retryCount == 20) {
                        printError(query, cursor)
                        return null
                    }
                    delay((TimeUnit.SECONDS.toMillis(2) * retryCount))
                } else {
                    printError(query, cursor)
                    return null
                }
            } catch (e: Exception) {
                printError(query, cursor)
                return null
            }
        }
        return result
    }


    private fun printError(query: GraphQLRequestEntity?, cursor: String?) {
        println("Retry limit hit $query")
        println("Cursor $cursor")
    }


    private fun readToken(): String {
        var token = System.getenv("GITHUB_TOKEN")

        if (token == null) {

            val userHome = System.getProperty("user.home")

            // e.g /home/foo/.adopt_api/token.properties
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