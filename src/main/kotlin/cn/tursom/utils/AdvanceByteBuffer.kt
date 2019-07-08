package cn.tursom.utils

import java.io.OutputStream
import java.nio.ByteBuffer

@Suppress("unused", "MemberVisibilityCanBePrivate")
class AdvanceByteBuffer(val buffer: ByteBuffer) {
	
	constructor(size: Int) : this(ByteBuffer.allocate(size))
	
	constructor(buffer: ByteArray, offset: Int = 0, size: Int = buffer.size - offset) : this(HeapByteBuffer.warp(buffer, size, offset))
	
	private var readLocation = 0
	
	/**
	 * 各种位置变量
	 */
	var writePosition
		get() = buffer.position()
		set(value) {
			buffer.position(value)
		}
	var limit
		get() = buffer.limit()
		set(value) {
			buffer.limit(value)
		}
	
	val capacity: Int = buffer.capacity()
	val array: ByteArray = buffer.array()
	val arrayOffset: Int = buffer.arrayOffset()
	val readPosition get() = readLocation
	val readOffset get() = arrayOffset + readPosition
	val readSize get() = writePosition - readPosition
	val available get() = readSize
	val writeOffset get() = arrayOffset + writePosition
	val writeSize get() = limit - writePosition
	
	/*
	 * 位置控制方法
	 */
	
	fun needReadSize(size: Int) {
		if (readSize < size) throw OutOfBufferException()
	}
	
	fun useReadSize(size: Int): Int {
		needReadSize(size)
		readLocation += size
		return size
	}
	
	fun take(size: Int): Int {
		needReadSize(size)
		val offset = readOffset
		readLocation += size
		return offset
	}
	
	fun push(size: Int): Int {
		val offset = writeOffset
		writePosition += size
		return offset
	}
	
	fun readAllSize() = useReadSize(readSize)
	fun takeAll() = take(readSize)
	
	fun clear() {
		readLocation = 0
		buffer.clear()
	}
	
	fun reset() {
		array.copyInto(array, arrayOffset, readOffset, arrayOffset + writePosition)
		writePosition = readSize
		readLocation = 0
	}
	
	fun reset(outputStream: OutputStream) {
		outputStream.write(array, readOffset, arrayOffset + writePosition)
		writePosition = 0
		readLocation = 0
	}
	
	
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
	fun getBytes() = array.copyOfRange(arrayOffset, readAllSize())
	fun getString(size: Int = readSize) = String(array, readOffset, useReadSize(size))
	
	fun get(buffer: ByteArray, size: Int = readSize, bufferOffset: Int = 0): Int {
		array.copyInto(buffer, bufferOffset, arrayOffset, useReadSize(size))
		return size
	}
	
	fun toByteArray() = getBytes()
	
	
	/*
	 * 数据写入方法
	 */
	
	fun putByte(byte: Byte): ByteBuffer = buffer.put(byte)
	fun put(char: Char) = array.put(char, push(2))
	fun put(short: Short) = array.put(short, push(2))
	fun put(int: Int) = array.put(int, push(4))
	fun put(long: Long) = array.put(long, push(8))
	fun put(float: Float) = array.put(float, push(4))
	fun put(double: Double) = array.put(double, push(8))
	fun put(str: String) = put(str.toByteArray())
	fun put(byteArray: ByteArray, startIndex: Int = 0, endIndex: Int = byteArray.size) =
		byteArray.copyInto(array, push(endIndex - startIndex), startIndex, endIndex)
	
	/**
	 * 缓冲区用完异常
	 */
	class OutOfBufferException : Exception()
}

fun main() {
	val buffer = AdvanceByteBuffer(ByteBuffer.allocate(1024))
	buffer.put("hello world!")
	println(buffer.getString())
}