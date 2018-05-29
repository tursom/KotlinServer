package cn.tursom.login

import cn.tursom.register.exitcode
import cn.tursom.database.mysql.SQLHelper
import cn.tursom.socket.server.Interactive
import cn.tursom.socket.server.RandomCode
import cn.tursom.socket.server.SocketServer
import cn.tursom.socket.server.loadConfigJson

val passcode = RandomCode()

var port = 12346
var database: String = "localhost:3306/test"
var user = "test"
var password = "test"

var sqlHelper = SQLHelper("//$database", user, password)

val loginServer = object : SocketServer(port, cpuNumber * 2) {
	override val handler: Runnable
		get() = LoginHandler(socketQueue.poll()!!, SQLHelper("//$database", user, password))

	override fun whenClose() {
		System.exit(0)
	}
}

fun createTable(sqlHelper: SQLHelper) {
	sqlHelper.createTable(LoginHandler.table, arrayOf(
			"`id` INT AUTO_INCREMENT",
			"`name` VARCHAR(32) not null",
			"`password` VARCHAR(256) not null",
			"`time` VARCHAR(64) not null",
			"PRIMARY KEY (`id`)"))
}

val interactiveCommand = object : HashMap<String, () -> Unit>() {
	init {
		this[passcode.toString()] = {
			loginServer.close()
			throw Interactive.CloseException()
		}
		this["close"] = {
			loginServer.close()
			throw Interactive.CloseException()
		}
		this["port"] = { println(port) }
		this["exitcode"] = { println(passcode) }
		this["exit code"] = { println(passcode) }
	}
}

val interactive = object : Interactive(interactiveCommand) {
	override fun run() {
		println("server running in port $port")
		exitcode.showCode(filepath = "passcode")
		super.run()
	}
}

data class ConfigData(val port: Int?, val database: String?, val user: String?, var password: String?)

fun loadConfig() {
	val configData: ConfigData = loadConfigJson("login.cfg.json") ?: return
	port = configData.port ?: port
	loginServer.setPort(port)
	database = configData.database ?: database
	user = configData.user ?: user
	password = when (configData.password) {
		"input" -> {
			print("please input database ${cn.tursom.register.database}'s password:")
			System.`in`.bufferedReader().readLine()
		}
		else -> configData.password ?: password
	}
	sqlHelper = SQLHelper("//$database", user, password)
}

fun main(args: Array<String>) {
	loadConfig()
	createTable(sqlHelper)

	interactive.start()
	loginServer.start()
}