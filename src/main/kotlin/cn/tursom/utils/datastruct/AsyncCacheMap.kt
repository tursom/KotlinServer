package cn.tursom.utils.datastruct

import cn.tursom.utils.asynclock.ReadWriteLockHashMap

class AsyncCacheMap<K, V>(val timeout: Long) {
	private val valueMap = ReadWriteLockHashMap<K, Pair<Long, V>>()

	suspend fun get(key: K): V? {
		val (time, value) = getCache(key) ?: return null
		if (time.isTimeOut()) {
			delCache(key)
			return null
		}
		return value
	}

	suspend fun get(key: K, constructor: suspend () -> V): V {
		val (time, value) = getCache(key) ?: run {
			val newValue = System.currentTimeMillis() to constructor()
			addCache(key, newValue)
			newValue
		}
		if (time.isTimeOut()) {
			delCache(key)
			return run {
				val newValue = constructor()
				addCache(key, System.currentTimeMillis() to newValue)
				newValue
			}
		}
		return value
	}

	suspend fun set(key: K, value: V) {
		valueMap.set(key, System.currentTimeMillis() to value)
	}

	private suspend fun getCache(key: K): Pair<Long, V>? {
		return valueMap.get(key)
	}

	private suspend fun delCache(key: K) {
		valueMap.remove(key)
	}

	private suspend fun addCache(key: K, value: Pair<Long, V>) {
		valueMap.set(key, value)
	}

	private fun Long.isTimeOut() = timeout != 0L && System.currentTimeMillis() - this > timeout
}