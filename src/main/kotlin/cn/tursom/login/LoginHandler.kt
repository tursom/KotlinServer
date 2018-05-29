package cn.tursom.login

import com.google.gson.Gson
import cn.tursom.database.mysql.SQLAdapter
import cn.tursom.database.mysql.SQLHelper
import cn.tursom.socket.server.ServerHandler
import org.json.JSONObject
import cn.tursom.tools.fromJson
import cn.tursom.tools.sha256
import java.net.Socket

class LoginHandler(socket: Socket, private val sqlHelper: SQLHelper) : ServerHandler(socket) {
	private val salt = System.currentTimeMillis()
	private val loginAdapter = SQLAdapter(LoginData::class.java)
	
	override fun handle() {
		handleRequest1(recv(1024) ?: return)
		handleRequest2(recv(1024) ?: return)
	}
	
	private fun handleRequest1(request: String) {
		val req: Request1 = Gson().fromJson(request)
		checkRequest1(req)
		
		if (req.id == null)
			sqlHelper.select(loginAdapter, table, where = "name=${req.name
					?: throw ReqException("request username is null")}")
		else
			sqlHelper.select(loginAdapter, table, where = "id=${req.id}")
		
		if (loginAdapter.count() == 0) throw CantFindUserException("cant find user ${req.id ?: req.name}")
		
		outputStream.write(("{\"result\":\"success\",\"success\":{" +
				"\"id\":\"${loginAdapter[0].id}\"" +
				"\"name\":\"${loginAdapter[0].name}\"" +
				"\"time\":\"${loginAdapter[0].time}\"" +
				"\"salt\":\"$salt\"}}").toByteArray())
	}
	
	private fun checkRequest1(req: Request1) {
		when (req.request ?: throw ReqException("not find request")) {
			passcode.toString() -> {
				loginServer.close()
				throw ClosedException()
			}
			"request" -> return
			else -> throw ReqException()
		}
	}
	
	private fun handleRequest2(request: String) {
		val req: Request2 = Gson().fromJson(request)
		checkRequest2(req)
		
		val password = sha256("${loginAdapter[0].password ?: throw ReqException("password null")}$salt")
		if (req.request == password) {
			outputStream.write("{\"result\":\"success\",\"success\":\"$salt\"}".toByteArray())
		} else throw PasswordError()
	}
	
	private fun checkRequest2(req: Request2) {
		req.request ?: throw ReqException("not find request")
	}
	
	override val serverError: ByteArray
		get() = Companion.serverError
	
	data class Request1(val request: String?, val id: Int?, val name: String?)
	data class Request2(val request: String?)
	
	class ReqException(s: String? = "request error") : ServerException(s) {
		override val code: ByteArray
			get() {
				if (debug) {
					return Gson().toJson(ReqErrorMsg("error", "request error", message)).toByteArray()
				} else
					return reqError
			}
	}
	
	class PasswordError(s: String? = null) : ServerException(s) {
		override val code: ByteArray
			get() = passwordError
	}
	
	class ClosedException(s: String? = null) : ServerException(s) {
		override val code: ByteArray
			get() = serverClosed
	}
	
	class CantFindUserException(s: String? = "cant find user") : ServerException(s) {
		override val code: ByteArray
			get() {
				if (debug) {
					val json = JSONObject()
					json.put("result", "error")
					json.put("error", "cant find user")
					json.put("error code", message)
					return json.toString().toByteArray()
				} else
					return cantFindUser
			}
	}
	
	data class ReqErrorMsg(val result: String?, val error: String?, val errcode: String?)
	
	companion object Companion {
		const val table = "account"
		//const val debug = false
		val reqError = "{\"result\":\"error\",\"error\":\"handleRequest error\"}".toByteArray()
		val serverError = "{\"result\":\"error\",\"error\":\"server error\"}".toByteArray()
		val serverClosed = "{\"result\":\"error\",\"error\":\"server closed\"}".toByteArray()
		val cantFindUser = "{\"result\":\"error\",\"error\":\"cant find user\"}".toByteArray()
		val passwordError = "{\"result\":\"error\",\"error\":\"password error\"}".toByteArray()
	}
}