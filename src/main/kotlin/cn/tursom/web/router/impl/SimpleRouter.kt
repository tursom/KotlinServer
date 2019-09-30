package cn.tursom.web.router.impl

import cn.tursom.utils.datastruct.StringRadixTree
import cn.tursom.web.router.IRouter

/**
 * 基于 Radix Tree，功能少，速度快。
 * 不支持解析参数，仅支持解析固定路径
 */
class SimpleRouter<T> : IRouter<T> {
	private val router = StringRadixTree<T?>()

	override fun addSubRoute(route: String, value: T?, onDestroy: ((oldValue: T) -> Unit)?) {
		val old = router[route]
		if (old != null) onDestroy?.invoke(old)
		router[route] = value
	}

	override fun delRoute(route: String) {
		router[route] = null
	}

	override fun get(route: String): Pair<T?, List<Pair<String, String>>> = router[route] to listOf()
}