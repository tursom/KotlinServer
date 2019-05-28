package cn.tursom.asynclock

import java.util.concurrent.atomic.AtomicBoolean

class MutexAsyncRWLock(val delayTime: Long) : AsyncLock {
	private val lock = AtomicBoolean(false)
	
	override suspend fun sync(block: suspend () -> Unit) {
		invoke(block)
	}
	
	override suspend fun isLock(): Boolean {
		return lock.get()
	}
	
	override suspend operator fun <T> invoke(block: suspend () -> T): T {
		lock.lock(delayTime)
		try {
			return block()
		} finally {
			lock.release()
		}
	}
}