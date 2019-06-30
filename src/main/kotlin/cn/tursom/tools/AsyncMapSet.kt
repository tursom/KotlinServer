package cn.tursom.tools


class AsyncMapSet<K>(private val map: AsyncMap<K, *>) : AsyncSet<K> {
	override val size: Int
		get() = map.size
	
	override fun iterator(): AsyncIterator<K> {
		return SetIterator(map)
	}
	
	override suspend fun isEmpty(): Boolean {
		return map.isEmpty()
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
	
	class SetIterator<K>(map: AsyncMap<K, *>) : AsyncIterator<K> {
		private val iterator = map.iterator()
		override suspend fun next(): K {
			return iterator.next().key
		}
		
		override suspend fun hasNext(): Boolean {
			return iterator.hasNext()
		}
	}
}

val <K> AsyncMap<K, *>.keySet
	get() = AsyncMapSet(this)