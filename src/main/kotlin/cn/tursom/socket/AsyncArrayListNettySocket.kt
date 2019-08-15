package cn.tursom.socket

import cn.tursom.socket.client.AsyncClient
import cn.tursom.socket.server.async.AsyncNettySocketServer
import cn.tursom.utils.asynclock.AsyncWaitList
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.socket.SocketChannel
import io.netty.util.CharsetUtil
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

class AsyncArrayListNettySocket(override val channel: SocketChannel) : AsyncNettySocket {
	private val waitReadList = AsyncWaitList()
	private val msgList = ArrayList<ByteBuf>(4)
	
	init {
		channel.pipeline().addLast(object : ChannelInboundHandlerAdapter() {
			override fun channelRead(ctx: ChannelHandlerContext, msg: Any) = synchronized(msgList) {
				msg as ByteBuf
				if (msgList.size >= 15) {
					channel.config().isAutoRead = false
				}
				msgList.add(msg)
				if (waitReadList.notEmpty) {
					waitReadList.resume()
				}
			}
			
			override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
				cause.printStackTrace()
				ctx.close()
			}
		})
	}
	
	private suspend fun waitRead(): ByteBuf {
		waitReadList.wait()
		return msgList.toCompositeBufferAndClear()
	}
	
	override suspend fun read(timeout: Long): ByteBuf? {
		return if (msgList.isNotEmpty()) {
			msgList.toCompositeBufferAndClear()
		} else {
			when {
				timeout < 0 -> {
					waitRead()
				}
				timeout == 0L -> null
				timeout > 0 -> {
					withTimeoutOrNull(timeout) { waitRead() }
				}
				else -> throw Exception()
			}
		}
	}
	
	override fun close() {
		channel.close()
	}
	
	companion object {
		@JvmStatic
		fun ArrayList<ByteBuf>.toCompositeBufferAndClear(): ByteBuf = synchronized(this) {
			if (size == 1) {
				get(0)
			} else {
				val compositeByteBuf = Unpooled.compositeBuffer(size)
				forEach { compositeByteBuf.addComponent(true, it) }
				clear()
				compositeByteBuf
			}
		}
	}
}