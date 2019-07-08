package cn.tursom.socket.server

import cn.tursom.socket.BaseSocket
import cn.tursom.socket.SecuritySocket
import cn.tursom.utils.encrypt.AES
import cn.tursom.utils.encrypt.RSA
import java.net.ServerSocket
import java.net.SocketException
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class SecuritySocketServer(
	port: Int,
	val exception: Exception.() -> Unit = { printStackTrace() },
	handle: SecuritySocket.() -> Unit = {}
) : SecurityServer(handle) {
	val socket = ServerSocket(port)
	
	override fun run() {
		while (!socket.isClosed) {
			try {
				socket.accept().use {
					val rsa = RSA()
					val preSocket = BaseSocket(it)
					
					preSocket.send(rsa.publicKey.encoded)
					val keySize = preSocket.recvInt()!!
					val originalKey = preSocket.recv(keySize)
					val socket = SecuritySocket(it, AES(originalKey, keySize))
					
					try {
						socket.handler()
					} catch (e: Exception) {
						e.exception()
					}
				}
			} catch (e: SocketException) {
				if (e.message == "Socket closed") {
					break
				} else {
					e.exception()
				}
			}
		}
	}
	
	override fun close() {
		try {
			socket.close()
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}
}