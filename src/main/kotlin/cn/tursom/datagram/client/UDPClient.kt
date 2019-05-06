package cn.tursom.datagram.client

import java.io.Closeable
import java.net.*


class UdpClient(
	private val host: String,
	private val port: Int,
	private val packageSize: Int = defaultLen
) : Closeable {
	
	private val socket = DatagramSocket()
	
	fun send(data: ByteArray, callback: ((ByteArray, size: Int) -> Unit)? = null) {
		val address = InetSocketAddress(host, port)
		socket.send(DatagramPacket(data, data.size, address))
		callback?.let {
			//定义接受网络数据的字节数组
			val inBuff = ByteArray(packageSize)
			//已指定字节数组创建准备接受数据的DatagramPacket对象
			val inPacket = DatagramPacket(inBuff, inBuff.size, address)
			socket.receive(inPacket)
			it(inPacket.data ?: return, inPacket.length)
		}
	}
	
	fun recv(address: SocketAddress, buffer: ByteArray, callback: (ByteArray, size: Int) -> Unit) {
		val inPacket = DatagramPacket(buffer, buffer.size, address)
		socket.receive(inPacket)
		callback(inPacket.data ?: return, inPacket.length)
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