package cn.tursom.utils.bytebuffer

import java.io.OutputStream
import java.nio.ByteBuffer

interface AdvanceByteBuffer {
	val nioBuffer: ByteBuffer

	/**
	 * 各种位置变量
	 */
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
		array.copyInto(array, arrayOffset, readOffset, arrayOffset + writePosition)
		writePosition = readableSize
		readPosition = 0
	}

	fun reset(outputStream: OutputStream) {
		outputStream.write(array, readOffset, arrayOffset + writePosition)
		writePosition = 0
		readPosition = 0
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

	fun writeTo(buffer: ByteArray, bufferOffset: Int = 0, size: Int = readableSize): Int
	fun writeTo(os: OutputStream): Int
	fun writeTo(buffer: AdvanceByteBuffer): Int {
		val size = readAllSize()
		readNioBuffer {
			buffer.nioBuffer.put(it)
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
}

inline fun <T> AdvanceByteBuffer.readMode(action: () -> T): T {
	readMode()
	return try {
		action()
	} finally {
		resumeWriteMode()
	}
}

inline fun <T> AdvanceByteBuffer.readNioBuffer(action: (nioBuffer: ByteBuffer) -> T): T {
	val buffer = nioBuffer
	val position = nioBuffer.position()
	return try {
		action(buffer)
	} finally {
		resumeWriteMode(nioBuffer.position() - position)
	}
}

inline fun <T> AdvanceByteBuffer.writeNioBuffer(action: (nioBuffer: ByteBuffer) -> T): T {
	val buffer = nioBuffer
	val position = writePosition
	val bufferPosition = nioBuffer.position()
	return try {
		action(buffer)
	} finally {
		writePosition = position + (nioBuffer.position() - bufferPosition)
	}
}

