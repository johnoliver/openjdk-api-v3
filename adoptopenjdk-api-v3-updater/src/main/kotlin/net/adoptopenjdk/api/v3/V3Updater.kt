package net.adoptopenjdk.api.v3

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.dataSources.ApiPersistenceFactory
import net.adoptopenjdk.api.v3.models.Variants
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.Application
import kotlin.concurrent.timerTask

class V3Updater : Application() {
    val variants: Variants

    init {
        val objectMapper = ObjectMapper()
        objectMapper.findAndRegisterModules()

        val variantData = this.javaClass.getResource("/JSON/variants.json").readText()
        variants = objectMapper.readValue(variantData, Variants::class.java)

        Executors
                .newSingleThreadScheduledExecutor()
                .scheduleWithFixedDelay(timerTask {
                    periodicUpdate()
                }, 0, 30, TimeUnit.MINUTES)

    }

    private fun periodicUpdate() {
        //Must catch errors or may kill the scheduler
        try {
            runBlocking {
                val repo = AdoptReposBuilder.build(variants.versions)
                ApiPersistenceFactory.get().updateAllRepos(repo)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}
