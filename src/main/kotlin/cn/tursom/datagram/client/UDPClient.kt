package cn.tursom.datagram.client

import java.io.Closeable
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress


class UdpClient(
	private val host: String,
	private val port: Int,
	private val packageSize: Int = defaultLen
) : Closeable {
	
	private val socket = DatagramSocket()
	
	fun send(data: ByteArray, callback: ((ByteArray) -> Unit)? = null) {
		socket.send(DatagramPacket(data, data.size, InetAddress.getByName(host), port))
		callback?.let {
			//定义接受网络数据的字节数组
			val inBuff = ByteArray(packageSize)
			//已指定字节数组创建准备接受数据的DatagramPacket对象
			val inPacket = DatagramPacket(inBuff, inBuff.size)
			socket.receive(inPacket)
			it(inPacket.data ?: return)
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