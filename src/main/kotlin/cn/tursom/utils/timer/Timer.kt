package cn.tursom.utils.timer

interface Timer {
	fun exec(timeout: Long, task: () -> Unit): TimerTask
}
