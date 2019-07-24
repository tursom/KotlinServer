package cn.tursom.socket.server.async

import cn.tursom.socket.AsyncArrayListNettySocket
import cn.tursom.socket.AsyncNettySocket
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

/**
 * 使用简单的阻塞模型进行netty开发的初次尝试
 *
 */
class AsyncNettySocketServer(override val port: Int, handler: suspend AsyncNettySocket.() -> Unit) : AsyncServer {
	private val childHandler = ServerChannelInitializer(handler)
	private val bossGroup = NioEventLoopGroup()
	private val workerGroup = NioEventLoopGroup()
	private val b = ServerBootstrap().group(bossGroup, workerGroup)
		.channel(NioServerSocketChannel::class.java)
		.childHandler(childHandler)
		.option(ChannelOption.SO_BACKLOG, 128)
		.childOption(ChannelOption.SO_KEEPALIVE, true)
	private lateinit var f: ChannelFuture
	
	override fun run() {
		f = b.bind(port).sync()
	}
	
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
	
	@ChannelHandler.Sharable
	class ServerChannelInitializer(private val handler: suspend AsyncNettySocket.() -> Unit) : ChannelInitializer<SocketChannel>() {
		override fun initChannel(ch: SocketChannel) {
			val socket = AsyncArrayListNettySocket(ch)
			GlobalScope.launch { socket.use { it.handler() } }
		}
	}
}