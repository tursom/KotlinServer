package cn.tursom.socket.server

import cn.tursom.database.sqlite.SQLHelper
import java.util.concurrent.Executors

fun main(args: Array<String>) {
	println(Runtime.getRuntime().availableProcessors()*2)
	val sqlHelper = SQLHelper("test.db")
	val pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*2)
	for (n in 1..10) {
		pool.execute {
			for (i in 1..10)
				sqlHelper.insert("test", arrayOf(Pair("name","$n:$i")))
			println("thread $n finished")
		}
	}
	pool.shutdown()
}