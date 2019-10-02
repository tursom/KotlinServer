package cn.tursom.socket.niothread

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

@Suppress("MemberVisibilityCanBePrivate")
class ThreadPoolNioThread : INioThread {
	val threadPool: ExecutorService = Executors.newSingleThreadExecutor {
		val thread = Thread(it)
		thread.isDaemon = true
		thread
	}

	override fun execute(command: Runnable) =		threadPool.execute(command)
	override fun <T> call(task: Callable<T>): T = threadPool.submit(task).get()
	override fun <T> submit(task: Callable<T>): NioThreadFuture<T> = ThreadPoolFuture(threadPool.submit(task))

	class ThreadPoolFuture<T>(val future: Future<T>) : NioThreadFuture<T> {
		override fun get(): T = future.get()
	}
}