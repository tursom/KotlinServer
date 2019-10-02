package cn.tursom.socket.niothread

import java.nio.channels.SelectableChannel
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.util.concurrent.*

@Suppress("MemberVisibilityCanBePrivate")
class SingleThreadNioThread(
	val threadName: String = "",
	override val selector: Selector = Selector.open()
) : INioThread {
	lateinit var workerThread: Thread
	//val threadPool: ExecutorService = Executors.newSingleThreadExecutor {
	//	val thread = Thread(it)
	//	workerThread = thread
	//	thread.isDaemon = true
	//	thread.name = threadName
	//	thread
	//}
	val threadPool: ExecutorService = ThreadPoolExecutor(1, 1,
		0L, TimeUnit.MILLISECONDS,
		LinkedBlockingQueue<Runnable>(),
		ThreadFactory {
			val thread = Thread(it)
			workerThread = thread
			thread.isDaemon = true
			thread.name = threadName
			thread
		})

	override var closed: Boolean = false

	override fun wakeup() {
		selector.wakeup()
	}

	override fun register(channel: SelectableChannel, onComplete: (key: SelectionKey) -> Unit) {
		if (Thread.currentThread() == workerThread) {
			onComplete(channel.register(selector, 0))
		} else {
			threadPool.execute { register(channel, onComplete) }
			wakeup()
		}
	}

	override fun execute(command: Runnable) = threadPool.execute(command)
	override fun <T> call(task: Callable<T>): T = threadPool.submit(task).get()
	override fun <T> submit(task: Callable<T>): NioThreadFuture<T> = ThreadPoolFuture(threadPool.submit(task))

	override fun close() {
		closed = true
		threadPool.shutdown()
	}

	class ThreadPoolFuture<T>(val future: Future<T>) : NioThreadFuture<T> {
		override fun get(): T = future.get()
	}

	override fun toString(): String {
		return "SingleThreadNioThread($threadName)"
	}
}