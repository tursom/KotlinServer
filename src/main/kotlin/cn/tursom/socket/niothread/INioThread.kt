package cn.tursom.socket.niothread

import java.util.concurrent.Callable

interface INioThread {
	fun execute(command: Runnable)
	fun execute(command: () -> Unit) {
		execute(Runnable { command() })
	}

	fun <T> call(task: Callable<T>): T
	fun <T> call(task: () -> T): T {
		return call(Callable<T> { task() })
	}

	fun <T> submit(task: Callable<T>): NioThreadFuture<T>
	fun <T> submit(task: () -> T): NioThreadFuture<T> {
		return submit(Callable<T> { task() })
	}
}

