package cn.tursom.utils.asynccache.interfaces

interface AsyncCacheChannel<T> {
	suspend fun put(cache: T): Boolean
	suspend fun get(): T
}

