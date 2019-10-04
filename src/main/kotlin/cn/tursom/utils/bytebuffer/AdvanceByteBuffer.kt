package cn.tursom.utils.bytebuffer

import sun.reflect.generics.reflectiveObjects.NotImplementedException
import java.io.OutputStream
import java.nio.ByteBuffer
import kotlin.math.min

interface AdvanceByteBuffer {
	val nioBuffer: ByteBuffer
	val nioBuffers: Array<out ByteBuffer> get() = arrayOf(nioBuffer)

	/**
	 * 各种位置变量
	 */
	val hasArray: Boolean
	val readOnly: Boolean
	val singleBuffer: Boolean get() = true

	var writePosition: Int
	var limit: Int
	val capacity: Int
	val array: ByteArray
	val arrayOffset: Int
	var readPosition: Int
	val readOffset: Int
	val readableSize: Int
	val available: Int
	val writeOffset: Int
	val writeableSize: Int
	val size: Int
	val readMode: Boolean


	fun readMode()
	fun resumeWriteMode(usedSize: Int = 0)

	fun needReadSize(size: Int) {
		if (readableSize < size) throw OutOfBufferException()
	}

	fun useReadSize(size: Int): Int {
		needReadSize(size)
		readPosition += size
		return size
	}

	fun take(size: Int): Int {
		needReadSize(size)
		val offset = readOffset
		readPosition += size
		return offset
	}

	fun push(size: Int): Int {
		val offset = writeOffset
		writePosition += size
		return offset
	}

	fun readAllSize() = useReadSize(readableSize)
	fun takeAll() = take(readableSize)

	fun clear()

	fun reset() {
		if (hasArray) {
			array.copyInto(array, arrayOffset, readOffset, arrayOffset + writePosition)
			writePosition = readableSize
			readPosition = 0
		}
	}

	fun reset(outputStream: OutputStream) {
		if (hasArray) {
			outputStream.write(array, readOffset, arrayOffset + writePosition)
			writePosition = 0
			readPosition = 0
		}
	}

	fun requireAvailableSize(size: Int) {
		if (limit - readPosition < size) reset()
	}


	/*
	 * 数据获取方法
	 */

	fun get(): Byte
	fun getChar(): Char
	fun getShort(): Short
	fun getInt(): Int
	fun getLong(): Long
	fun getFloat(): Float
	fun getDouble(): Double
	fun getBytes(): ByteArray
	fun getString(size: Int = readableSize): String

	fun writeTo(buffer: ByteArray, bufferOffset: Int = 0, size: Int = min(readableSize, buffer.size)): Int {
		val readSize = min(readableSize, size)
		if (hasArray) {
			array.copyInto(buffer, bufferOffset, readOffset, readOffset + readSize)
			readPosition += readOffset
			reset()
		} else {
			readBuffer {
				it.put(buffer, bufferOffset, readSize)
			}
		}
		return readSize
	}

	fun writeTo(os: OutputStream): Int {
		val size = readableSize
		if (hasArray) {
			os.write(array, arrayOffset + readPosition, size)
			readPosition += size
			reset()
		} else {
			val buffer = ByteArray(1024)
			readBuffer {
				while (it.remaining() > 0) {
					it.put(buffer)
					os.write(buffer)
				}
			}
		}
		return size
	}

	fun writeTo(buffer: AdvanceByteBuffer): Int {
		val size = min(readableSize, buffer.writeableSize)
		if (hasArray && buffer.hasArray) {
			array.copyInto(buffer.array, buffer.writeOffset, readOffset, readOffset + size)
			buffer.writePosition += size
			readPosition += size
			reset()
		} else {
			readBuffer {
				buffer.nioBuffer.put(it)
			}
		}
		return size
	}

	fun toByteArray() = getBytes()


	/*
	 * 数据写入方法
	 */

	fun put(byte: Byte)
	fun put(char: Char)
	fun put(short: Short)
	fun put(int: Int)
	fun put(long: Long)
	fun put(float: Float)
	fun put(double: Double)
	fun put(str: String)

	fun put(byteArray: ByteArray, startIndex: Int = 0, endIndex: Int = byteArray.size) {
		for (i in startIndex until endIndex) {
			put(byteArray[i])
		}
	}

	fun put(array: CharArray, index: Int = 0, size: Int = array.size - index) {
		for (i in index until index + size) {
			put(array[i])
		}
	}

	fun put(array: ShortArray, index: Int = 0, size: Int = array.size - index) {
		for (i in index until index + size) {
			put(array[i])
		}
	}

	fun put(array: IntArray, index: Int = 0, size: Int = array.size - index) {
		for (i in index until index + size) {
			put(array[i])
		}
	}

	fun put(array: LongArray, index: Int = 0, size: Int = array.size - index) {
		for (i in index until index + size) {
			put(array[i])
		}
	}

	fun put(array: FloatArray, index: Int = 0, size: Int = array.size - index) {
		for (i in index until index + size) {
			put(array[i])
		}
	}

	fun put(array: DoubleArray, index: Int = 0, size: Int = array.size - index) {
		for (i in index until index + size) {
			put(array[i])
		}
	}

	fun peekString(size: Int = readableSize): String {
		val readP = readPosition
		val str = getString(size)
		readPosition = readP
		return str
	}

	fun <T> readBuffer(action: (nioBuffer: ByteBuffer) -> T): T = readNioBuffer(action)
	fun <T> writeBuffer(action: (nioBuffer: ByteBuffer) -> T): T = writeNioBuffer(action)

	suspend fun <T> readSuspendBuffer(action: suspend (nioBuffer: ByteBuffer) -> T): T = readNioBuffer { action(it) }
	suspend fun <T> writeSuspendBuffer(action: suspend (nioBuffer: ByteBuffer) -> T): T = writeNioBuffer { action(it) }

	fun split(from: Int = readPosition, to: Int = writePosition): AdvanceByteBuffer {
		return if (hasArray) {
			ByteArrayAdvanceByteBuffer(array, arrayOffset + readPosition, to - from)
		} else {
			throw NotImplementedException()
		}
	}
}

inline fun <T> AdvanceByteBuffer.readNioBuffer(action: (nioBuffer: ByteBuffer) -> T): T {
	readMode()
	val buffer = nioBuffer
	val position = nioBuffer.position()
	return try {
		action(buffer)
	} finally {
		resumeWriteMode(buffer.position() - position)
	}
}

inline fun <T> AdvanceByteBuffer.writeNioBuffer(action: (nioBuffer: ByteBuffer) -> T): T {
	val buffer = nioBuffer
	val position = writePosition
	val bufferPosition = nioBuffer.position()
	return try {
		action(buffer)
	} finally {
		writePosition = position + (buffer.position() - bufferPosition)
	}
}

