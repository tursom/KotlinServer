package cn.tursom.socket.enhance.impl

import cn.tursom.socket.enhance.SocketReader
import cn.tursom.utils.bytebuffer.AdvanceByteBuffer
import cn.tursom.utils.timer.TimerTask
import cn.tursom.utils.timer.WheelTimer

class TimeoutReader<Read>(val prevReader: SocketReader<Read>, val timeout: Long = 5000L) : SocketReader<Read> {
	private var timerTask: TimerTask? = null
	override suspend fun get(buffer: AdvanceByteBuffer, timeout: Long): Read {
		timerTask?.cancel()
		timerTask = timer.exec(this.timeout) {
			prevReader.close()
		}
		return prevReader.get(buffer, timeout)
	}

	override fun close() {
		prevReader.close()
	}

	companion object {
		val timer = WheelTimer.timer
	}
}