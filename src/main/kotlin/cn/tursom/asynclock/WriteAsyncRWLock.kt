package cn.tursom.asynclock

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * 读优化锁
 */
@Suppress("MemberVisibilityCanBePrivate")
class WriteAsyncRWLock(val maxReadOperatorTime: Long, val delayTime: Long = (maxReadOperatorTime shr 2) or 1) : AsyncRWLock {
	private val lock = AtomicBoolean(false)
	private val readNumber = AtomicInteger(0)
	private val writeNumber = AtomicInteger(0)
	
	override suspend fun <T> doRead(block: suspend () -> T): T {
		readNumber.incrementAndGet()
		lock.wait(delayTime)
		try {
			return block()
		} finally {
			readNumber.decrementAndGet()
		}
	}
	
	override suspend fun doWrite(block: suspend () -> Unit) {
		invoke(block)
	}
	
	override suspend fun sync(block: suspend () -> Unit) {
		invoke(block)
	}
	
	override suspend fun <T> invoke(block: suspend () -> T): T {
		readNumber.wait(delayTime)
		writeNumber.incrementAndGet()
		if (readNumber.get() != 0) delay(maxReadOperatorTime)
		lock.lock(delayTime)
		try {
			return block()
		} finally {
			lock.release()
			writeNumber.decrementAndGet()
		}
	}
	
	override suspend fun isLock(): Boolean {
		return lock.get()
	}
}