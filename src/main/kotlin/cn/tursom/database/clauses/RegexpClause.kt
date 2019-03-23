package cn.tursom.database.clauses

import cn.tursom.database.fieldName
import cn.tursom.database.sqlStr
import cn.tursom.regex.RegexMaker
import cn.tursom.regex.RegexUnit
import java.lang.reflect.Field
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField


class RegexpClause(val field: String, val value: String) : Clause {
	constructor(field: Field, value: String) : this(field.fieldName, value)
	constructor(field: KProperty<*>, value: String) : this(field.javaField!!, value)
	
	constructor(field: String, value: Regex) : this(field, value.toString())
	constructor(field: Field, value: Regex) : this(field, value.toString())
	constructor(field: KProperty<*>, value: Regex) : this(field, value.toString())
	
	constructor(field: String, value: RegexUnit) : this(field, value.toString())
	constructor(field: Field, value: RegexUnit) : this(field, value.toString())
	constructor(field: KProperty<*>, value: RegexUnit) : this(field, value.toString())
	
	constructor(field: String, value: RegexMaker.() -> RegexUnit) : this(field, RegexMaker.value())
	constructor(field: Field, value: RegexMaker.() -> RegexUnit) : this(field, RegexMaker.value())
	constructor(field: KProperty<*>, value: RegexMaker.() -> RegexUnit) : this(field, RegexMaker.value())
	
	override val sqlStr = "$field REGEXP ${value.sqlStr}"
	override fun toString() = sqlStr
}