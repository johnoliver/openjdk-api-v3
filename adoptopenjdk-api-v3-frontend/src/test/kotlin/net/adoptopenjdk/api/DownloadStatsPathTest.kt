package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.response.ValidatableResponse
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.JsonMapper
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.dataSources.persitence.mongo.MongoApiPersistence.Companion.DOCKER_STATS_DB
import net.adoptopenjdk.api.v3.dataSources.persitence.mongo.MongoApiPersistence.Companion.GITHUB_STATS_DB
import net.adoptopenjdk.api.v3.dataSources.persitence.mongo.MongoClientFactory
import net.adoptopenjdk.api.v3.models.DockerDownloadStatsDbEntry
import net.adoptopenjdk.api.v3.models.DownloadStats
import net.adoptopenjdk.api.v3.models.GithubDownloadStatsDbEntry
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.test.assertFails


@QuarkusTest
class DownloadStatsPathTest : BaseTest() {

    companion object {
        @JvmStatic
        @BeforeAll
        fun before() {
            populateDb()
        }
    }

    @Test
    fun whenEmptyResultIsSane() {
        runBlocking {
            MongoClientFactory.get().database.dropCollection(GITHUB_STATS_DB)
            MongoClientFactory.get().database.dropCollection(DOCKER_STATS_DB)
            RestAssured.given()
                    .`when`()
                    .get("/v3/stats/downloads/total")
                    .then()
                    .body(object : TypeSafeMatcher<String>() {

                        override fun describeTo(description: Description?) {
                            description!!.appendText("json")
                        }

                        override fun matchesSafely(p0: String?): Boolean {
                            val stats = JsonMapper.mapper.readValue(p0, DownloadStats::class.java)
                            return stats.total_downloads.total == 0L
                        }
                    })
        }
    }

    @Test
    fun totalDownloadReturnsSaneData() {
        runBlocking {
            mockStats()
            RestAssured.given()
                    .`when`()
                    .get("/v3/stats/downloads/total")
                    .then()
                    .body(object : TypeSafeMatcher<String>() {

                        override fun describeTo(description: Description?) {
                            description!!.appendText("json")
                        }

                        override fun matchesSafely(p0: String?): Boolean {
                            val stats = JsonMapper.mapper.readValue(p0, DownloadStats::class.java)
                            return stats.total_downloads.total == 170L &&
                                    stats.total_downloads.docker_pulls == 100L &&
                                    stats.total_downloads.github_downloads == 70L &&
                                    stats.github_downloads[8] == 30L &&
                                    stats.github_downloads[9] == 40L &&
                                    stats.docker_pulls["a-repo-name"] == 60L &&
                                    stats.docker_pulls["b-repo-name"] == 40L
                        }
                    })
        }
    }


    @Test
    fun totalVersionReturnsSaneData() {
        runBlocking {
            mockStats()
            RestAssured.given()
                    .`when`()
                    .get("/v3/stats/downloads/total/8")
                    .then()
                    .body(object : TypeSafeMatcher<String>() {

                        override fun describeTo(description: Description?) {
                            description!!.appendText("json")
                        }

                        override fun matchesSafely(p0: String?): Boolean {
                            val stats = JsonMapper.mapper.readValue(p0, Map::class.java)
                            return stats.isNotEmpty() && !stats.containsValue(0L)
                        }
                    })
        }
    }

    @Test
    fun totalTagReturnsSaneData() {
        runBlocking {
            mockStats()
            RestAssured.given()
                    .`when`()
                    .get("/v3/stats/downloads/total/8/jdk8u232-b09")
                    .then()
                    .body(object : TypeSafeMatcher<String>() {

                        override fun describeTo(description: Description?) {
                            description!!.appendText("json")
                        }

                        override fun matchesSafely(p0: String?): Boolean {
                            val stats = JsonMapper.mapper.readValue(p0, Map::class.java)
                            return stats.isNotEmpty() && !stats.containsValue(0L)
                        }
                    })
        }
    }

    @Test
    fun trackingReturnsSaneData() {
        runBlocking {
            mockStats()
            RestAssured.given()
                    .`when`()
                    .get("/v3/stats/downloads/tracking")
                    .then()
                    .body(object : TypeSafeMatcher<String>() {

                        override fun describeTo(description: Description?) {
                            description!!.appendText("json")
                        }

                        override fun matchesSafely(p0: String?): Boolean {
                            val stats = JsonMapper.mapper.readValue(p0, List::class.java)
                            return stats.isNotEmpty() &&
                                    (stats[1] as Map<String, *>).get("total") == 170 &&
                                    (stats[1] as Map<String, *>).get("daily") == 30
                        }
                    })
        }
    }


