package cn.tursom.tools


interface AsyncIterator<out T> {
	suspend operator fun next(): T
	suspend operator fun hasNext(): Boolean
}

interface AsyncIterable<out T> {
	operator fun iterator(): AsyncIterator<T>
}

suspend inline infix fun <T> AsyncIterable<T>.forEach(action: (T) -> Unit) {
	for (element in this) action(element)
}

interface AsyncCollection<out E> : AsyncIterable<E> {
	val size: Int
	
	suspend fun isEmpty(): Boolean
	suspend infix fun contains(element: @UnsafeVariance E): Boolean
	suspend infix fun containsAll(elements: AsyncCollection<@UnsafeVariance E>): Boolean
}

interface AsyncSet<out E> : AsyncCollection<E>

interface AsyncMap<K, V> {
	val size: Int
	val entries: AsyncSet<Map.Entry<K, V>>
	val keys: AsyncSet<K>
	val values: AsyncCollection<V>
	
	suspend infix fun containsKey(key: K): Boolean
	suspend infix fun containsValue(value: V): Boolean
	suspend infix fun get(key: K): V?
	
	suspend fun isEmpty(): Boolean
}

operator fun <K, V> AsyncMap<out K, V>.iterator(): AsyncIterator<Map.Entry<K, V>> = entries.iterator()

interface AsyncPutableMap<K, V> : AsyncMap<K, V> {
	suspend fun clear()
	suspend fun put(key: K, value: V): V?
	suspend infix fun putAll(from: Map<out K, V>)
	suspend infix fun remove(key: K): V?
}

interface AsyncPutableSet<K> : AsyncSet<K> {
	suspend fun clear(): AsyncPutableSet<K>
	suspend fun put(key: K): AsyncPutableSet<K>
	suspend infix fun putAll(from: Set<K>): AsyncPutableSet<K>
	suspend infix fun remove(key: K): AsyncPutableSet<K>
}
