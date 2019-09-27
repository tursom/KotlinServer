package cn.tursom.aop.advice

import cn.tursom.aop.ProxyHandler
import java.lang.reflect.Method

@Suppress("MemberVisibilityCanBePrivate")
class AdviceContent(
	val target: Any,
	val method: Method,
	val args: Array<out Any>?
) {
	val bean: Any = if (target is ProxyHandler) target.getTopBean() else target

	fun invoke() {
		if (args != null) {
			method.invoke(target, *args)
		} else {
			method.invoke(target)
		}
	}
}