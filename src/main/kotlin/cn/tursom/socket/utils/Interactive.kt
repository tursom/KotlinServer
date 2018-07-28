package cn.tursom.socket.utils

open class Interactive(
		private val command: Map<String, () -> Unit>,
		private val indicator: String = ">>>",
		private val cantFindCommand: String = "can't understand command") : Thread() {
	private val input = System.`in`.bufferedReader()
	
	override fun run() {
		try {
			while (true) {
				print(indicator)
				val command = input.readLine()
				when (command) {
					"" -> {
					}
					else -> {
						val running = this.command[command]
						if (running == null) println(cantFindCommand)
						else running()
					}
				}
			}
		} catch (e: Exception) {
			input.close()
			e.printStackTrace()
		}
		whenClose()
	}
	
	open fun whenClose() {}
	
	class CloseException : Exception()
}