package cn.tursom.weather

import java.util.*

data class WeatherDBData(val time: Long?, val city: String?, val weather: String?)

data class WeatherDataJson(val result: Result)

data class Result(
		val city: String?,
		val date: String?,
		val week: String?,
		val weather: String?,
		val temp:String?,
		val temphigh:String?,
		val templow:String?,
		val pressure:String?,
		val humidity:String?,
		val winddirect: String?,
		val windpower: String?,
		val updatetime: String?,
		val daily:Array<Daily>?,
		val hourly:Array<Hourly>?) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as Result

		if (city != other.city) return false
		if (date != other.date) return false
		if (week != other.week) return false
		if (weather != other.weather) return false
		if (temp != other.temp) return false
		if (temphigh != other.temphigh) return false
		if (templow != other.templow) return false
		if (pressure != other.pressure) return false
		if (humidity != other.humidity) return false
		if (winddirect != other.winddirect) return false
		if (windpower != other.windpower) return false
		if (updatetime != other.updatetime) return false
		if (!Arrays.equals(daily, other.daily)) return false
		if (!Arrays.equals(hourly, other.hourly)) return false

		return true
	}

	override fun hashCode(): Int {
		var result = city?.hashCode() ?: 0
		result = 31 * result + (date?.hashCode() ?: 0)
		result = 31 * result + (week?.hashCode() ?: 0)
		result = 31 * result + (weather?.hashCode() ?: 0)
		result = 31 * result + (temp?.hashCode() ?: 0)
		result = 31 * result + (temphigh?.hashCode() ?: 0)
		result = 31 * result + (templow?.hashCode() ?: 0)
		result = 31 * result + (pressure?.hashCode() ?: 0)
		result = 31 * result + (humidity?.hashCode() ?: 0)
		result = 31 * result + (winddirect?.hashCode() ?: 0)
		result = 31 * result + (windpower?.hashCode() ?: 0)
		result = 31 * result + (updatetime?.hashCode() ?: 0)
		result = 31 * result + (daily?.let { Arrays.hashCode(it) } ?: 0)
		result = 31 * result + (hourly?.let { Arrays.hashCode(it) } ?: 0)
		return result
	}
}

data class Daily(
		val date:String?,
		val week:String?,
		val sunrise:String?,
		val sunset:String?,
		val night: NightWeather?,
		val day: DayWeather?)

data class NightWeather(val weather:String?,val templow:String?,val winddirect:String?,val windpower:String?)
data class DayWeather(val weather:String?,val temphigh:String?,val winddirect:String?,val windpower:String?)

data class Hourly(val time:String?,val weather:String?,val temp:String?)