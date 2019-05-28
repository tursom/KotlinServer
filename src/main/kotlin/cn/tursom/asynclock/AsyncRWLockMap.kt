package cn.tursom.asynclock

open class AsyncRWLockMap<K, V>(
	private val lock: AsyncRWLock,
	private val map: Map<K, V>
) : AsyncLockMap<K, V>(lock, map) {
	
	override suspend fun size(): Int {
		return lock.doRead { map.size }
	}
	
	override suspend fun isEmpty(): Boolean {
		return lock.doRead { map.isEmpty() }
	}
	
	override suspend fun isNotEmpty(): Boolean {
		return lock.doRead { map.isNotEmpty() }
	}
	
	override suspend fun get(key: K): V? {
		return lock.doRead { map[key] }
	}
	
	override suspend fun contains(key: K): Boolean {
		return lock.doRead { map.contains(key) }
	}
	
	override suspend fun forEach(action: suspend (K, V) -> Unit) {
		lock.doRead {
			map.forEach { (k, v) ->
				action(k, v)
			}
		}
	}
}