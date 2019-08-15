package cn.tursom.web.router

interface RouterNode<T> {
	val value: T?
	
	fun forEach(action: (node: RouterNode<T>) -> Unit)
}