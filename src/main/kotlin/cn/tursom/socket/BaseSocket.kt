package cn.tursom.socket

import java.lang.Thread.sleep
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import kotlin.math.log2
import kotlin.math.min

/**
 * 对基础的Socket做了些许封装
 */
open class BaseSocket(val socket: Socket) {
	val address = socket.inetAddress?.toString()?.drop(1) ?: "0.0.0.0"
	val port = socket.port
	val localPort = socket.localPort
	private val inputStream = socket.getInputStream()!!
	private val outputStream = socket.getOutputStream()!!
	
	constructor(host: String, port: Int) : this(
		kotlin.run {
			val socket = Socket()
			socket.connect(InetSocketAddress(host, port))
			socket
		}
	)
	
	fun send(message: String) = send(message.toByteArray())
	fun send(message: ByteArray) {
		if (socket.isClosed) throw SocketException("Socket Closed")
		try {
			outputStream.write(message)
		} catch (e: SocketException) {
			e.printStackTrace()
		}
	}
	
	fun clean(blockSize: Int = defaultReadSize) {
		try {
			while (recvByteArraySingle(blockSize, 1)?.size ?: 0 == blockSize) {
			}
		} catch (e: SocketException) {
		}
	}
	
	fun recv(maxsize: Int = defaultReadSize * 10,
	         timeout1: Long = timeout)
		: String? {
		return String(recvByteArray(maxsize, timeout1) ?: return null)
	}
	
	fun recvSingle(maxsize: Int, timeout1: Long = timeout): String? {
		return String(recvByteArraySingle(maxsize, timeout1) ?: return null)
	}
	
	fun recvByteArray(
		maxsize: Int = defaultReadSize * 10,
		timeout1: Long = timeout)
		: ByteArray? {
		var buffer = ByteArray(0)
		try {
			buffer += recvByteArraySingle(defaultReadSize, timeout1) ?: return null
		} catch (e: SocketException) {
			return null
		}
		
		var loopTime = 0
		while (buffer.size == defaultReadSize && (buffer.size + loopTime * defaultReadSize) < maxsize) {
			try {
				buffer += (recvByteArraySingle(defaultReadSize, 1)
					?: return buffer)
			} catch (e: SocketException) {
				return buffer
			}
			loopTime++
		}
		return buffer
	}
	
	private fun recvByteArraySingle(
		maxsize: Int,
		timeout1: Long = timeout)
		: ByteArray? {
		if (socket.isClosed) {
			throw SocketException("Socket Closed")
		}
		val buffer = ByteArray(maxsize)
		var readSize = 0
		try {
			//等待数据到达
			val maxTimeOut = System.currentTimeMillis() + timeout1
			var sleepTime = 0L
			while (inputStream.available() == 0) {
				if (socket.isClosed) {
					throw SocketException("Socket Closed")
				}
				sleepTime++
				if (System.currentTimeMillis() > maxTimeOut) {
					throw SocketException("socket out of time")
				} else {
					sleep(log2(sleepTime.shl(1).toFloat()).toLong())
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
				val maxTimeMillis = System.currentTimeMillis() + 1
				while (inputStream.available() == 0) {
					if (System.currentTimeMillis() > maxTimeMillis) {
						val ret = ByteArray(readSize)
						System.arraycopy(buffer, 0, ret, 0, readSize)
						return ret
					}
				}
			}
		} catch (e: StringIndexOutOfBoundsException) {
			e.printStackTrace()
			return null
		}
		return buffer.copyOf(readSize)
	}
	
	protected fun closeSocket() {
		if (!socket.isClosed) {
			closeInputStream()
			closeOutputStream()
			socket.close()
		}
	}
	
	protected fun closeInputStream() {
		try {
			inputStream.close()
		} catch (e: Exception) {
		}
	}
	
	protected fun closeOutputStream() {
		try {
			outputStream.close()
		} catch (e: Exception) {
		}
	}
	
	fun isConnected(): Boolean {
		return socket.isConnected
	}
	
	companion object Companion {
		const val defaultReadSize: Int = 10240
		const val timeout: Long = 3 * 60 * 1000
		fun formatIpAddress(ip: Int) =
			"${ip and 0xff}.${(ip shr 8) and 0xff}.${(ip shr 16) and 0xff}.${(ip shr 24) and 0xff}"
		
		fun Int.toByteArray(): ByteArray {
			val array = ByteArray(4)
			array[0] = this.shr(3 * 8).toByte()
			array[1] = this.shr(2 * 8).toByte()
			array[2] = this.shr(1 * 8).toByte()
			array[3] = this.shr(0 * 8).toByte()
			return array
		}
		
		fun ByteArray.toInt(): Int =
			(this[0].toInt() shl 24) or
				(this[1].toInt() shl 16 and 0xff0000) or
				(this[2].toInt() shl 8 and 0xff00) or
				(this[3].toInt() and 0xFF)
		
		fun Long.toByteArray(): ByteArray {
			val array = ByteArray(4)
			array[0] = this.shr(7 * 8).toByte()
			array[1] = this.shr(6 * 8).toByte()
			array[2] = this.shr(5 * 8).toByte()
			array[3] = this.shr(4 * 8).toByte()
			array[4] = this.shr(3 * 8).toByte()
			array[5] = this.shr(2 * 8).toByte()
			array[6] = this.shr(1 * 8).toByte()
			array[7] = this.shr(0 * 8).toByte()
			return array
		}
		
		fun ByteArray.toLong(): Long =
			(this[0].toLong() shl 56 and 0xff000000000000) or
				(this[1].toLong() shl 48 and 0xff0000000000) or
				(this[2].toLong() shl 40 and 0xff00000000) or
				(this[3].toLong() shl 32 and 0xff00000000) or
				(this[4].toLong() shl 24 and 0xff000000) or
				(this[5].toLong() shl 16 and 0xff0000) or
				(this[6].toLong() shl 8 and 0xff00) or
				(this[7].toLong() and 0xFF)
		
	}
}