package subsonic

enum class AuthMethod {
    PLAIN_TEXT, // Sends username + password in URL query
    OBFUSCATED, // Hex encoded password in URL query
    TOKEN, // MD5 encoded password with salt, and sends both
    APIKEY // API key generated from server, cannot be used with the other 3
}