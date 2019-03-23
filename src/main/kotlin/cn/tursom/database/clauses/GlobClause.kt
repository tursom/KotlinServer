package cn.tursom.database.clauses

import cn.tursom.database.fieldName
import cn.tursom.database.sqlStr
import java.lang.reflect.Field
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField

object GlobValue {
	const val one = '*'
	const val any = '*'
	@Suppress("UNUSED_EXPRESSION")
	fun make(maker: GlobValue.() -> String) = maker()
}

class GlobClause(field: String, value: String) : Clause {
	constructor(field: Field, value: String) : this(field.fieldName, value)
	constructor(field: KProperty<*>, value: String) : this(field.javaField!!, value)
	
	override val sqlStr = "$field GLOB ${value.sqlStr}"
	override fun toString() = sqlStr
}