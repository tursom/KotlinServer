package cn.tursom.cache

import cn.tursom.tools.CurrentBitSet

interface ListCache<T> {
	fun put(cache: T): Boolean
	fun get(): T?
}

class NoCacheException : Exception()

fun <T> ListCache<T>.forceGet(): T {
	return get() ?: throw NoCacheException()
}

class ArrayListCache<T> : ListCache<T> {
	private val bitSet = CurrentBitSet(64)
	private var poll = Array<Any?>(bitSet.size.toInt()) { null }
	
	override fun put(cache: T): Boolean {
		val index = synchronized(bitSet) {
			val index = getDown()
			bitSet.up(index)
			index
		}.toInt()
		poll[index] = cache
		return true
	}
	
	override fun get(): T? {
		synchronized(bitSet) {
			val index = bitSet.firstUp()
			return if (index < 0) null
			else {
				@Suppress("UNCHECKED_CAST")
				val ret = poll[index.toInt()] as T
				bitSet.down(index)
				ret
			}
		}
	}
	
	private fun getDown(): Long {
		val index = bitSet.firstDown()
		return if (index < 0) {
			resize()
			bitSet.firstDown()
		} else {
			index
		}
	}
	
	private fun resize() {
		bitSet.resize(bitSet.size * 2)
	}
}
