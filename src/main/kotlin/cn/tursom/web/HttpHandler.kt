package cn.tursom.web

interface HttpHandler<T : HttpContent> {
    fun handle(content: T)

    fun exception(e: Throwable?)
}