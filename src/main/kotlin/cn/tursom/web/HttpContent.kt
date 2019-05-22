package cn.tursom.web

interface HttpContent {
    val uri: String
    var responseCode: Int
    var responseMessage: String?

    fun getHeader(header: String): String?
    fun getHeaders(): List<Map.Entry<String, String>>

    fun getParam(param: String): String?
    fun getParams(): Map<String, List<String>>
    fun getParams(param: String): List<String>?

    fun addResponseHeader(name: String, value: Any)

    fun write(message: String)
    fun write(byte: Int)
    fun write(bytes: ByteArray)

    fun finish()
}

fun HttpContent.setResponseCode(code: Int, message: String) {
    responseCode = code
    responseMessage = message
}