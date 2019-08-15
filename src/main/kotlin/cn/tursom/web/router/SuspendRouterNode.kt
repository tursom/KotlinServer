package cn.tursom.web.router

interface SuspendRouterNode<T> {
	val value: T?
	val lastRoute: String
	val fullRoute: String
	val empty: Boolean
	
	suspend fun forEach(action: suspend (node: SuspendRouterNode<T>) -> Unit)
}