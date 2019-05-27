package cn.tursom.web

@Target(AnnotationTarget.CLASS)
annotation class Path(vararg val path: String)

val <T : HttpContent>  HttpHandler<T>.getPath: List<String>
    get() {
        val list = ArrayList<String>()
        val path = javaClass.getAnnotation(Path::class.java)
        path?.path?.forEach {
            list.add(it)
        }
        return list
    }