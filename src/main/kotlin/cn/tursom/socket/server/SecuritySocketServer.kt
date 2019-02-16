package cn.tursom.socket.server

import cn.tursom.socket.utils.RSA
import java.net.ServerSocket
import java.net.SocketException
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class SecuritySocketServer(
	port: Int,
	val exception: Exception.() -> Unit = { printStackTrace() },
	handle: SecurityHandler.() -> Unit = {}
) : SecurityServer(handle) {
	val socket = ServerSocket(port)
	
	override fun run() {
		while (!socket.isClosed) {
			try {
				socket.accept().use {
					val decrypt = Cipher.getInstance("AES")
					val encrypt = Cipher.getInstance("AES")
					val rsa = RSA()
					val socket = SecurityHandler(it, decrypt, encrypt)
					
					socket.send(rsa.publicKey.encoded)
					val keySize = socket.recvInt()!!
					val originalKey = SecretKeySpec(rsa.decrypt(socket.recv(128)), 0, keySize, "AES")
					
					decrypt.init(Cipher.DECRYPT_MODE, originalKey)
					encrypt.init(Cipher.ENCRYPT_MODE, originalKey)
					
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