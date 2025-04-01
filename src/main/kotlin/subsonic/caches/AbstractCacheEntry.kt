package subsonic.caches

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import root

interface AbstractCacheEntry {
    val endpoint: String
    val params: HashMap<String, String>
    val expires: Instant
    val response: root
}