package cn.tursom.web.router.suspend

interface SuspendColonStarNode<T> {
	val value: T?
	val lastRoute: String
	val fullRoute: String
	val empty: Boolean
	
	suspend fun forEach(action: suspend (node: SuspendColonStarNode<T>) -> Unit)
}