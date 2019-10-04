package cn.tursom.utils.datastruct

interface SimpMap<K, V> : Map<K, V> {
	infix fun remove(key: K): V?

	/**
	 * @return prev value
	 */
	operator fun set(key: K, value: V)

	fun setAndGet(key: K, value: V): V? {
		val prev = get(key)
		set(key, value)
		return prev
	}

	/**
	 * 清空整个表
	 */
	fun clear()

	fun first(): V?

	infix fun putAll(from: Map<out K, V>) {
		from.forEach { (k, u) ->
			set(k, u)
		}
	}
}

