package cn.tursom.utils.asynclock

open class AsyncLockMap<K, V>(
	private val lock: AsyncLock,
	private val map: Map<K, V>
) {
	open val size: Int
		get() = map.size
	
	open suspend fun size(): Int {
		return lock { map.size }
	}
	
	open suspend fun isEmpty(): Boolean {
		return lock { map.isEmpty() }
	}
	
	open suspend fun isNotEmpty(): Boolean {
		return lock { map.isNotEmpty() }
	}
	
	open suspend fun get(key: K): V? {
		return lock { map[key] }
	}
	
	open suspend fun contains(key: K): Boolean {
		return lock { map.contains(key) }
	}
	
	open suspend fun forEach(action: suspend (K, V) -> Unit) {
		lock {
			map.forEach { (k, v) ->
				action(k, v)
			}
		}
	}
}