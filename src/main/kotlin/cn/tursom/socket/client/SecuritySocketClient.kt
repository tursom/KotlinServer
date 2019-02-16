package cn.tursom.socket.client

import cn.tursom.socket.BaseSocket
import cn.tursom.socket.utils.Gzip
import cn.tursom.socket.utils.RSA
import java.net.Socket
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import kotlin.random.Random

class SecuritySocketClient(host: String, port: Int, use: (SecuritySocketClient.() -> Unit)? = null) : BaseSocket(Socket(host, port)) {
	private val decrypt = Cipher.getInstance("AES")!!
	private val encrypt = Cipher.getInstance("AES")!!
	
	init {
		val generator = KeyGenerator.getInstance("AES")
		generator.init(256)
		val secKey = generator.generateKey()
		val serRsa = RSA(recv(162))
		send(secKey.encoded.size)
		
		val sandedKey = ByteArray(117)
		secKey.encoded.copyInto(sandedKey)
		Random.nextBytes(sandedKey, secKey.encoded.size, 116)
		send(serRsa.encrypt(sandedKey))
		
		encrypt.init(Cipher.ENCRYPT_MODE, secKey)
		decrypt.init(Cipher.DECRYPT_MODE, secKey)
		
		use?.let {
			it()
			close()
		}
	}
	
	fun sSend(data: ByteArray) = send(encrypt.doFinal(data))
	fun sRecv() = decrypt.doFinal(recv())!!
	fun sGzSend(data: ByteArray) = send(encrypt.doFinal(Gzip.compress(data)))
	fun sGzRecv() = Gzip.uncompress(decrypt.doFinal(recv()))
}