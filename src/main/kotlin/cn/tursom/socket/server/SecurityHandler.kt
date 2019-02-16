package cn.tursom.socket.server

import cn.tursom.socket.BaseSocket
import cn.tursom.socket.utils.Gzip
import java.net.Socket
import javax.crypto.Cipher

class SecurityHandler(
	socket: Socket,
	private val decrypt: Cipher,
	private val encrypt: Cipher
) : BaseSocket(socket) {
	fun sSend(data: ByteArray) = send(encrypt.doFinal(data))
	fun sRecv() = decrypt.doFinal(recv())
	fun sGzSend(data: ByteArray) = send(encrypt.doFinal(Gzip.compress(data)))
	fun sGzRecv() = Gzip.uncompress(decrypt.doFinal(recv()))
}