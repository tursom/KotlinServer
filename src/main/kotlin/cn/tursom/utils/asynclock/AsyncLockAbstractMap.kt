package cn.tursom.utils.asynclock

class AsyncLockAbstractMap<K, V>(
	private val lock: AsyncLock,
	private val map: java.util.AbstractMap<K, V> = HashMap()
) : AsyncLockMap<K, V>(lock, map) {

	constructor(lock: AsyncRWLock) : this(lock, HashMap())

	suspend fun set(key: K, value: V) {
		lock {
			map[key] = value
		}
	}

	suspend fun remove(key: K) {
		lock { map.remove(key) }
	}

	override fun toString(): String = map.toString()
}