package cn.tursom.socket

import cn.tursom.datagram.client.UdpClient
import cn.tursom.socket.BaseSocket.Companion.timeout
import cn.tursom.socket.client.AioClient
import cn.tursom.socket.server.aio.AioServer
import org.junit.Test
import java.lang.Thread.sleep
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class AioServerTest {
	@Test
	fun testServer() {
		val port = 12345
		val server = AioServer(port) {
			timeout = 1000L
			recvStr { str ->
				val time = System.currentTimeMillis()
				println("$time: server recved:\"$str\"")
			}
			// 执行完应当循环执行第一步
			send(next = { 0 }) {
				buffer.flip()
				buffer
			}
		}
		
		for (i in 1..10000) {
			var j = 1
			AioClient("127.0.0.1", port) {
				sendStr { "client $i loop $j" }
				recvStr({ buffer }, {
					if (j <= 10) 0
					else 100
				}) { recv ->
					println("${System.currentTimeMillis()}: client recving: $recv")
					j++
				}
			}
		}

//		for (i in 1..500) {
//			Thread({
//				for (k in 1..200) {
//					SocketClient("127.0.0.1", port).use {
//						for (j in 1..1) {
//							write("client $i$k, loop $j")
//							val read = recvString()
////						println("${System.currentTimeMillis()}: client recving: $read")
//						}
//					}
//				}
//			}, "SocketClient$i").start()
//		}

//		sleep(15000)
		println(AioSocket.maxId)
		server.close()
	}
	
	fun datagramServer(port: Int) {
		val threadPool = ThreadPoolExecutor(
			1,
			1,
			0L,
			TimeUnit.MILLISECONDS,
			LinkedBlockingQueue(16)
		)
		val channel = DatagramChannel.open()
		channel.configureBlocking(false)
		channel.socket().bind(InetSocketAddress(port))
		val selector = Selector.open()
		channel.register(selector, SelectionKey.OP_READ)
		
		val byteBuffer = ByteBuffer.allocate(65536)
		Thread {
			while (true) {
				try {
					// 进行选择
					if (selector.select() > 0) {
						// 获取以选择的键的集合
						val iterator = selector.selectedKeys().iterator()
						
						while (iterator.hasNext()) {
							val key = iterator.next() as SelectionKey
							
							// 必须手动删除
							iterator.remove()
							
							if (key.isReadable) {
								val datagramChannel = key.channel() as DatagramChannel
								threadPool.execute {
									// 读取
									byteBuffer.clear()
									val address = datagramChannel.receive(byteBuffer) ?: return@execute
									
									// 删除缓冲区中的数据
									byteBuffer.clear()
									val message = "data come from server"
									byteBuffer.put(message.toByteArray())
									byteBuffer.flip()
									
									// 发送数据
									datagramChannel.send(byteBuffer, address)
								}
							}
						}
					}
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
		}.start()
	}
	
	@Test
	fun a() {
		val port = 12345
		datagramServer(port)
		val udpClient = UdpClient("127.0.0.1", port)
		println("server started")
		udpClient.send("hello".toByteArray()) { it, size ->
			println(String(it, 0, size))
		}
	}
}