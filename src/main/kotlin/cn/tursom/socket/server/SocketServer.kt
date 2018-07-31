package cn.tursom.socket.server

import cn.tursom.socket.utils.parseXmlAttribute
import cn.tursom.tools.getTAG
import org.dom4j.io.SAXReader
import java.io.File
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * SocketServer多线程服务器
 * 每当有新连接接入时就会将handler:Runnable加入线程池的任务队列中运行
 * 通过重载handler:Runnable的getter处理业务逻辑
 * start()函数实现无限循环监听，同时自动处理异常
 * 最新接入的套接字出存在socket变量中
 * 通过调用close()或closeServer()关闭服务器，造成的异常会被自动处理
 *
 * 标准使用例：
 * object : SocketServer(port) {
 *     override val handler: Runnable
 *         get() = object : ServerHandler(socket) {
 *             override fun handle() {
 *                 ... // 业务逻辑代码
 *             }
 *         }
 * }
 *
 */
open class SocketServer : Thread {
	
	var socket = Socket()
	private val pool: ThreadPoolExecutor
	private var serverSocket: ServerSocket
	
	/**
	 * 使用代码而不是配置文件的构造函数
	 *
	 * @param port 运行端口，必须指定
	 * @param threads 线程池最大线程数
	 * @param queueSize 线程池任务队列大小
	 * @param timeout 超时
	 * @param timeUnit timeout的单位，默认毫秒
	 * @param startImmediately 是否立即启动
	 */
	constructor(
		port: Int, threads: Int = 1,
		queueSize: Int = 1,
		timeout: Long = 0L,
		timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
		startImmediately: Boolean = false) {
		pool = ThreadPoolExecutor(threads, threads, timeout, timeUnit, LinkedBlockingQueue(queueSize))
		serverSocket = ServerSocket(port)
		if (startImmediately) {
			start()
		}
	}
	
	/**
	 * 使用配置文件初始化的构造函数
	 * 用法同另一个初始函数，除了timeUnit以外的属性都支持
	 *
	 * 例（config path: config/server.xml）：
	 * <server
	 *     port="12345"
	 *     threads="10"
	 * />
	 *
	 * @param configPath 配置文件路径
	 */
	constructor(configPath: String) {
		val configData = ServerConfigData()
		
		val rootElement = SAXReader().read(File(configPath)).rootElement
		parseXmlAttribute(configData, rootElement)
		
		pool = ThreadPoolExecutor(configData.threads, configData.threads, configData.timeout, TimeUnit.MILLISECONDS, LinkedBlockingQueue(configData.queueSize))
		serverSocket = ServerSocket(configData.port)
		if (configData.startImmediately) {
			start()
		}
	}
	
	/**
	 * 主要作用：封闭start()，防止用户重载start()
	 */
	final override fun start() {
		super.start()
	}
	
	/**
	 * 主要作用：
	 * 循环接受连接请求
	 * 讲接收的连接交给handler处理
	 * 连接初期异常处理
	 * 自动关闭套接字服务器与线程池
	 */
	final override fun run() {
		while (!serverSocket.isClosed) {
			try {
				socket = serverSocket.accept()
				println("$TAG: run(): get connect: $socket")
				pool.execute(handler)
			} catch (e: IOException) {
				if (pool.isShutdown || serverSocket.isClosed) {
					System.err.println("server closed")
					break
				}
				e.printStackTrace()
			} catch (e: SocketException) {
				e.printStackTrace()
				break
			} catch (e: RejectedExecutionException) {
				socket.getOutputStream()?.write(poolIsFull)
			} catch (e: Exception) {
				e.printStackTrace()
				break
			}
		}
		whenClose()
		close()
		System.err.println("server closed")
	}
	
	/**
	 * 交给用户重载的属性，用于处理连接请求
	 * 默认是立即关闭套接字
	 */
	protected open val handler: Runnable
		get() = Runnable {
			socket.close()
		}
	
	/**
	 * 关闭服务器套接字
	 */
	fun closeServer() {
		if (!serverSocket.isClosed)
			serverSocket.close()
	}
	
	/**
	 * 关闭线程池
	 */
	fun shutdownPool() {
		if (!pool.isShutdown)
			pool.shutdown()
	}
	
	/**
	 * 服务器是否已经关闭
	 */
	fun isClosed() = pool.isShutdown || serverSocket.isClosed
	
	/**
	 * 关闭服务器
	 */
	fun close() {
		shutdownPool()
		closeServer()
	}
	
	/**
	 * 关闭服务器时执行
	 */
	open fun whenClose() {
	}
	
	/**
	 * 线程池满时返回给客户端的信息
	 */
	open val poolIsFull
		get() = Companion.poolIsFull
	
	private data class ServerConfigData(
		val port: Int = 0, val threads: Int = 1,
		val queueSize: Int = 1,
		val timeout: Long = 0L,
		val startImmediately: Boolean = false)
	
	companion object {
		val TAG = getTAG(this::class.java)
		val cpuNumber = Runtime.getRuntime().availableProcessors() //CPU处理器的个数
		val poolIsFull = "server pool is full".toByteArray()
	}
}