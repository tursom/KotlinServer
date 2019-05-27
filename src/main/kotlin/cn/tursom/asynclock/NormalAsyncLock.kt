package cn.tursom.asynclock

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean

class NormalAsyncLock(val delayTime: Long) : AsyncLock {
	private val lock = AtomicBoolean(false)
	
	private suspend fun AtomicBoolean.lock() {
		// 如果得不到锁，先自旋20次
		var maxLoopTime = 20
		while (maxLoopTime-- > 0) {
			if (compareAndSet(false, true)) return
		}
		while (!compareAndSet(false, true)) {
			delay(delayTime)
		}
	}
	
	private fun AtomicBoolean.release() {
		set(false)
	}
	
	override suspend fun <T> doRead(block: suspend () -> T): T {
		lock.lock()
		val ret = block()
		lock.release()
		return ret
	}
	
	override suspend fun doWrite(block: suspend () -> Unit) {
		lock.lock()
		block()
		lock.release()
	}
	
	suspend operator fun <T> invoke(block: suspend () -> T): T {
		return doRead(block)
	}
}