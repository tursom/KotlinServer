package cn.tursom.asyncstream

import java.io.InputStream

@Suppress("unused")
class AsyncInputStream(
	@Suppress("MemberVisibilityCanBePrivate") val inputStream: InputStream
) : AsyncStream {
	suspend fun read(): Int {
		return run { inputStream.read() }
	}
	
	suspend fun read(b: ByteArray): Int {
		return run { inputStream.read(b) }
	}
	
	suspend fun read(b: ByteArray?, off: Int, len: Int): Int {
		return run { inputStream.read(b, off, len) }
	}
	
	suspend fun skip(n: Long): Long {
		return run { inputStream.skip(n) }
	}
	
	suspend fun available(): Int {
		return run { inputStream.available() }
	}
	
	suspend fun close() {
		return run { inputStream.close() }
	}
	
	suspend fun mark(readlimit: Int) {
		return run { inputStream.mark(readlimit) }
	}
	
	suspend fun reset() {
		return run { inputStream.reset() }
	}
	
	suspend fun markSupported(): Boolean {
		return run { inputStream.markSupported() }
	}
}