package subsonic

import io.github.aakira.napier.Napier

data class ServerDetails(
    val url: String,
    val authMethod: AuthMethod,
    val username: String? = null,
    val password: String? = null,
    val apikey: String? = null)