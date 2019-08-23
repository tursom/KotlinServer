package cn.tursom.utils.datastruct.async.collections

import cn.tursom.utils.asynclock.AsyncLock
import cn.tursom.utils.asynclock.AsyncRWLock

class AsyncLockAbstractMap<K, V>(
	override val lock: AsyncLock,
	override val map: java.util.AbstractMap<K, V> = HashMap()
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