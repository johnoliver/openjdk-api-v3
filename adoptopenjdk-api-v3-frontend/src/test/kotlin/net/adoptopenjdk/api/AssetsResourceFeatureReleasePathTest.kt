package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import junit.framework.Assert.assertTrue
import net.adoptopenjdk.api.v3.JsonMapper
import net.adoptopenjdk.api.v3.dataSources.SortOrder
import net.adoptopenjdk.api.v3.models.Architecture
import net.adoptopenjdk.api.v3.models.ImageType
import net.adoptopenjdk.api.v3.models.OperatingSystem
import net.adoptopenjdk.api.v3.models.ReleaseType
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.stream.Stream


@QuarkusTest
class AssetsResourceFeatureReleasePathTest : AssetsPathTest() {

    fun getPath() = "/v3/assets/feature_releases"

    @TestFactory
    fun noFilter(): Stream<DynamicTest> {
        return (8..12)
                .flatMap { version ->
                    ReleaseType.values()
                            .map { "/v3/assets/feature_releases/${version}/${it}" }
                            .map {
                                DynamicTest.dynamicTest(it) {
                                    RestAssured.given()
                                            .`when`()
                                            .get(it)
                                            .then()
                                            .statusCode(200)
                                }
                            }
                }
                .stream()
    }

    @Test
    fun badReleaseType() {
        RestAssured.given()
                .`when`()
                .get("${getPath()}/8/foo")
                .then()
                .statusCode(404)
    }

    @Test
    fun badVersion() {
        RestAssured.given()
                .`when`()
                .get("2/${getPath()}")
                .then()
                .statusCode(404)
    }

    @Test
    fun sortOrderASCIsHonoured() {

        val times = getTimestamps(SortOrder.ASC)

        times
                .fold(LocalDateTime.MIN, { previous, updatedAt ->
                    assertTrue("${previous} is after ${updatedAt}", updatedAt.isAfter(previous) || updatedAt.equals(previous))
                    updatedAt
                })

    }

    @Test
    fun sortOrderDESIsHonoured() {

        val times = getTimestamps(SortOrder.DESC)

        times
                .fold(LocalDateTime.MAX, { previous, updatedAt ->
                    assertTrue("${previous} is before ${updatedAt}", updatedAt.isBefore(previous) || updatedAt.equals(previous))
                    updatedAt
                })

    }

    private fun getTimestamps(sortOrder: SortOrder): List<LocalDateTime> {
        val body = RestAssured.given()
                .`when`()
                .get("${getPath()}/8/ga?sort_order=${sortOrder.name}")
                .body

        val releasesStr = body.prettyPrint()

        val releases = JsonMapper.mapper.readValue(releasesStr, List::class.java) as List<Map<String, String>>
        val times = releases
                .map { it.get("timestamp") }
                .map { Instant.from(DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.of("UTC")).parse(it)).atZone(ZoneId.of("UTC")).toLocalDateTime() }
        return times
    }


    override fun <T> runFilterTest(filterParamName: String, values: Array<T>): Stream<DynamicTest> {
        return ReleaseType.values()
                .flatMap { releaseType ->
                    //test the ltses and 1 non-lts
                    listOf(8, 11, 12)
                            .flatMap { version ->
                                createTest(values, "${getPath()}/${version}/${releaseType}", filterParamName, { element ->
                                    getExclusions(version, element)
                                })
                            }
                }
                .stream()
    }

    private fun <T> getExclusions(version: Int, element: T): Boolean {
        return version == 11 && element == OperatingSystem.solaris ||
                version == 12 && element == OperatingSystem.solaris ||
                version == 8 && element == Architecture.arm ||
                version != 8 && element == Architecture.sparcv9 ||
                version == 8 && element == ImageType.testimage ||
                version == 11 && element == ImageType.testimage ||
                version == 12 && element == ImageType.testimage
    }
}

