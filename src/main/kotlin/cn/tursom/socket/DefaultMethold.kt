package cn.tursom.socket

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

val Int.ipStr
	get() = formatIpAddress(this)

fun formatIpAddress(ip: Int) =
	"${ip and 0xff}.${(ip shr 8) and 0xff}.${(ip shr 16) and 0xff}.${(ip shr 24) and 0xff}"

fun Int.toByteArray(): ByteArray {
	val array = ByteArray(4)
	array[0] = shr(3 * 8).toByte()
	array[1] = shr(2 * 8).toByte()
	array[2] = shr(1 * 8).toByte()
	array[3] = shr(0 * 8).toByte()
	return array
}

fun ByteArray.toInt(): Int =
	(this[0].toInt() shl 24) or
		(this[1].toInt() shl 16 and 0xff0000) or
		(this[2].toInt() shl 8 and 0xff00) or
		(this[3].toInt() and 0xFF)

fun Long.toByteArray(): ByteArray {
	val array = ByteArray(4)
	array[0] = shr(7 * 8).toByte()
	array[1] = shr(6 * 8).toByte()
	array[2] = shr(5 * 8).toByte()
	array[3] = shr(4 * 8).toByte()
	array[4] = shr(3 * 8).toByte()
	array[5] = shr(2 * 8).toByte()
	array[6] = shr(1 * 8).toByte()
	array[7] = shr(0 * 8).toByte()
	return array
}

fun ByteArray.toLong(): Long =
	(this[0].toLong() shl 56 and 0xff000000000000) or
		(this[1].toLong() shl 48 and 0xff0000000000) or
		(this[2].toLong() shl 40 and 0xff00000000) or
		(this[3].toLong() shl 32 and 0xff00000000) or
		(this[4].toLong() shl 24 and 0xff000000) or
		(this[5].toLong() shl 16 and 0xff0000) or
		(this[6].toLong() shl 8 and 0xff00) or
		(this[7].toLong() and 0xff)

fun Int.left1(): Int {
	if (this == 0) {
		return -1
	}
	var exp = 4
	var pos = 1 shl exp
	while (exp > 0) {
		exp--
		if ((this shr pos) != 0) {
			pos += 1 shl exp
		} else {
			pos -= 1 shl exp
		}
	}
	return if (this shr pos != 0) pos else pos - 1
}

fun Long.left1(): Int {
	if (this == 0L) {
		return -1
	}
	var exp = 8
	var pos = 1 shl exp
	while (exp > 0) {
		exp--
		if ((this shr pos) != 0L) {
			pos += 1 shl exp
		} else {
			pos -= 1 shl exp
		}
	}
	return if (this shr pos != 0L) pos else pos - 1
}

/**
 * 序列化
 */
fun serialize(`object`: Any): ByteArray? = try {
	val baos = ByteArrayOutputStream()
	val oos = ObjectOutputStream(baos)
	oos.writeObject(`object`)
	baos.toByteArray()
} catch (e: Exception) {
	null
}

/**
 * 反序列化
 */
fun unSerialize(bytes: ByteArray): Any? = try {
	ObjectInputStream(ByteArrayInputStream(bytes)).readObject()
} catch (e: Exception) {
	null
}
