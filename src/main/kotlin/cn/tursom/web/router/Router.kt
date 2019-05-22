package cn.tursom.web.router

import java.lang.Exception

fun <T> List<T>.binarySearch(comparison: (T) -> Int): T? {
	val index = binarySearch(0, size, comparison)
	return if (index < 0) null
	else get(index)
}

class Router<T> {
	val rootNode = RouteNode<T>("", null)
	
	fun addSubRoute(route: String, value: T) {
		val routeList = route.split('/').drop(0)
		var routeNode = rootNode
		var index = 0
		while (index < routeList.size) {
			val r = routeList[index]
			routeNode = when {
				r.isEmpty() -> routeNode
				
				r == "*" -> routeNode.wildSubRouter ?: {
					val node = AnyRouteNode<T>()
					routeNode.wildSubRouter = node
					node
				}()
				
				r[0] == ':' -> synchronized(routeNode.placeholderRouterList!!) {
					val matchLength = PlaceholderRouteNode.matchLength(routeList, index)
					routeNode.placeholderRouterList!!.binarySearch { it.routeList.size - matchLength } ?: {
						index += routeNode.addNode(routeList, index) - 1
						routeNode.placeholderRouterList!!.binarySearch { it.routeList.size - matchLength }!!
					}()
				}
				
				else -> synchronized(routeNode.subRouterMap) {
					routeNode.subRouterMap[r] ?: {
						val node = RouteNode<T>(r)
						routeNode.subRouterMap[r] = node
						node
					}()
				}
			}
			index++
		}
		routeNode.value = value
	}
	
	fun delRoute(route: String) {
		val routeList = route.split('/').drop(0)
		var index = 0
		var routeNode = rootNode
		while (index < routeList.size) {
			if (routeList[index].isEmpty()) {
				index++
				continue
			}
			val (node, size) = routeNode[routeList, index]
			node ?: return
			if (index + size >= routeList.size) {
				routeNode.deleteNode(routeList, index)
				break
			}
			routeNode = node
		}
	}
	
	operator fun set(route: String, value: T) = addSubRoute(route, value)
	operator fun get(route: String): Pair<RouteNode<T>?, ArrayList<RouteNode<T>>> {
		val list = ArrayList<RouteNode<T>>()
		val router = rootNode.getRoute(route.split('/'), routeList = list)
		return router to list
	}
}

