package cn.tursom.register

import cn.tursom.socket.server.Interactive
import cn.tursom.socket.server.RandomCode
import cn.tursom.socket.server.SocketServer
import cn.tursom.socket.server.loadConfigJson
import cn.tursom.database.mysql.SQLHelper
import cn.tursom.login.LoginHandler
import java.util.*

val exitcode = RandomCode()

var port = 12345
var database: String = "localhost:3306/test"
var user = "test"
var password = "test"

var sqlHelper = SQLHelper("//$database", user, password)

val registerServer = object : SocketServer(port = 12345, threads = cpuNumber) {
	override val handler
		get() = RegisterHandler(socketQueue.poll()!!, SQLHelper("//$database", user, password))

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

/*
 * 实现控制台注册功能
 */
fun register() {
	TODO() //实现控制台注册功能
}

val interactiveCommand = object : HashMap<String, () -> Unit>() {
	init {
		this[exitcode.toString()] = {
			registerServer.close()
			throw Interactive.CloseException()
		}
		this["close"] = {
			registerServer.close()
			throw Interactive.CloseException()
		}
		this["port"] = { println(port) }
		this["exitcode"] = { println(exitcode) }
		this["exit code"] = { println(exitcode) }
		this["register"] = { register() }
	}
}

val interactive = object : Interactive(interactiveCommand) {
	override fun run() {
		println("server running in port $port")
		exitcode.showCode(filepath = "exitcode")
		super.run()
	}
}

data class ConfigData(val port: Int?, val database: String?, val user: String?, var password: String?)

fun loadConfig() {
	val configData: ConfigData = loadConfigJson("register.cfg") ?: return
	port = configData.port ?: port
	registerServer.setPort(port)
	database = configData.database ?: database
	user = configData.user ?: user

	password = when (configData.password) {
		"input" -> {
			print("please input database $database's password:")
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
	registerServer.start()
}