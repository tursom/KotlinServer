package cn.tursom.web.netty

import cn.tursom.utils.bytebuffer.AdvanceByteBuffer
import cn.tursom.utils.bytebuffer.NettyAdvanceByteBuffer
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.stream.ChunkedInput

class NettyChunkedByteBuffer(val bufList: List<AdvanceByteBuffer>) : ChunkedInput<ByteBuf> {
	constructor(vararg bufList: AdvanceByteBuffer) : this(bufList.asList())

	var iterator = bufList.iterator()
	var progress: Long = 0
	val length = run {
		var len = 0L
		bufList.forEach {
			len += it.readableSize
		}
		len
	}

	override fun progress(): Long {
		System.err.println("progress: $progress")
		return progress
	}

	private fun readChunk(): ByteBuf {
		System.err.println("readChunk")
		val next = iterator.next()
		progress += next.readableSize
		return if (next is NettyAdvanceByteBuffer) next.byteBuf
		else Unpooled.wrappedBuffer(next.nioBuffer)
	}

	override fun readChunk(ctx: ChannelHandlerContext?): ByteBuf = readChunk()
	override fun readChunk(allocator: ByteBufAllocator?): ByteBuf = readChunk()

	override fun length(): Long {
		System.err.println("length: $length")
		return length
	}

	override fun isEndOfInput(): Boolean {
		System.err.println("isEndOfInput: ${!iterator.hasNext()}")
		return !iterator.hasNext()
	}

	override fun close() {
		System.err.println("close")
	}
}

