package cn.tursom.web.netty

import cn.tursom.web.ExceptionContent
import cn.tursom.web.HttpHandler
import cn.tursom.web.HttpServer
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpRequestDecoder
import io.netty.handler.codec.http.HttpResponseEncoder

class NettyHttpServer(
	override val port: Int,
	handler: HttpHandler<NettyHttpContent>,
	bodySize: Int = 512 * 1024
) : HttpServer {
	constructor(
		port: Int,
		bodySize: Int = 512 * 1024,
		handler: (content: NettyHttpContent) -> Unit
	) : this(
		port,
		object : HttpHandler<NettyHttpContent> {
			override fun handle(content: NettyHttpContent) {
				handler(content)
			}

			override fun exception(e: ExceptionContent) {
				e.cause.printStackTrace()
			}
		},
		bodySize
	)
	
	val httpHandler = NettyHttpHandler(handler)
	private val group = NioEventLoopGroup()
	private val b = ServerBootstrap().group(group)
		.channel(NioServerSocketChannel::class.java)
		.childHandler(object : ChannelInitializer<SocketChannel>() {
			@Throws(Exception::class)
			override fun initChannel(ch: SocketChannel) {
				ch.pipeline()
					.addLast("decoder", HttpRequestDecoder())
					.addLast("encoder", HttpResponseEncoder())
					.addLast("aggregator", HttpObjectAggregator(bodySize))
					.addLast("handle", httpHandler)
			}
		})
		.option(ChannelOption.SO_BACKLOG, 1024) // determining the number of connections queued
		.option(ChannelOption.SO_REUSEADDR, true)
		.childOption(ChannelOption.SO_KEEPALIVE, java.lang.Boolean.TRUE)
	private lateinit var future: ChannelFuture
	
	override fun run() {
		future = b.bind(port)
		future.sync()
	}
	
	override fun close() {
		future.cancel(false)
		future.channel().close()
	}
}