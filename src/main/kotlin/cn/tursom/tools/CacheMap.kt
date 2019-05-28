package cn.tursom.tools

import cn.tursom.asynclock.ReadWriteLockHashMap

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