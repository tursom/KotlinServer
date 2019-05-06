package cn.tursom.datagram.server

import io.netty.util.HashedWheelTimer
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class AioUdpServer(
	val port: Int,
	private val threadPool: ThreadPoolExecutor = ThreadPoolExecutor(
		1,
		1,
		0L,
		TimeUnit.MILLISECONDS,
		LinkedBlockingQueue(16)
	),
	private val queue: BlockingQueue<() -> Unit> = LinkedBlockingQueue(128),
	private val connectionMap: java.util.AbstractMap<SocketAddress, () -> Unit> = HashMap(),
	private val handler: Connection.() -> Unit
) : Runnable {
	private val excWheelTimer = HashedWheelTimer()
	
	init {
		excWheelTimer.start()
		excWheelTimer.newTimeout({
		
		}, 100L, TimeUnit.MILLISECONDS)
	}
	
	override fun run() {
		
		val channel = DatagramChannel.open()
		channel.configureBlocking(false)
		channel.socket().bind(InetSocketAddress(port))
		val selector = Selector.open()
		channel.register(selector, SelectionKey.OP_READ)
		
		val byteBuffer = ByteBuffer.allocate(65536)
		Thread {
			while (true) {
				try {
					// 进行选择
					if (selector.select() > 0) {
						// 获取以选择的键的集合
						val iterator = selector.selectedKeys().iterator()
						
						while (iterator.hasNext()) {
							val key = iterator.next() as SelectionKey
							
							// 必须手动删除
							iterator.remove()
							
							queue.forEach {
								it()
							}
							
							if (key.isReadable) {
								val datagramChannel = key.channel() as DatagramChannel
								threadPool.execute {
									// 读取
									byteBuffer.clear()
									val address = datagramChannel.receive(byteBuffer) ?: return@execute
									
									val handler = synchronized(connectionMap) { connectionMap[address] } ?: run {
										val newConnection = Connection(address)
										newConnection.handler()
										newConnection.run
									}
									
									// 删除缓冲区中的数据
									byteBuffer.clear()
									val message = "data come from server"
									byteBuffer.put(message.toByteArray())
									byteBuffer.flip()
									
									// 发送数据
									datagramChannel.send(byteBuffer, address)
								}
							}
						}
					}
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
		}.start()
	}
	
	private fun read(
		socket: SocketAddress,
		timeout: Long = 0L,
		timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
		onTimeout: () -> Unit,
		onComplete: () -> Unit
	) {
		val timeoutTask = if (timeout > 0) {
			excWheelTimer.newTimeout({
				queue.offer {
					synchronized(connectionMap) {
						connectionMap.remove(socket)
					}
				}
				onTimeout()
			}, timeout, timeUnit)
		} else {
			null
		}
		connectionMap[socket] = {
			timeoutTask?.cancel()
			onComplete()
		}
	}
	
	inner class Connection(val socket: SocketAddress) {
		private lateinit var init: () -> Unit
		
		val run
			get() = init
		
		fun read(
			timeout: Long = 0L,
			timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
			onTimeout: () -> Unit,
			onComplete: () -> Unit
		) {
			read(timeout, timeUnit, onTimeout, onComplete)
		}
	}
}