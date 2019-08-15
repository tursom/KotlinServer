package cn.tursom.web.router

class AnyRouteNode<T>(
	route: List<String>,
	index: Int,
	value: T? = null
) : RouteNode<T>(route, index, value) {
	override fun match(route: List<String>, startIndex: Int) = true to 1
}