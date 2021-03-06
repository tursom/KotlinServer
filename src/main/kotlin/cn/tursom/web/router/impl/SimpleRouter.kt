package cn.tursom.web.router.impl

import cn.tursom.utils.datastruct.StringRadixTree
import cn.tursom.web.router.IRouter
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * 基于 Radix Tree，功能少，速度快。
 * 不支持参数解析，仅支持解析固定路径
 */
class SimpleRouter<T> : IRouter<T> {
	private val router = StringRadixTree<T?>()
	private val lock = ReentrantReadWriteLock()

	override fun addSubRoute(route: String, value: T?, onDestroy: ((oldValue: T) -> Unit)?) = lock.write {
		val old = router.set(route, value)
		if (old != null) onDestroy?.invoke(old)
	}

	override fun delRoute(route: String) = lock.write {
		router[route] = null
	}

	override fun get(route: String): Pair<T?, List<Pair<String, String>>> = lock.read { router[route] } to listOf()

	override fun toString(): String = router.toString()
}