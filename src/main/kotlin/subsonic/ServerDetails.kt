package subsonic

import io.github.aakira.napier.Napier

data class ServerDetails(
    val url: String,
    val authMethod: AuthMethod,
    val username: String,
    val password: String) {

    init {
        if (authMethod != AuthMethod.TOKEN) {
            Napier.w("Not using token authentication... this is NOT recommended and is insecure, especially on non-HTTPS servers!")
        }
    }
}