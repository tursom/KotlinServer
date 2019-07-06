package cn.tursom.asynclock

class AsyncRWLockAbstractMap<K, V>(
	private val lock: AsyncRWLock,
	private val map: java.util.AbstractMap<K, V> = HashMap()
) : AsyncRWLockMap<K, V>(lock, map) {
	
	constructor(lock: AsyncRWLock) : this(lock, HashMap())
	
	suspend fun set(key: K, value: V) {
		lock.doWrite {
			map[key] = value
		}
	}
	
	suspend fun remove(key: K) {
		lock.doWrite { map.remove(key) }
	}
}

fun <K, V> ReadWriteLockHashMap() = AsyncRWLockAbstractMap<K, V>(AsyncWriteFirstRWLock())
fun <K, V> WriteLockHashMap(maxReadTime: Long = 5) =
	AsyncRWLockAbstractMap<K, V>(AsyncReadFirstRWLock(maxReadTime))
