package cn.tursom.web

interface ExceptionContent {
    val cause: Throwable

    fun write(message: String)
    fun write(bytes: ByteArray)

    fun finish()
}