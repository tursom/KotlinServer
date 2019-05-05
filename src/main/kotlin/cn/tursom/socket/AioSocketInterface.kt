package cn.tursom.socket

import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.channels.CompletionHandler
import java.util.concurrent.TimeUnit


/**
 * 将异步操作分为一个个按顺序执行的代码块，实现异步操作语法同步化
 * 执行顺序除 next 外为皆代码块书写的先后顺序
 * 而 next 代码块都是在函数最后执行的代码块后执行
 * 构造流程时每个函数的返回值是当前函数的索引
 * next 代码块接受当前函数的索引，返回下一个函数的索引，默认为索引+1
 */

interface AioSocketInterface : Closeable, Runnable {
	var timeout: Long
	var timeUnit: TimeUnit
	fun send(
		next: (Int) -> Int = { it + 1 },
		bufferGetter: () -> ByteBuffer
	): Int
	
	fun recv(
		bufferGetter: () -> ByteBuffer,
		next: (Int) -> Int = { it + 1 },
		handler: (size: Int, buffer: ByteBuffer, failed: Throwable.() -> Unit) -> Unit
	): Int
	
	infix fun tryCatch(exceptionHandler: Throwable.() -> Unit)
	fun run(
		next: (Int) -> Int = { it + 1 },
		runBlock: () -> Unit
	): Int
}

fun AioSocketInterface.recv(
	buffer: ByteBuffer,
	next: (Int) -> Int = { it + 1 },
	a: (size: Int, buffer: ByteBuffer, failed: Throwable.() -> Unit) -> Unit
) = recv({ buffer }, next, a)

fun AioSocketInterface.recvStr(
	bufferGetter: () -> ByteBuffer,
	next: (Int) -> Int = { it + 1 },
	handler: (String) -> Unit
) = recv(bufferGetter, next) { size, buffer, failed ->
	try {
		handler(String(buffer.array(), 0, size))
	} catch (e: Throwable) {
		e.failed()
	}
}

fun AioSocketInterface.recvStr(
	buffer: ByteBuffer,
	next: (Int) -> Int = { it + 1 },
	handler: (String) -> Unit
) = recv(buffer, next) { size, recvBuffer, failed ->
	try {
		handler(String(recvBuffer.array(), 0, size))
	} catch (e: Throwable) {
		e.failed()
	}
}

fun AioSocketInterface.sendStr(
	str: String,
	next: (Int) -> Int = { it + 1 }
) = send(next) { ByteBuffer.wrap(str.toByteArray()) }


infix fun AioSocketInterface.sendStr(
	str: String
) = send { ByteBuffer.wrap(str.toByteArray()) }


fun AioSocketInterface.sendStr(
	next: (Int) -> Int = { it + 1 },
	str: () -> String
) = send(next) { ByteBuffer.wrap(str().toByteArray()) }


infix fun AioSocketInterface.send(bufferGetter: () -> ByteBuffer
) = send({ it + 1 }, bufferGetter)


fun AioSocketInterface.recv(
	bufferGetter: () -> ByteBuffer,
	handler: (size: Int, buffer: ByteBuffer, failed: Throwable.() -> Unit) -> Unit
) = recv(bufferGetter, { it + 1 }, handler)


class AioHandler<T>(
	val failed: Throwable.(buffer: T) -> Unit = { printStackTrace() },
	val handler: (result: Int, attachment: T) -> Unit
) : CompletionHandler<Int, T> {
	override fun completed(result: Int?, a: T) {
		handler(result!!, a)
	}
	
	override fun failed(e: Throwable, a: T) {
		e.failed(a)
	}
}