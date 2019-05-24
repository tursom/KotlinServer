package cn.tursom.web.router

import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun <T> List<T>.binarySearch(comparison: (T) -> Int): T? {
	val index = binarySearch(0, size, comparison)
	return if (index < 0) null
	else get(index)
}

@Suppress("unused", "unused", "MemberVisibilityCanBePrivate", "UNUSED_PARAMETER")
class SuspendRouter<T> {
	private val rootNode = SuspendRouteNode<T>(listOf(""), 0)
	private val threadPool = Executors.newSingleThreadExecutor()
	
	@Volatile
	private var lastChangeTime: Long = System.currentTimeMillis()
	@Volatile
	private var strBuf: String = ""
	@Volatile
	private var strBufTime: Long = 0
	
	private fun setSubRoute(
		route: String,
		value: T?,
		onDestroy: ((oldValue: T) -> Unit)? = null
	) {
		val routeList = route.split('?')[0].split('/').drop(0)
		var routeNode = rootNode
		var r: String
		var index = 0
		while (index < routeList.size) {
			r = routeList[index]
			routeNode = when {
				r.isEmpty() -> routeNode
				
				r == "*" -> routeNode.wildSubRouter ?: {
					val node = SuspendAnyRouteNode<T>(routeList, index)
					routeNode.wildSubRouter = node
					index = routeList.size - 1
					node
				}()
				
				r[0] == ':' -> run {
					val matchLength = SuspendPlaceholderRouteNode.matchLength(routeList, index)
					val node = routeNode.placeholderRouterList!!.binarySearch { it.size - matchLength } ?: {
						routeNode.addNode(routeList, index, null)
						routeNode.placeholderRouterList!!.binarySearch { it.size - matchLength }!!
					}()
					index += node.size - 1
					node
				}
				
				else -> routeNode.subRouterMap[r] ?: {
					val node = SuspendRouteNode<T>(routeList, index)
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
		lastChangeTime = System.currentTimeMillis()
	}
	
	fun delRoute(route: String, onDestroy: ((oldValue: T) -> Unit)? = null) {
		this[route, onDestroy] = null
	}
	
	fun set(
		route: String,
		value: T?,
		onDestroy: ((oldValue: T) -> Unit)? = null
	) = threadPool.execute { setSubRoute(route, value, onDestroy) }
	
	operator fun set(
		route: String,
		onDestroy: ((oldValue: T) -> Unit)? = null,
		value: T?
	) = threadPool.execute { setSubRoute(route, value, onDestroy) }
	
	suspend fun get(route: String): Pair<T?, List<Pair<String, String>>> {
		val list = ArrayList<Pair<String, String>>()
		return suspendCoroutine { cont ->
			threadPool.execute {
				cont.resume(rootNode[route.split('?')[0].split('/'), list]?.value to list)
			}
		}
	}
	
	private fun toString(node: SuspendRouteNode<T>, stringBuilder: StringBuilder, indentation: String) {
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
		
		if (node is SuspendAnyRouteNode) return
		
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
	
	suspend fun suspendToString(): String {
		if (strBufTime < lastChangeTime) {
			val stringBuilder = StringBuilder()
			suspendCoroutine<Int> { cont ->
				threadPool.execute {
					toString(rootNode, stringBuilder, "")
					strBuf = stringBuilder.toString()
					strBufTime = System.currentTimeMillis()
					cont.resume(0)
				}
			}
		}
		return strBuf
	}
	
	override fun toString(): String {
		if (strBufTime < lastChangeTime) {
			val stringBuilder = StringBuilder()
			val job = threadPool.submit {
				toString(rootNode, stringBuilder, "")
			}
			job.get()
			strBuf = stringBuilder.toString()
			strBufTime = System.currentTimeMillis()
		}
		return strBuf
	}
}

@Suppress("MemberVisibilityCanBePrivate")
open class SuspendRouteNode<T>(
	var routeList: List<String>,
	var index: Int,
	var value: T? = null
) {
	val route: String = routeList[index]
	var wildSubRouter: SuspendAnyRouteNode<T>? = null
	open val placeholderRouterList: ArrayList<SuspendPlaceholderRouteNode<T>>? = ArrayList(0)
	val subRouterMap = HashMap<String, SuspendRouteNode<T>>(0)
	
	open val singleRoute
		get() = "/$route"
	
	open fun match(
		route: List<String>,
		startIndex: Int
	): Pair<Boolean, Int> = (route.size > startIndex && route[startIndex] == this.route) to 1
	
	fun addNode(route: List<String>, startIndex: Int, value: T? = null): Int {
		val r = route[startIndex]
		return when {
			r.isEmpty() -> return addNode(route, startIndex + 1)
			r == "*" -> {
				wildSubRouter = SuspendAnyRouteNode(route, startIndex)
				1
			}
			r[0] == ':' -> {
				val node = SuspendPlaceholderRouteNode(route, startIndex, value = value)
				// 必须保证 placeholderRouterList 存在，而且还不能有这个长度的节点
				if (placeholderRouterList!!.binarySearch { it.size - node.size } != null) {
					throw Exception()
				}
				placeholderRouterList?.add(node)
				placeholderRouterList?.sortBy { it.size }
				node.size
			}
			else -> {
				subRouterMap[r] = SuspendRouteNode(route, startIndex, value)
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
	
	operator fun get(
		route: List<String>,
		startIndex: Int = 0,
		routeList: java.util.AbstractList<Pair<String, String>>
	): Pair<SuspendRouteNode<T>?, Int> {
		val r = route[startIndex]
		if (r.isEmpty()) {
			return this to 1
		}
		
		val value = subRouterMap[r]
		if (value != null) return value to 1
		
		val matchLength = route.size - startIndex
		val exactRoute = placeholderRouterList?.let { list ->
			list.binarySearch { matchLength - it.size }
		}
		if (exactRoute != null) {
			exactRoute.routeList.forEachIndexed { index, s ->
				if (s.isNotEmpty() && s[0] == ':') routeList.add(s.substring(1) to route[index])
			}
			return exactRoute to matchLength
		}
		
		val list = ArrayList<Pair<String, String>>()
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
					return subRoute to route.size - startIndex
				}
			}
		}
		
		routeList.add("*" to route[startIndex])
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
	): SuspendRouteNode<T>? {
		var index = startIndex
		var routeNode = this
		while (routeNode !is SuspendAnyRouteNode && index < route.size) {
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

class SuspendPlaceholderRouteNode<T>(
	route: List<String>,
	private val startIndex: Int = 0,
	endIndex: Int = startIndex + route.matchLength(startIndex),
	value: T? = null
) : SuspendRouteNode<T>(route, endIndex - 1, value) {
	override val placeholderRouterList: ArrayList<SuspendPlaceholderRouteNode<T>>?
		get() = null
	
	val size: Int = route.matchLength(startIndex, endIndex)
	
	override fun match(
		route: List<String>,
		startIndex: Int
	): Pair<Boolean, Int> =
		(size == route.matchLength(startIndex)) to size
	
	override val singleRoute: String
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

class SuspendAnyRouteNode<T>(
	route: List<String>,
	index: Int,
	value: T? = null
) : SuspendRouteNode<T>(route, index, value) {
	override fun match(route: List<String>, startIndex: Int) = true to 1
}

fun main() = runBlocking {
	val router = SuspendRouter<Int>()
	router.set("/123", 1)
	router.set("/1234", 2)
	router["/abc/def"] = 3
	println(router.suspendToString())
}