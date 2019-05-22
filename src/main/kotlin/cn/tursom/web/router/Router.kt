package cn.tursom.web.router

interface Router<T> {
	val route: String
	var value: T?
	val wildSubRouter: Router<T>?
	val placeholderRouter: List<PlaceholderRouter<T>>
	val subRouterMap: Map<String, Router<T>>
	
	fun addSubRoute(router: Router<T>)
	fun delRouter(route: String)
	fun match(route: String): Boolean
	fun getSubRouter(route: String): Router<T>?
}

operator fun <T> Router<T>.set(fullRoute: String, value: T) = addRoute(fullRoute, value)
operator fun <T> Router<T>.get(fullRoute: String) = findRouter(fullRoute)

fun <T> Router<T>.findRouter(
	fullRoute: String,
	forEachRouter: ((route: String, router: Router<T>) -> Unit)? = null
) = findRouter(fullRoute.split('/').drop(0), forEachRouter)

fun <T> Router<T>.findRouter(
	fullRoute: List<String>,
	forEachRouter: ((route: String, router: Router<T>) -> Unit)? = null
): Router<T>? {
	var router = this
	var index = -1
	while (++index < fullRoute.size) {
		val r = fullRoute[index]
		router = when {
			r.isEmpty() -> router
			r[0] == ':' -> {
				var subRouter: Router<T> = router.placeholderRouter.find {
					it.length == fullRoute.matchLength(index)
				} ?: return null
				while (subRouter is PlaceholderRouter) {
					forEachRouter?.invoke(r, subRouter)
					
				}
				subRouter
			}
			else -> router.getSubRouter(r)
		} ?: return null
		forEachRouter?.invoke(r, router)
	}
	return router
}

fun List<String>.matchLength(startIndex: Int = 0): Int {
	var endIndex = startIndex
	var length = 1
	while (++endIndex < size && this[endIndex][0] == ':') length++
	return length
}

fun <T> buildPlaceholderRouter(routeList: List<String>, index: Int): PlaceholderRouter<T>? {
	var endIndex = index
	while (++endIndex < routeList.size && routeList[endIndex][0] == ':');
	var rootRouter: PlaceholderRouter<T>? = null
	var router: PlaceholderRouter<T>
	while (endIndex-- > index) {
		router = PlaceholderRouter(routeList[endIndex].substring(1))
		rootRouter?.addSubRoute(router)
		rootRouter = router
	}
	return rootRouter
}

fun <T> Router<T>.addRoute(fullRoute: String, value: T?) {
	val routeList = fullRoute.split('/').drop(0)
	var router: Router<T> = this
	var index = 0
	while (index < routeList.size) {
		val r = routeList[index]
		val subRouter = router.getSubRouter(r)
		router = when (subRouter) {
			null -> run {
				val newRouter = when {
					r == "*" -> AnyRouter<T>(null)
					r[0] == ':' -> buildPlaceholderRouter(routeList, index)!!
					else -> DefaultRouter<T>(r, null)
				}
				router.addSubRoute(newRouter)
				newRouter
			}
			is PlaceholderRouter -> {
				router.placeholderRouter.forEach {
					// TODO
					if (routeList.matchLength(index) == it.length) throw Exception()
				}
				val newRouter = buildPlaceholderRouter<T>(routeList, index)!!
				router.addSubRoute(newRouter)
				newRouter
			}
			else -> subRouter
		}
		index++
	}
	router.value = value
}

open class DefaultRouter<T>(
	override val route: String = "",
	override var value: T? = null
) : Router<T> {
	override val subRouterMap = HashMap<String, Router<T>>(1)
	override var wildSubRouter: AnyRouter<T>? = null
	override val placeholderRouter: ArrayList<PlaceholderRouter<T>> = ArrayList(1)
	
	override fun addSubRoute(router: Router<T>) {
		when (router) {
			is AnyRouter -> {
				wildSubRouter = router
			}
			is PlaceholderRouter -> synchronized(placeholderRouter) {
				if (placeholderRouter.binarySearch { it.length - router.length } > 0)
					throw Exception("route exist")
				placeholderRouter.add(router)
				placeholderRouter.sortBy { it.length }
			}
			else -> synchronized(subRouterMap) {
				val route = router.route
				subRouterMap[route] = router
			}
		}
	}
	
	override fun delRouter(route: String) {
		subRouterMap.remove(route)
	}
	
	override fun match(route: String): Boolean {
		return route == this.route
	}
	
	override fun getSubRouter(route: String) =
		when {
			route.isEmpty() -> this
			route == "*" -> wildSubRouter
			route[0] == ':' -> synchronized(placeholderRouter) { placeholderRouter.find { it.match(route) } }
			else -> synchronized(subRouterMap) { subRouterMap[route] }
		}
	
	override fun toString(): String {
		return "DefaultRouter(route='$route', value=$value, subRouterMap=$subRouterMap, wildSubRouter=$wildSubRouter, placeholderRouter=$placeholderRouter)"
	}
}

class AnyRouter<T>(value: T? = null) : DefaultRouter<T>("*", value) {
	override fun match(route: String) = true
	override fun getSubRouter(route: String) = this
	override fun toString() = "AnyRouter(value='$value')"
	override fun addSubRoute(router: Router<T>) = Unit
}

class PlaceholderRouter<T>(
	@Suppress("MemberVisibilityCanBePrivate") val matchRoute: String,
	value: T? = null
) : DefaultRouter<T>(":$matchRoute", value) {
	private var routeSize: Int = 1
	val length: Int
		get() = routeSize
	
	override fun match(route: String) = true
	override fun toString() = "PlaceholderRouter(route='$route', value='$value')"
	override fun addSubRoute(router: Router<T>) {
		super.addSubRoute(router)
		if (router is PlaceholderRouter)
			routeSize = router.routeSize + 1
	}
}

fun main() {
	val router = DefaultRouter("", 1)
	router["/:123"] = 2
	router["/:456/:789"] = 3
	router["/:456/:789"] = 3
	println(router)
}