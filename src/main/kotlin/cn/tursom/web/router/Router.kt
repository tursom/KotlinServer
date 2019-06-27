package cn.tursom.web.router

import cn.tursom.tools.binarySearch


interface RouterNode<T> {
	val value: T?
	
	fun forEach(action: (node: RouterNode<T>) -> Unit)
}

@Suppress("unused", "unused", "MemberVisibilityCanBePrivate", "UNUSED_PARAMETER")
class Router<T> {
	private val rootNode = RouteNode<T>(listOf(""), 0)
	val root: RouterNode<T> = rootNode
	
	fun addSubRoute(route: String, value: T?, onDestroy: ((oldValue: T) -> Unit)? = null) {
		val routeList = route.split('?')[0].split('/').filter { it.isNotEmpty() }
		var routeNode = rootNode
		var r: String
		var index = 0
		while (index < routeList.size) {
			r = routeList[index]
			routeNode = when {
				r.isEmpty() -> routeNode
				
				r == "*" -> routeNode.wildSubRouter ?: {
					val node = AnyRouteNode<T>(routeList, index)
					routeNode.wildSubRouter = node
					index = routeList.size - 1
					node
				}()
				
				r[0] == ':' -> run {
					val node = synchronized(routeNode.placeholderRouterList!!) {
						val matchLength = PlaceholderRouteNode.matchLength(routeList, index)
						routeNode.placeholderRouterList!!.binarySearch { it.size - matchLength } ?: {
							routeNode.addNode(routeList, index, null)
							routeNode.placeholderRouterList!!.binarySearch { it.size - matchLength }!!
						}()
					}
					index += node.size - 1
					node
				}
				
				else -> synchronized(routeNode.subRouterMap) {
					routeNode.subRouterMap[r] ?: {
						val node = RouteNode<T>(routeList, index)
						routeNode.subRouterMap[r] = node
						node
					}()
				}
			}
			index++
		}
		val oldValue = routeNode.value
		if (oldValue != null) onDestroy?.invoke(oldValue)
		routeNode.value = value
		routeNode.routeList = routeList
		routeNode.index = index - 1
	}
	
	fun delRoute(route: String) {
		this[route] = null
	}
	
	operator fun set(
		route: String,
		onDestroy: ((oldValue: T) -> Unit)? = null,
		value: T?
	) = addSubRoute(route, value, onDestroy)
	
	operator fun get(route: String): Pair<T?, List<Pair<String, String>>> {
		val list = ArrayList<Pair<String, String>>()
		return rootNode[route.split('?')[0].split('/').filter { it.isNotEmpty() }, list]?.value to list
	}
	
	private fun toString(node: RouteNode<T>, stringBuilder: StringBuilder, indentation: String) {
		if (
			node.value == null &&
			node.subRouterMap.isEmpty() &&
			node.placeholderRouterList?.isEmpty() != false &&
			node.wildSubRouter == null
		) {
			return
		}
		
		if (indentation.isNotEmpty()) {
			stringBuilder.append(indentation)
			stringBuilder.append("- ")
		}
		stringBuilder.append("${node.singleRoute}${if (node.value != null) "    ${node.value}" else ""}\n")
		
		if (node is AnyRouteNode) return
		
		val subIndentation = if (indentation.isEmpty()) "|" else "$indentation  |"
		
		node.subRouterMap.forEach { (_, u) ->
			toString(u, stringBuilder, subIndentation)
		}
		node.placeholderRouterList?.forEach {
			toString(it, stringBuilder, subIndentation)
		}
		toString(node.wildSubRouter ?: return, stringBuilder, subIndentation)
		return
	}
	
	override fun toString(): String {
		val stringBuilder = StringBuilder()
		toString(rootNode, stringBuilder, "")
		return stringBuilder.toString()
	}
}

