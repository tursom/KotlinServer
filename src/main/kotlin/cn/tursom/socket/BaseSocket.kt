package cn.tursom.socket

import java.lang.Thread.sleep
import java.net.Socket
import java.net.SocketException
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
	
	fun send(message: String?) {
		send((message ?: return).toByteArray())
	}
	
	fun send(message: ByteArray) {
		if (socket.isClosed) throw SocketException("Socket Closed")
		outputStream.write(message)
	}
	
	fun send(message: Int) {
		send(message.toByteArray())
	}
	
	fun send(message: Long) {
		send(message.toByteArray())
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
	
	fun recvInt(timeout1: Long = timeout): Int? {
		val buffer = ByteArray(4)
		try {
			//等待数据到达
			val maxTimeOut = System.currentTimeMillis() + timeout1
			var sleepTime = 0L
			while (inputStream.available() < 4) {
				sleepTime++
				if (System.currentTimeMillis() > maxTimeOut) {
					throw SocketException("socket read out of time")
				} else {
					sleep(sleepTime.left1().toLong())
				}
			}
			
			//读取数据
			inputStream.read(buffer, 0, 4)
		} catch (e: StringIndexOutOfBoundsException) {
			e.printStackTrace()
			return null
		}
		return buffer.toInt()
	}
	
	fun recvLong(timeout1: Long = timeout): Long? {
		val buffer = ByteArray(8)
		try {
			//等待数据到达
			val maxTimeOut = System.currentTimeMillis() + timeout1
			var sleepTime = 0L
			while (inputStream.available() < 4) {
				sleepTime++
				if (System.currentTimeMillis() > maxTimeOut) {
					throw SocketException("socket read out of time")
				} else {
					sleep(sleepTime.left1().toLong())
				}
			}
			
			//读取数据
			inputStream.read(buffer, 0, 8)
		} catch (e: StringIndexOutOfBoundsException) {
			e.printStackTrace()
			return null
		}
		return buffer.toLong()
	}
	
	private fun recvByteArray(
		maxsize: Int = defaultReadSize * 10,
		timeout1: Long = timeout)
		: ByteArray? {
		
		var buffer = ByteArray(0)
		try {
			buffer += recvByteArraySingle(maxsize, timeout1) ?: return null
		} catch (e: SocketException) {
			return null
		}
		
		while (inputStream.available() != 0) {
			try {
				buffer += (recvByteArraySingle(maxsize - buffer.size, 1)
					?: return buffer)
			} catch (e: SocketException) {
				return buffer
			}
			sleep(1)
		}
		return buffer
	}
	
	private fun recvByteArraySingle(
		maxsize: Int,
		timeout1: Long = timeout)
		: ByteArray? {
//		if (testConnection()) {
//			throw SocketException("Socket Closed")
//		}
		val buffer = ByteArray(maxsize)
		var readSize = 0
		try {
			//等待数据到达
			val maxTimeOut = System.currentTimeMillis() + timeout1
			var sleepTime = 0L
			while (inputStream.available() == 0) {
//				if (testConnection()) {
//					throw SocketException("Socket Closed")
//				}
				sleepTime++
				if (System.currentTimeMillis() > maxTimeOut) {
					throw SocketException("socket read out of time")
				} else {
					sleep(sleepTime.left1().toLong())
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
	
	fun isConnected(): Boolean {
		return socket.isConnected
	}
	
	/**
	 * @warning dangerous
	 */
	fun testConnection() = try {
		socket.sendUrgentData(0xff)
		true
	} catch (e: Exception) {
		false
	}
	
	companion object Companion {
		const val defaultReadSize: Int = 10240
		const val timeout: Long = 60 * 1000
		
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
		
		fun Int.left1(): Int {
			if (this == 0) {
				return -1
			}
			var exp = 4
			var pos = 1 shl exp
			while (exp > 0) {
				exp--
				if ((this shr pos) != 0) {
					pos += 1 shl exp
				} else {
					pos -= 1 shl exp
				}
			}
			return if (this shr pos != 0) pos else pos - 1
		}
		
		fun Long.left1(): Int {
			if (this == 0L) {
				return -1
			}
			var exp = 8
			var pos = 1 shl exp
			while (exp > 0) {
				exp--
				if ((this shr pos) != 0L) {
					pos += 1 shl exp
				} else {
					pos -= 1 shl exp
				}
			}
			return if (this shr pos != 0L) pos else pos - 1
		}
	}
}