package cn.tursom.web.router

internal class SuspendAnyRouteNode<T>(
	route: List<String>,
	index: Int,
	value: T? = null,
	maxReadTime: Long
) : SuspendRouteNode<T>(route, index, value, maxReadTime) {
	override fun match(route: List<String>, startIndex: Int) = true to 1
}