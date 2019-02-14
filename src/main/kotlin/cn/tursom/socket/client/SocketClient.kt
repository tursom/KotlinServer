package cn.tursom.socket.client

import cn.tursom.socket.BaseSocket
import java.io.IOException
import java.net.Socket
import java.net.SocketException

class SocketClient(
	socket: Socket,
	timeout: Int = Companion.timeout,
	private val ioException: IOException.() -> Unit = { printStackTrace() },
	private val exception: Exception.() -> Unit = { printStackTrace() },
	func: (SocketClient.() -> Unit)? = null
) : BaseSocket(socket, timeout) {
	
	init {
		func?.let {
			use(null, it)
		}
	}
	
	constructor(
		host: String,
		port: Int,
		timeout: Int = Companion.timeout,
		ioException: IOException.() -> Unit = { printStackTrace() },
		exception: Exception.() -> Unit = { printStackTrace() },
		func: (SocketClient.() -> Unit)? = null
	) : this(Socket(host, port), timeout, ioException, exception, func)
	
	fun <T> execute(default: T? = null, func: SocketClient.() -> T?): T? {
		return try {
			func()
		} catch (io: IOException) {
			io.ioException()
			default
		} catch (e: SocketException) {
			if (e.message == null) {
				e.printStackTrace()
			} else {
				System.err.println("$address: ${e::class.java}: ${e.message}")
			}
			default
		} catch (e: Exception) {
			e.exception()
			default
		}
	}
	
	fun <T> use(default: T? = null, func: SocketClient.() -> T): T? {
		val ret = execute(default, func)
		closeSocket()
		return ret
	}
}
