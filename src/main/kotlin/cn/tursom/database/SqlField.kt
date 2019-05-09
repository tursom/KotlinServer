package cn.tursom.database

import cn.tursom.database.annotation.FieldName
import cn.tursom.database.annotation.StringField
import java.lang.reflect.Field
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField

interface SqlField<T> {
	fun get(): T
	val sqlValue: String
}

val Field.fieldName: String
	get() = getAnnotation(FieldName::class.java)?.name ?: name

val KProperty<*>.fieldName: String
	get() = javaField!!.fieldName

val Any.fieldValue: String
	get() = when (this) {
		is SqlField<*> -> this.javaClass.getAnnotation(StringField::class.java)?.let {
			sqlValue.sqlStr
		} ?: sqlValue
		is String -> sqlStr
		else -> toString()
	}

val Class<*>.isSqlField
	get() = interfaces.contains(SqlField::class.java)