package cn.tursom.socket.client

import cn.tursom.socket.AsyncSocket
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler

object AsyncClient {
	private val handler = object : CompletionHandler<Void, Channel<Throwable?>> {
		override fun completed(result: Void?, attachment: Channel<Throwable?>?) {
			GlobalScope.launch {
				attachment?.send(null)
			}
		}
		
		override fun failed(exc: Throwable?, attachment: Channel<Throwable?>?) {
			GlobalScope.launch {
				attachment?.send(exc)
			}
		}
	}
	
	fun connect(host: String, port: Int): AsyncSocket {
		val socketChannel = AsynchronousSocketChannel.open()!!
		return runBlocking { return@runBlocking connect(socketChannel, host, port) }
	}
	
	suspend fun connect(socketChannel: AsynchronousSocketChannel, host: String, port: Int): AsyncSocket {
		val channel = Channel<Throwable?>()
		socketChannel.connect(InetSocketAddress(host, port), channel, handler)
		channel.receive()?.let { throw it }
		channel.close()
		return AsyncSocket(socketChannel)
	}
}