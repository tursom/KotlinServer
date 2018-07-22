package cn.tursom.tools

import com.google.gson.Gson
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

/**
 * Created by Tursom Ulefits on 2017/8/8.
 * Author: Tursom K. Ulefits
 * email: tursom@foxmail.com
 *
 * 功能介绍：
 * hideTitle(activity)
 *  隐藏默认的标题栏
 * startActivity(context, cls)
 *  创建新的activity
 *  例： Tools.startActivity(activity, LoginActivity::class.java)
 * setImageBitmap(imageView,bitmap,radiust,scaleRatio,context)
 *  将高斯模糊后的图片设置给imageView
 * blurBitmap(bitmap,radius, context): Bitmap
 *  将图片高斯模糊
 * cn.tursom.tools.md5(content: String)： String
 *  md5加密
 * cn.tursom.tools.randomInt(min, max): Int
 *  获取从min到max之间的随机整数
 * makeToast(activity, message: String)
 *  创建Toast信息
 * setSpinnerAdapter(spinner: Spinner?, collegeItems: Array<String>?, context: Context)
 *  给定Spinner设置Adapter
 */

val cpuNumber = Runtime.getRuntime().availableProcessors()

fun md5(content: ByteArray): ByteArray? {
	return try {
		//获取md5加密对象
		val instance = MessageDigest.getInstance("MD5")
		//加密，返回字节数组
		instance.digest(content)
	} catch (e: NoSuchAlgorithmException) {
		e.printStackTrace()
		null
	}
}

fun MD5(content: String): String? {
	return byteArrayToHexString(md5(content.toByteArray()))
}

fun sha256(content: ByteArray): ByteArray? {
	return try {
		//获取md5加密对象
		val instance = MessageDigest.getInstance("SHA-256")
		//加密，返回字节数组
		instance.digest(content)
	} catch (e: NoSuchAlgorithmException) {
		e.printStackTrace()
		null
	}
}

fun sha256(content: String): String? {
	return byteArrayToHexString(sha256(content.toByteArray()))
}

fun byteArrayToHexString(array: ByteArray?): String? {
	array ?: return null
	val sb = StringBuilder()
	array.forEach {
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

fun randomInt(min: Int, max: Int) = Random().nextInt(max) % (max - min + 1) + min


fun getTAG(cls: Class<*>): String {
	return cls.name.split(".").last().dropLast(10)
}

inline fun <reified T : Any> Gson.fromJson(json: String): T {
	return fromJson(json, T::class.java)
}
