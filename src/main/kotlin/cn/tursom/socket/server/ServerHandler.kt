package cn.tursom.socket.server

import cn.tursom.socket.BaseSocket
import java.net.Socket

/**
 * ServerHandler请求处理类
 * 通过重载handle()函数处理逻辑
 * recv()提供了网络通讯常见的recv函数，避免BuffedReader.getLine造成的阻塞
 * 自动关闭套接字，自动处理异常（全局）
 * 通拥有较好的异常处理体系，可通过异常实现基本的逻辑
 * 可以处理异常的同时给客户端发送异常信息，通过重载ServerException.code的getter实现
 */
abstract class ServerHandler(socket: Socket) : Runnable, BaseSocket(socket) {
	init {
		if (socket.isClosed) {
			throw SocketClosedException()
		}
	}
	
	final override fun run() {
		try {
			handle()
		} catch (e: ServerException) {
			if (e.message == null)
				e.printStackTrace()
			else
				System.err.println("$address: ${e::class.java}: ${e.message}")
			
			try {
				send(serverError)
			} catch (e: SocketClosedException) {
				System.err.println("$address: ${e::class.java}: ${e.message}")
			}
			
		} catch (e: Exception) {
			e.printStackTrace()
			try {
				send(serverError)
			} catch (e: SocketClosedException) {
				System.err.println("$address: ${e::class.java}: ${e.message}")
			}
		}
		closeSocket()
		println("$address: connection closed")
	}
	
	abstract fun handle()
	
	open val serverError: ByteArray
		get() = Companion.serverError
	
	open class ServerException(s: String? = null) : Exception(s) {
		open val code: ByteArray?
			get() = null
	}
	
	class SocketClosedException(s: String? = null) : ServerException(s)
	
	companion object Companion {
		val serverError = "server error".toByteArray()
	}
}

