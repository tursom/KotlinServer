package cn.tursom.utils.bytebuffer

import io.netty.buffer.ByteBuf
import java.io.OutputStream
import java.nio.ByteBuffer

class NettyAdvanceByteBuffer(val byteBuf: ByteBuf) : AdvanceByteBuffer {
	override val nioBuffer: ByteBuffer get() = byteBuf.nioBuffer()
	override val nioBuffers: Array<out ByteBuffer> get() = byteBuf.nioBuffers()

	override var writePosition: Int
		get() = byteBuf.writerIndex()
		set(value) {
			byteBuf.writerIndex(value)
		}
	override var limit: Int
		get() = byteBuf.capacity()
		set(value) {}
	override val capacity: Int get() = byteBuf.capacity()
	override val array: ByteArray get() = byteBuf.array()
	override val arrayOffset: Int get() = byteBuf.arrayOffset()
	override val readPosition: Int get() = byteBuf.readerIndex()
	override val readOffset: Int get() = byteBuf.arrayOffset() + byteBuf.readerIndex()
	override val readableSize: Int
		get() = byteBuf.readableBytes()
	override val available: Int get() = readableSize
	override val writeOffset: Int
		get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
	override val writeableSize: Int
		get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
	override val size: Int
		get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
	override val readMode: Boolean
		get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

	override fun readMode() {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun resumeWriteMode() {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun needReadSize(size: Int) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun useReadSize(size: Int): Int {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun take(size: Int): Int {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun push(size: Int): Int {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun readAllSize(): Int {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun takeAll(): Int {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun clear() {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun reset() {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun reset(outputStream: OutputStream) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun requireAvailableSize(size: Int) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun get(): Byte {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun getChar(): Char {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun getShort(): Short {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun getInt(): Int {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun getLong(): Long {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun getFloat(): Float {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun getDouble(): Double {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun getBytes(): ByteArray {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun getString(size: Int): String {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun writeTo(buffer: ByteArray, bufferOffset: Int, size: Int): Int {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun writeTo(buffer: AdvanceByteBuffer): Int {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun writeTo(os: OutputStream) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun putByte(byte: Byte): ByteBuffer {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun put(char: Char) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun put(short: Short) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun put(int: Int) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun put(long: Long) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun put(float: Float) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun put(double: Double) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun put(str: String) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun put(byteArray: ByteArray, startIndex: Int, endIndex: Int) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

}