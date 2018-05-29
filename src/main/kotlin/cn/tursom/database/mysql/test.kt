package cn.tursom.database.mysql

data class SQLData(val id: Int?, val name: String)

fun main(args: Array<String>) {
	val sqlHelper = SQLHelper("//localhost:3306/test", "test", "test")
	sqlHelper.createTable("test", arrayOf(
			"`id` int AUTO_INCREMENT",
			"`name` VARCHAR(100)",
			"PRIMARY KEY (`id`)"
	))
	/*
	sqlHelper.update("test", arrayOf(Pair("name","1234")), arrayOf(Pair("id","2")))
	val sqlAdapter = sqlHelper.select<SQLData>("test")
	println(sqlAdapter)
	println()
	sqlHelper.select(sqlAdapter,"test")
	println(sqlAdapter)
	println()
	sqlAdapter.forEach(::println)
	*/
	val time = System.currentTimeMillis()
	val threads = ArrayList<Thread>()
	for (i in 1..10) {
		val thread = object : Thread() {
			override fun run() {
				for (n in 1..100) {
					//sqlHelper.insert("test1", arrayOf(Pair("name", "$i:$n")))
					println("$i:$n")
				}
			}
		}
		thread.start()
		threads.add(thread)
	}
	
	try {
		threads.forEach {
			it.join()
		}
	} catch (e: InterruptedException) {
		e.printStackTrace()
	}
	
	println("running ${System.currentTimeMillis() - time} ms")
}

