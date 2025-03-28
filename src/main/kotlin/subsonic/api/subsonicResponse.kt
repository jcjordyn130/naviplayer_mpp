import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class root(
    @SerialName("subsonic-response")
    val subsonicResponse: subsonicResponse
)

@Serializable
data class subsonicResponse(
    val status: String,
    val version: String,
    val type: String,
    val serverVersion: String,

    // It's safe to assume a server is NOT openSubsonic compliant if
    // this field does not exist.
    val openSubsonic: Boolean = false,
    val openSubsonicExtensions: List<openSubsonicExtension> = emptyList()
)

@Serializable
data class openSubsonicExtension (
    val name: String,
    val versions: List<Int>
)