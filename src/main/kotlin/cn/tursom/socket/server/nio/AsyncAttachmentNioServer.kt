package cn.tursom.socket.server.nio

import cn.tursom.socket.AttachmentAsyncNioSocket
import cn.tursom.socket.INioProtocol
import cn.tursom.socket.server.ISocketServer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.nio.channels.SelectionKey

class AsyncAttachmentNioServer(val port: Int, val handler: suspend AttachmentAsyncNioSocket.() -> Unit)
	: ISocketServer by AttachmentNioServer(port, object : INioProtocol by AttachmentAsyncNioSocket.nioSocketProtocol {
	override fun handleAccept(key: SelectionKey) {
		GlobalScope.launch { AttachmentAsyncNioSocket(key).handler() }
	}
})