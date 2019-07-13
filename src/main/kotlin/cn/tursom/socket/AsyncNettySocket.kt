package cn.tursom.socket

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.socket.SocketChannel
import java.io.Closeable

interface AsyncNettySocket : Closeable {
	val channel: SocketChannel
	
	/**
	 * return buffer if have buffed msg
	 * @param timeout < 0 if block forever
	 *                = 0 return null instantly
	 *                > 0 block max {timeout} ms
	 *                    return null when timeout
	 */
	suspend fun read(timeout: Long = 0): ByteBuf?
}

fun AsyncNettySocket.write(buf: ByteBuf) {
	channel.writeAndFlush(buf)
}

fun AsyncNettySocket.write(buf: ByteArray) {
	channel.writeAndFlush(Unpooled.wrappedBuffer(buf))
}

fun AsyncNettySocket.write(string: String) {
	channel.writeAndFlush(string.toByteArray())
}