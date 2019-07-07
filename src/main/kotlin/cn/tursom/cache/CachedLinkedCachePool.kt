package cn.tursom.cache

class CachedLinkedCachePool<T> : CachePool<T> {
	private var rootNode: Node<T>? = null
	private var cacheNode: Node<T>? = null
	
	override fun put(cache: T): Boolean {
		synchronized(this) {
			rootNode = if (cacheNode != null) {
				val node = cacheNode!!
				cacheNode = node.next
				node.next = rootNode
				rootNode = node
				node
			} else {
				Node(cache, rootNode)
			}
		}
		return true
	}
	
	override fun get(): T? {
		synchronized(this) {
			val node = rootNode
			rootNode = rootNode?.next
			val value = node?.value
			node?.value = null
			node?.next = cacheNode
			cacheNode = node
			return value
		}
	}
	
	private class Node<T>(var value: T? = null, var next: Node<T>? = null)
}