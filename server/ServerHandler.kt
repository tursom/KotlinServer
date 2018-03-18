package server

import java.net.Socket

/*
 * ServerHandler请求处理类
 * 通过重载handle()函数处理逻辑
 * recv()提供了网络通讯常见的recv函数，避免getLine造成的阻塞
 * 自动关闭套接字，自动处理异常（全局）
 * 通拥有较好的异常处理体系，可通过异常实现基本的逻辑
 * 可以处理异常的同时给客户端发送异常信息，通过重载ServerException.code的getter实现
 */
open class ServerHandler(private val socket: Socket) : Runnable {
	protected val inputStream = socket.getInputStream()!!
	protected val outputStream = socket.getOutputStream()!!
	
	final override fun run() {
		try {
			handle()
		} catch (e: ServerException) {
			if (e.message == null)
				e.printStackTrace()
			else
				System.err.println("${e::class.java}:${e.message}")
			outputStream.write(e.code ?: serverError ?: Companion.serverError)
		} catch (e: Exception) {
			e.printStackTrace()
			outputStream.write(serverError ?: Companion.serverError)
		}
		if (!socket.isClosed)
			socket.close()
	}
	
	open fun handle() {}
	
	protected fun recv(maxsize: Int): String? {
		if (socket.isClosed) return null
		val buffer = ByteArray(maxsize)
		val size: Int
		try {
			size = inputStream.read(buffer, 0, maxsize)
		} catch (e: StringIndexOutOfBoundsException) {
			System.err.println("connection ${socket.inetAddress} closed")
			return null
		}
		return String(buffer, 0, size)
	}
	
	open class ServerException(s: String? = null) : Exception(s) {
		open val code: ByteArray?
			get() = null
	}
	
	open val serverError: ByteArray?
		get() = null
	
	companion object Companion {
		val serverError = "server error".toByteArray()
	}
}

