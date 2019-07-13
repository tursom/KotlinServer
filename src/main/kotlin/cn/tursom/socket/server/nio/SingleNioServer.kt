//package cn.tursom.socket.server.nio
//
//import io.netty.buffer.ByteBuf
//import io.netty.channel.ChannelHandler
//import io.netty.channel.ChannelHandlerContext
//import io.netty.channel.ChannelInboundHandlerAdapter
//
//class SingleNioServer(
//	port: Int,
//	handler: ChannelHandler.(ctx: ChannelHandlerContext, msg: ByteBuf) -> Unit
//) : NioServer(port, {
//	object : ChannelInboundHandlerAdapter() {
//		override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
//			handler(ctx, msg as ByteBuf)
//		}
//	}
//})