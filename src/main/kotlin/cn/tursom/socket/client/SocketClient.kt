package cn.tursom.socket.client

import cn.tursom.socket.BaseSocket
import java.io.IOException
import java.net.SocketException

class SocketClient(
	host: String,
	port: Int,
	private val ioException: (io: IOException) -> Unit = { it.printStackTrace() }) : BaseSocket(host, port) {
	
	val address: String = "$host:$port"
	
	fun execute(func: () -> Unit) {
		try {
			func()
		} catch (io: IOException) {
			ioException(io)
		} catch (e: SocketException) {
			if (e.message == null)
				e.printStackTrace()
			else
				System.err.println("$address: ${e::class.java}: ${e.message}")
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}
	
	fun run(func: () -> Unit) {
		execute(func)
		closeSocket()
	}
}
