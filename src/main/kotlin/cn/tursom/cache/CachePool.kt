package cn.tursom.cache

interface CachePool<T> {
	fun put(cache: T): Boolean
	fun get(): T?
}

class NoCacheException : Exception()

fun <T> CachePool<T>.forceGet(): T {
	return get() ?: throw NoCacheException()
}

