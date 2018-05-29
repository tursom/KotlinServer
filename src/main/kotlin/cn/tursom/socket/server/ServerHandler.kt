package cn.tursom.socket.server

import cn.tursom.tools.getTAG
import org.apache.http.util.ByteArrayBuffer
import java.lang.Thread.sleep
import java.net.Socket

/**
 * ServerHandler请求处理类
 * 通过重载handle()函数处理逻辑
 * recv()提供了网络通讯常见的recv函数，避免getLine造成的阻塞
 * 自动关闭套接字，自动处理异常（全局）
 * 通拥有较好的异常处理体系，可通过异常实现基本的逻辑
 * 可以处理异常的同时给客户端发送异常信息，通过重载ServerException.code的getter实现
 */
abstract class ServerHandler(val socket: Socket) : Runnable {
	protected val inputStream = socket.getInputStream()!!
	protected val outputStream = socket.getOutputStream()!!
	val address = socket.inetAddress?.toString()?.drop(1) ?: "0.0.0.0:0"
	val localport = socket.localPort

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
			send(serverError)
		} catch (e: Exception) {
			e.printStackTrace()
			send(serverError)
		}
		closeSocket()
		println("$address: connection closed")
	}

	abstract fun handle()

	protected fun send(message: String) = send(message.toByteArray())
	protected fun send(message: ByteArray) {
		try {
			outputStream.write(message)
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	fun recv(maxsize: Int = 102000, maxReadTime: Long = defaultMaxReadTime, maxWaitTime: Long = 100): String? {
		return String(recvByteArray(maxsize, maxReadTime, maxWaitTime) ?: return null)
	}

	fun recvSingle(maxsize: Int, maxReadTime: Long = defaultMaxReadTime, maxWaitTime: Long = defaultMaxWaitTime): String? {
		return String(recvByteArraySingle(maxsize, maxReadTime, maxWaitTime) ?: return null)
	}

	fun recvByteArray(
			maxsize: Int = defaultReadSize * 10,
			maxReadTime: Long = defaultMaxReadTime,
			maxWaitTime: Long = 100)
			: ByteArray? {
		val byteArrayBuffer = ByteArrayBuffer(maxsize / defaultReadSize + 1)
		var buffer = recvByteArraySingle(defaultReadSize, maxReadTime, defaultMaxWaitTime) ?: return null
		byteArrayBuffer.append(buffer, 0, buffer.size)
		var loopTime = 0
		while ((buffer.size + loopTime * defaultReadSize) < maxsize && buffer.size == defaultReadSize) {
			buffer = (recvByteArraySingle(defaultReadSize, maxReadTime, maxWaitTime)
					?: return byteArrayBuffer.toByteArray())
			byteArrayBuffer.append(buffer, 0, buffer.size)
			loopTime++
		}
		return byteArrayBuffer.toByteArray()
	}

	fun recvByteArraySingle(
			maxsize: Int,
			maxReadTime: Long = defaultMaxReadTime,
			maxWaitTime: Long = defaultMaxWaitTime)
			: ByteArray? {
		if (socket.isClosed) {
			System.err.println("socket closed")
			return null
		}
		val buffer = ByteArray(maxsize)
		var readSize = 0
		try {
			//等待数据到达
			val maxTimeOut = System.currentTimeMillis() + maxWaitTime
			while (inputStream.available() == 0) {
				if (System.currentTimeMillis() > maxTimeOut) {
					System.err.println("socket out of time")
					return null
				} else {
					sleep(10)
				}
			}

			//读取数据
			while (readSize < maxsize) {
				val readLength = java.lang.Math.min(
						inputStream.available(), maxsize - readSize)
				if (readLength <= 0) {
					sleep(maxReadTime xor 4)
					continue
				}
				// can alternatively use bufferedReader, guarded by isReady():
				val readResult = inputStream.read(buffer, readSize, readLength)
				if (readResult == -1) break
				readSize += readResult
				val maxTimeMillis = System.currentTimeMillis() + maxReadTime
				while (inputStream.available() == 0) {
					if (System.currentTimeMillis() > maxTimeMillis) {
						return buffer.copyOf(readSize)
					}
				}
			}
		} catch (e: StringIndexOutOfBoundsException) {
			e.printStackTrace()
			return null
		}
		return buffer.copyOf(readSize)
	}

	private fun closeSocket() {
		if (!socket.isClosed) {
			closeInputStream()
			closeOutputStream()
			socket.close()
		}
	}

	private fun closeInputStream() {
		try {
			inputStream.close()
		} catch (e: Exception) {
		}
	}

	private fun closeOutputStream() {
		try {
			outputStream.close()
		} catch (e: Exception) {
		}
	}

	open val serverError: ByteArray
		get() = Companion.serverError

	open class ServerException(s: String? = null) : Exception(s) {
		open val code: ByteArray?
			get() = null
	}

	class SocketClosedException(s: String? = null) : ServerException(s)

	companion object Companion {
		const val defaultReadSize: Int = 10240
		const val defaultMaxReadTime: Long = 10
		const val defaultMaxWaitTime: Long = 3 * 60 * 1000
		val TAG = getTAG(this::class.java)
		const val debug: Boolean = true
		val serverError = "server error".toByteArray()

	}
}

