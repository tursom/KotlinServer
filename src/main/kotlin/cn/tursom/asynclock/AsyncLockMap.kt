package cn.tursom.asynclock

open class AsyncLockMap<K, V>(
	private val lock: AsyncLock,
	private val map: Map<K, V>
) {
	val size: Int
		get() = map.size
	
	suspend fun size(): Int {
		return lock.doRead { map.size }
	}
	
	suspend fun isEmpty(): Boolean {
		return lock.doRead { map.isEmpty() }
	}
	
	suspend fun isNotEmpty(): Boolean {
		return lock.doRead { map.isNotEmpty() }
	}
	
	suspend fun get(key: K): V? {
		return lock.doRead { map[key] }
	}
	
	suspend fun contains(key: K): Boolean {
		return lock.doRead { map.contains(key) }
	}
	
	suspend fun forEach(action: suspend (K, V) -> Unit) {
		lock.doRead {
			map.forEach { (k, v) ->
				action(k, v)
			}
		}
	}
}