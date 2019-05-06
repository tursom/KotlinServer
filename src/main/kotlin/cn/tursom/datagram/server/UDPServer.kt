package cn.tursom.datagram.server

import io.netty.util.HashedWheelTimer
import sun.misc.Signal.handle
import java.io.Closeable
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketAddress
import java.net.SocketException
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

// TODO

class UDPServer(
	port: Int,
	private val threadPool: ThreadPoolExecutor = ThreadPoolExecutor(
		1,
		1,
		0L,
		TimeUnit.MILLISECONDS,
		LinkedBlockingQueue(32)
	),
	private val queue: BlockingQueue<() -> Unit> = LinkedBlockingQueue(128),
	private val connectionMap: java.util.AbstractMap<
		SocketAddress,
		UDPServer.(
			address: SocketAddress,
			buffer: ByteArray,
			size: Int
		) -> Unit
		> = HashMap(),
	private val packageSize: Int = defaultLen,
	private val exception: Exception.() -> Unit = { printStackTrace() },
	private val handler: UDPServer.(address: SocketAddress, buffer: ByteArray, size: Int) -> Unit
) : Runnable, Closeable {
	private val excWheelTimer = HashedWheelTimer()
	
	private val socket = DatagramSocket(port)
	
	override fun run() {
		val inBuff = ByteArray(packageSize)
		val inPacket = DatagramPacket(inBuff, inBuff.size)
		while (true) {
			try {
				val taskQueue = queue.iterator()
				while (taskQueue.hasNext()) {
					taskQueue.next()()
					taskQueue.remove()
				}
				
				//读取inPacket的数据
				socket.receive(inPacket)
				threadPool.execute {
					try {
						val address = inPacket.socketAddress
						(synchronized(connectionMap) { connectionMap[address] } ?: handler)(address, inPacket.data, inPacket.length)
					} catch (e: SocketException) {
						if (e.message == "Socket closed") {
							return@execute
						} else {
							e.exception()
						}
					} catch (e: Exception) {
						e.exception()
					}
				}
			} catch (e: SocketException) {
				if (e.message == "Socket closed") {
					break
				} else {
					e.exception()
				}
			} catch (e: Exception) {
				e.exception()
			}
		}
	}
	
	override fun close() {
		socket.close()
	}
	
	@Suppress("MemberVisibilityCanBePrivate")
	companion object {
		//定义不同环境下数据报的最大大小
		const val LANNetLen = 1472
		const val internetLen = 548
		const val defaultLen = internetLen
	}
}