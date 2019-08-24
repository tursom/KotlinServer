package cn.tursom.utils.cache.interfaces

interface AsyncCacheChannel<T> {
	suspend fun put(cache: T): Boolean
	suspend fun get(): T
}