@Suppress("MemberVisibilityCanBePrivate")
open class RouteNode<T>(
	var routeList: List<String>,
	var index: Int,
	override var value: T? = null
) : RouterNode<T> {
	val route: String = routeList[index]
	var wildSubRouter: AnyRouteNode<T>? = null
	open val placeholderRouterList: ArrayList<PlaceholderRouteNode<T>>? = ArrayList(0)
	val subRouterMap = HashMap<String, RouteNode<T>>(0)
	
	open val singleRoute
		get() = "/$route"
	
	override fun forEach(action: (node: RouterNode<T>) -> Unit) {
		placeholderRouterList?.forEach(action)
		subRouterMap.forEach { (_, u) -> action(u) }
		wildSubRouter?.let(action)
	}
	
	open fun match(
		route: List<String>,
		startIndex: Int
	): Pair<Boolean, Int> = (route.size > startIndex && route[startIndex] == this.route) to 1
	
	fun addNode(route: List<String>, startIndex: Int, value: T? = null): Int {
		val r = route[startIndex]
		return when {
			r.isEmpty() -> return addNode(route, startIndex + 1)
			r == "*" -> {
				wildSubRouter = AnyRouteNode(route, startIndex)
				1
			}
			r[0] == ':' -> {
				val node: PlaceholderRouteNode<T> = PlaceholderRouteNode(route, startIndex, value = value)
				// 必须保证 placeholderRouterList 存在，而且还不能有这个长度的节点
				if (synchronized(placeholderRouterList!!) {
						placeholderRouterList!!.binarySearch { it.size - node.size }
					} != null) {
					throw Exception()
				}
				synchronized(placeholderRouterList!!) {
					placeholderRouterList?.add(node)
					placeholderRouterList?.sortBy { it.size }
				}
				node.size
			}
			else -> synchronized(subRouterMap) {
				subRouterMap[r] = RouteNode(route, startIndex, value)
				1
			}
		}
	}
	
	operator fun get(route: List<String>, startIndex: Int = 0): Pair<RouteNode<T>?, Int> {
		val r = route[startIndex]
		if (r.isEmpty()) return this to 1
		
		val value = synchronized(subRouterMap) { subRouterMap[r] }
		if (value != null) return value to 1
		
		val matchLength = route.size - startIndex
		val exactRoute = placeholderRouterList?.let { list ->
			synchronized(list) { list.binarySearch { matchLength - it.size } }
		}
		if (exactRoute != null) return exactRoute to matchLength
		
		placeholderRouterList?.let { list ->
			synchronized(list) {
				list.forEach {
					val subRoute = it.getRoute(route, startIndex + it.size)
					if (subRoute != null) return subRoute to route.size - startIndex
				}
			}
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
		routeList: java.util.AbstractList<Pair<String, String>>
	): Pair<RouteNode<T>?, Int> {
		val r = route[startIndex]
		if (r.isEmpty()) {
			return this to 1
		}
		
		val value = synchronized(subRouterMap) { subRouterMap[r] }
		if (value != null) return value to 1
		
		val matchLength = route.size - startIndex
		val exactRoute = placeholderRouterList?.let { list ->
			synchronized(list) { list.binarySearch { matchLength - it.size } }
		}
		if (exactRoute != null) {
			exactRoute.routeList.forEachIndexed { index, s ->
				if (s.isNotEmpty() && s[0] == ':') routeList.add(s.substring(1) to route[index])
			}
			return exactRoute to matchLength
		}
		
		val list = ArrayList<Pair<String, String>>()
		placeholderRouterList?.let { routerList ->
			synchronized(routerList) {
				routerList.forEach {
					list.clear()
					val subRoute = it.getRoute(route, startIndex + it.size, list)
					if (subRoute != null) {
						subRoute.routeList.forEachIndexed { index, s ->
							if (s.isNotEmpty()) when {
								s == "*" -> for (i in index until route.size) {
									routeList.add("*" to route[i])
								}
								s[0] == ':' -> routeList.add(s.substring(1) to route[index])
							}
						}
						var listIndex = 0
						var routeIndex = 0
						while (listIndex < list.size && routeIndex <= index) {
							val s = this.routeList[routeIndex++]
							if (s.isNotEmpty() && s[0] == ':') {
								routeList.add(s to list[listIndex++].second)
							}
						}
						return subRoute to route.size - startIndex
					}
				}
			}
		}
		
		for (i in startIndex until route.size)
			routeList.add("*" to route[i])
		return wildSubRouter to 1
	}
	
	operator fun get(
		route: List<String>,
		routeList: java.util.AbstractList<Pair<String, String>>
	) = getRoute(route, 0, routeList)
	
	fun getRoute(
		route: List<String>,
		startIndex: Int = 0,
		routeList: java.util.AbstractList<Pair<String, String>>
	): RouteNode<T>? {
		var index = startIndex
		var routeNode = this
		while (routeNode !is AnyRouteNode && index < route.size) {
			val (node, size) = routeNode[route, index, routeList]
			routeNode = node ?: return null
			index += size
		}
		return routeNode
	}
	
	override fun toString(): String {
		val stringBuilder = StringBuilder("/")
		for (i in 0..index) {
			val s = routeList[i]
			if (s.isNotEmpty()) stringBuilder.append("$s/")
		}
		if (value != null) {
			stringBuilder.append("    $value")
		}
		return stringBuilder.toString()
	}
}

class PlaceholderRouteNode<T>(
	route: List<String>,
	private val startIndex: Int = 0,
	endIndex: Int = startIndex + route.matchLength(startIndex),
	value: T? = null
) : RouteNode<T>(route, endIndex - 1, value) {
	override val placeholderRouterList: ArrayList<PlaceholderRouteNode<T>>?
		get() = null
	
	val size: Int = route.matchLength(startIndex, endIndex)
	
	override val singleRoute: String
		get() {
			val sb = StringBuilder()
			for (i in startIndex..index) {
				sb.append("/")
				sb.append(routeList[i])
			}
			return sb.toString()
		}
	
	override fun match(
		route: List<String>,
		startIndex: Int
	): Pair<Boolean, Int> =
		(size == route.matchLength(startIndex)) to size
	
	companion object {
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
	route: List<String>,
	index: Int,
	value: T? = null
) : RouteNode<T>(route, index, value) {
	override fun match(route: List<String>, startIndex: Int) = true to 1
}
