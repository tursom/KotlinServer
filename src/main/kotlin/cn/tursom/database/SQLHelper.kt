package cn.tursom.database

import cn.tursom.database.annotation.*
import cn.tursom.database.clauses.Clause
import java.io.Closeable
import java.lang.reflect.Field
import java.util.AbstractCollection
import kotlin.collections.forEach
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField

/**
 * MySQLHelper，SQLite辅助使用类
 * 实现创建表格、查询、插入和更新功能
 */

interface SQLHelper : Closeable {
	/**
	 * 创建表格
	 * @param table: 表格名
	 * @param keys: 属性列表
	 */
	fun createTable(table: String, keys: Iterable<String>)
	
	/**
	 * 根据提供的class对象自动化创建表格
	 * 但是有诸多缺陷，所以不是很建议使用
	 */
	fun createTable(fields: Class<*>)
	
	/**
	 * 删除表格
	 */
	fun deleteTable(table: String)
	
	/**
	 * 删除表格
	 */
	fun dropTable(table: String)
	
	/**
	 * 查询
	 * @param adapter 用于保存查询结果的数据类，由SQLAdapter继承而来
	 * @param fields 查询字段
	 * @param where 指定从一个表或多个表中获取数据的条件,Pair左边为字段名，右边为限定的值
	 * @param maxCount 最大查询数量
	 */
	fun <T : Any> select(
		adapter: SQLAdapter<T>,
		fields: Iterable<String>? = null,
		where: Clause,
		order: Field? = null,
		reverse: Boolean = false,
		maxCount: Int? = null
	): SQLAdapter<T>
	
	/**
	 * 用于支持灵活查询
	 */
	fun <T : Any> select(
		adapter: SQLAdapter<T>,
		fields: String = "*",
		where: String? = null,
		order: String? = null,
		reverse: Boolean = false,
		maxCount: Int? = null
	): SQLAdapter<T>
	
	/**
	 * 插入
	 * @param value 值
	 */
	fun <T : Any> insert(value: T)
	
	fun insert(valueList: Iterable<*>)
	
	fun insert(table: String, fields: String, values: String)
	
	fun <T : Any> update(value: T, where: Clause)
	
	fun delete(table: String, where: String? = null)
	
	fun delete(table: String, where: Clause)
	
	fun commit()
	
	interface SqlField<T> {
		fun get(): T
		val sqlValue: String
	}
	
	interface Where {
		val sqlStr: String
	}
}

val Field.fieldName: String
	get() = getAnnotation(FieldName::class.java)?.name ?: name
val KProperty<*>.fieldName: String
	get() = javaField!!.fieldName

val <T : Any>T.tableName: String
	get() = javaClass.tableName

val <T> Class<T>.tableName: String
	get() = (getAnnotation<TableName>()?.name ?: name.split('.').last()).toLowerCase()

val <T : Any>T.fieldValue: String
	get() = when (this) {
		is SQLHelper.SqlField<*> -> this.javaClass.getAnnotation(StringField::class.java)?.let {
			sqlValue.sqlStr
		} ?: sqlValue
		is String -> sqlStr
		else -> toString()
	}

val Class<*>.isSqlField
	get() = interfaces.contains(SQLHelper.SqlField::class.java)

/**
 * 用于支持灵活查询
 */
inline fun <reified T : Any> SQLHelper.select(
	fields: String = "*",
	where: String? = null,
	order: String? = null,
	reverse: Boolean = false,
	maxCount: Int? = null
): SQLAdapter<T> = select(SQLAdapter(T::class.java), fields, where, order, reverse, maxCount)

inline fun <reified T : Any> SQLHelper.select(
	fields: Iterable<String> = listOf("*"),
	where: Clause,
	order: Field? = null,
	reverse: Boolean = false,
	maxCount: Int? = null
): SQLAdapter<T> = select(SQLAdapter(T::class.java), fields, where, order, reverse, maxCount)

inline fun <reified T : Annotation> Field.getAnnotation(): T? = getAnnotation(T::class.java)
inline fun <reified T : Annotation> Class<*>.getAnnotation(): T? = getAnnotation(T::class.java)

