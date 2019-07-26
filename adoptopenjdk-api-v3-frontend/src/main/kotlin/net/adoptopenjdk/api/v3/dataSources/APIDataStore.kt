package net.adoptopenjdk.api.v3.dataSources

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.annotations.VisibleForTesting
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.models.Platforms
import net.adoptopenjdk.api.v3.models.Variants
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask

object APIDataStore {
    private lateinit var binaryRepos: AdoptRepos

    val platforms: Platforms
    val variants: Variants
    val objectMapper: ObjectMapper

    init {

        objectMapper = ObjectMapper()
        objectMapper.findAndRegisterModules()

        val platformData = this.javaClass.getResource("/JSON/platforms.json").readText()
        platforms = objectMapper.readValue(platformData, Platforms::class.java)
        val variantData = this.javaClass.getResource("/JSON/variants.json").readText()
        variants = objectMapper.readValue(variantData, Variants::class.java)

        try {
            binaryRepos = loadDataFromDb()
        } catch (e: Exception) {
            println("Failed to read cache")
        }

        Executors
                .newSingleThreadScheduledExecutor()
                .scheduleWithFixedDelay(timerTask {
                    periodicUpdate()
                }, 0, 15, TimeUnit.MINUTES)
    }

    @VisibleForTesting
    fun loadDataFromDb(): AdoptRepos {
        return runBlocking {
            val data = variants
                    .versions
                    .map { version ->
                        ApiPersistenceFactory.get().readReleaseData(version)
                    }
                    .filter { it.releases.nodes.isNotEmpty() }
                    .toList()

            AdoptRepos(data)
        }
    }

    fun getAdoptRepos(): AdoptRepos {
        return binaryRepos
    }

    private fun periodicUpdate() {
        //Must catch errors or may kill the scheduler
        try {
            binaryRepos = loadDataFromDb()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}