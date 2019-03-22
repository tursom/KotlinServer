package cn.tursom.database.clauses

import cn.tursom.database.fieldName
import java.lang.reflect.Field
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField

class InfixExpressionClause(val field: String, val value: String, val expression: String) : Clause {
	constructor(field: Field, value: String, expression: String) : this(field.fieldName, value, expression)
	constructor(field: KProperty<*>, value: String, expression: String) : this(field.javaField!!, value, expression)
	
	override val sqlStr: String
		get() = "${this.field}$expression$value"
}