package cn.tursom.asynclock

import java.util.concurrent.locks.ReadWriteLock

class AsyncLockHashMap<K, V>(
	private val lock: AsyncLock
) {
	private val map = HashMap<K, V>()
	
	suspend fun set(key: K, value: V) {
		lock.doWrite {
			map[key] = value
		}
	}
	
	suspend fun get(key: K): V? {
		return lock.doRead { map[key] }
	}
	
	suspend fun remove(key: K) {
		lock.doWrite { map.remove(key) }
	}
	
	suspend fun contains(key: K): Boolean {
		return lock.doRead { map.contains(key) }
	}
	
	suspend fun forEach(action: (K, V) -> Unit) {
		lock.doRead {
			map.forEach { (k, v) ->
				action(k, v)
			}
		}
	}
}

fun <K, V> ReadWriteLockHashMap() = AsyncLockHashMap<K, V>(ReadWriteAsyncLock())
fun <K, V> WriteLockHashMap(maxReadTime: Long = 5) =
	AsyncLockHashMap<K, V>(WriteAsyncLock(maxReadTime))
fun <K, V> NormalLockHashMap(maxReadTime: Long = 5) =
	AsyncLockHashMap<K, V>(NormalAsyncLock(maxReadTime))