package server

import java.io.File
import java.util.*

class RandomCode {
	private val randomCode = "${Companion.randomInt(10000000, 99999999)}"
	
	override fun toString(): String {
		return randomCode
	}
	
	fun showCode(filepath: String? = null) {
		println("RandomCode: $randomCode")
		filepath ?: return
		val file = File(filepath)
		file.createNewFile()
		file.writeText("passcode = $randomCode")
	}
	
	companion object {
		private fun randomInt(min: Int, max: Int) = Random().nextInt(max) % (max - min + 1) + min
	}
}