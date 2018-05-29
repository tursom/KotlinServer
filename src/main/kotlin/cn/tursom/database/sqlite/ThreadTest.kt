package cn.tursom.database.sqlite

class thread(val id: Int, val sqlHelper: SQLHelper) : Thread() {
	
	override fun run() {
		for (n in 1..10)
			sqlHelper.insert("test", arrayOf(Pair("name","$id:$n")))
	}
}

fun main(args: Array<String>) {
	println(System.currentTimeMillis())
	val sqlHelper = testDB
	for (n in 1..10) {
		thread(n, sqlHelper).start()
	}
}