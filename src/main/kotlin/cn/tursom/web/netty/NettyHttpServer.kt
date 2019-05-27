package cn.tursom.web.netty

import cn.tursom.web.ExceptionContent
import cn.tursom.web.HttpHandler
import cn.tursom.web.HttpServer
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpRequestDecoder
import io.netty.handler.codec.http.HttpResponseEncoder

class NettyHttpHandler(
	private val handler: HttpHandler<NettyHttpContent>
) : SimpleChannelInboundHandler<FullHttpRequest>() {
	
	override fun channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest) {
		val handlerContext = NettyHttpContent(ctx, msg, msg.uri())
		try {
			handler.handle(handlerContext)
		} catch (e: Throwable) {
			handlerContext.write("${e.javaClass}: ${e.message}")
		}
	}
	
	override fun channelReadComplete(ctx: ChannelHandlerContext) {
		super.channelReadComplete(ctx)
		ctx.flush()
	}
	
	override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
		if (cause != null) handler.exception(NettyExceptionContent(ctx, cause))
		ctx?.close()
	}
}

class NettyHttpServer(
	override val port: Int,
	handler: HttpHandler<NettyHttpContent>,
	timeout: Int = 0,
	bodySize: Int = 512 * 1024
) : HttpServer {
	constructor(
		port: Int,
		timeout: Int = 0,
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
		timeout,
		bodySize
	)
	
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
					.addLast("handle", NettyHttpHandler(handler))
			}
		})
		.option(ChannelOption.SO_BACKLOG, 1024) // determining the number of connections queued
		.option(ChannelOption.SO_REUSEADDR, true)
		.option(ChannelOption.SO_TIMEOUT, timeout)
		.childOption(ChannelOption.SO_KEEPALIVE, java.lang.Boolean.TRUE)
	private lateinit var future: ChannelFuture
	
	override fun run() {
		try {
			close()
		} catch (e: Exception) {
		}
		future = b.bind(port)
		future.sync()
	}
	
	override fun close() {
		future.cancel(false)
		future.channel().close()
	}
}