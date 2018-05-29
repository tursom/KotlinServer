package cn.tursom.database.mysql

val testDB = SQLHelper("//localhost:3306/test", "test", "test")

class thread(val id: Int, val sqlHelper: SQLHelper) : Thread() {

	override fun run() {
		for (n in 1..10)
			sqlHelper.insert("test", arrayOf(Pair("name", "$id:$n")))
	}
}

fun main(args: Array<String>) {
	println(System.currentTimeMillis())
	val sqlHelper = testDB
	for (n in 1..10) {
		thread(n, sqlHelper).start()
	}
}