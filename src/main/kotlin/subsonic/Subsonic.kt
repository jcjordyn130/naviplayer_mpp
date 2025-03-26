package subsonic

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

const val SUBSONIC_CLIENT = "SubsonicLibrary For Kotlin"
const val SUBSONIC_VERSION = "0.0.1"

class Subsonic(var serverDetails: ServerDetails) {
    var client: HttpClient

    init {
        Napier.base(DebugAntilog())
        Napier.i("Subsonic class initialized using $serverDetails")
        client = HttpClient(CIO) {
            defaultRequest {
                url.takeFrom(serverDetails.url).apply {
                    parameters.append("v", SUBSONIC_VERSION)
                    parameters.append("c", SUBSONIC_CLIENT)
                    parameters.append("f", "json") // Return JSON instead of XML

                    // Authentication
                    parameters.append("u", serverDetails.username)
                    parameters.append("p", serverDetails.password)
                }
            }
        }

        client.plugin(HttpSend).intercept { request ->
            Napier.d("Using URL: ${request.url.buildString()}")

            execute(request)
        }
    }

    fun ping(): Boolean {
        runBlocking {
            val response: HttpResponse = client.get("/rest/ping")
            Napier.d(response.bodyAsText())
        }

        return true
    }
}