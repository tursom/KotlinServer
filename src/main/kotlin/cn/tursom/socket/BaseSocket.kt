@file:Suppress("MemberVisibilityCanBePrivate")

package cn.tursom.socket

import cn.tursom.tools.toUTF8String
import java.io.*
import java.net.Socket
import java.net.SocketTimeoutException

/**
 * 对基础的Socket做了些许封装
 */
@Suppress("unused")
open class BaseSocket(
	private val socket: Socket,
	private val timeout: Int = Companion.timeout
) : Closeable {
	
	val address = socket.inetAddress?.toString()?.drop(1) ?: "0.0.0.0"
	val port by lazy { socket.port }
	val localPort by lazy { socket.localPort }
	private val inputStream by lazy { socket.getInputStream()!! }
	private val outputStream by lazy { socket.getOutputStream()!! }
	
	fun send(message: String?) {
		send((message ?: return).toByteArray())
	}
	
	fun send(message: ByteArray?) {
		outputStream.write(message ?: return)
	}
	
	fun send(message: Int) {
		send(message.toByteArray())
	}
	
	fun send(message: Long) {
		send(message.toByteArray())
	}
	
	fun sendObject(obj: Any?): Boolean {
		send(serialize(obj ?: return false) ?: return false)
		return true
	}
	
	inline fun <reified T> recvObject(): T? {
		return try {
			unSerialize(recv()) as T
		} catch (e: Exception) {
			null
		}
	}
	
	fun recvString(
		readTimeout: Int = 100,
		firstTimeout: Int = timeout
	): String {
		return recv(readTimeout, firstTimeout).toUTF8String()
	}
	
	fun recvString(
		maxsize: Int,
		readTimeout: Int = 100,
		firstTimeout: Int = timeout
	): String {
		return recv(maxsize, readTimeout, firstTimeout).toUTF8String()
	}
	
	fun recvInt(
		timeout1: Int = timeout
	): Int? {
		val buffer = ByteArray(4)
		socket.soTimeout = timeout1
		var sTime = System.currentTimeMillis()
		//读取数据
		var rSize = inputStream.read(buffer, 0, 4)
		while (rSize < 4) {
			val sTime2 = System.currentTimeMillis()
			socket.soTimeout -= (sTime2 - sTime).toInt()
			sTime = sTime2
			val sReadSize = inputStream.read(buffer, rSize, 8 - rSize)
			if (sReadSize <= 0) {
				break
			} else {
				rSize += sReadSize
			}
		}
		return buffer.toInt()
	}
	
	fun recvLong(
		timeout1: Int = timeout
	): Long? {
		val buffer = ByteArray(8)
		socket.soTimeout = timeout1
		var sTime = System.currentTimeMillis()
		//读取数据
		var rSize = inputStream.read(buffer, 0, 8)
		while (rSize < 4) {
			val sTime2 = System.currentTimeMillis()
			socket.soTimeout -= (sTime2 - sTime).toInt()
			sTime = sTime2
			val sReadSize = inputStream.read(buffer, rSize, 8 - rSize)
			if (sReadSize <= 0) {
				break
			} else {
				rSize += sReadSize
			}
		}
		return buffer.toLong()
	}
	
	fun recv(
		readTimeout: Int = 100,
		firstTimeout: Int = timeout
	): ByteArray {
		val outputStream = ByteArrayOutputStream()
		recv(outputStream, readTimeout, firstTimeout)
		return outputStream.toByteArray()
	}
	
	fun recv(
		maxsize: Int,
		readTimeout: Int = 100,
		firstTimeout: Int = timeout
	): ByteArray {
		val buffer = ByteArray(maxsize)
		var readSize = 0
		socket.soTimeout = firstTimeout
		
		try {
			readSize = inputStream.read(buffer)
			
			socket.soTimeout = readTimeout
			while (readSize < buffer.size) {
				val sReadSize = inputStream.read(buffer, readSize, buffer.size - readSize)
				if (sReadSize <= 0) {
					break
				} else {
					readSize += sReadSize
				}
			}
		} catch (e: SocketTimeoutException) {
		}
		return buffer.copyOf(readSize)
	}
	
	fun recv(
		outputStream: OutputStream,
		readTimeout: Int = 100,
		firstTimeout: Int = timeout
	) {
		val buffer = ByteArray(1024)
		socket.soTimeout = firstTimeout
		
		try {
			val readSize = inputStream.read(buffer)
			outputStream.write(buffer, 0, readSize)
			socket.soTimeout = readTimeout
			while (true) {
				val sReadSize = inputStream.read(buffer)
				if (sReadSize <= 0) {
					break
				} else {
					outputStream.write(buffer, 0, readSize)
				}
			}
		} catch (e: SocketTimeoutException) {
		}
	}
	
	override fun close() {
		closeSocket()
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
	
	companion object Companion {
		const val defaultReadSize: Int = 1024 * 8
		const val timeout: Int = 60 * 1000
		
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
		
		
		/**
		 * 序列化
		 */
		fun serialize(`object`: Any): ByteArray? = try {
			val baos = ByteArrayOutputStream()
			val oos = ObjectOutputStream(baos)
			oos.writeObject(`object`)
			baos.toByteArray()
		} catch (e: Exception) {
			null
		}
		
		/**
		 * 反序列化
		 */
		fun unSerialize(bytes: ByteArray): Any? = try {
			ObjectInputStream(ByteArrayInputStream(bytes)).readObject()
		} catch (e: Exception) {
			null
		}
	}
}