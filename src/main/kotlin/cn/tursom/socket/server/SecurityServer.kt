package cn.tursom.socket.server

import cn.tursom.socket.BaseSocket

@Suppress("UNCHECKED_CAST")
abstract class SecurityServer(handler: SecurityHandler.() -> Unit) : SocketServer(handler as BaseSocket.() -> Unit)