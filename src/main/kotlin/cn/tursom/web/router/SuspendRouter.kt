package cn.tursom.web.router

import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

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

				r == "*" -> routeNode.wildSubRouter ?: run {
					val node = SuspendAnyRouteNode<T>(routeList, index, null, maxReadTime)
					routeNode.wildSubRouter = node
					index = routeList.size - 1
					node
				}

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
		val endIndex = route.indexOf('?')
		return rootNode.get(
			(if (endIndex < 0) route else route.substring(0, endIndex))
				.split('/').filter { it.isNotEmpty() },
			list
		)?.value to list
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

