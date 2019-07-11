package net.adoptopenjdk.api.v3.dataSources

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import net.adoptopenjdk.api.v3.dataSources.models.AdoptRepos
import net.adoptopenjdk.api.v3.models.Platforms
import net.adoptopenjdk.api.v3.models.Variants
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask

object AdoptDataStore {
    private var adoptRepos: Deferred<AdoptRepos>

    val platforms: Platforms
    val variants: Variants

    init {

        val platformData = this.javaClass.getResource("/JSON/platforms.json").readText()
        platforms = ObjectMapper().readValue(platformData, Platforms::class.java)
        val variantData = this.javaClass.getResource("/JSON/variants.json").readText()
        variants = ObjectMapper().readValue(variantData, Variants::class.java)

        adoptRepos = loadData()
        adoptRepos.start()

        Timer().schedule(timerTask {
            periodicUpdate()
        }, TimeUnit.MINUTES.toMillis(15), TimeUnit.MINUTES.toMillis(15))
    }

    fun getAdoptRepos(): AdoptRepos {
        return runBlocking { adoptRepos.await() }
    }

    private fun periodicUpdate() {
        //Must catch errors or may kill the scheduler
        try {
            runBlocking {
                val newAdoptRepos = loadData()
                newAdoptRepos.await()
                adoptRepos = newAdoptRepos
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadData(): Deferred<AdoptRepos> {
        return GlobalScope.async {
            AdoptRepos.Builder.build(variants.versions)
        }
    }
}