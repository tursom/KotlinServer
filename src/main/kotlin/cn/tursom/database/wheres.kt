package cn.tursom.database

import java.lang.reflect.Field

class EqualWhere(field: Field, private val value: String) : SQLHelper.Where {
	private val first: String = field.fieldName
	
	override val sqlStr: String
		get() = "$first=$value"
}