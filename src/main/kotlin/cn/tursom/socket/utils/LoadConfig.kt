package cn.tursom.socket.utils

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.io.File
import java.io.InputStreamReader

fun <T : Any> loadConfigJson(file: String, clazz: Class<T>): T? {
	var configDataJson: T? = null
	val configFile = File(file)
	if (configFile.exists()) {
		val reader = InputStreamReader(configFile.inputStream())
		val config = StringBuffer()
		reader.readLines().forEach {
			config.append(it)
		}
		try {
			configDataJson = Gson().fromJson(config.toString(), clazz)
		} catch (e: JsonSyntaxException) {
			System.err.println("JSON Syntax Error")
			return null
		}
	}
	return configDataJson
}

inline fun <reified T:Any> loadConfigJson(file: String): T? {
	return loadConfigJson(file, T::class.java)
}