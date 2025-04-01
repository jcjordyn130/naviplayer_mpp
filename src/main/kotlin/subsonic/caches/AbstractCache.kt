package subsonic.caches

import root

interface AbstractCache {
    public var expiryTime: Int

    fun init(expiryTime: Int)
    fun tick()
    fun addResponse(endpoint: String, params: HashMap<String, String>? = null, response: root)
    fun getResponse(endpoint: String, params: HashMap<String, String>? = null): root?
    fun deleteResponse(endpoint: String, params: HashMap<String, String>? = null)
}