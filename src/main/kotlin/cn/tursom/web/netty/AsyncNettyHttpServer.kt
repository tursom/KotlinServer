package cn.tursom.web.netty

import cn.tursom.web.*
import io.netty.bootstrap.AbstractBootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpRequestDecoder
import io.netty.handler.codec.http.HttpResponseEncoder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class AsyncNettyHttpHandler(
    private val handler: AsyncHttpHandler<NettyHttpContent>
) : SimpleChannelInboundHandler<FullHttpRequest>() {

    override fun channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest) {
        val handlerContext = NettyHttpContent(ctx, msg, msg.uri())
        GlobalScope.launch {
            @Suppress("UNCHECKED_CAST")
            handler.handle(handlerContext)
            handlerContext.finish()
        }
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        super.channelReadComplete(ctx)
        ctx.flush()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        if (cause != null) GlobalScope.launch {
            handler.exception(cause)
        }
        ctx?.close()
    }
}

class AsyncNettyHttpServer(
    override val port: Int,
    handler: AsyncHttpHandler<NettyHttpContent>,
    bodySize: Int = 512 * 1024
) : HttpServer {

    constructor(
        port: Int,
        bodySize: Int = 512 * 1024,
        handler: suspend (content: NettyHttpContent) -> Unit
    ) : this(
        port,
        object : AsyncHttpHandler<NettyHttpContent> {
            override suspend fun handle(content: NettyHttpContent) = handler(content)
            override suspend fun exception(e: Throwable) = e.printStackTrace()
        },
        bodySize
    )

    private val group = NioEventLoopGroup()
    private val b = ServerBootstrap()
        .group(group)
        .channel(NioServerSocketChannel::class.java)
        .childHandler(object : ChannelInitializer<SocketChannel>() {
            @Throws(Exception::class)
            override fun initChannel(ch: SocketChannel) {
                ch.pipeline()
                    .addLast("decoder", HttpRequestDecoder())
                    .addLast("encoder", HttpResponseEncoder())
                    .addLast("aggregator", HttpObjectAggregator(bodySize))
                    .addLast("handle", AsyncNettyHttpHandler(handler))
            }
        })
        .option(ChannelOption.SO_BACKLOG, 128) // determining the number of connections queued
        .childOption(ChannelOption.SO_KEEPALIVE, java.lang.Boolean.TRUE)
    private val f = b.bind(port)

    override fun run() {
        val field = AbstractBootstrap::class.java.getDeclaredField("options")
        field.isAccessible = true
        val options = (field.get(b) as Map<ChannelOption<*>, Object>)
        println(options)
        f.sync()
    }

    override fun close() {
        f.cancel(false)
    }
}