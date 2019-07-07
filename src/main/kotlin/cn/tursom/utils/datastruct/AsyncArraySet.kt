package cn.tursom.utils.datastruct

/**
 * map require iterator functioncv
 */
class AsyncArraySet<K : Comparable<K>>(private val map: AsyncPutableMap<K, Unit> = AsyncArrayMap()) : AsyncPutableSet<K> {
	
	override val size: Int
		get() = map.size
	
	override fun iterator(): AsyncIterator<K> {
		return SetIterator(map)
	}
	
	override suspend fun isEmpty(): Boolean {
		return size == 0
	}
	
	override suspend fun contains(element: K): Boolean {
		return map.containsKey(element)
	}
	
	override suspend fun containsAll(elements: AsyncCollection<K>): Boolean {
		elements.forEach {
			if (!map.containsKey(it)) return false
		}
		return true
	}
	
	override suspend fun clear(): AsyncPutableSet<K> {
		map.clear()
		return this
	}
	
	override suspend fun put(key: K): AsyncPutableSet<K> {
		map.put(key, Unit)
		return this
	}
	
	override suspend fun putAll(from: Set<K>): AsyncPutableSet<K> {
		map.putAll(SetMap(from))
		return this
	}
	
	override suspend fun remove(key: K): AsyncPutableSet<K> {
		map.remove(key)
		return this
	}
	
	class SetIterator<K : Comparable<K>>(map: AsyncMap<K, *>) : AsyncIterator<K> {
		val iterator = map.iterator()
		override suspend fun next(): K {
			return iterator.next().key
		}
		
		override suspend fun hasNext(): Boolean {
			return iterator.hasNext()
		}
	}
}

class SetMap<K>(private val set: Set<K>) : Map<K, Unit> {
	
	override val keys: Set<K>
		get() = set
	override val size: Int
		get() = set.size
	override val values: Collection<Unit> = listOf()
	
	override fun containsKey(key: K): Boolean {
		return set.contains(key)
	}
	
	override fun containsValue(value: Unit): Boolean {
		return true
	}
	
	override fun get(key: K): Unit? {
		return if (set.contains(key)) Unit else null
	}
	
	override fun isEmpty(): Boolean {
		return size == 0
	}
	
	override val entries: Set<Map.Entry<K, Unit>> = object : Set<Map.Entry<K, Unit>> {
		override val size: Int
			get() = set.size
		
		override fun contains(element: Map.Entry<K, Unit>): Boolean {
			return set.contains(element.key)
		}
		
		override fun containsAll(elements: Collection<Map.Entry<K, Unit>>): Boolean {
			elements.forEach {
				if (!set.contains(it.key)) return false
			}
			return true
		}
		
		override fun isEmpty(): Boolean {
			return size == 0
		}
		
		override fun iterator(): Iterator<Map.Entry<K, Unit>> {
			return SetMapIterator(set)
		}
	}
	
	class SetMapIterator<K>(set: Set<K>) : Iterator<Map.Entry<K, Unit>> {
		private val iterator = set.iterator()
		override fun hasNext(): Boolean {
			return iterator.hasNext()
		}
		
		override fun next(): Map.Entry<K, Unit> {
			return Entry(iterator.next())
		}
	}
	
	data class Entry<K>(override val key: K) : Map.Entry<K, Unit> {
		override val value: Unit
			get() = Unit
	}
}