package cn.tursom.web

interface HttpHandler<T : HttpContent> {
	fun handle(content: T)

	fun exception(e: ExceptionContent) {
		e.cause.printStackTrace()
	}

	operator fun invoke(content: T) {
		handle(content)
	}
}