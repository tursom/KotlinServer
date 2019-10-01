package cn.tursom.socket.server.nio

import cn.tursom.socket.INioProtocol
import cn.tursom.socket.ProtocolAsyncNioSocket
import cn.tursom.socket.server.ISocketServer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.nio.channels.SelectionKey

class AsyncProtocolNioServer(val port: Int, val handler: suspend ProtocolAsyncNioSocket.() -> Unit)
	: ISocketServer by ProtocolNioServer(port, object : INioProtocol by ProtocolAsyncNioSocket.nioSocketProtocol {
	override fun handleAccept(key: SelectionKey) {
		GlobalScope.launch { ProtocolAsyncNioSocket(key).handler() }
	}
})