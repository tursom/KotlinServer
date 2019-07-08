package cn.tursom.socket.server

import cn.tursom.socket.BaseSocket
import cn.tursom.socket.SecuritySocket

@Suppress("UNCHECKED_CAST")
abstract class SecurityServer(handler: SecuritySocket.() -> Unit) : SocketServer(handler as BaseSocket.() -> Unit)