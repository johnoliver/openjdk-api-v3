package net.adoptopenjdk.api

import com.fasterxml.jackson.databind.ObjectMapper
import de.flapdoodle.embed.mongo.MongodExecutable
import de.flapdoodle.embed.mongo.MongodStarter
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder
import de.flapdoodle.embed.mongo.config.Net
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.process.runtime.Network
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import net.adoptopenjdk.api.util.GithubStub
import net.adoptopenjdk.api.v3.GitHubClientFactory
import net.adoptopenjdk.api.v3.HttpClientFactory
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHMetaData
import net.adoptopenjdk.api.v3.dataSources.github.graphql.models.GHVersion
import net.adoptopenjdk.api.v3.models.Architecture
import net.adoptopenjdk.api.v3.models.ImageType
import net.adoptopenjdk.api.v3.models.JvmImpl
import net.adoptopenjdk.api.v3.models.OperatingSystem
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.extension.ExtendWith
import java.net.http.HttpClient
import java.net.http.HttpResponse
import java.util.concurrent.CompletableFuture
import kotlin.random.Random

@ExtendWith(MockKExtension::class)
abstract class BaseTest {

    companion object {
        private var mongodExecutable: MongodExecutable? = null
        val objectMapper = ObjectMapper()


        @JvmStatic
        @BeforeAll
        fun startDb() {
            objectMapper.findAndRegisterModules()
            HttpClientFactory.client = mockkHttpClient()

            val starter = MongodStarter.getDefaultInstance()

            val bindIp = "localhost"
            val port = 12345;// Random.nextInt(10000, 16000)
            val mongodConfig = MongodConfigBuilder()
                    .version(Version.V4_0_2)
                    .net(Net(bindIp, port, Network.localhostIsIPv6()))
                    .build()

            System.setProperty("MONGO_DB", "mongodb://localhost:${port}")

            mongodExecutable = starter.prepare(mongodConfig)
            mongodExecutable!!.start()

            GitHubClientFactory.client = GithubStub()

        }

        private fun mockkHttpClient(): HttpClient {

            val client = mockk<HttpClient>()
            val checksumResponse = mockk<HttpResponse<String>>()


            every { client.sendAsync(match({ it.uri().toString().endsWith("sha256.txt") }), any<HttpResponse.BodyHandler<String>>()) } returns CompletableFuture.completedFuture(checksumResponse)
            every { checksumResponse.statusCode() } returns 200
            every { checksumResponse.body() } returns "I am a checksum"

            every { client.sendAsync(match({ it.uri().toString().endsWith("json") }), any<HttpResponse.BodyHandler<String>>()) } answers { arg ->

                val regex = """.*openjdk([0-9]+).*""".toRegex()
                val majorVersion = regex.find(arg.invocation.args.get(0).toString())!!.destructured.component1().toInt();

                val metadataResponse = mockk<HttpResponse<String>>()
                every { metadataResponse.statusCode() } returns 200

                every { metadataResponse.body() } answers {

                    objectMapper.writeValueAsString(GHMetaData(
                            "a",
                            OperatingSystem.linux,
                            Architecture.aarch64,
                            JvmImpl.hotspot,
                            GHVersion(majorVersion, 2, 3, "s", 5, "6", "7", 2, "9"),
                            "scm",
                            "4",
                            ImageType.jdk,
                            "I am a checksum"
                    ))
                }

                CompletableFuture.completedFuture(metadataResponse)
            }

            return client
        }

        @JvmStatic
        @AfterAll
        fun closeMongo() {
            mongodExecutable!!.stop()
        }

    }
}
