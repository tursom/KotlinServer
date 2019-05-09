package cn.tursom.database

import java.lang.reflect.Field
import java.lang.reflect.Method

data class SqlFieldData(val field: Field, val getter: Method? = null)

fun Iterable<SqlFieldData>.valueStr(value: Iterable<*>): String? {
	val values = StringBuilder()
	forEach field@{ (field, _) ->
		field.isAccessible = true
	}
	value.forEach { obj ->
		values.append('(')
		val iterator = iterator()
		if (!iterator.hasNext()) return@forEach
		iterator.next().let { (field, getter) ->
			values.append(getter?.invoke(obj) ?: field.get(obj)?.fieldValue)
		}
		for ((field, getter) in iterator) {
			values.append(',')
			values.append(getter?.invoke(obj) ?: field.get(obj)?.fieldValue)
		}
		values.append("),")
	}
	if (values.isNotEmpty()) {
		values.deleteCharAt(values.length - 1)
	} else {
		return null
	}
	return values.toString()
}