package cn.tursom.utils.cache

interface AsyncCacheChannel<T> {
	suspend fun put(cache: T): Boolean
	suspend fun get(): T
}

