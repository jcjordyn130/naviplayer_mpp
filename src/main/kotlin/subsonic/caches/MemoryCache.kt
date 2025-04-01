package subsonic.caches

import io.github.aakira.napier.Napier
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import root
import kotlin.time.Duration

class MemoryCache(override var expiryTime: Int) : AbstractCache {
    // Mapping of endpoint to mutable cache entries
    private val entries = mutableMapOf<String, MutableList<MemoryCacheEntry>>()

    fun size(): Int {
        var size: Int = 0
        entries.forEach { (endpoint, entries) ->
            entries.forEach {
                size += 1
            }
        }

        return size
    }

    fun tick() {
        entries.forEach { endpoint, cacheEntries ->
            cacheEntries.forEach { it ->
                if (it.expires > Clock.System.now()) {
                    Napier.d("Removing expired cache entry for endpoint ${it.endpoint} with params ${it}")
                }
            }
        }
    }
    override fun init(expiryTime: Int) {
        Napier.d("Initing MemoryCache with entries expiring in $expiryTime seconds!")
    }

    override fun addResponse(endpoint: String, params: HashMap<String, String>?, response: root) {
        val entry = MemoryCacheEntry(
            endpoint,
            params ?: hashMapOf(),
            response,
            Clock.System.now().plus(expiryTime, DateTimeUnit.SECOND)
        )

        Napier.d("Adding cache entry for $endpoint expiring at ${entry.expires} with parameters $params")
        // Add a new endpoint mapping if it does not exist
        if (entries[endpoint] == null) {
            entries[endpoint] = mutableListOf<MemoryCacheEntry>()
        }

        // Add endpoint to list
        entries[endpoint]!!.add(entry)
    }

    override fun getResponse(endpoint: String, params: HashMap<String, String>?): root? {
        // Early return if we don't have a list for this endpoint
        if (entries[endpoint] == null) {
            return null
        }

        // Find entries matching our endpoint and params
        val entries = entries[endpoint]!!.filter { it.endpoint == endpoint && ((params != null && params == it.params) || params == null) }
        if (entries.isEmpty()) {
            return null
        } else if (entries.size > 1) {
            throw RuntimeException("Cache state is invalid due to multiple matching entries for endpoint and params")
        }

        return entries.first().response
    }

    override fun deleteResponse(endpoint: String, params: HashMap<String, String>?) {
        // Early return if we don't have a list for this endpoint
        if (entries[endpoint] == null) {
            return
        }

        // Remove a specific entry if we are given params, otherwise remove them *all*
        if (params != null) {
            entries[endpoint]!!.removeIf { it.endpoint == endpoint && params == it.params }
        } else {
            entries[endpoint]!!.removeAll { true }
        }
    }
}