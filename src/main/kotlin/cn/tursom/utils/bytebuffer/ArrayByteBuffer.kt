package cn.tursom.utils.bytebuffer

import cn.tursom.utils.*
import java.io.OutputStream

class ArrayByteBuffer(val array: ByteArray, val offset: Int = 0, val size: Int = array.size - offset) {
	var writePosition = 0
	var readPosition = 0
	
	val readOffset get() = offset + readPosition
	val writeOffset get() = offset + writePosition
	
	val readByteBuffer get() = HeapByteBuffer.wrap(array, writePosition - readPosition, offset + readPosition)
	val writeByteBuffer get() = HeapByteBuffer.wrap(array, size - writePosition, offset + writePosition)
	
	val readableSize get() = writePosition - readPosition
	
	val position get() = "ArrayByteBuffer(size=$size, writePosition=$writePosition, readPosition=$readPosition)"
	
	/*
	 * 位置控制方法
	 */
	
	fun clear() {
		writePosition = 0
		readPosition = 0
	}
	
	fun reset() {
		array.copyInto(array, offset, readOffset, offset + writePosition)
		writePosition = readableSize
		readPosition = 0
	}
	
	fun reset(outputStream: OutputStream) {
		outputStream.write(array, readOffset, offset + writePosition)
		writePosition = 0
		readPosition = 0
	}
	
	fun needReadSize(size: Int) {
		if (readableSize < size) throw OutOfBufferException()
	}
	
	fun take(size: Int): Int {
		needReadSize(size)
		val offset = readOffset
		readPosition += size
		return offset
	}
	
	fun useReadSize(size: Int): Int {
		needReadSize(size)
		readPosition += size
		return size
	}
	
	fun push(size: Int): Int {
		val offset = writeOffset
		writePosition += size
		return offset
	}
	
	fun readAllSize() = useReadSize(readableSize)
	fun takeAll() = take(readableSize)
	
	
	/*
	 * 数据获取方法
	 */
	
	fun get() = array[take(1)]
	fun getChar() = array.toChar(take(2))
	fun getShort() = array.toShort(take(2))
	fun getInt() = array.toInt(take(4))
	fun getLong() = array.toLong(take(8))
	fun getFloat() = array.toFloat(take(4))
	fun getDouble() = array.toDouble(take(8))
	fun getBytes() = array.copyOfRange(readPosition, readAllSize())
	fun getString(size: Int = readableSize) = String(array, readPosition, useReadSize(size))
	
	fun writeTo(buffer: ByteArray, bufferOffset: Int = 0, size: Int = readableSize): Int {
		array.copyInto(buffer, bufferOffset, offset, useReadSize(size))
		return size
	}
	
	fun writeTo(buffer: AdvanceByteBuffer): Int {
		val size = readAllSize()
		buffer.writePosition += size
		array.copyInto(buffer.array, buffer.arrayOffset, offset, size)
		return size
	}
	
	fun toByteArray() = getBytes()
	
	
	/*
	 * 数据写入方法
	 */
	
	fun putByte(byte: Byte) = array.put(byte, push(1))
	fun put(char: Char) = array.put(char, push(2))
	fun put(short: Short) = array.put(short, push(2))
	fun put(int: Int) = array.put(int, push(4))
	fun put(long: Long) = array.put(long, push(8))
	fun put(float: Float) = array.put(float, push(4))
	fun put(double: Double) = array.put(double, push(8))
	fun put(str: String) = put(str.toByteArray())
	fun put(byteArray: ByteArray, startIndex: Int = 0, endIndex: Int = byteArray.size) =
		byteArray.copyInto(array, push(endIndex - startIndex), startIndex, endIndex)
	
	override fun toString(): String {
		return String(array, readOffset, readableSize)
	}
	
	/**
	 * 缓冲区用完异常
	 */
	class OutOfBufferException : Exception()
}