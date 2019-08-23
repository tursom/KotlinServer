package cn.tursom.utils.asynccache.interfaces

interface CachePool<T> {
	fun put(cache: T): Boolean
	fun get(): T?
	
	class NoCacheException : Exception()
	
	fun <T> CachePool<T>.forceGet(): T {
		return get() ?: throw NoCacheException()
	}
}
