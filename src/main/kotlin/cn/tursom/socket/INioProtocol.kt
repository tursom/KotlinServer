package cn.tursom.socket

import java.nio.channels.SelectionKey

interface INioProtocol {
	@Throws(Throwable::class)
	fun handleAccept(key: SelectionKey)

	@Throws(Throwable::class)
	fun handleRead(key: SelectionKey)

	@Throws(Throwable::class)
	fun handleWrite(key: SelectionKey)

	@Throws(Throwable::class)
	fun exceptionCause(key: SelectionKey, e: Throwable)
}