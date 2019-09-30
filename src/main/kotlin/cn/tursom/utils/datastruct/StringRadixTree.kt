package cn.tursom.utils.datastruct

/**
 * 基数树，查找优化
 */
class StringRadixTree<T> {
	private val root = Node<T>()

	operator fun set(route: String, value: T) {
		val context = Context(route)
		var node: Node<T>? = root
		var prev: Node<T> = root
		while (node != null) {
			var nodeLocation = 0
			while (nodeLocation < node.length && !context.end) {
				if (node[nodeLocation] != context.peek) {
					insert(node, nodeLocation, context, value)
					return
				}
				context.add
				nodeLocation++
			}
			if (context.end) {
				if (nodeLocation == node.length) {
					node.value = value
				} else {
					branchNode(node, nodeLocation, context, value)
				}
				return
			}
			prev = node
			node = node.subNodes[context.peek]
		}
		insert(prev, prev.length, context, value)
	}

	private fun branchNode(node: Node<T>, nodeLocation: Int, context: Context, value: T) {
		val subNode = Node(node.str.substring(nodeLocation, node.str.length), node.value)
		subNode.subNodes = node.subNodes
		node.subNodes = ArrayMap(2)
		node.subNodes[node[nodeLocation]] = subNode
		node.str = node.str.substring(0, nodeLocation)
		if (context.end) {
			node.value = value
		} else {
			node.value = null
			node.subNodes[context.peek] = Node(context.remains, value)
		}
	}

	private fun insert(node: Node<T>, nodeLocation: Int, context: Context, value: T) {
		if (node.value == null) {
			if (node.subNodes.isEmpty()) {
				node.value = value
				node.str = context.remains
				return
			} else {
				if (node.str.isEmpty()) {
					node.subNodes[context.peek] = Node(context.remains, value)
					return
				} else if (node.str == context.remains) {
					node.value = value
					return
				}
			}
		}
		branchNode(node, nodeLocation, context, value)
	}

	operator fun get(route: String): T? {
		val context = Context(route)
		var node: Node<T>? = root
		while (node != null) {
			var nodeLocation = 0
			while (nodeLocation < node.length) {
				if (node[nodeLocation] != context.get) return null
				nodeLocation++
			}
			if (context.end) return node.value
			node = node.subNodes[context.peek]
		}
		return null
	}


	private fun toString(node: Node<T>, stringBuilder: StringBuilder, indentation: String) {
		if (indentation.isEmpty()) {
			stringBuilder.append("\"${node.str.replace("\"", "\"\"")}\": ${node.value}\n")
			node.subNodes.forEach { subNode ->
				toString(subNode.value, stringBuilder, " ")
			}
		} else {
			stringBuilder.append("$indentation|- \"${node.str.replace("\"", "\"\"")}\": ${node.value}\n")
			node.subNodes.forEach { subNode ->
				toString(subNode.value, stringBuilder, "$indentation|  ")
			}
		}
	}

	override fun toString(): String {
		val stringBuilder = StringBuilder()
		toString(root, stringBuilder, "")
		if (stringBuilder.isNotEmpty()) stringBuilder.deleteCharAt(stringBuilder.length - 1)
		return stringBuilder.toString()
	}

	data class Node<T>(var str: String = "", var value: T? = null, var subNodes: ArrayMap<Char, Node<T>> = ArrayMap(0)) {
		val length get() = str.length
		operator fun get(index: Int) = str[index]
	}

	data class Context(val route: String, var location: Int = 0) {
		val peek get() = route[location]
		val get get() = route[location++]
		val add get() = location++
		val end get() = location == route.length
		val remains get() = route.substring(location, route.length)

		fun reset() {
			location = 0
		}
	}
}
