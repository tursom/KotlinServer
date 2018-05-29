package cn.tursom.register

import com.google.gson.Gson
import cn.tursom.database.mysql.SQLHelper
import cn.tursom.login.LoginHandler
import cn.tursom.socket.server.ServerHandler
import org.json.JSONObject
import cn.tursom.tools.fromJson
import java.net.Socket

class RegisterHandler(socket: Socket, private val sqlHelper: SQLHelper)
	: ServerHandler(socket) {
	
	override fun handle() {
		val req = recv(1024) ?: return
		handleRequest(req)
	}
	
	private fun handleRequest(req: String) {
		try {
			val request: Request = Gson().fromJson(req)
			requestCheck(request)
			register(request)
		} catch (e: RegisteredException) {
			System.err.println("${e::class.java}:${e.message}")
			outputStream.write(userRegistered)
		} catch (e: ReqException) {
			printReqError(e)
		} catch (e: ClosedException) {
			outputStream.write(serverClosed)
		}
	}
	
	private fun register(result: Request) {
		registeredCheck(result)
		registering(result)
	}
	
	private fun registering(result: Request) {
		sqlHelper.insert(table, arrayOf(
				Pair("name", result.username ?: throw ReqException("request username is null")),
				Pair("time", "${result.time ?: throw ReqException("request time is null")}"),
				Pair("password", result.password ?: throw ReqException("request password is null"))))
		val registerAdapter = sqlHelper.select<RegisterData>(
				table,
				where = arrayOf(Pair("name", result.username)))
		outputStream.write("{\"result\":\"success\",\"success\":\"${registerAdapter[0].id}\"}".toByteArray())
	}
	
	private fun registeredCheck(result: Request) {
		val registerAdapter = sqlHelper.select<RegisterData>(table,
				where = arrayOf(Pair("name", result.username ?: throw ReqException("request username is null"))))
		if (registerAdapter.count() != 0)
			throw RegisteredException("user ${result.username} already registered")
	}
	
	private fun requestCheck(result: Request) {
		when (result.request) {
			exitcode.toString() -> {
				registerServer.shutdownPool()
				registerServer.closeServer()
				throw ClosedException("server closeServer")
			}
			"register" -> return
			else -> throw ReqException("request \"${result.request}\" error")
		}
	}
	
	private fun printReqError(e: ReqException) {
		System.err.println("${e::class.java}:${e.message}")
		if (debug) {
			outputStream.write(Gson().toJson(ReqErrorMsg("error", "request error", e.message)).toByteArray())
		} else {
			outputStream.write(reqError)
		}
	}
	
	override val serverError: ByteArray
		get() = Companion.serverError
	
	class ReqException(s: String = "request error") : ServerException(s) {
		override val code: ByteArray
			get() {
				if (debug) {
					val json = JSONObject()
					json.put("result", "error")
					json.put("error", "request error")
					json.put("error code", message)
					return json.toString().toByteArray()
				} else
					return LoginHandler.reqError
			}
	}
	
	class RegisteredException(s: String) : Throwable(s)
	class ClosedException(s: String) : Throwable(s)
	data class Request(
			val request: String?,
			val username: String?,
			val time: Int?,
			val password: String?)
	
	data class ReqErrorMsg(val result: String?, val error: String?, val errcode: String?)
	
	companion object Companion {
		const val table = "account"
		//const val debug = false
		val reqError = "{\"result\":\"error\",\"error\":\"handleRequest error\"}".toByteArray()
		val serverError = "{\"result\":\"error\",\"error\":\"server error\"}".toByteArray()
		val userRegistered = "{\"result\":\"error\",\"error\":\"user registered\"}".toByteArray()
		val serverClosed = "{\"result\":\"error\",\"error\":\"server closed\"}".toByteArray()
	}
}