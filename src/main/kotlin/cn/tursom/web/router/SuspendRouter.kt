package cn.tursom.web.router

import cn.tursom.asynclock.AsyncReadFirstRWLock
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors
import cn.tursom.tools.binarySearch

interface SuspendRouterNode<T> {
	val value: T?
	val lastRoute: String
	val fullRoute: String
	val empty: Boolean
	
	suspend fun forEach(action: suspend (node: SuspendRouterNode<T>) -> Unit)
}

@Suppress("unused", "unused", "MemberVisibilityCanBePrivate", "UNUSED_PARAMETER")
class SuspendRouter<T>(val maxReadTime: Long = 5) {
	private val rootNode = SuspendRouteNode<T>(listOf(""), 0, null, maxReadTime)
	private val threadPool = Executors.newSingleThreadExecutor()
	
	@Volatile
	private var _lastChangeTime: Long = System.currentTimeMillis()
	val lashChangeTime
		get() = _lastChangeTime
	@Volatile
	private var strBuf: String = ""
	@Volatile
	private var strBufTime: Long = 0
	
	val root: SuspendRouterNode<T> = rootNode
	
	private suspend fun setSubRoute(
		route: String,
		value: T?,
		onDestroy: ((oldValue: T) -> Unit)? = null
	) {
		val routeList = route.split('?')[0].split('/').filter { it.isNotEmpty() }
		var routeNode = rootNode
		var r: String
		var index = 0
		while (index < routeList.size) {
			r = routeList[index]
			routeNode = when {
				r.isEmpty() -> routeNode
				
				r == "*" -> routeNode.wildSubRouter ?: {
					val node = SuspendAnyRouteNode<T>(routeList, index, null, maxReadTime)
					routeNode.wildSubRouter = node
					index = routeList.size - 1
					node
				}()
				
				r[0] == ':' -> {
					val matchLength = SuspendPlaceholderRouteNode.matchLength(routeList, index)
					val node = routeNode.getPlaceholderRouter(matchLength) ?: suspend {
						routeNode.addNode(routeList, index, null)
						routeNode.getPlaceholderRouter(matchLength)!!
					}()
					index += node.size - 1
					node
				}
				
				else -> routeNode.subRouterMap[r] ?: {
					val node = SuspendRouteNode<T>(routeList, index, null, maxReadTime)
					routeNode.subRouterMap[r] = node
					node
				}()
			}
			index++
		}
		val oldValue = routeNode.value
		if (oldValue != null) onDestroy?.invoke(oldValue)
		routeNode.value = value
		routeNode.routeList = routeList
		routeNode.index = index - 1
		_lastChangeTime = System.currentTimeMillis()
	}
	
	suspend fun delRoute(route: String, onDestroy: ((oldValue: T) -> Unit)? = null) {
		this.set(route, null, onDestroy)
	}
	
	suspend fun set(
		route: String,
		value: T?,
		onDestroy: ((oldValue: T) -> Unit)? = null
	) = setSubRoute(route, value, onDestroy)
	
	suspend fun set(
		route: String,
		onDestroy: ((oldValue: T) -> Unit)? = null,
		value: T?
	) = setSubRoute(route, value, onDestroy)
	
	suspend fun get(route: String): Pair<T?, List<Pair<String, String>>> {
		val list = ArrayList<Pair<String, String>>()
		return rootNode.get(route.split('?')[0].split('/').filter { it.isNotEmpty() }, list)?.value to list
	}
	
	private suspend fun toString(node: SuspendRouteNode<T>, stringBuilder: StringBuilder, indentation: String) {
		if (
			node.value == null &&
			node.subRouterMap.isEmpty() &&
			node.placeholderRouterListEmpty &&
			node.wildSubRouter == null
		) {
			return
		}
		
		if (indentation.isNotEmpty()) {
			stringBuilder.append(indentation)
			stringBuilder.append("- ")
		}
		stringBuilder.append("${node.lastRoute}${if (node.value != null) "    ${node.value}" else ""}\n")
		
		if (node is SuspendAnyRouteNode) return
		
		val subIndentation = if (indentation.isEmpty()) "|" else "$indentation  |"
		
		node.subRouterMap.forEach { (_, u) ->
			toString(u, stringBuilder, subIndentation)
		}
		node.forEachPlaceholderRouter {
			toString(it, stringBuilder, subIndentation)
		}
		toString(node.wildSubRouter ?: return, stringBuilder, subIndentation)
		return
	}
	
