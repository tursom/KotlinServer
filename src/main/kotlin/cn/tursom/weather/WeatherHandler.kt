package cn.tursom.weather

import cn.tursom.socket.server.ServerHandler
import java.net.Socket

class WeatherHandler(socket: Socket) : ServerHandler(socket) {
	override fun handle() {
		val result = StringBuffer()
		println("${System.currentTimeMillis()}:接入客户端连接:${socket.inetAddress}:${socket.port}")
		synchronized(connectCount) {
			if (connectCount[socket.inetAddress!!] ?: 0 > 3) throw object : ServerException(
					"${System.currentTimeMillis()}:客户端${socket.inetAddress}:${socket.port}请求过多") {
				override val code: ByteArray?
					get() = tooManyRequest
			}
			connectCount[socket.inetAddress!!] = connectCount[socket.inetAddress!!] ?: 0 + 1
		}
		val req = recv(128)!!
		if (req.count() >= 64)
			throw object : ServerException("${System.currentTimeMillis()}:客户端${socket.inetAddress}:${socket.port}请求过长") {
				override val code: ByteArray?
					get() = tooLongRequest
			}
		println("${System.currentTimeMillis()}:客户端${socket.inetAddress}:${socket.port}请求获取城市\"$req\"的天气信息")
		
		val arrays = req.trim().split("\\s+".toRegex())
		arrays.forEach {
			result.append("--------------------------------------------\n")
			result.append("${getWeatherStr(it)}\n\n")
			//result.append("$weatherData\n\n")
			println("${System.currentTimeMillis()}:客户端${socket.inetAddress}:${socket.port}请求\"$it\"成功")
		}
		val ret = result.toString().dropLast(2).toByteArray()
		outputStream.write(ret)
		synchronized(connectCount) {
			connectCount[socket.inetAddress!!] = connectCount[socket.inetAddress!!] ?: 0 - 1
		}
		println("${System.currentTimeMillis()}:返回客户端${socket.inetAddress}:${socket.port}结果，共${ret.count()}字节")
	}
	
	private fun getWeatherStr(city: String): String {
		val weatherData: String
		val weatherString: String
		if (swap.containsKey(city)) {
			val data = swap[city]
			if (System.currentTimeMillis() - data!!.first < 60000) {
				weatherString = data.second
			} else {
				weatherData = getWeatherData(city) ?: throw throw CantGetCityWeatherException(
						"${socket.inetAddress}:${socket.port}", city)
				weatherString = weatherStr(weatherData) ?: throw throw CantGetCityWeatherException(
						"${socket.inetAddress}:${socket.port}", city)
				swap[city] = Pair(System.currentTimeMillis(), weatherString)
			}
		} else {
			weatherData = getWeatherData(city) ?: throw throw CantGetCityWeatherException(
					"${socket.inetAddress}:${socket.port}", city)
			weatherString = weatherStr(weatherData) ?: throw throw CantGetCityWeatherException(
					"${socket.inetAddress}:${socket.port}", city)
			swap[city] = Pair(System.currentTimeMillis(), weatherString)
		}
		return weatherString
	}
	
	class CantGetCityWeatherException(client: String? = null, private val city: String? = null) : ServerException(
			"${System.currentTimeMillis()}:客户端${client}请求\"$city\"的天气信息失败") {
		override val code: ByteArray?
			get() =
				if (debug)
					"无法获取城市\"$city\"的天气信息".toByteArray()
				else
					cantGetCityWeather
	}
	
	companion object {
		private val swap = HashMap<String?, Pair<Long, String>?>()
		val cantGetCityWeather = "无法获取天气信息".toByteArray()
		val tooManyRequest = "错误：请求过快".toByteArray()
		val tooLongRequest = "错误：请求过长".toByteArray()
	}
}