package cn.tursom.socket.server.nio

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import java.io.Closeable

open class NioServer(
	port: Int,
	onAccept: (ctx: SocketChannel) -> ChannelHandler
) : Closeable {
	private val bossGroup = NioEventLoopGroup(1)
	private val workerGroup = NioEventLoopGroup()
	private val b = ServerBootstrap()
		.group(bossGroup, workerGroup)
		.channel(NioServerSocketChannel::class.java)
		.childHandler(object : ChannelInitializer<SocketChannel>() {
			public override fun initChannel(ch: SocketChannel) {
				ch.pipeline().addLast(onAccept(ch))
			}
		})
		.option(ChannelOption.SO_BACKLOG, 128)
		.childOption(ChannelOption.SO_KEEPALIVE, true)!!
	private val f = b.bind(port).sync()!!
	
	final override fun close() {
		f.channel().close()
		workerGroup.shutdownGracefully()
		bossGroup.shutdownGracefully()
	}
}

fun ChannelHandlerContext.send(buf: ByteBuf) {
	write(buf)
}

fun ChannelHandlerContext.send(data: ByteArray) {
	val buffer = ByteBufAllocator.DEFAULT.heapBuffer(data.size)!!
	buffer.writeBytes(data)
	write(buffer)
}

fun ChannelHandlerContext.send(message: String) {
	send(message.toByteArray())
}