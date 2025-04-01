package subsonic

import io.github.aakira.napier.Antilog
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.serialization.json.Json
import org.kotlincrypto.hash.md.MD5
import root
import subsonic.caches.AbstractCache
import java.security.cert.Extension
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// Default client identifier and version
const val SUBSONIC_CLIENT = "SubsonicLibrary For Kotlin"
const val SUBSONIC_VERSION = "0.0.1"

// Unauthenticated endpoints, as of right now this is just getOpenSubsonicExtensions
private val UNPROTECTED_ENDPOINTS = arrayOf("getOpenSubsonicExtensions")

class Subsonic(var serverDetails: ServerDetails,
               val cacheImpl: AbstractCache,
               private val clientIden: String = SUBSONIC_CLIENT,
               private val versionIden: String = SUBSONIC_VERSION) {
    private var client: HttpClient

    init {
        System.setProperty("kotlinx.coroutines.debug", "on")
        Napier.base(DebugAntilog())
        Napier.i("Subsonic class initialized using $serverDetails")

        // Check for insecure authentication
        // We no longer log authentication details by default, however, intermediate servers can.
        when (serverDetails.authMethod) {
            AuthMethod.APIKEY, AuthMethod.TOKEN -> {
                Napier.w("Not using token or apikey authentication... this is NOT recommended and is insecure, especially on non-HTTPS servers!")
            }

            // Exhaustive branches are required for when's
            else -> {}
        }

        client = HttpClient(CIO) {
            // Add client identity and response format
            // These are in defaultRequest as they never change
            defaultRequest {
                url.takeFrom(serverDetails.url).apply {
                    parameters.append("v", versionIden)
                    parameters.append("c", clientIden)
                    parameters.append("f", "json") // Return JSON instead of XML
                }
            }

            //HttpResponseValidator {
            //}
        }

        // Intercept all requests to log the URL and to add authentication details
        // to protected endpoints
        client.plugin(HttpSend).intercept { request ->
            // This returns true only if the predicate does NOT equal true
            // so pathSegment NOT in UNPROTECTED_ENDPOINTS
            val protected = UNPROTECTED_ENDPOINTS.none { request.url.pathSegments.contains(it) }
            if (protected) {
                request.url.parameters.appendAll(generateAuthDetails(serverDetails))
            }

            // Redact logging if requested
            val loggingUrl = if (serverDetails.loggingRedacted) {
                // takeFrom required as just setting redactedUrl = request.url
                // results in a reference and NOT a copy
                val redactedUrl = URLBuilder().takeFrom(request.url)

                // Remove username, password, salt, token, and apikey for secure logging
                arrayOf("u", "p", "s", "t", "apikey").forEach {
                    if (redactedUrl.parameters.contains(it)) {
                        Napier.d("Removing URL parameter $it for logging redaction!")
                        //redactedUrl.parameters[it] = "[redacted]"
                    }
                }
                redactedUrl
            } else {
                request.url
            }

            Napier.d("Using URL: ${loggingUrl.buildString()}")
            execute(request)
        }
    }

    @OptIn(ExperimentalStdlibApi::class, ExperimentalUuidApi::class)
    private suspend fun generateAuthDetails(serverDetails: ServerDetails): ParametersBuilder {
        val params = ParametersBuilder()
        // Check for username + password for legacy auth
        if ((serverDetails.authMethod != AuthMethod.APIKEY) &&
                (serverDetails.username.isNullOrBlank() || serverDetails.password.isNullOrBlank())) {
            throw RuntimeException("AuthMethod is not APIKEY and either username and/or password is undefined")
        }

        // Check for apiKeyAuthentication extension support and a valid api key,
        // If we're using APIKEY authentication
        if ((serverDetails.authMethod == AuthMethod.APIKEY) &&
            (!checkForOpenSubsonicExtension("apiKeyAuthentication", 1)
                    || serverDetails.apikey.isNullOrBlank())) {
            throw RuntimeException("AuthMethod is APIKEY and we have no API key or no server support")
        }

        // Add username here to avoid repeating it in the when statement
        // Plus logging
        serverDetails.username?.let {
            Napier.i("Logging in as $it")
            params.append("u", it)
        }

        when (serverDetails.authMethod) {
            AuthMethod.PLAIN_TEXT -> {
                // username and password in URL
                Napier.i("Using plain text auth")
                serverDetails.password?.let { params.append("p", it) }
            }
            AuthMethod.OBFUSCATED -> {
                // hex encoded password in URL
                Napier.i("Using obfuscated auth")
                val encodedPw = serverDetails.password?.toByteArray()?.toHexString()
                params.append("p", "enc:$encodedPw")
            }
            AuthMethod.TOKEN -> {
                // MD5(password + salt) and salt in URL
                Napier.i("Using token auth")
                val salt = Uuid.random().toHexString()
                Napier.d("Generated salt: $salt")
                val encodedPw = MD5().let {
                    it.update("${serverDetails.password}$salt".toByteArray())
                    it.digest().toHexString()
                }
                params.append("t", encodedPw)
                params.append("s", salt)
            }
            AuthMethod.APIKEY -> {
                // api key in URL
                Napier.i("Using APIKEY auth")
                // NOTE: is a null check (!!) the correct thing to use here???
                params.append("apiKey", serverDetails.apikey!!)
            }
        }

        return params
    }

    suspend fun getOpenSubsonicExtensions(): HashMap<String, MutableList<Int>> {
        val cacheEntry = cacheImpl.getResponse("getOpenSubsonicExtensions")
        val decodedResp = if (cacheEntry != null) {
            Napier.d("Found cache entry")
            cacheEntry
        } else {
            val response: HttpResponse = client.get("/rest/getOpenSubsonicExtensions").apply {
                // Early fail to save on resources
                if (this.status != HttpStatusCode.OK) {
                    Napier.i("Returning no openSubsonic extensions due to status code ${this.status.value}")
                    return HashMap()
                }
            }
            val decodedResp = Json.decodeFromString<root>(response.bodyAsText())
            cacheImpl.addResponse("getOpenSubsonicExtensions", response = decodedResp)

            decodedResp
        }

        // Convert extensions to return type
        val extensions: HashMap<String, MutableList<Int>> = HashMap()
        decodedResp.subsonicResponse.openSubsonicExtensions.forEach {
            val name: String = it.name
            val versions: MutableList<Int> = emptyList<Int>().toMutableList()
            it.versions.forEach {
                versions.add(it)
            }

            extensions[name] = versions
        }

        Napier.d("$decodedResp:: $extensions")
        return extensions
    }

    suspend fun checkForOpenSubsonicExtension(extension: String, version: Int): Boolean {
        Napier.d("Checking for openSubsonic extension $extension with version $version")
        val extensions = getOpenSubsonicExtensions()
        Napier.d("$extensions")
        var supported: Boolean = false

        extensions.forEach { (name, versions) ->
            if (name == extension) {
                Napier.d("Extension $extension was found")
                versions.forEach { supportedVersion ->
                    if (supportedVersion == version) {
                        Napier.d("Wanted version $version was found")
                        supported = true
                    } else {
                        Napier.d("Wanted version was NOT found, version $supportedVersion")
                    }
                }
            }
        }

        Napier.d("Returning $supported with $extension:$version")
        return supported
    }

    suspend fun ping(): Boolean {
        val response: HttpResponse = client.get("/rest/ping").apply {
            // Early fail on ping to save on decoding resources
            if (this.status != HttpStatusCode.OK) {
                Napier.i("Returning false from ping() due to status code ${this.status.value}")
                return false
            }
        }

        val decodedResp = Json.decodeFromString<root>(response.bodyAsText())
        Napier.i("ping returned ${decodedResp.subsonicResponse.status}")

        return when (decodedResp.subsonicResponse.status) {
            "ok" -> {
                true
            }

            "error" -> {
                false
            }
            else -> {
                throw RuntimeException("status in response was ${decodedResp.subsonicResponse.status}, expected ok or error")
            }
        }
    }
}