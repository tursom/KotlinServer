package cn.tursom.socket

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.socket.SocketChannel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class AsyncCoroutineChannelNettySocket(override val channel: SocketChannel) : AsyncNettySocket {
	private val msgChannel = Channel<ByteBuf>(16)
	
	init {
		channel.pipeline().addLast(object : ChannelInboundHandlerAdapter() {
			override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
				msg as ByteBuf
				if (msgChannel.isBufferFull) {
					channel.config().isAutoRead = false
				}
				GlobalScope.launch { msgChannel.send(msg) }
			}
			
			override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
				cause.printStackTrace()
				ctx.close()
			}
		})
	}
	
	@ObsoleteCoroutinesApi
	override suspend fun read(timeout: Long): ByteBuf? {
		return when {
			timeout < 0 -> msgChannel.receive()
			timeout == 0L -> msgChannel.receiveOrNull()
			timeout > 0 -> withTimeout(timeout) { msgChannel.receive() }
			else -> throw Exception()
		}
	}
	
	override fun close() {
		channel.close()
		msgChannel.close()
	}
	
	companion object {
		private val methodIsBufferFull = run {
			val intChannel = Channel<Int>(1)
			intChannel.close()
			val method = intChannel.javaClass.getDeclaredMethod("isBufferFull")
			method.isAccessible = true
			method
		}
		
		val Channel<*>.isBufferFull get() = methodIsBufferFull.invoke(this) as Boolean
	}
}