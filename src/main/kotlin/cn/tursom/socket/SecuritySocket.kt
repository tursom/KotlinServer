package cn.tursom.socket

import cn.tursom.utils.Gzip
import cn.tursom.utils.encrypt.Encrypt
import java.net.Socket

class SecuritySocket(
	socket: Socket,
	private val cipher: Encrypt
) : BaseSocket(socket) {
	
	
	fun sSend(data: ByteArray) = send(cipher.encrypt(data))
	fun sRecv() = cipher.decrypt(recv())
	fun sGzSend(data: ByteArray) = send(cipher.encrypt(Gzip.compress(data)))
	fun sGzRecv() = Gzip.uncompress(cipher.decrypt(recv()))
}