package cn.tursom.socket.utils

import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher


class RSA {
	val publicKey: RSAPublicKey
	private val privateKey: RSAPrivateKey?
	
	constructor() {
		val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
		keyPairGenerator.initialize(1024)//512-65536 & 64的倍数
		val keyPair = keyPairGenerator.generateKeyPair()
		publicKey = keyPair.public as RSAPublicKey
		privateKey = keyPair.private as RSAPrivateKey
	}
	
	constructor(publicKey: RSAPublicKey) {
		this.publicKey = publicKey
		privateKey = null
	}
	
	constructor(publicKey: ByteArray) : this(KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(publicKey)) as RSAPublicKey)
	
	/**
	 * 使用公钥对数据进行加密
	 */
	fun encrypt(data: ByteArray): ByteArray {
		cipher.init(Cipher.ENCRYPT_MODE, publicKey)
		return if (data.size < 117)
			cipher.doFinal(data)
		else {
			val buffer = ByteArray(data.size / 117 * 128 + 128)
			var i1 = 0
			var decodeIndex = 0
			
			while (i1 + 117 < data.size) {
				decodeIndex += cipher.doFinal(data, i1, 117, buffer, decodeIndex)
				i1 += 117
			}
			decodeIndex += cipher.doFinal(data, i1, data.size - i1, buffer, decodeIndex)
			
			buffer.copyOf(decodeIndex)
		}
	}
	
	/**
	 * 使用私钥解密
	 */
	fun decrypt(data: ByteArray): ByteArray {
		cipher.init(Cipher.DECRYPT_MODE, privateKey ?: throw NoPrivateKeyException())
		return if (data.size < 128) {
			cipher.doFinal(data)
		} else {
			val buffer = ByteArray(data.size / 128 * 117 + 11)
			var i1 = 0
			var decodeIndex = 0
			
			while (i1 + 128 < data.size) {
				decodeIndex += cipher.doFinal(data, i1, 128, buffer, decodeIndex)
				i1 += 128
			}
			decodeIndex += cipher.doFinal(data, i1, data.size - i1, buffer, decodeIndex)
			buffer.copyOf(decodeIndex)
		}
	}
	
	class NoPrivateKeyException(message: String? = null) : Exception(message)
	
	companion object {
		private val cipher by lazy { Cipher.getInstance("RSA")!! }
	}
}