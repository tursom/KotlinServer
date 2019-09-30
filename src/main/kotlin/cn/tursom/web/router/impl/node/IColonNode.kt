package cn.tursom.web.router.impl.node

interface IColonNode<T> {
	val value: T?
	
	fun forEach(action: (node: IColonNode<T>) -> Unit)
}