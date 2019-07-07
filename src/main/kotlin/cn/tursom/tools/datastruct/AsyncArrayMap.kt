package cn.tursom.tools.datastruct

import cn.tursom.asynclock.AsyncMutexLock
import cn.tursom.asynclock.AsyncWriteFirstRWLock

class AsyncArrayMap<K : Comparable<K>, V> : AsyncPutableMap<K, V> {
	private val lock = AsyncWriteFirstRWLock()
	private val map = ArrayMap<K, V>()
	
	override val size: Int
		get() = map.size
	override val entries: AsyncSet<Map.Entry<K, V>> = AsyncEntrySet(this)
	override val keys: AsyncSet<K> = AsyncKeySet(this)
	override val values: AsyncCollection<V> = AsyncValueCollection(this)
	
	override suspend fun containsKey(key: K): Boolean {
		return lock.doRead { map.containsKey(key) }
	}
	
	override suspend fun containsValue(value: V): Boolean {
		return lock.doRead { map.containsValue(value) }
	}
	
	override suspend fun get(key: K): V? {
		return lock.doRead { map[key] }
	}
	
	override suspend fun isEmpty(): Boolean {
		return lock.doRead { map.isEmpty() }
	}
	
	override suspend fun clear() {
		lock.doWrite { map.clear() }
	}
	
	override suspend fun put(key: K, value: V): V? {
		return lock { map.set(key, value) }
	}
	
	override suspend fun putAll(from: Map<out K, V>) {
		return lock { map.putAll(from) }
	}
	
	override suspend fun remove(key: K): V? {
		return lock { map.remove(key) }
	}
	
	
	class AsyncEntrySet<K : Comparable<K>, V>(private val map: AsyncArrayMap<K, V>) : AsyncSet<Map.Entry<K, V>> {
		override val size: Int
			get() = map.size
		
		override suspend fun contains(element: Map.Entry<K, V>): Boolean {
			return map.get(element.key) == element.value
		}
		
		override suspend fun containsAll(elements: AsyncCollection<Map.Entry<K, V>>): Boolean {
			elements.forEach {
				if (!contains(it)) return false
			}
			return true
		}
		
		override suspend fun isEmpty(): Boolean {
			return map.isEmpty()
		}
		
		override fun iterator(): AsyncIterator<Map.Entry<K, V>> {
			return MapIterator(map)
		}
	}
	
	class MapIterator<K : Comparable<K>, V>(map: AsyncArrayMap<K, V>) : AsyncIterator<Map.Entry<K, V>> {
		private val iterator = map.map.iterator()
		private val lock = AsyncMutexLock(5)
		
		override suspend fun hasNext(): Boolean {
			return lock { iterator.hasNext() }
		}
		
		override suspend fun next(): Map.Entry<K, V> {
			return lock { iterator.next() }
		}
	}
	
	class AsyncKeySet<K : Comparable<K>>(private val map: AsyncArrayMap<K, *>) : AsyncSet<K> {
		override val size: Int
			get() = map.size
		
		override suspend fun contains(element: K): Boolean {
			return map.containsKey(element)
		}
		
		override suspend fun containsAll(elements: AsyncCollection<K>): Boolean {
			elements.forEach {
				if (!map.containsKey(it)) return false
			}
			return true
		}
		
		override suspend fun isEmpty(): Boolean {
			return size == 0
		}
		
		override fun iterator(): AsyncIterator<K> {
			return KeyIterator(map)
		}
	}
	
	class KeyIterator<K : Comparable<K>>(map: AsyncArrayMap<K, *>) : AsyncIterator<K> {
		private val iterator = map.iterator()
		override suspend fun hasNext(): Boolean {
			return iterator.hasNext()
		}
		
		override suspend fun next(): K {
			return iterator.next().key
		}
	}
	
	class AsyncValueCollection<V>(private val map: AsyncArrayMap<*, V>) : AsyncCollection<V> {
		override val size: Int
			get() = map.size
		
		override suspend fun contains(element: V): Boolean {
			return map.containsValue(element)
		}
		
		override suspend fun containsAll(elements: AsyncCollection<V>): Boolean {
			elements.forEach {
				if (!map.containsValue(it)) return false
			}
			return true
		}
		
		override suspend fun isEmpty(): Boolean {
			return size == 0
		}
		
		override fun iterator(): AsyncIterator<V> {
			return ValueIterator(map)
		}
	}
	
	class ValueIterator<V>(map: AsyncArrayMap<*, V>) : AsyncIterator<V> {
		private val iterator = map.iterator()
		
		override suspend fun hasNext(): Boolean {
			return iterator.hasNext()
		}
		
		override suspend fun next(): V {
			return iterator.next().value
		}
	}
}