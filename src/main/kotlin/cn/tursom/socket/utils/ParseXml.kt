package cn.tursom.socket.utils

import org.dom4j.Element

private val typeTrans = mapOf(
	Pair(Byte::class.javaPrimitiveType, { value: String -> value.toByte() }),
	Pair(Short::class.javaPrimitiveType, { value: String -> value.toShort() }),
	Pair(Int::class.javaPrimitiveType, { value: String -> value.toInt() }),
	Pair(Long::class.javaPrimitiveType, { value: String -> value.toLong() }),
	Pair(Float::class.javaPrimitiveType, { value: String -> value.toFloat() }),
	Pair(Double::class.javaPrimitiveType, { value: String -> value.toDouble() }),
	Pair(Boolean::class.javaPrimitiveType, { value: String -> value.toBoolean() }),
	
	Pair(java.lang.String::class.java, { value: String -> value }),
	Pair(java.lang.Byte::class.java, { value: String -> value.toByte() }),
	Pair(java.lang.Short::class.java, { value: String -> value.toShort() }),
	Pair(java.lang.Integer::class.java, { value: String -> value.toInt() }),
	Pair(java.lang.Long::class.java, { value: String -> value.toLong() }),
	Pair(java.lang.Float::class.java, { value: String -> value.toFloat() }),
	Pair(java.lang.Double::class.java, { value: String -> value.toDouble() }),
	Pair(java.lang.Boolean::class.java, { value: String -> value.toBoolean() })
)

/**
 * 解析一个XML标签的属性
 * 创建一个javaBean，并将其和表示XML标签的Element对象一并传入即可解析
 *
 * @param bean 储存属性值的 javaBean 对象
 * @param element XML标签的 org.dom4j.Element 对象
 */
fun <T : Any> parseXmlAttribute(bean: T, element: Element) {
	bean.javaClass.declaredFields?.forEach {
		it.isAccessible = true
		try {
			val value = element.attributeValue(it.name) ?: return@forEach
			it.set(bean, (typeTrans[it.type]
				?: { valueObj: String -> it.type.getConstructor(java.lang.String::class.java).newInstance(valueObj) })(value))
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}
}
