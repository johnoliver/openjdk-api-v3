package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import net.adoptopenjdk.api.util.GithubStub
import net.adoptopenjdk.api.v3.dataSources.github.GitHubClient
import net.adoptopenjdk.api.v3.models.*
import org.hamcrest.Matcher
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.util.stream.Stream


@QuarkusTest
open class AssetsResourceTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            GitHubClient.client = GithubStub()
        }
    }


    @TestFactory
    fun filtersOs(): Stream<DynamicTest> {
        return runFilterTest("os", OperatingSystem.values().map { it.name })
    }

    @TestFactory
    fun filtersArchitecture(): Stream<DynamicTest> {
        return runFilterTest("architecture", Architecture.values().map { it.name })
    }

    @TestFactory
    fun filtersImageType(): Stream<DynamicTest> {
        return runFilterTest("image_type", ImageType.values().map { it.name })
    }

    @TestFactory
    fun filtersJvmImpl(): Stream<DynamicTest> {
        return runFilterTest("jvm_impl", JvmImpl.values().map { it.name })
    }

    @TestFactory
    fun filtersHeapSize(): Stream<DynamicTest> {
        return runFilterTest("heap_size", HeapSize.values().map { it.name })
    }

    fun runFilterTest(filterParamName: String, values: List<String>): Stream<DynamicTest> {
        return values
                .map { it.toLowerCase() }
                .map {
                    DynamicTest.dynamicTest("/v3/assets/ga/8?${filterParamName}=${it}") {
                        given()
                                .`when`()
                                .get("/v3/assets/ga/8?${filterParamName}=${it}")
                                .then()
                                .statusCode(200)
                                .body("binaries.${filterParamName}.flatten()", everyItem(`is`(it)))
                                .body("binaries.${filterParamName}.flatten().size()", greaterThan(0))
                    }
                }
                .stream()
    }

    @TestFactory
    fun noFilter(): Stream<DynamicTest> {
        return (8..12)
                .flatMap { version ->
                    ReleaseType.values()
                            .map { "/v3/assets/${it}/${version}" }
                            .map {
                                DynamicTest.dynamicTest(it) {
                                    given()
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
    fun badVersion() {
        given()
                .`when`()
                .get("/v3/assets/ga/2")
                .then()
                .statusCode(404)
    }

    @Test
    fun badReleaseType() {
        given()
                .`when`()
                .get("/v3/assets/foo/8")
                .then()
                .statusCode(404)
    }

}

