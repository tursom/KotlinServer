package cn.tursom.web.router.impl

import cn.tursom.web.router.IRouter

/**
 * 基于 Radix Tree，功能少，速度快。
 * 不支持解析参数，仅支持解析固定路径
 */
class SimpleRouter<T> : IRouter<T> {
	override fun addSubRoute(route: String, value: T?, onDestroy: ((oldValue: T) -> Unit)?) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun delRoute(route: String) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun get(route: String): Pair<T?, List<Pair<String, String>>> {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
}