package net.adoptopenjdk.api.v3.dataSources.persitence.mongo

import com.mongodb.client.model.CreateCollectionOptions
import com.mongodb.client.model.InsertManyOptions
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.models.FeatureRelease
import net.adoptopenjdk.api.v3.dataSources.models.Releases
import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import net.adoptopenjdk.api.v3.models.Release
import org.bson.Document
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo


class MongoApiPersistence : ApiPersistence {

    val releasesCollection: CoroutineCollection<Release>


    init {
        val client = KMongo.createClient(System.getProperty("MONGO_DB", "mongodb://localhost:12345")).coroutine
        val database = client.getDatabase("api-data")
        runBlocking {
            database.createCollection("release", CreateCollectionOptions())
        }
        releasesCollection = database.getCollection()
    }

    override suspend fun updateAllRepos(repos: AdoptRepos) {
        repos
                .repos
                .forEach { repo ->
                    writeReleases(repo.key, repo.value)
                }
    }

    override suspend fun writeReleases(featureVersion: Int, value: FeatureRelease) {
        releasesCollection.deleteMany(majorVersionMatcher(featureVersion))
        releasesCollection.insertMany(value.releases.nodes, InsertManyOptions())
    }

    override suspend fun readReleaseData(featureVersion: Int): FeatureRelease {
        val releases = releasesCollection
                .find(majorVersionMatcher(featureVersion))
                .toList()

        return FeatureRelease(featureVersion, Releases(releases))

    }

    private fun majorVersionMatcher(featureVersion: Int) = Document("version_data.major", featureVersion)
}