package cn.tursom.weather

import cn.tursom.database.sqlite.SQLAdapter
import cn.tursom.database.sqlite.SQLHelper
import cn.tursom.socket.server.HttpRequest
import cn.tursom.socket.server.SocketServer
import com.google.gson.Gson
import java.net.InetAddress

val weatherDB = SQLHelper("weather.db")
const val weatherTable = "weather"
val connectCount = HashMap<InetAddress, Int>()

val server = object : SocketServer(15432, SocketServer.Companion.cpuNumber, 1000) {
	override val poolIsFull: ByteArray
		get() = "抱歉，服务器请求已满，请稍后重试".toByteArray()
	override val handler: Runnable
		get() = WeatherHandler(socketQueue.poll()!!)
}

fun getWeatherData(city: String): String? {
	val host = "http://weather.tursom.cn"
	val path = "json"

	val weatherData: String
	val weatherDBData = SQLAdapter(WeatherDBData::class.java)
	println("${System.currentTimeMillis()}:database selecting:$city")
	weatherDB.select(weatherDBData, weatherTable, where = arrayOf(Pair("city", city)))
	if (weatherDBData.count() != 0) {
		if (System.currentTimeMillis() - (weatherDBData[0].time ?: 0) > 600000) {
			println("${System.currentTimeMillis()}:getting weather data:$city")
			weatherData = HttpRequest.sendGet("$host$path", "city=$city")
			if (weatherData.isEmpty()) return null

			println("${System.currentTimeMillis()}:database updating:$city")
			weatherDB.update(weatherTable, arrayOf(
					Pair("time", "${System.currentTimeMillis()}"),
					Pair("weather", weatherData)
			), arrayOf(
					Pair("city", city)
			))
		} else {
			weatherData = weatherDBData[0].weather!!
		}
	} else {
		println("${System.currentTimeMillis()}:getting weather data:$city")
		weatherData = HttpRequest.sendGet("$host$path", "city=$city")
		if (weatherData.isEmpty()) return null

		println("${System.currentTimeMillis()}:database inserting:$city")
		weatherDB.insert(weatherTable, arrayOf(
				Pair("time", "${System.currentTimeMillis()}"),
				Pair("city", city),
				Pair("weather", weatherData)
		))
	}

	return weatherData
}

fun weatherStr(data: String): String? {
	val result = Gson().fromJson(data, WeatherDataJson::class.java).result
	val stringBuilder = StringBuilder()
	stringBuilder.append(">更新时间:${result.updatetime}\n")
	stringBuilder.append(">${result.city} ${result.date} ${result.week} ${result.weather} ${result.winddirect} ${result.windpower}\n")
	stringBuilder.append(">平均温度${result.temp}℃ ，最高温度${result.temphigh}℃ ，最低温度${result.templow}℃ \n")
	stringBuilder.append(">气压${result.pressure}百帕，湿度${result.humidity}%\n")
	stringBuilder.append("\n>时刻天气：\n")
	result.hourly?.forEach {
		stringBuilder.append(">${it.time}\t${it.weather}\t${it.temp}℃ \n")
	}
	stringBuilder.append("\n>最近一周：")
	result.daily?.forEach {
		val day = it.day
		val night = it.night
		stringBuilder.append("\n>${it.date} ${it.week} 日出：${it.sunrise} 日落：${it.sunset}\n")
		stringBuilder.append(">白天：${day?.weather} ${day?.winddirect} ${day?.windpower} 最高气温${day?.temphigh}℃ \n")
		stringBuilder.append(">夜晚：${night?.weather} ${night?.winddirect} ${night?.windpower} 最低气温${night?.templow}℃ ")
	}
	return stringBuilder.toString()
}

fun main(args: Array<String>) {
	weatherDB.createTable(weatherTable, arrayOf(
			"time LONG",
			"city TEXT",
			"weather TEXT"
	))
	server.start()
}