    @Test
    fun dateRangeFilterWithStartAndEndIsCorrect() {
        requestStats(
                LocalDate.now().minusDays(6).format(DateTimeFormatter.ISO_DATE),
                LocalDate.now().minusDays(2).format(DateTimeFormatter.ISO_DATE),
                null,
                { stats ->
                    stats.size == 1 &&
                            (stats[0] as Map<String, *>).get("total") == 50 &&
                            (stats[0] as Map<String, *>).get("daily") == 4
                })
    }

    @Test
    fun dateRangeFilterWithEndIsCorrect() {
        requestStats(
                null,
                LocalDate.now().format(DateTimeFormatter.ISO_DATE),
                null,
                { stats ->
                    stats.size == 2 &&
                            (stats[0] as Map<String, *>).get("total") == 50 &&
                            (stats[0] as Map<String, *>).get("daily") == 4
                })
    }

    @Test
    fun dateRangeFilterWithEndAndDayIsCorrect() {
        requestStats(
                null,
                LocalDate.now().format(DateTimeFormatter.ISO_DATE),
                1,
                { stats ->
                    stats.size == 1 &&
                            (stats[0] as Map<String, *>).get("total") == 170 &&
                            (stats[0] as Map<String, *>).get("daily") == 30
                })
    }

    @Test
    fun throwsOnABadDate() {
        assertFails({
            requestStats(
                    null,
                    "foo",
                    1,
                    { stats ->
                        stats.size == 1 &&
                                (stats[0] as Map<String, *>).get("total") == 170 &&
                                (stats[0] as Map<String, *>).get("daily") == 30
                    })
        })
    }


    private fun requestStats(from: String?, to: String?, days: Int?, check: (List<*>) -> Boolean): ValidatableResponse? {
        return runBlocking {
            mockStats()

            var url = "/v3/stats/downloads/tracking?"
            if (from != null) {
                url += "from=$from&"
            }
            if (to != null) {
                url += "to=$to&"
            }
            if (days != null) {
                url += "days=$days&"
            }

            RestAssured.given()
                    .`when`()
                    .get(url)
                    .then()
                    .body(object : TypeSafeMatcher<String>() {

                        override fun describeTo(description: Description?) {
                            description!!.appendText("json")
                        }

                        override fun matchesSafely(p0: String?): Boolean {
                            return check(JsonMapper.mapper.readValue(p0, List::class.java))
                        }
                    })
        }
    }

    private suspend fun mockStats() {
        val persistance = ApiPersistenceFactory.get()

        persistance.addDockerDownloadStatsEntries(
                createDockerStatsWithRepoName()
        )

        persistance.addGithubDownloadStatsEntries(
                createGithubData()
        )
    }

    private fun createGithubData(): List<GithubDownloadStatsDbEntry> {
        return listOf(
                GithubDownloadStatsDbEntry(
                        TestTime.now().minusDays(10),
                        10,
                        8
                ),
                GithubDownloadStatsDbEntry(
                        TestTime.now().minusDays(5),
                        20,
                        9
                ),
                GithubDownloadStatsDbEntry(
                        TestTime.now().minusDays(1),
                        40,
                        9
                ),
                GithubDownloadStatsDbEntry(
                        TestTime.now().minusDays(1).minusMinutes(1),
                        25,
                        8
                ),
                GithubDownloadStatsDbEntry(
                        TestTime.now().minusDays(1),
                        30,
                        8
                )
        )
    }

    private fun createDockerStatsWithRepoName(): List<DockerDownloadStatsDbEntry> {
        return listOf(
                DockerDownloadStatsDbEntry(
                        TestTime.now().minusDays(10),
                        20,
                        "a-repo-name"
                ),
                DockerDownloadStatsDbEntry(
                        TestTime.now().minusDays(5),
                        30,
                        "b-repo-name"
                ),
                DockerDownloadStatsDbEntry(
                        TestTime.now().minusDays(1),
                        40,
                        "b-repo-name"
                ),
                DockerDownloadStatsDbEntry(
                        TestTime.now().minusDays(1).minusMinutes(1),
                        50,
                        "a-repo-name"
                ),
                DockerDownloadStatsDbEntry(
                        TestTime.now().minusDays(1),
                        60,
                        "a-repo-name"
                )
        )
    }


}

