package cn.tursom.datagram.server

import io.netty.util.HashedWheelTimer
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketAddress
import java.net.SocketException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class MultiThreadUDPServer(
	override val port: Int,
	val thread: Int = Runtime.getRuntime().availableProcessors(),
	private val connectionMap: java.util.AbstractMap<
		SocketAddress,
		MultiThreadUDPServer.(
			address: SocketAddress,
			buffer: ByteArray,
			size: Int
		) -> Unit
		> = HashMap(),
	private val packageSize: Int = UdpPackageSize.defaultLen,
	private val exception: Exception.() -> Unit = { printStackTrace() },
	private val handler: MultiThreadUDPServer.(address: SocketAddress, buffer: ByteArray, size: Int) -> Unit
) : UDPServer {
	private val excWheelTimer = HashedWheelTimer()
	
	private val socket = DatagramSocket(port)
	
	override fun run() {
		val inBuff = ByteArray(packageSize)
		val inPacket = DatagramPacket(inBuff, inBuff.size)
		while (true) {
			try {
				//读取inPacket的数据
				socket.receive(inPacket)
				val address = inPacket.socketAddress
				(synchronized(connectionMap) { connectionMap[address] } ?: handler)(address, inPacket.data, inPacket.length)
			} catch (e: SocketException) {
				if (e.message == "Socket closed" || e.message == "socket closed") {
					break
				} else {
					e.exception()
				}
			} catch (e: Exception) {
				e.exception()
			}
		}
	}
	
	override fun start() {
		for (i in 1..thread) {
			Thread(this, "UDPServer$i").start()
		}
	}
	
	fun send(address: SocketAddress, buffer: ByteArray, size: Int = -1) {
		socket.send(if (size >= 0) {
			DatagramPacket(buffer, size, address)
		} else {
			DatagramPacket(buffer, buffer.size, address)
		})
	}
	
	fun recv(
		address: SocketAddress,
		timeout: Long = 0L,
		timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
		onTimeout: (e: Exception) -> Unit = { it.printStackTrace() },
		handler: MultiThreadUDPServer.(address: SocketAddress, buffer: ByteArray, size: Int) -> Unit
	) {
		val timeoutTask = if (timeout > 0L) {
			excWheelTimer.newTimeout({
				onTimeout(TimeoutException())
			}, timeout, timeUnit)
		} else {
			null
		}
		synchronized(connectionMap) {
			connectionMap[address] = { address: SocketAddress, buffer: ByteArray, size: Int ->
				timeoutTask?.cancel()
				handler(address, buffer, size)
			}
		}
	}
	
	override fun close() {
		socket.close()
	}
}