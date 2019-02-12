package cn.tursom.socket.client

import cn.tursom.socket.BaseSocket
import java.io.IOException
import java.net.Socket
import java.net.SocketException

class SocketClient(
	socket: Socket,
	private val ioException: IOException.() -> Unit = { printStackTrace() },
	private val exception: Exception.() -> Unit = { printStackTrace() },
	func: (SocketClient.() -> Unit)? = null
) : BaseSocket(socket) {
	
	init {
		func?.let {
			use(it)
		}
	}
	
	constructor(
		host: String,
		port: Int,
		ioException: IOException.() -> Unit = { printStackTrace() },
		exception: Exception.() -> Unit = { printStackTrace() },
		func: (SocketClient.() -> Unit)? = null
	) : this(Socket(host, port), ioException, exception, func)
	
	fun execute(func: SocketClient.() -> Unit) {
		try {
			func()
		} catch (io: IOException) {
			io.ioException()
		} catch (e: SocketException) {
			if (e.message == null) {
				e.printStackTrace()
			} else {
				System.err.println("$address: ${e::class.java}: ${e.message}")
			}
		} catch (e: Exception) {
			e.exception()
		}
	}
	
	fun use(func: SocketClient.() -> Unit) {
		execute(func)
		closeSocket()
	}
}
