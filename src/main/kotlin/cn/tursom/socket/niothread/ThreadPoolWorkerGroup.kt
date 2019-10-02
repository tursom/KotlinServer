package cn.tursom.socket.niothread

import java.nio.channels.SelectableChannel

@Suppress("MemberVisibilityCanBePrivate")
class ThreadPoolWorkerGroup(
	val poolSize: Int = Runtime.getRuntime().availableProcessors(),
	val groupName: String = "",
	val worker: (thread: INioThread) -> Unit
) : IWorkerGroup {
	val workerGroup = Array(poolSize) {
		ThreadPoolNioThread("$groupName-$it", workLoop = worker)
	}
	var registered = 0
	override fun register(channel: SelectableChannel, onComplete: (key: SelectionContext) -> Unit) {
		val workerThread = workerGroup[registered++ % poolSize]
		workerThread.register(channel) {
			onComplete(SelectionContext(it, workerThread))
		}
	}

	override fun close() {
		workerGroup.forEach {
			it.close()
			it.selector.close()
		}
	}
}