fun <T : Any> SQLHelper.select(
	clazz: Class<T>,
	fields: Iterable<String> = listOf("*"),
	where: Clause,
	order: Field? = null,
	reverse: Boolean = false,
	maxCount: Int? = null
): SQLAdapter<T> = select(SQLAdapter(clazz), fields, where, order, reverse, maxCount)

/**
 * 用于支持灵活查询
 */
fun <T : Any> SQLHelper.select(
	clazz: Class<T>,
	fields: String = "*",
	where: String? = null,
	order: String? = null,
	reverse: Boolean = false,
	maxCount: Int? = null
): SQLAdapter<T> = select(SQLAdapter(clazz), fields, where, order, reverse, maxCount)

fun SQLHelper.delete(
	clazz: Class<*>,
	where: String? = null
) = delete(clazz.tableName, where)

fun Array<out Field>.fieldStr(): String {
	val fields = StringBuilder()
	forEach field@{ field ->
		field.isAccessible = true
		fields.append("${field.fieldName},")
	}
	fields.deleteCharAt(fields.length - 1)
	return fields.toString()
}

fun Iterable<String>.fieldStr(): String {
	val stringBuffer = StringBuffer()
	forEach {
		if (it.isNotEmpty())
			stringBuffer.append("$it,")
	}
	stringBuffer.delete(stringBuffer.length - 1, stringBuffer.length)
	return stringBuffer.toString()
}

fun Iterable<String>.fieldNameStr(): String {
	val stringBuffer = StringBuffer()
	forEach {
		if (it.isNotEmpty())
			stringBuffer.append("`$it`,")
	}
	stringBuffer.delete(stringBuffer.length - 1, stringBuffer.length)
	return stringBuffer.toString()
}

fun Array<out Field>.valueStr(value: Any): String? {
	val values = StringBuilder()
	forEach field@{ field ->
		field.isAccessible = true
		values.append(field.get(value)?.fieldValue)
		values.append(',')
	}
	if (values.isNotEmpty()) {
		values.deleteCharAt(values.length - 1)
	} else {
		return null
	}
	return values.toString()
}

fun Iterable<*>.valueStr(sqlFieldMap: Array<out Field>): String? {
	val values = StringBuilder()
	forEach { value ->
		value ?: return@forEach
		values.append("(${sqlFieldMap.valueStr(value) ?: return@forEach}),")
	}
	if (values.isNotEmpty()) {
		values.deleteCharAt(values.length - 1)
	} else {
		return null
	}
	return values.toString()
}


fun Iterable<SQLHelper.Where>.whereStr(): String {
	val stringBuilder = StringBuilder()
	forEach {
		stringBuilder.append("${it.sqlStr} AND ")
	}
	if (stringBuilder.isNotEmpty())
		stringBuilder.delete(stringBuilder.length - 5, stringBuilder.length)
	return stringBuilder.toString()
}

fun List<Pair<String, String>>.fieldStr(): Pair<String, String> {
	val first = StringBuilder()
	val second = StringBuilder()
	forEach { (f, s) ->
		first.append("$f,")
		second.append("$s,")
	}
	if (first.isNotEmpty()) first.deleteCharAt(first.length - 1)
	if (second.isNotEmpty()) second.deleteCharAt(second.length - 1)
	return first.toString() to second.toString()
}


fun StringBuilder.appendField(
	field: Field,
	fieldType: Field.() -> String?,
	foreignKeyList: AbstractCollection<Pair<String, String>>,
	primaryKey: Field.() -> Unit
) {
	val fieldName = field.fieldName
	append("`$fieldName` ${field.fieldType() ?: return}")
	field.annotations.forEach annotations@{ annotation ->
		append(" ${when (annotation) {
			is NotNull -> "NOT NULL"
			is AutoIncrement -> "AUTO_INCREMENT"
			is Unique -> "UNIQUE"
			is Default -> "DEFAULT ${annotation.default}"
			is Check -> "CHECK(${field.fieldName}${annotation.func})"
			is ExtraAttribute -> annotation.attributes
			is ForeignKey -> {
				foreignKeyList.add(fieldName to if (annotation.target.isNotEmpty()) annotation.target else fieldName)
				return@annotations
			}
			is PrimaryKey -> {
				field.primaryKey()
				return@annotations
			}
			else -> return@annotations
		}}")
	}
	append(',')
}

val String.sqlStr
	get() = "'${replace("'", "''")}'"