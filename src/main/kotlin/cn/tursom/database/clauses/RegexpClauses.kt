package cn.tursom.database.clauses

import cn.tursom.database.fieldName
import cn.tursom.database.sqlStr
import cn.tursom.regex.RegexMaker
import java.lang.reflect.Field
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField


class RegexpClauses(val field: String, val value: String) : Clause {
	constructor(field: Field, value: String) : this(field.fieldName, value)
	constructor(field: KProperty<*>, value: String) : this(field.javaField!!, value)
	
	constructor(field: String, value: RegexMaker.() -> String)
		: this(field, RegexMaker.value())
	
	constructor(field: Field, value: RegexMaker.() -> String)
		: this(field, RegexMaker.value())
	
	constructor(field: KProperty<*>, value: RegexMaker.() -> String)
		: this(field, RegexMaker.value())
	
	override val sqlStr: String
		get() = "${this.field} REGEXP '${value.sqlStr}'"
}