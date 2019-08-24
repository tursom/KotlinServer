package cn.tursom.utils.cache.cachepool

import cn.tursom.utils.cache.interfaces.CachePool

class LinkedCachePool<T> : CachePool<T> {
	@Volatile
	private var rootNode: Node<T>? = null
	
	override fun put(cache: T): Boolean {
		synchronized(this) {
			rootNode = Node(cache, rootNode)
		}
		return true
	}
	
	override fun get(): T? {
		synchronized(this) {
			val node = rootNode
			rootNode = rootNode?.next
			return node?.value
		}
	}
	
	private class Node<T>(var value: T? = null, var next: Node<T>? = null)
}