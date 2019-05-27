package cn.tursom.web.netty

import cn.tursom.web.ExceptionContent
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext

class NettyExceptionContent(
	val ctx: ChannelHandlerContext?,
	override val cause: Throwable
) : ExceptionContent {
	override fun write(message: String) {
		ctx?.write(Unpooled.wrappedBuffer(message.toByteArray()))
	}
	
	override fun write(bytes: ByteArray) {
		ctx?.write(Unpooled.wrappedBuffer(bytes))
	}
	
	override fun finish() {
		ctx?.flush()
	}
}