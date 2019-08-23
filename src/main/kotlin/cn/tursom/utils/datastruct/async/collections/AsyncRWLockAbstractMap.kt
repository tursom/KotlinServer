package cn.tursom.utils.datastruct.async.collections

import cn.tursom.utils.asynclock.AsyncRWLock
import cn.tursom.utils.asynclock.AsyncReadFirstRWLock
import cn.tursom.utils.asynclock.AsyncWriteFirstRWLock
import cn.tursom.utils.datastruct.async.interfaces.AsyncPotableMap

class AsyncRWLockAbstractMap<K, V>(
	lock: AsyncRWLock,
	override val map: java.util.AbstractMap<K, V> = HashMap()
) : AsyncRWLockMap<K, V>(lock, map), AsyncPotableMap<K, V> {
	constructor(lock: AsyncRWLock) : this(lock, HashMap())

	override suspend fun set(key: K, value: V): V? {
		lock.doWrite {
			map[key] = value
		}
		return value
	}

	override suspend fun remove(key: K): V? {
		return lock.doWrite { map.remove(key) }
	}

	override suspend fun clear() {
		lock { map.clear() }
	}

	override suspend fun putAll(from: Map<out K, V>) {
		lock {
			from.forEach { (k, u) ->
				map[k] = u
			}
		}
	}

	override fun toString(): String = map.toString()
}
