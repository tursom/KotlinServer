package cn.tursom.utils.cache

interface AsyncCachePool<T> {
	suspend fun put(cache: T): Boolean
	suspend fun get(): T?
	
	suspend fun <T> AsyncCachePool<T>.forceGet(): T {
		return get() ?: throw CachePool.NoCacheException()
	}
}