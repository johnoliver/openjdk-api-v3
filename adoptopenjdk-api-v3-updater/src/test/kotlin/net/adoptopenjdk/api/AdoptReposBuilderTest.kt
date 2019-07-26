package net.adoptopenjdk.api

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.AdoptReposBuilder
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals


class AdoptReposBuilderTest : BaseTest() {
    @Test
    fun reposHasElements() {
        runBlocking {
            val repo = AdoptReposBuilder.build(APIDataStore.variants.versions)

            assert(repo.getFeatureRelease(8)!!.releases.nodes.size > 0)
        }
    }


    @Test
    fun dataIsStoredToDbCorrectly() {
        runBlocking {
            val repo = AdoptReposBuilder.build(APIDataStore.variants.versions)
            ApiPersistenceFactory.get().updateAllRepos(repo);
            val dbData = APIDataStore.loadDataFromDb()

            assertEquals(
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(repo),
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dbData))
        }
    }

}

