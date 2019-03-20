package cn.tursom.database

import kotlin.reflect.KCallable


class EqualWhere<T>(field: KCallable<T>, private val value: String) : SQLHelper.Where {
	private val first: String = field.name
	override val sqlStr: String
		get() = "$first=$value"
}