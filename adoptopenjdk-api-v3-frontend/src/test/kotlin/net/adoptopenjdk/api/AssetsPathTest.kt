package net.adoptopenjdk.api

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.AdoptReposBuilder
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.models.*
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.util.stream.Stream


@QuarkusTest
abstract class AssetsPathTest : BaseTest() {

    companion object {
        @JvmStatic
        @BeforeAll
        fun populateDb() {
            runBlocking {
                val repo = AdoptReposBuilder.build(APIDataStore.variants.versions)
                //Reset connection
                ApiPersistenceFactory.set(null)
                ApiPersistenceFactory.get().updateAllRepos(repo)
                APIDataStore.loadDataFromDb()
            }
        }
    }

    abstract fun <T> runFilterTest(filterParamName: String, values: Array<T>): Stream<DynamicTest>


    @TestFactory
    fun filtersOs(): Stream<DynamicTest> {
        return runFilterTest("os", OperatingSystem.values())
    }

    @TestFactory
    fun filtersArchitecture(): Stream<DynamicTest> {
        return runFilterTest("architecture", Architecture.values())
    }

    @TestFactory
    fun filtersImageType(): Stream<DynamicTest> {
        return runFilterTest("image_type", ImageType.values())
    }

    @TestFactory
    fun filtersJvmImpl(): Stream<DynamicTest> {
        return runFilterTest("jvm_impl", JvmImpl.values())
    }

    @TestFactory
    fun filtersHeapSize(): Stream<DynamicTest> {
        return runFilterTest("heap_size", HeapSize.values())
    }


    protected fun <T> createTest(values: Array<T>, path: String, filterParamName: String, exclude: (element: T) -> Boolean = { a -> false }): List<DynamicTest> {
        return values
                .filter { !exclude(it) }
                .map { value ->
                    val path2 = "${path}?${filterParamName}=${value.toString().toLowerCase()}"
                    DynamicTest.dynamicTest(path2) {
                        RestAssured.given()
                                .`when`()
                                .get(path2)
                                .then()
                                .statusCode(200)
                                .body("binaries.${filterParamName}.flatten()", Matchers.everyItem(Matchers.`is`(value.toString())))
                                .body("binaries.${filterParamName}.flatten().size()", Matchers.greaterThan(0))
                    }
                }
    }

}

