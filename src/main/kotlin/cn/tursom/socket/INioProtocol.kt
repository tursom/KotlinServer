package cn.tursom.socket

import cn.tursom.socket.niothread.INioThread
import java.nio.channels.SelectionKey

interface INioProtocol {
	@Throws(Throwable::class)
	fun handleAccept(key: SelectionKey, nioThread: INioThread)

	@Throws(Throwable::class)
	fun handleRead(key: SelectionKey, nioThread: INioThread)

	@Throws(Throwable::class)
	fun handleWrite(key: SelectionKey, nioThread: INioThread)

	@Throws(Throwable::class)
	fun exceptionCause(key: SelectionKey, nioThread: INioThread, e: Throwable)
}