package cn.tursom.socket.server

import cn.tursom.socket.AsyncArrayListNettySocket
import cn.tursom.socket.AsyncNettySocket
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.Closeable
import kotlin.concurrent.thread

class AsyncNettySocketServer(val port: Int, handler: suspend AsyncNettySocket.() -> Unit) : Closeable {
	private val bossGroup = NioEventLoopGroup()
	private val workerGroup = NioEventLoopGroup()
	private val b = ServerBootstrap().group(bossGroup, workerGroup)
		.channel(NioServerSocketChannel::class.java)
		.childHandler(object : ChannelInitializer<SocketChannel>() {
			override fun initChannel(ch: SocketChannel) {
				val socket = AsyncArrayListNettySocket(ch)
				GlobalScope.launch { socket.use { it.handler() } }
			}
		})
		.option(ChannelOption.SO_BACKLOG, 128)
		.childOption(ChannelOption.SO_KEEPALIVE, true)
	
	// Bind and start to accept incoming connections.
	val f = b.bind(port).sync()
	
	override fun close() {
		thread(start = true, isDaemon = true) {
			try {
				f.channel().closeFuture().sync()
			} finally {
				workerGroup.shutdownGracefully()
				bossGroup.shutdownGracefully()
			}
		}
		f.channel().close()
	}
}