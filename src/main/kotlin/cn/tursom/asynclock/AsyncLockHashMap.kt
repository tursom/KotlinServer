package cn.tursom.asynclock

class AsyncLockHashMap<K, V>(
	private val lock: AsyncLock,
	private val map: java.util.AbstractMap<K, V> = HashMap()
) : AsyncLockMap<K, V>(lock, map) {
	
	suspend fun set(key: K, value: V) {
		lock.doWrite {
			map[key] = value
		}
	}
	
	suspend fun remove(key: K) {
		lock.doWrite { map.remove(key) }
	}
}

fun <K, V> ReadWriteLockHashMap() = AsyncLockHashMap<K, V>(ReadWriteAsyncLock())
fun <K, V> WriteLockHashMap(maxReadTime: Long = 5) =
	AsyncLockHashMap<K, V>(WriteAsyncLock(maxReadTime))

fun <K, V> NormalLockHashMap(maxReadTime: Long = 5) =
	AsyncLockHashMap<K, V>(NormalAsyncLock(maxReadTime))