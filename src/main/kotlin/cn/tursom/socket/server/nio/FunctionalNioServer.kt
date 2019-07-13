package cn.tursom.socket.server.nio

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter

//class FunctionalNioServer(
//	port: Int,
//	onAccept: (ChannelHandler.(ctx: ChannelHandlerContext?) -> Unit)? = null,
//	onClose: (ChannelHandler.(ctx: ChannelHandlerContext?) -> Unit)? = null,
//	handler: (ChannelHandler.(ctx: ChannelHandlerContext, msg: ByteBuf) -> Unit)? = null
//) : NioServer(port, {
//	object : ChannelInboundHandlerAdapter() {
//		override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
//			handler?.let { it(ctx, msg as ByteBuf) }
//		}
//
//		override fun channelUnregistered(ctx: ChannelHandlerContext?) {
//			onClose?.let { it(ctx) }
//		}
//
//		override fun channelActive(ctx: ChannelHandlerContext?) {
//			onAccept?.let { it(ctx) }
//		}
//	}
//})