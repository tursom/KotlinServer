package cn.tursom.cache

import cn.tursom.asynclock.AsyncMutexLock

class AsyncCachedLinkedCachePool<T> : AsyncCachePool<T> {
	private var rootNode: Node<T>? = null
	private var cacheNode: Node<T>? = null
	private val lock = AsyncMutexLock()
	
	override suspend fun put(cache: T): Boolean = lock {
		rootNode = if (cacheNode != null) {
			val node = cacheNode!!
			cacheNode = node.next
			node.next = rootNode
			rootNode = node
			node
		} else {
			Node(cache, rootNode)
		}
		true
	}
	
	override suspend fun get(): T? = lock {
		val node = rootNode
		rootNode = rootNode?.next
		val value = node?.value
		node?.value = null
		node?.next = cacheNode
		cacheNode = node
		value
	}
	
	private class Node<T>(var value: T? = null, var next: Node<T>? = null)
}
