package net.adoptopenjdk.api.v3

import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.dataSources.APIDataStore
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.dataSources.persitence.ApiPersistence
import net.adoptopenjdk.api.v3.models.Variants
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask

class V3Updater {
    private var database: ApiPersistence
    val variants: Variants
    var repo: AdoptRepos

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            V3Updater().run(true)
        }
    }

    init {
        val variantData = this.javaClass.getResource("/JSON/variants.json").readText()
        variants = JsonMapper.mapper.readValue(variantData, Variants::class.java)

        database = ApiPersistenceFactory.get()
        try {
            repo = APIDataStore.loadDataFromDb()
        } catch (e: java.lang.Exception) {
            repo = AdoptRepos(emptyList())
            fullUpdate()
        }
    }

    fun run(instantFullUpdate: Boolean) {
        val executor = Executors.newSingleThreadScheduledExecutor()

        executor.scheduleWithFixedDelay(timerTask {
            //Full update on boot and every 24h
            fullUpdate()
        }, if (instantFullUpdate) 0 else 1, 1, TimeUnit.DAYS)

        executor.scheduleWithFixedDelay(timerTask {
            incrementalUpdate()
        }, 0, 1, TimeUnit.MINUTES)
    }

    private fun fullUpdate() {
        //Must catch errors or may kill the scheduler
        try {
            runBlocking {
                repo = AdoptReposBuilder.build(variants.versions)
                database.updateAllRepos(repo)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun incrementalUpdate() {
        //Must catch errors or may kill the scheduler
        try {
            runBlocking {
                repo = AdoptReposBuilder.incrementalUpdate(repo)
                database.updateAllRepos(repo)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}
