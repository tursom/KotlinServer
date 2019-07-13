package cn.tursom.socket.server.nio

import cn.tursom.socket.client.AsyncClient
import cn.tursom.socket.send
import cn.tursom.utils.bytebuffer.ArrayByteBuffer
import cn.tursom.utils.doEach
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.Closeable
import java.io.IOException
import java.lang.Thread.sleep
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.*
import kotlin.concurrent.thread
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * 使用非阻塞模式的SocketChannel,ServerSocketChannel.
 */
class AsyncNioServer(val port: Int, val handler: suspend AsyncNioSocket.() -> Unit) : Closeable, Runnable {
	private var selector: Selector = Selector.open()
	private var serverSocketChannel: ServerSocketChannel = ServerSocketChannel.open()
	
	init {
		serverSocketChannel.socket().reuseAddress = true
		// 使serverSocketChannel工作于非阻塞模式
		serverSocketChannel.configureBlocking(false)
		serverSocketChannel.socket().bind(InetSocketAddress(port))
	}
	
	override fun run() {
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT)
		while (selector.select() > 0) {
			val readyKeys = selector.selectedKeys()
			val it = readyKeys.iterator()
			it.forEach { key ->
				try {
					it.remove()
					key ?: return@forEach
					when {
						key.isAcceptable -> {
							val ssc = key.channel() as ServerSocketChannel
							val socketChannel = ssc.accept() as SocketChannel
							println("接收到客户连接，来自：${socketChannel.socket().inetAddress}:${socketChannel.socket().port}")
							GlobalScope.launch { AsyncNioSocket(socketChannel).handler() }
						}
					}
				} catch (e: IOException) {
					e.printStackTrace()
					try {
						if (key != null) {
							key.cancel()
							key.channel().close()
						}
					} catch (ex: Exception) {
						ex.printStackTrace()
					}
				}
			}
		}
	}
	
	override fun close() {
		serverSocketChannel.close()
		selector.close()
	}
}

class AsyncNioSocket(private val socketChannel: SocketChannel) {
	init {
		socketChannel.configureBlocking(false)
		selector.wakeup()
	}
	
	val socket get() = socketChannel.socket()
	val localAddress get() = socketChannel.socket().localAddress
	val address get() = socketChannel.socket().inetAddress
	val port get() = socketChannel.socket().port
	
	val key = socketChannel.register(selector, SelectionKey.OP_READ)
	
	suspend fun read(buffer: ArrayByteBuffer): Int {
		return suspendCoroutine { cont ->
			key.attach(Context(buffer, cont))
			key.interestOps(SelectionKey.OP_READ)
			//socketChannel.register(selector, SelectionKey.OP_READ, Context(buffer, cont))
			//selector.wakeup()
		}
	}
	
	suspend fun write(buffer: ArrayByteBuffer): Int {
		return suspendCoroutine { cont ->
			key.attach(Context(buffer, cont))
			key.interestOps(SelectionKey.OP_WRITE)
			//socketChannel.register(selector, SelectionKey.OP_WRITE, Context(buffer, cont))
			//selector.wakeup()
		}
	}
	
	data class Context(val buffer: ArrayByteBuffer, var cont: Continuation<Int>)
	
	companion object {
		private var selector: Selector = Selector.open()
		private val selectBlock = {
			while (selector.select() >= 0) {
				//System.err.println("${System.currentTimeMillis()}: new select")
				//System.err.println(selector.keys().doEach { key -> "($key: ${key.interestOps()}, ${key.isReadable}, ${key.isWritable})" })
				val readyKeys = selector.selectedKeys()
				System.err.println(readyKeys.doEach { key -> "($key: ${key.isReadable}, ${key.isWritable}, ${key.attachment()})" })
				val iterator = readyKeys.iterator()
				iterator.forEach { key ->
					key ?: return@forEach
					val ctx = key.attachment() as Context? ?: return@forEach
					key.attach(null)
					iterator.remove()
					try {
						when {
							key.isReadable -> {
								key.interestOps(0)
								val socketChannel = key.channel() as SocketChannel
								val writeBuffer = ctx.buffer.writeByteBuffer
								if (writeBuffer.limit() == 0) ctx.cont.resume(0)
								socketChannel.read(writeBuffer)
								if (writeBuffer.position() == 0) ctx.cont.resumeWithException(Exception())
								ctx.buffer.writePosition += writeBuffer.position()
								ctx.cont.resume(writeBuffer.position())
							}
							key.isWritable -> {
								key.interestOps(0)
								val socketChannel = key.channel() as SocketChannel
								val readBuffer = ctx.buffer.readByteBuffer
								if (readBuffer.limit() == 0) ctx.cont.resume(0)
								socketChannel.write(readBuffer)
								if (readBuffer.position() == 0) ctx.cont.resumeWithException(Exception())
								ctx.buffer.readPosition += readBuffer.position()
								ctx.cont.resume(readBuffer.position())
							}
						}
					} catch (e: Throwable) {
						e.printStackTrace()
						try {
							ctx.cont.resumeWithException(e)
						} catch (e: Throwable) {
							e.printStackTrace()
						}
					}
				}
			}
		}
		
		init {
			thread(start = true, isDaemon = true, block = selectBlock)
		}
	}
}

fun main() {
	val port = 12345
	val server = AsyncNioServer(port) {
		val buffer = ArrayByteBuffer(ByteArray(1024))
		try {
			while (true) {
				buffer.clear()
				read(buffer)
				println("从客户端返回数据: $buffer")
				write(buffer)
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}
	thread(block = server::run, start = true, isDaemon = true)
	runBlocking {
		repeat(10) {
			val client = AsyncClient.connect("127.0.0.1", port)
			val buffer = ByteBuffer.allocate(1024)
			client.send("hello")
			buffer.clear()
			client.read(buffer)
			buffer.flip()
			println(String(buffer.array(), buffer.arrayOffset(), buffer.limit()))
			client.close()
		}
	}
	//server.close()
	sleep(100)
}