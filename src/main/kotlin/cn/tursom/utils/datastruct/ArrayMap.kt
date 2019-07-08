package cn.tursom.utils.datastruct

class ArrayMap<K : Comparable<K>, V> : Map<K, V> {
	@Volatile
	private var arr: Array<Node<K, V>?> = Array(4) { null }
	@Volatile
	private var end = 0
	
	override val size: Int
		get() = end
	override val entries: Set<Map.Entry<K, V>> = EntrySet(this)
	override val keys: Set<K> = KeySet(this)
	override val values: Collection<V> = ValueCollection(this)
	
	fun clear() {
		end = 0
	}
	
	operator fun set(key: K, value: V): V? {
		val index = search(key)
		if (index < 0) insert(key, value, -index - 1)
		else {
			val node = arr[index]
			if (node != null) node.value = value
			else arr[index] = Node(key, value)
		}
		return value
	}
	
	infix fun putAll(from: Map<out K, V>) {
		from.forEach { (key, value) ->
			val index = search(key)
			if (index < 0) arr[end++] = Node(key, value)
			else {
				val node = arr[index]
				if (node != null) node.value = value
				else arr[index] = Node(key, value)
			}
		}
		arr.sort()
	}
	
	infix fun remove(key: K): V? {
		val index = search(key)
		if (index >= 0) {
			val oldNode = arr[index]
			for (i in index until end) {
				arr[i] = arr[i + 1]
			}
			return oldNode?.value
		}
		return null
	}
	
	override infix fun containsKey(key: K): Boolean {
		return search(key) >= 0
	}
	
	override infix fun containsValue(value: V): Boolean {
		arr.forEach {
			if (it?.value == value) return true
		}
		return false
	}
	
	override infix operator fun get(key: K): V? {
		val index = search(key)
		return if (index < 0) null
		else arr[index]?.value
	}
	
	override fun isEmpty(): Boolean {
		return end == 0
	}
	
	override fun toString(): String {
		if (end == 0) return "{}"
		val sb = StringBuilder("{${arr[0]}")
		for (i in 1 until end) {
			sb.append(", ")
			sb.append(arr[i])
		}
		sb.append("}")
		return sb.toString()
	}
	
	private fun resize() {
		val oldArr = arr
		arr = Array(arr.size * 2) { null }
		oldArr.forEachIndexed { i, v ->
			arr[i] = v
		}
	}
	
	private fun insert(key: K, value: V, index: Int): V? {
		if (end == arr.size) resize()
		for (i in end++ until index) {
			arr[i] = arr[i - 1]
		}
		arr[index] = Node(key, value)
		return value
	}
	
	private fun search(key: K): Int {
		if (end == 0) return -1
		return arr.binarySearch(key, 0, end)
	}
	
	class Node<K : Comparable<K>, V>(
		override val key: K,
		@Volatile override var value: V
	) : Comparable<K>, Map.Entry<K, V> {
		override fun compareTo(other: K): Int {
			return key.compareTo(other)
		}
		
		override fun toString(): String {
			return "$key=$value"
		}
	}
	
	
	class EntrySet<K : Comparable<K>, V>(private val map: ArrayMap<K, V>) : Set<Map.Entry<K, V>> {
		override val size: Int
			get() = map.size
		
		override fun contains(element: Map.Entry<K, V>): Boolean {
			return map[element.key] == element.value
		}
		
		override fun containsAll(elements: Collection<Map.Entry<K, V>>): Boolean {
			elements.forEach {
				if (!contains(it)) return false
			}
			return true
		}
		
		override fun isEmpty(): Boolean {
			return map.isEmpty()
		}
		
		override fun iterator(): Iterator<Map.Entry<K, V>> {
			return MapIterator(map)
		}
	}
	
	class MapIterator<K : Comparable<K>, V>(private val map: ArrayMap<K, V>) : Iterator<Map.Entry<K, V>> {
		private var index = 0
		
		override fun hasNext(): Boolean {
			while (index <= map.end && map.arr[index++] == null);
			index--
			return index < map.end
		}
		
		override fun next(): Map.Entry<K, V> {
			return map.arr[index++]!!
		}
	}
	
	class KeySet<K : Comparable<K>>(private val map: ArrayMap<K, *>) : Set<K> {
		override val size: Int
			get() = map.size
		
		override fun contains(element: K): Boolean {
			return map.containsKey(element)
		}
		
		override fun containsAll(elements: Collection<K>): Boolean {
			elements.forEach {
				if (!map.containsKey(it)) return false
			}
			return true
		}
		
		override fun isEmpty(): Boolean {
			return size == 0
		}
		
		override fun iterator(): Iterator<K> {
			return KeyIterator(map)
		}
	}
	
	class KeyIterator<K : Comparable<K>>(map: ArrayMap<K, *>) : Iterator<K> {
		private val iterator = map.iterator()
		override fun hasNext(): Boolean {
			return iterator.hasNext()
		}
		
		override fun next(): K {
			return iterator.next().key
		}
	}
	
	class ValueCollection<V>(private val map: ArrayMap<*, V>) : Collection<V> {
		override val size: Int
			get() = map.size
		
		override fun contains(element: V): Boolean {
			return map.containsValue(element)
		}
		
		override fun containsAll(elements: Collection<V>): Boolean {
			elements.forEach {
				if (!map.containsValue(it)) return false
			}
			return true
		}
		
		override fun isEmpty(): Boolean {
			return size == 0
		}
		
		override fun iterator(): Iterator<V> {
			return ValueIterator(map)
		}
	}
	
	class ValueIterator<V>(map: ArrayMap<*, V>) : Iterator<V> {
		private val iterator = map.iterator()
		
		override fun hasNext(): Boolean {
			return iterator.hasNext()
		}
		
		override fun next(): V {
			return iterator.next().value
		}
	}
}