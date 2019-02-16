package cn.tursom.socket.utils

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object Gzip{
	fun compress(data: ByteArray): ByteArray {
		val out = ByteArrayOutputStream()
		val gzip = GZIPOutputStream(out)
		gzip.write(data)
		gzip.close()
		return out.toByteArray()
	}
	
	
	fun uncompress(bytes: ByteArray): ByteArray {
		return GZIPInputStream(ByteArrayInputStream(bytes)).readBytes()
	}
}