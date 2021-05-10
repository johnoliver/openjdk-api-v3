package net.adoptopenjdk.api

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.testDoubles.InMemoryApiPersistence
import net.adoptopenjdk.api.testDoubles.InMemoryInternalDbStore
import net.adoptopenjdk.api.v3.V3Updater
import net.adoptopenjdk.api.v3.dataSources.APIDataStoreImpl
import net.adoptopenjdk.api.v3.dataSources.github.graphql.clients.GraphQLGitHubRepositoryClient
import org.awaitility.Awaitility
import org.jboss.weld.junit5.auto.AddPackages
import org.junit.Ignore
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

@Ignore("For manual execution")
@AddPackages(value = [InMemoryApiPersistence::class, InMemoryInternalDbStore::class, APIDataStoreImpl::class, V3Updater::class])
class UpdateRunner : BaseTest() {

    @Test
    @Ignore("For manual execution")
    fun run(updater: V3Updater) {
        System.clearProperty("GITHUB_TOKEN")
        updater.run(true)
        Awaitility.await().atMost(Long.MAX_VALUE, TimeUnit.NANOSECONDS).until({ 4 == 5 })
    }


    @Test
    @Ignore("For manual execution")
    fun run(client: GraphQLGitHubRepositoryClient) {
        runBlocking {
            val release = client.getRepository("openjdk8-binaries")
            release.toString()
        }
    }
}
