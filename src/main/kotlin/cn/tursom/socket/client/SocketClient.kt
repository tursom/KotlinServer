package cn.tursom.socket.client

import org.apache.http.util.ByteArrayBuffer
import java.io.*
import java.lang.Thread.sleep
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

class SocketClient(
		private val host: String,
		private val port: Int,
		private val ioException: (io: IOException) -> Unit = { it.printStackTrace() },
		private val socketTimeoutException: (e: SocketTimeoutException) -> Unit = { it.printStackTrace() },
		private val exception: (e: Exception) -> Unit = { it.printStackTrace() }) {

	val address: String = "$host:$port"
	private val socket: Socket = Socket()
	private val bufferedReader: BufferedReader
	private val outputStream: OutputStream
	private val inputStream: InputStream

	init {
		println(this@SocketClient.address)
		socket.connect(InetSocketAddress(host, port))
		bufferedReader = BufferedReader(InputStreamReader(socket.getInputStream()))
		outputStream = socket.getOutputStream()
		inputStream = socket.getInputStream()
	}

	fun close() {
		try {
			if (!socket.isClosed) {
				socket.close()
			}
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}

	fun execute(func: () -> Unit) {
		try {
			func()
		} catch (io: IOException) {
			ioException(io)
		} catch (e: SocketClosedException) {
			System.err.println("socket $address has closed")
			close()
		} catch (e: ClientException) {
			if (e.message == null)
				e.printStackTrace()
			else
				System.err.println("$address: ${e::class.java}: ${e.message}")
		}
	}

	fun run(func: () -> Unit) {
		execute(func)
		close()
	}

	fun send(message: String) = send(message.toByteArray())
	fun send(message: ByteArray) {
		if (socket.isClosed) throw SocketClosedException()
		outputStream.write(message)
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

	fun isConnected(): Boolean {
		return socket.isConnected
	}

	open class ClientException(s: String? = null) : Exception(s) {
		open val code: ByteArray?
			get() = null
	}

	class SocketClosedException(s: String? = null) : ClientException(s)

	companion object {
		const val defaultReadSize: Int = 10240
		const val defaultMaxReadTime: Long = 10
		const val defaultMaxWaitTime: Long = 10 * 1000
		fun formatIpAddress(ip: Int) =
				"${ip and 0xff}.${(ip shr 8) and 0xff}.${(ip shr 16) and 0xff}.${(ip shr 24) and 0xff}"
	}
}
