package cn.tursom.tools

import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class AES(secKey: SecretKey) {
	private val decrypt = Cipher.getInstance("AES")!!
	private val encrypt = Cipher.getInstance("AES")!!
	
	init {
		encrypt.init(Cipher.ENCRYPT_MODE, secKey)
		decrypt.init(Cipher.DECRYPT_MODE, secKey)
	}
	
	constructor(key: ByteArray, keySize: Int, offset: Int = 0) : this(SecretKeySpec(key, offset, keySize, "AES"))
	
	constructor() : this(defaultGenerator.generateKey())
	
	companion object {
		private val generator128 = KeyGenerator.getInstance("AES")
		private val generator192 = KeyGenerator.getInstance("AES")
		private val generator256 = KeyGenerator.getInstance("AES")
		private val defaultGenerator = generator256
		
		init {
			generator128.init(128)
			generator192.init(192)
			generator256.init(256)
		}
		
		fun get128() = AES(generator128.generateKey())
		fun get192() = AES(generator192.generateKey())
		fun get256() = AES(generator256.generateKey())
	}
}