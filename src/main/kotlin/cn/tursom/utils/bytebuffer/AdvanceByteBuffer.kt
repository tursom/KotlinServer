package cn.tursom.utils.bytebuffer

import java.io.OutputStream
import java.nio.ByteBuffer

interface AdvanceByteBuffer {
	val nioBuffer: ByteBuffer
	val nioBuffers: Array<out ByteBuffer>

	/**
	 * 各种位置变量
	 */
	var writePosition: Int
	var limit: Int
	val capacity: Int
	val array: ByteArray
	val arrayOffset: Int
	val readPosition: Int
	val readOffset: Int
	val readableSize: Int
	val available: Int
	val writeOffset: Int
	val writeableSize: Int
	val size: Int
	val readMode: Boolean


	fun readMode()
	fun resumeWriteMode()

	fun needReadSize(size: Int)
	fun useReadSize(size: Int): Int
	fun take(size: Int): Int
	fun push(size: Int): Int
	fun readAllSize(): Int
	fun takeAll(): Int

	fun clear()
	fun reset()
	fun reset(outputStream: OutputStream)
	fun requireAvailableSize(size: Int)


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
	fun writeTo(buffer: AdvanceByteBuffer): Int
	fun writeTo(os: OutputStream)
	fun toByteArray() = getBytes()


	/*
	 * 数据写入方法
	 */

	fun putByte(byte: Byte): ByteBuffer
	fun put(char: Char)
	fun put(short: Short)
	fun put(int: Int)
	fun put(long: Long)
	fun put(float: Float)
	fun put(double: Double)
	fun put(str: String)
	fun put(byteArray: ByteArray, startIndex: Int = 0, endIndex: Int = byteArray.size)

}

inline fun <T> AdvanceByteBuffer.readMode(action: () -> T): T {
	readMode()
	return try {
		action()
	} finally {
		resumeWriteMode()
	}
}

