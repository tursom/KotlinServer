package cn.tursom.utils.timer

import java.lang.Thread.sleep
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread


class WheelTimer(
	val tick: Long = 200,
	val wheelSize: Int = 512,
	val threadPool: ExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
) {
	var closed = false
	val taskQueueArray = Array(wheelSize) { TaskQueue() }
	private var position = 0

	fun exec(timeout: Long, task: () -> Unit): TimerTask {
		val index = ((timeout / tick) % wheelSize).toInt()
		return taskQueueArray[index].offer(task, timeout)
	}

	init {
		thread(isDaemon = true) {
			while (!closed) {
				position %= wheelSize

				val newQueue = TaskQueue()
				val taskQueue = taskQueueArray[position]
				taskQueueArray[position] = newQueue

				val time = System.currentTimeMillis()
				var node = taskQueue.root
				while (node != null) {
					node = if (node.isOutTime(time)) {
						val sNode = node
						threadPool.execute { sNode.task() }
						node.next
					} else {
						val next = node.next
						newQueue.offer(node)
						next
					}
				}

				position++
				sleep(tick)
			}
		}
	}

	data class TaskNode(
		val timeout: Long,
		val task: () -> Unit,
		var prev: TaskNode?,
		var next: TaskNode?,
		val lock: Any
	) : TimerTask {
		val outTime = System.currentTimeMillis() + timeout
		val isOutTime get() = System.currentTimeMillis() > outTime

		fun isOutTime(time: Long) = time > outTime

		override fun run() = task()

		override fun cancel() {
			synchronized(lock) {
				prev?.next = next
				next?.prev = prev
			}
		}
	}

	class TaskQueue {
		var root: TaskNode? = null
		val lock = Any()

		fun offer(task: () -> Unit, timeout: Long): TaskNode {
			synchronized(lock) {
				val insert = TaskNode(timeout, task, null, root, lock)
				root?.prev = insert
				root = insert
				return insert
			}
		}

		fun offer(node: TaskNode): TaskNode {
			synchronized(lock) {
				node.next = root
				node.prev = null
				root = node
				return node
			}
		}
	}

	companion object {
		val timer = WheelTimer()
	}
}