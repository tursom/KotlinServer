package cn.tursom.datagram.server

import java.io.Closeable
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException


class UDPServer(
	port: Int,
	private val packageSize: Int = defaultLen,
	private val exception: Exception.() -> Unit = { printStackTrace() },
	private val handle: (ByteArray) -> ByteArray?
) : Runnable, Closeable {
	
	private val socket = DatagramSocket(port)
	
	override fun run() {
		val inBuff = ByteArray(packageSize)
		val inPacket = DatagramPacket(inBuff, inBuff.size)
		while (true) {
			try {
				//读取inPacket的数据
				socket.receive(inPacket)
				val sendData = handle(inBuff) ?: continue
				val outPacket = DatagramPacket(
					sendData,
					sendData.size,
					inPacket.socketAddress
				)
				//发送数据
				socket.send(outPacket)
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