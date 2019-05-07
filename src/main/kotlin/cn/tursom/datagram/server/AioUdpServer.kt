package cn.tursom.datagram.server

import io.netty.util.HashedWheelTimer
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.util.concurrent.*

class AioUdpServer(
	override val port: Int,
	private val threadPool: ThreadPoolExecutor = ThreadPoolExecutor(
		1,
		1,
		0L,
		TimeUnit.MILLISECONDS,
		LinkedBlockingQueue(32)
	),
	private val queue: BlockingQueue<() -> Unit> = LinkedBlockingQueue(128),
	private val connectionMap: java.util.AbstractMap<
		SocketAddress,
		AioUdpServer.(
			channel: DatagramChannel,
			address: SocketAddress,
			buffer: ByteBuffer
		) -> Unit
		> = HashMap(),
	private val handler: AioUdpServer.(channel: DatagramChannel, address: SocketAddress, buffer: ByteBuffer) -> Unit
) : UDPServer {
	private val excWheelTimer = HashedWheelTimer()
	private val channel = DatagramChannel.open()!!
	private val selector = Selector.open()!!
	private var closed: Boolean = false
	
	init {
		excWheelTimer.start()
		channel.configureBlocking(false)
		channel.socket().bind(InetSocketAddress(port))
		channel.register(selector, SelectionKey.OP_READ)
	}
	
	override fun run() {
		val byteBuffer = ByteBuffer.allocateDirect(2048)
		
		while (!closed) {
			try {
				val taskQueue = queue.iterator()
				while (taskQueue.hasNext()) {
					taskQueue.next()()
					taskQueue.remove()
				}
				
				// 进行选择
				val select = selector.select(60000)
				if (select > 0) {
					// 获取以选择的键的集合
					val iterator = selector.selectedKeys().iterator()
					
					while (iterator.hasNext()) {
						val key = iterator.next() as SelectionKey
						// 必须手动删除
						iterator.remove()
						if (key.isReadable) {
							val datagramChannel = key.channel() as DatagramChannel
							// 读取
							byteBuffer.clear()
							println(datagramChannel === channel)
							val address = datagramChannel.receive(byteBuffer) ?: continue
							val handler =
								connectionMap[address] ?: handler
							threadPool.execute { handler(datagramChannel, address, byteBuffer) }
						}
					}
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}
	
	override fun start() {
		Thread(this, "AioUdpSer").start()
	}
	
	override fun close() {
		closed = true
		channel.close()
		threadPool.shutdown()
		selector.close()
		excWheelTimer.stop()
	}
	
	fun read(
		address: SocketAddress,
		timeout: Long = 0L,
		timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
		exc: (e: Exception) -> Unit = { it.printStackTrace() },
		onComplete: (byteBuffer: ByteBuffer) -> Unit
	) {
		val timeoutTask = if (timeout > 0) {
			excWheelTimer.newTimeout({
				queue.offer {
					synchronized(connectionMap) {
						connectionMap.remove(address)
					}
				}
				exc(TimeoutException("datagram address $address read time out"))
			}, timeout, timeUnit)
		} else {
			null
		}
		connectionMap[address] = { _, _, buffer ->
			timeoutTask?.cancel()
			onComplete(buffer)
		}
	}
	
	
	fun send(
		channel: DatagramChannel,
		address: SocketAddress,
		buffer: ByteBuffer
	) {
		channel.send(buffer, address)
	}
}