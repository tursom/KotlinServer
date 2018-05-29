package cn.tursom.socket.server

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.URL

object HttpRequest {
	const val debug = false

	/**
	 * 向指定URL发送GET方法的请求
	 *
	 * @param url
	 * 发送请求的URL
	 * @param param
	 * 请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
	 * @return URL 所代表远程资源的响应结果
	 */
	fun sendGet(url: String, param: String, header: String? = null): String {
		var result = ""
		var `in`: BufferedReader? = null
		try {
			val urlNameString = "$url?$param"
			val realUrl = URL(urlNameString)
			// 打开和URL之间的连接
			val connection = realUrl.openConnection()
			// 设置通用的请求属性
			connection.setRequestProperty("accept", "*/*")
			connection.setRequestProperty("connection", "Keep-Alive")
			connection.setRequestProperty("user-agent",
					"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)")
			if (header != null) {
				connection.addRequestProperty("Authorization", header)
			}
			// 建立实际的连接
			connection.connect()
			// 获取所有响应头字段
			//val map = connection.headerFields
			// 遍历所有的响应头字段
//			for (key in map.keys) {
//				println(key + "--->" + map[key])
//			}
			// 定义 BufferedReader输入流来读取URL的响应
			`in` = BufferedReader(InputStreamReader(
					connection.getInputStream()))
			var line: String
			while (true) {
				line = `in`.readLine() ?: break
				result += line
			}
		} catch (e: Exception) {
			System.err.println("发送GET请求出现异常！$e")
			if (debug)
				e.printStackTrace()
		} finally {
			try {
				if (`in` != null) {
					`in`.close()
				}
			} catch (e2: Exception) {
				if (debug)
					e2.printStackTrace()
			}

		}// 使用finally块来关闭输入流
		return result
	}

	/**
	 * 向指定 URL 发送POST方法的请求
	 *
	 * @param url
	 * 发送请求的 URL
	 * @param param
	 * 请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
	 * @return 所代表远程资源的响应结果
	 */
	fun sendPost(url: String, param: String): String {
		var out: PrintWriter? = null
		var `in`: BufferedReader? = null
		var result = ""
		try {
			val realUrl = URL(url)
			// 打开和URL之间的连接
			val conn = realUrl.openConnection()
			// 设置通用的请求属性
			conn.setRequestProperty("accept", "*/*")
			conn.setRequestProperty("connection", "Keep-Alive")
			conn.setRequestProperty("user-agent",
					"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)")
			// 发送POST请求必须设置如下两行
			conn.doOutput = true
			conn.doInput = true
			// 获取URLConnection对象对应的输出流
			out = PrintWriter(conn.getOutputStream())
			// 发送请求参数
			out.print(param)
			// flush输出流的缓冲
			out.flush()
			// 定义BufferedReader输入流来读取URL的响应
			`in` = BufferedReader(
					InputStreamReader(conn.getInputStream()))
			var line: String
			while (true) {
				line = `in`.readLine() ?: break
				result += line
			}
		} catch (e: Exception) {
			println("发送 POST 请求出现异常！$e")
			e.printStackTrace()
		} finally {
			try {
				if (out != null) {
					out.close()
				}
				if (`in` != null) {
					`in`.close()
				}
			} catch (ex: IOException) {
				ex.printStackTrace()
			}

		}//使用finally块来关闭输出流、输入流
		return result
	}
}