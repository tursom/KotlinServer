package cn.tursom.socket.server

import cn.tursom.socket.client.SocketClient.Companion.defaultReadSize
import cn.tursom.socket.client.SocketClient.Companion.defaultReadTimeout
import cn.tursom.socket.client.SocketClient.Companion.defaultWaitTimeout
import cn.tursom.tools.getTAG
import org.apache.http.util.ByteArrayBuffer
import java.lang.Thread.sleep
import java.net.Socket
import java.net.SocketException
import kotlin.math.min

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

	protected fun send(message: String) = send(message.toByteArray())
	protected fun send(message: ByteArray) {
		if (socket.isClosed) throw SocketClosedException()
		try {
			outputStream.write(message)
		} catch (e: SocketException) {
			e.printStackTrace()
		}
	}

	fun clean(blockSize: Int = defaultReadSize) {
		try {
			while (recvByteArraySingle(maxsize = blockSize, dataWaitTimeout = 1, readTimeout = 1)?.size ?: 0 == blockSize) {
			}
		} catch (e: ServerException) {
		}
	}

	fun recv(maxsize: Int = defaultReadSize * 10,
	         readTimeout: Long = 1,
	         dataWaitTimeout: Long = 1,
	         firstWaitTime: Long = defaultWaitTimeout)
		: String? {
		return String(recvByteArray(maxsize, readTimeout, dataWaitTimeout, firstWaitTime) ?: return null)
	}

	fun recvSingle(maxsize: Int, readTimeout: Long = defaultReadTimeout, dataWaitTimeout: Long = defaultWaitTimeout): String? {
		return String(recvByteArraySingle(maxsize, readTimeout, dataWaitTimeout) ?: return null)
	}

	fun recvByteArray(
		maxsize: Int = defaultReadSize * 10,
		readTimeout: Long = 1,
		dataWaitTimeout: Long = 1,
		firstWaitTime: Long = defaultWaitTimeout)
		: ByteArray? {
		val byteArrayBuffer = ByteArrayBuffer(maxsize / defaultReadSize + 1)
		var buffer: ByteArray
		try {
			buffer = recvByteArraySingle(defaultReadSize, readTimeout, firstWaitTime) ?: return null
		} catch (e: ServerException) {
			return null
		}
		byteArrayBuffer.append(buffer, 0, buffer.size)

		var loopTime = 0
		while (buffer.size == defaultReadSize && (buffer.size + loopTime * defaultReadSize) < maxsize) {
			try {
				buffer = (recvByteArraySingle(defaultReadSize, readTimeout, dataWaitTimeout)
					?: return byteArrayBuffer.toByteArray())
			} catch (e: ServerException) {
				return byteArrayBuffer.toByteArray()
			}
			byteArrayBuffer.append(buffer, 0, buffer.size)
			loopTime++
		}
		return byteArrayBuffer.toByteArray()
	}

	fun recvByteArraySingle(
		maxsize: Int,
		readTimeout: Long = defaultReadTimeout,
		dataWaitTimeout: Long = defaultWaitTimeout)
		: ByteArray? {
		if (socket.isClosed) {
			throw SocketClosedException()
		}
		val buffer = ByteArray(maxsize)
		var readSize = 0
		try {
			//等待数据到达
			val maxTimeOut = System.currentTimeMillis() + dataWaitTimeout
			while (inputStream.available() == 0) {
				if (socket.isClosed) {
					throw SocketClosedException()
				}
				if (System.currentTimeMillis() > maxTimeOut) {
					throw ServerException("socket out of time")
				} else {
					sleep(2)
				}
			}

			//读取数据
			while (readSize < maxsize) {
				val readLength = min(inputStream.available(), maxsize - readSize)
				if (readLength <= 0) {
					continue
				}
				// can alternatively use bufferedReader, guarded by isReady():
				val readResult = inputStream.read(buffer, readSize, readLength)
				if (readResult == -1) break
				readSize += readResult
				val maxTimeMillis = System.currentTimeMillis() + readTimeout
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
		const val defaultReadTimeout: Long = 10
		const val defaultWaitTimeout: Long = 3 * 60 * 1000
		val TAG = getTAG(this::class.java)
		const val debug: Boolean = true
		val serverError = "server error".toByteArray()

	}
}

