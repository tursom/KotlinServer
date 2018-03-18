package server

open class Interactive(
		private val command: Map<String, () -> Unit>,
		private val indicator: String = ">>>",
		private val cantFindCommand: String = "can't understand command") : Thread() {
	
	override fun run() {
		try {
			loop@ while (true) {
				print(indicator)
				val input = System.`in`.bufferedReader().readLine()
				when (input) {
					"" -> continue@loop
					else -> {
						val running = command[input]
						if (running == null) println(cantFindCommand)
						else running()
					}
				}
			}
		} catch (e: CloseException) {
		}
	}
	
	class CloseException : Exception()
}