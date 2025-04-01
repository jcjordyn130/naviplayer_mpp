package subsonic.caches

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import root

data class MemoryCacheEntry(
    override val endpoint: String,
    override val params: HashMap<String, String>,
    override val response: root,
    override val expires: Instant
) : AbstractCacheEntry