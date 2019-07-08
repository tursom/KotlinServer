package cn.tursom.utils

import java.io.OutputStream
import java.nio.ByteBuffer

@Suppress("unused", "MemberVisibilityCanBePrivate")
class AdvanceByteBuffer(val buffer: ByteBuffer) {
	
	constructor(size: Int) : this(ByteBuffer.allocate(size))
	
	constructor(buffer: ByteArray, offset: Int = 0, size: Int = buffer.size - offset) : this(HeapByteBuffer.warp(buffer, size, offset))
	
	private var readLocation = 0
	
	var position
		get() = buffer.position()
		set(value) {
			buffer.position(value)
		}
	var limit
		get() = buffer.limit()
		set(value) {
			buffer.limit(value)
		}
	
	val capacity get() = buffer.capacity()
	val array: ByteArray get() = buffer.array()
	val arrayOffset get() = buffer.arrayOffset()
	val readPosition get() = readLocation
	
	val readOffset get() = arrayOffset + readPosition
	val readSize get() = position - readPosition
	val writeOffset get() = arrayOffset + position
	val writeSize get() = limit - position
	
	fun needSize(size: Int) {
		if (readSize < size) throw OutOfBufferException()
	}
	
	fun useSize(size: Int): Int {
		needSize(size)
		readLocation += size
		return size
	}
	
	fun take(size: Int): Int {
		needSize(size)
		val offset = readOffset
		readLocation += size
		return offset
	}
	
	fun push(size: Int): Int {
		val offset = writeOffset
		position += size
		return offset
	}
	
	fun get() = array[take(1)]
	fun getChar() = array.toChar(take(2))
	fun getShort() = array.toShort(take(2))
	fun getInt() = array.toInt(take(4))
	fun getLong() = array.toLong(take(8))
	fun getFloat() = array.toFloat(take(4))
	fun getDouble() = array.toDouble(take(8))
	fun getString(size: Int = readSize) = String(array, readOffset, useSize(size))
	
	fun put(char: Char) = array.push(char, push(2))
	fun put(short: Short) = array.push(short, push(2))
	fun put(int: Int) = array.push(int, push(4))
	fun put(long: Long) = array.push(long, push(8))
	fun put(float: Float) = array.push(float, push(4))
	fun put(double: Double) = array.push(double, push(8))
	
	fun putByte(byte: Byte): ByteBuffer = buffer.put(byte)
	fun put(byteArray: ByteArray): ByteBuffer = buffer.put(byteArray)
	fun put(str: String) = put(str.toByteArray())
	
	fun clear() {
		readLocation = 0
		buffer.clear()
	}
	
	fun reset() {
		val array = this.array
		array.copyInto(array, arrayOffset, readOffset, arrayOffset + position)
		position = readSize
		readLocation = 0
	}
	
	fun reset(outputStream: OutputStream) {
		outputStream.write(array, readOffset, arrayOffset + position)
		position = 0
		readLocation = 0
	}
	
	class OutOfBufferException : Exception()
}

fun main() {
	val buffer = AdvanceByteBuffer(ByteBuffer.allocate(1024))
	buffer.put("hello world!")
	println(buffer.getString())
}