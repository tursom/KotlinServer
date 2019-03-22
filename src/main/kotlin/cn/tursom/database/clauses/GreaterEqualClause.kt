package cn.tursom.database.clauses

import cn.tursom.database.fieldName
import java.lang.reflect.Field
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField

class GreaterEqualClause(val field: String, val value: String) : Clause {
	constructor(field: Field, value: String) : this(field.fieldName, value)
	constructor(field: KProperty<*>, value: String) : this(field.javaField!!, value)
	
	override val sqlStr: String
		get() = "${this.field}>=$value"
}