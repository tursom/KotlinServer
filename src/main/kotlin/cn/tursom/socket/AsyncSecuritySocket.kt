package cn.tursom.socket

import cn.tursom.utils.encrypt.AES
import java.io.Closeable
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

class AsyncSecuritySocket(private val asyncSocket: AsyncSocket, private val key: AES) : Closeable {
	suspend fun write(buffer: ByteBuffer, timeout: Long = AsyncSocket.defaultTimeout, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): Int {
		return write(
			ByteBuffer.wrap(key.encrypt(
				buffer.array(),
				buffer.arrayOffset() + buffer.position(),
				buffer.limit()
			)),
			timeout,
			timeUnit
		)
	}
	
	suspend fun read(buffer: ByteBuffer, timeout: Long = AsyncSocket.defaultTimeout, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): ByteArray {
		asyncSocket.read(buffer, timeout, timeUnit)
		buffer.flip()
		return key.encrypt(
			buffer.array(),
			buffer.arrayOffset() + buffer.position(),
			buffer.limit()
		)
	}
	
	override fun close() {
		asyncSocket.close()
	}
}