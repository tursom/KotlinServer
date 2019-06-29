package cn.tursom.tools

class AsyncHashMap<K, V>(
	internal val loadFactor: Float = 0.75f
) : Cloneable, java.io.Serializable {
	@Transient
	private var _size: Int = 16
	@Transient
	private var table: Array<Node<K, V>?> = Array(_size) { null }
	@Transient
	internal var modCount: Int = 0
	internal var threshold: Int = 0
	
	val size: Int
		get() = _size
	val entries: MutableSet<MutableMap.MutableEntry<K, V>>
		get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
	val keys: MutableSet<K>
		get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
	val values: MutableCollection<V>
		get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
	
	suspend fun clear() {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
	
	suspend fun put(key: K, value: V): V? {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
	
	suspend infix fun putAll(from: Map<out K, V>) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
	
	suspend infix fun remove(key: K): V? {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
	
	suspend infix fun containsKey(key: K): Boolean {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
	
	suspend infix fun containsValue(value: V): Boolean {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
	
	suspend infix fun get(key: K): V? {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
	
	suspend fun isEmpty(): Boolean {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
	
	private data class Node<K, V>(
		val key: K,
		var value: V,
		var next: Node<K, V>?,
		var prev: Node<K, V>? = null
	) : Iterable<Node<K, V>> {
		override fun iterator(): Iterator<Node<K, V>> {
			return NodeIterator(this)
		}
	}
	
	private class NodeIterator<K, V>(private var node: Node<K, V>?) : Iterator<Node<K, V>> {
		
		override fun hasNext(): Boolean {
			return node != null
		}
		
		override fun next(): Node<K, V> {
			val thisNode = node!!
			node = node?.next
			return thisNode
		}
	}
}