	suspend fun suspendToString(): String {
		if (strBufTime < _lastChangeTime) {
			val stringBuilder = StringBuilder()
			toString(rootNode, stringBuilder, "")
			strBuf = stringBuilder.toString()
			strBufTime = System.currentTimeMillis()
		}
		return strBuf
	}
	
	override fun toString(): String {
		if (strBufTime < _lastChangeTime) {
			val stringBuilder = StringBuilder()
			runBlocking {
				toString(rootNode, stringBuilder, "")
			}
			strBuf = stringBuilder.toString()
			strBufTime = System.currentTimeMillis()
		}
		return strBuf
	}
}

@Suppress("MemberVisibilityCanBePrivate")
private open class SuspendRouteNode<T>(
	var routeList: List<String>,
	var index: Int,
	override var value: T? = null,
	val maxReadTime: Long
) : SuspendRouterNode<T> {
	val route: String = routeList[index]
	var wildSubRouter: SuspendAnyRouteNode<T>? = null
	
	private val placeholderRouterListLock = AsyncReadFirstRWLock(maxReadTime)
	protected open val placeholderRouterList: ArrayList<SuspendPlaceholderRouteNode<T>>? = ArrayList(0)
	
	private val subRouterMapLock = AsyncReadFirstRWLock(maxReadTime)
	val subRouterMap = HashMap<String, SuspendRouteNode<T>>(0)
	
	override val lastRoute
		get() = "/$route"
	
	override val fullRoute: String by lazy {
		val stringBuilder = StringBuilder("")
		for (i in 0..index) {
			val s = routeList[i]
			if (s.isNotEmpty()) stringBuilder.append("/$s")
		}
		stringBuilder.toString()
	}
	
	val placeholderRouterListEmpty
		get() = placeholderRouterList?.isEmpty() ?: true
	
	override val empty: Boolean
		get() = value == null &&
			subRouterMap.isEmpty() &&
			placeholderRouterListEmpty &&
			wildSubRouter == null
	
	override suspend fun forEach(action: suspend (node: SuspendRouterNode<T>) -> Unit) {
		placeholderRouterListLock.doRead {
			placeholderRouterList?.forEach { action(it) }
		}
		subRouterMapLock.doRead {
			subRouterMap.forEach { (_, u) -> action(u) }
		}
		wildSubRouter?.let { action(it) }
	}
	
	suspend fun forEachPlaceholderRouter(block: suspend (SuspendPlaceholderRouteNode<T>) -> Unit) {
		placeholderRouterListLock.doRead { placeholderRouterList?.forEach { block(it) } }
	}
	
	suspend fun getPlaceholderRouter(length: Int): SuspendPlaceholderRouteNode<T>? {
		return placeholderRouterListLock.doRead { placeholderRouterList!!.binarySearch { it.size - length } }
	}
	
	open fun match(
		route: List<String>,
		startIndex: Int
	): Pair<Boolean, Int> = (route.size > startIndex && route[startIndex] == this.route) to 1
	
	suspend fun addNode(route: List<String>, startIndex: Int, value: T? = null): Int {
		val r = route[startIndex]
		return when {
			r.isEmpty() -> return addNode(route, startIndex + 1)
			r == "*" -> {
				wildSubRouter = SuspendAnyRouteNode(route, startIndex, null, maxReadTime)
				1
			}
			r[0] == ':' -> {
				val node: SuspendPlaceholderRouteNode<T> = SuspendPlaceholderRouteNode(
					route,
					startIndex,
					value = value,
					maxReadTime = maxReadTime
				)
				// 必须保证 placeholderRouterList 存在，而且还不能有这个长度的节点
				if (placeholderRouterListLock.doRead { placeholderRouterList!!.binarySearch { it.size - node.size } } != null) {
					throw Exception()
				}
				placeholderRouterListLock.doWrite {
					placeholderRouterList?.add(node)
					placeholderRouterList?.sortBy { it.size }
				}
				node.size
			}
			else -> {
				subRouterMap[r] = SuspendRouteNode(route, startIndex, value, maxReadTime)
				1
			}
		}
	}
	
	operator fun get(route: List<String>, startIndex: Int = 0): Pair<SuspendRouteNode<T>?, Int> {
		val r = route[startIndex]
		if (r.isEmpty()) return this to 1
		
		val value = subRouterMap[r]
		if (value != null) return value to 1
		
		val matchLength = route.size - startIndex
		val exactRoute = placeholderRouterList?.let { list ->
			list.binarySearch { matchLength - it.size }
		}
		if (exactRoute != null) return exactRoute to matchLength
		
		placeholderRouterList?.let { list ->
			list.forEach {
				val subRoute = it.getRoute(route, startIndex + it.size)
				if (subRoute != null) return subRoute to route.size - startIndex
			}
		}
		
		return wildSubRouter to 1
	}
	
	fun getRoute(route: List<String>, startIndex: Int = 0): SuspendRouteNode<T>? {
		var index = startIndex
		var routeNode = this
		while (index < route.size) {
			val (node, size) = routeNode[route, index]
			routeNode = node ?: return null
			index += size
		}
		return routeNode
	}
	
	suspend fun get(
		route: List<String>,
		startIndex: Int = 0,
		routeList: java.util.AbstractList<Pair<String, String>>
	): Pair<SuspendRouteNode<T>?, Int> {
		val r = route[startIndex]
		if (r.isEmpty()) {
			return this to 1
		}
		
		val value = subRouterMapLock.doRead { subRouterMap[r] }
		if (value != null) return value to 1
		
		val matchLength = route.size - startIndex
		val exactRoute = placeholderRouterListLock.doRead {
			placeholderRouterList?.binarySearch { matchLength - it.size }
		}
		if (exactRoute != null) {
			exactRoute.routeList.forEachIndexed { index, s ->
				if (s.isNotEmpty() && s[0] == ':') routeList.add(s.substring(1) to route[index])
			}
			return exactRoute to matchLength
		}
		
		val list = ArrayList<Pair<String, String>>()
		val detected = placeholderRouterListLock.doRead {
			placeholderRouterList?.let { routerList ->
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
						return@doRead subRoute to route.size - startIndex
					}
				}
			}
			null
		}
		if (detected != null) return detected
		
		for (i in startIndex until route.size)
			routeList.add("*" to route[i])
		return wildSubRouter to 1
	}
	
	suspend fun get(
		route: List<String>,
		routeList: java.util.AbstractList<Pair<String, String>>
	) = getRoute(route, 0, routeList)
	
	suspend fun getRoute(
		route: List<String>,
		startIndex: Int = 0,
		routeList: java.util.AbstractList<Pair<String, String>>
	): SuspendRouteNode<T>? {
		var index = startIndex
		var routeNode = this
		while (routeNode !is SuspendAnyRouteNode && index < route.size) {
			val (node, size) = routeNode.get(route, index, routeList)
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

private class SuspendPlaceholderRouteNode<T>(
	route: List<String>,
	private val startIndex: Int = 0,
	endIndex: Int = startIndex + route.matchLength(startIndex),
	value: T? = null,
	maxReadTime: Long
) : SuspendRouteNode<T>(route, endIndex - 1, value, maxReadTime) {
	override val placeholderRouterList: ArrayList<SuspendPlaceholderRouteNode<T>>?
		get() = null
	
	val size: Int = route.matchLength(startIndex, endIndex)
	
	override fun match(
		route: List<String>,
		startIndex: Int
	): Pair<Boolean, Int> =
		(size == route.matchLength(startIndex)) to size
	
	override val lastRoute: String
		get() {
			val sb = StringBuilder()
			for (i in startIndex..index) {
				sb.append("/")
				sb.append(routeList[i])
			}
			return sb.toString()
		}
	
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

private class SuspendAnyRouteNode<T>(
	route: List<String>,
	index: Int,
	value: T? = null,
	maxReadTime: Long
) : SuspendRouteNode<T>(route, index, value, maxReadTime) {
	override fun match(route: List<String>, startIndex: Int) = true to 1
}

fun main() = runBlocking {
	val router = SuspendRouter<Int>()
	router.set("/123", 1)
	router.set("/1234", 2)
	router.set("/abc/def", 3)
	println(router.suspendToString())
}