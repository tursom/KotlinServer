package cn.tursom.utils.asynccache.interfaces

import cn.tursom.utils.datastruct.async.interfaces.AsyncMap

interface AsyncCacheMap<K, V> : AsyncMap<K, V> {
	suspend fun get(key: K, constructor: suspend () -> V): V
}
