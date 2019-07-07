package cn.tursom.utils

import kotlinx.coroutines.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.jar.JarFile

suspend fun <T> io(block: suspend CoroutineScope.() -> T): T {
	return withContext(Dispatchers.IO, block)
}

fun background(block: suspend CoroutineScope.() -> Unit) {
	GlobalScope.launch(block = block)
}

suspend fun <T> ui(block: suspend CoroutineScope.() -> T): T {
	return withContext(Dispatchers.Main, block)
}

fun getClassName(jarPath: String): List<String> {
	val myClassName = ArrayList<String>()
	for (entry in JarFile(jarPath).entries()) {
		val entryName = entry.name
		if (entryName.endsWith(".class")) {
			myClassName.add(entryName.replace("/", ".").substring(0, entryName.lastIndexOf(".")))
		}
	}
	return myClassName
}

fun <T> List<T>.binarySearch(comparison: (T) -> Int): T? {
	val index = binarySearch(0, size, comparison)
	return if (index < 0) null
	else get(index)
}

val cpuNumber = Runtime.getRuntime().availableProcessors()

fun String.simplifyPath(): String {
	if (isEmpty()) {
		return "/"
	}
	val strs = split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
	val list = LinkedList<String>()
	for (str in strs) {
		if (str.isEmpty() || "." == str) {
			continue
		}
		if (".." == str) {
			list.pollLast()
			continue
		}
		list.addLast(str)
	}
	var result = ""
	while (list.size > 0) {
		result += "/" + list.pollFirst()!!
	}
	return if (result.isNotEmpty()) result else "/"
}

fun ByteArray.md5(): ByteArray? {
	return try {
		//获取md5加密对象
		val instance = MessageDigest.getInstance("MD5")
		//加密，返回字节数组
		instance.digest(this)
	} catch (e: NoSuchAlgorithmException) {
		e.printStackTrace()
		null
	}
}

fun String.md5(): String? {
	return toByteArray().md5()?.toHexString()
}

fun ByteArray.sha256(): ByteArray? {
	return try {
		//获取md5加密对象
		val instance = MessageDigest.getInstance("SHA-256")
		//加密，返回字节数组
		instance.digest(this)
	} catch (e: NoSuchAlgorithmException) {
		e.printStackTrace()
		null
	}
}

fun String.sha256(): String? {
	return toByteArray().sha256()?.toHexString()
}

fun ByteArray.sha(): ByteArray? {
	return try {
		//获取md5加密对象
		val instance = MessageDigest.getInstance("SHA")
		//对字符串加密，返回字节数组
		instance.digest(this)
	} catch (e: NoSuchAlgorithmException) {
		e.printStackTrace()
		null
	}
}

fun String.sha(): String? = toByteArray().sha()?.toHexString()

fun ByteArray.sha1(): ByteArray? {
	return try {
		//获取md5加密对象
		val instance = MessageDigest.getInstance("SHA-1")
		//对字符串加密，返回字节数组
		instance.digest(this)
	} catch (e: NoSuchAlgorithmException) {
		e.printStackTrace()
		null
	}
}

fun String.sha1(): String? = toByteArray().sha1()?.toHexString()

fun ByteArray.sha384(): ByteArray? {
	return try {
		//获取md5加密对象
		val instance = MessageDigest.getInstance("SHA-384")
		//对字符串加密，返回字节数组
		instance.digest(this)
	} catch (e: NoSuchAlgorithmException) {
		e.printStackTrace()
		null
	}
}

fun String.sha384(): String? = toByteArray().sha384()?.toHexString()

fun ByteArray.sha512(): ByteArray? {
	return try {
		//获取md5加密对象
		val instance = MessageDigest.getInstance("SHA-512")
		//对字符串加密，返回字节数组
		instance.digest(this)
	} catch (e: NoSuchAlgorithmException) {
		e.printStackTrace()
		null
	}
}

fun String.sha512(): String? = toByteArray().sha512()?.toHexString()

fun ByteArray.toHexString(): String? {
	val sb = StringBuilder()
	forEach {
		//获取低八位有效值+
		val i: Int = it.toInt() and 0xff
		//将整数转化为16进制
		var hexString = Integer.toHexString(i)
		if (hexString.length < 2) {
			//如果是一位的话，补0
			hexString = "0$hexString"
		}
		sb.append(hexString)
	}
	return sb.toString()
}

fun ByteArray.toUTF8String() = String(this, Charsets.UTF_8)

fun String.base64() = this.toByteArray().base64().toUTF8String()

fun ByteArray.base64(): ByteArray {
	return Base64.getEncoder().encode(this)
}

fun String.base64decode() = Base64.getDecoder().decode(this).toUTF8String()

fun ByteArray.base64decode(): ByteArray = Base64.getDecoder().decode(this)

fun String.digest(type: String) = toByteArray().digest(type)?.toHexString()

fun ByteArray.digest(type: String) = try {
	//获取加密对象
	val instance = MessageDigest.getInstance(type)
	//加密，返回字节数组
	instance.digest(this)
} catch (e: NoSuchAlgorithmException) {
	e.printStackTrace()
	null
}

fun randomInt(min: Int, max: Int) = Random().nextInt(max) % (max - min + 1) + min


fun getTAG(cls: Class<*>): String {
	return cls.name.split(".").last().dropLast(10)
}
