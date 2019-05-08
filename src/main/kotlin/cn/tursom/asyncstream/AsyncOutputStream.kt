package cn.tursom.asyncstream

import java.io.OutputStream

@Suppress("unused")
class AsyncOutputStream(
	@Suppress("MemberVisibilityCanBePrivate") val outputStream: OutputStream
) : AsyncStream {
	suspend fun write(b: Int) {
		return run { outputStream.write(b) }
	}
	
	suspend fun write(b: ByteArray) {
		run { outputStream.write(b, 0, b.size) }
	}
	
	suspend fun write(b: ByteArray?, off: Int, len: Int) {
		return run { outputStream.write(b, off, len) }
	}
	
	suspend fun flush() {
		return run { outputStream.flush() }
	}
	
	suspend fun close() {
		return run { outputStream.close() }
	}
}