package cn.tursom.utils.asynclock

import cn.tursom.socket.client.AsyncClient
import cn.tursom.socket.recvStr
import cn.tursom.socket.send
import cn.tursom.socket.server.AsyncSocketServer
import cn.tursom.utils.cache.AsyncMemoryPool
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AsyncMutexLock : AsyncLock {
	private val lock = AtomicBoolean(false)
	@Volatile
	private var lockList: LockNode? = null
	private val listLock = AsyncLoopLock()
	
	suspend fun wait(delayTime: Long) {
		lock.wait(delayTime)
	}
	
	override suspend fun sync(block: suspend () -> Unit) {
		invoke(block)
	}
	
	override suspend fun isLock(): Boolean {
		return lock.get()
	}
	
	override suspend operator fun <T> invoke(block: suspend () -> T): T {
		lock.lock()
		try {
			return block()
		} finally {
			lock.release()
		}
	}
	
	private suspend fun AtomicBoolean.lock() {
		if (compareAndSet(false, true)) return
		listLock {
			suspendCoroutine<Int> { cont ->
				lockList = LockNode(cont, lockList)
			}
		}
	}
	
	private suspend fun AtomicBoolean.release() {
		if (lockList != null) listLock {
			val node = lockList!!
			lockList = node.next
			node.cont.resume(0)
		} else {
			set(false)
		}
	}
	
	data class LockNode(val cont: Continuation<Int>, val next: LockNode? = null)
}