open class RouteNode<T>(
	val route: String,
	var value: T? = null
) {
	var wildSubRouter: AnyRouteNode<T>? = null
	open val placeholderRouterList: ArrayList<PlaceholderRouteNode<T>>? = ArrayList(0)
	val subRouterMap = HashMap<String, RouteNode<T>>(0)
	
	open fun match(
		route: List<String>,
		startIndex: Int = 0
	): Pair<Boolean, Int> =
		(startIndex < route.size && route[0] == this.route) to 1
	
	fun addNode(route: List<String>, startIndex: Int, value: T? = null): Int {
		val r = route[startIndex]
		return when {
			r.isEmpty() -> return addNode(route, startIndex + 1)
			r == "*" -> {
				wildSubRouter = AnyRouteNode(value)
				1
			}
			r[0] == ':' -> synchronized(placeholderRouterList!!) {
				val node = PlaceholderRouteNode(route, startIndex, value = value)
				// 必须保证 placeholderRouterList 存在，而且还不能有这个长度的节点
				if (placeholderRouterList!!.binarySearch { it.routeList.size - node.routeList.size } != null) {
					throw Exception()
				}
				placeholderRouterList?.add(node)
				placeholderRouterList?.sortBy { it.routeList.size }
				node.routeList.size
			}
			else -> synchronized(subRouterMap) {
				subRouterMap[r] = RouteNode(r, value)
				1
			}
		}
	}
	
	fun deleteNode(route: List<String>, startIndex: Int = 0) {
		val r = route[startIndex]
		when {
			r.isEmpty() -> Unit
			r == "*" -> {
				wildSubRouter = null
			}
			r[0] == ':' -> synchronized(placeholderRouterList ?: return) {
				val length = PlaceholderRouteNode.matchLength(route, startIndex)
				placeholderRouterList?.removeIf { it.routeList.size == length }
				placeholderRouterList?.sortBy { it.routeList.size }
			}
			else -> synchronized(subRouterMap) {
				subRouterMap.remove(r)
			}
		}
	}
	
	fun findPlaceholderRouter(route: List<String>, startIndex: Int = 0) = placeholderRouterList?.let { node ->
		val length = PlaceholderRouteNode.matchLength(route, startIndex)
		synchronized(node) { node.binarySearch { it.routeList.size - length } }
	}
	
	operator fun get(route: List<String>, startIndex: Int = 0): Pair<RouteNode<T>?, Int> {
		val r = route[startIndex]
		if (r.isEmpty()) return this to 1
		
		val value = subRouterMap[r]
		if (value != null) return value to 1
		
		val matchLength = route.size - startIndex
		val exactRoute = placeholderRouterList?.let { list ->
			list.binarySearch { matchLength - it.routeList.size }
		}
		if (exactRoute != null) return exactRoute to matchLength
		
		placeholderRouterList?.forEach {
			val subRoute = it.getRoute(route, startIndex + it.routeList.size)
			if (subRoute != null) return subRoute to route.size - startIndex
		}
		
		return wildSubRouter to 1
	}
	
	fun getRoute(route: List<String>, startIndex: Int = 0): RouteNode<T>? {
		var index = startIndex
		var routeNode = this
		while (index < route.size) {
			val (node, size) = routeNode[route, index]
			routeNode = node ?: return null
			index += size
		}
		return routeNode
	}
	
	operator fun get(
		route: List<String>,
		startIndex: Int = 0,
		routeList: ArrayList<RouteNode<T>>
	): Pair<RouteNode<T>?, Int> {
		val r = route[startIndex]
		if (r.isEmpty()) {
			return this to 1
		}
		
		val value = subRouterMap[r]
		if (value != null) {
			routeList.add(value)
			return value to 1
		}
		
		val matchLength = route.size - startIndex
		val exactRoute = placeholderRouterList?.let { list ->
			list.binarySearch { matchLength - it.routeList.size }
		}
		if (exactRoute != null) {
			routeList.add(exactRoute)
			return exactRoute to matchLength
		}
		
		val list = ArrayList<RouteNode<T>>()
		placeholderRouterList?.forEach {
			list.clear()
			val subRoute = it.getRoute(route, startIndex + it.routeList.size, list)
			if (subRoute != null) {
				routeList.add(it)
				list.forEach { routeList.add(it) }
				return subRoute to route.size - startIndex
			}
		}
		
		return wildSubRouter to 1
	}
	
	fun getRoute(
		route: List<String>,
		startIndex: Int = 0,
		routeList: ArrayList<RouteNode<T>>
	): RouteNode<T>? {
		var index = startIndex
		var routeNode = this
		while (index < route.size) {
			val (node, size) = routeNode[route, index, routeList]
			routeNode = node ?: return null
			index += size
		}
		return routeNode
	}
	
	override fun toString() = "/$route"
}

class PlaceholderRouteNode<T>(
	route: List<String>,
	startIndex: Int = 0,
	endIndex: Int = run {
		var index = startIndex
		while (++index < route.size && (route[index].isEmpty() || route[index][0] == ':'));
		index
	},
	value: T? = null
) : RouteNode<T>(route.str(startIndex, endIndex), value) {
	override val placeholderRouterList: ArrayList<PlaceholderRouteNode<T>>?
		get() = null
	val routeList: List<String> = run {
		val list = ArrayList<String>()
		for (i in startIndex until endIndex) {
			if (route[i].isNotEmpty()) list.add(route[i])
		}
		list
	}
	
	override fun match(
		route: List<String>,
		startIndex: Int
	): Pair<Boolean, Int> =
		(routeList.size == route.matchLength(startIndex)) to routeList.size
	
	
	override fun toString(): String {
		val stringBuilder = StringBuilder()
		routeList.forEach {
			stringBuilder.append("/$it")
		}
		return stringBuilder.toString()
	}
	
	companion object {
		@JvmStatic
		private fun List<String>.str(startIndex: Int, endIndex: Int): String {
			val stringBuilder = StringBuilder()
			for (i in startIndex until endIndex) {
				if (this[i].isNotEmpty()) stringBuilder.append("/${this[i]}")
			}
			return stringBuilder.toString()
		}
		
		@JvmStatic
		private fun List<String>.matchLength(startIndex: Int, endIndex: Int = size): Int {
			var length = 0
			for (i in startIndex until endIndex) {
				if (this[i].isEmpty()) continue
				else if (this[i][0] != ':') return length
				else length++
			}
			return length
		}
		
		@JvmStatic
		fun matchLength(route: List<String>, startIndex: Int): Int {
			var length = 0
			for (i in startIndex until route.size) {
				if (route[i].isEmpty()) continue
				else if (route[i][0] != ':') return length
				else length++
			}
			return length
		}
	}
}

class AnyRouteNode<T>(
	value: T? = null
) : RouteNode<T>("*", value) {
	override fun match(route: List<String>, startIndex: Int) = true to 1
}