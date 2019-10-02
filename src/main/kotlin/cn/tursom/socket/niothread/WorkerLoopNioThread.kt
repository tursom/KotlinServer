package cn.tursom.socket.niothread

import java.nio.channels.Selector
import java.util.concurrent.Callable
import java.util.concurrent.LinkedBlockingDeque

@Suppress("MemberVisibilityCanBePrivate")
class WorkerLoopNioThread(
	override val selector: Selector = Selector.open(),
	override val workLoop: (thread: INioThread) -> Unit
) : INioThread {
	override var closed: Boolean = false

	val waitQueue = LinkedBlockingDeque<Runnable>()
	val taskQueue = LinkedBlockingDeque<Future<Any?>>()

	override val thread = Thread {
		while (!closed) {
			try {
				workLoop(this)
			} catch (e: Exception) {
				e.printStackTrace()
			}
			while (waitQueue.isNotEmpty()) try {
				waitQueue.poll().run()
			} catch (e: Exception) {
				e.printStackTrace()
			}
			while (taskQueue.isNotEmpty()) try {
				val task = taskQueue.poll()
				try {
					task.resume(task.task.call())
				} catch (e: Throwable) {
					task.resumeWithException(e)
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}

	init {
		thread.isDaemon = true
		thread.start()
	}

	override fun execute(command: Runnable) {
		waitQueue.add(command)
	}

	override fun <T> submit(task: Callable<T>): NioThreadFuture<T> {
		val f = Future(task)
		@Suppress("UNCHECKED_CAST")
		taskQueue.add(f as Future<Any?>)
		return f
	}

	override fun close() {
		closed = true
	}

	class Future<T>(val task: Callable<T>) : NioThreadFuture<T> {
		private val lock = Object()
		private var exception: Throwable? = null
		private var result: Pair<T, Boolean>? = null

		override fun get(): T {
			val result = this.result
			return when {
				exception != null -> throw RuntimeException(exception)
				result != null -> result.first
				else -> synchronized(lock) {
					lock.wait()
					val exception = this.exception
					if (exception != null) {
						throw RuntimeException(exception)
					} else {
						this.result!!.first
					}
				}
			}
		}

		fun resume(value: T) {
			result = value to true
			synchronized(lock) {
				lock.notifyAll()
			}
		}

		fun resumeWithException(e: Throwable) {
			exception = e
			synchronized(lock) {
				lock.notifyAll()
			}
		}
	}
}