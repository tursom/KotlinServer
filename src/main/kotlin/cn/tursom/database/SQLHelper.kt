package cn.tursom.database

import org.sqlite.SQLiteException
import java.io.Closeable
import java.lang.reflect.Field
import java.sql.Connection

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
	fun createTable(table: String, keys: List<String>)
	
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
		fields: List<String> = listOf("*"),
		where: List<Where>,
		maxCount: Int? = null
	): SQLAdapter<T>
	
	/**
	 * 用于支持灵活查询
	 */
	fun <T : Any> select(
		adapter: SQLAdapter<T>,
		fields: String = "*",
		where: String? = null,
		maxCount: Int? = null
	): SQLAdapter<T>
	
	/**
	 * 插入
	 * @param value 值
	 */
	fun <T : Any> insert(value: T)
	
	fun insert(valueList: List<*>)
	
	fun insert(table: String, fields: String, values: String)
	
	fun <T : Any> update(value: T, where: List<Where>)
	
	fun delete(table: String, where: String? = null)
	
	fun delete(table: String, where: List<Where>)
	
	fun commit()
	
	interface SqlField<T> {
		fun get(): T
		val sqlValue: String
	}
	
	interface Where {
		val sqlStr: String
	}
	
	@MustBeDocumented
	@Target(AnnotationTarget.FIELD)
	annotation class ExtraAttribute(val attributes: String)
	
	@MustBeDocumented
	@Target(AnnotationTarget.FIELD)
	annotation class NotNull
	
	@MustBeDocumented
	@Target(AnnotationTarget.FIELD)
	annotation class AutoIncrement
	
	@MustBeDocumented
	@Target(AnnotationTarget.FIELD)
	annotation class PrimaryKey
	
	@MustBeDocumented
	@Target(AnnotationTarget.FIELD)
	annotation class Unique
	
	/**
	 * only for string
	 */
	@MustBeDocumented
	@Target(AnnotationTarget.FIELD)
	annotation class TextLength(val length: Int)
	
	@MustBeDocumented
	@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
	annotation class FieldName(val name: String)
	
	@MustBeDocumented
	@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD)
	annotation class FieldType(val name: String)
	
	@MustBeDocumented
	@Target(AnnotationTarget.CLASS)
	annotation class TableName(val name: String)
}

val Field.fieldName: String
	get() = getAnnotation(SQLHelper.FieldName::class.java)?.name ?: name


val <T : Any>T.tableName: String
	get() = javaClass.tableName

val <T> Class<T>.tableName: String
	get() = getAnnotation<SQLHelper.TableName>()?.name ?: name.split('.').last()

val <T : Any>T.fieldValue: String
	get() = when (this) {
		is SQLHelper.SqlField<*> -> sqlValue
		is String -> "'${replace("'", "''")}'"
		else -> toString()
	}

inline fun <reified T : Annotation> Field.getAnnotation(): T? = getAnnotation(T::class.java)
inline fun <reified T : Annotation> Class<*>.getAnnotation(): T? = getAnnotation(T::class.java)

inline fun <reified T : Any> SQLHelper.select(
	fields: List<String> = listOf("*"),
	where: List<SQLHelper.Where>,
	maxCount: Int? = null
): SQLAdapter<T> = select(SQLAdapter(T::class.java), fields, where, maxCount)

/**
 * 用于支持灵活查询
 */
inline fun <reified T : Any> SQLHelper.select(
	fields: String = "*",
	where: String? = null,
	maxCount: Int? = null
): SQLAdapter<T> = select(SQLAdapter(T::class.java), fields, where, maxCount)

fun <T : Any> SQLHelper.select(
	clazz: Class<T>,
	fields: List<String> = listOf("*"),
	where: List<SQLHelper.Where>,
	maxCount: Int? = null
): SQLAdapter<T> = select(SQLAdapter(clazz), fields, where, maxCount)

/**
 * 用于支持灵活查询
 */
fun <T : Any> SQLHelper.select(
	clazz: Class<T>,
	fields: String = "*",
	where: String? = null,
	maxCount: Int? = null
): SQLAdapter<T> = select(SQLAdapter(clazz), fields, where, maxCount)

fun SQLHelper.delete(clazz: Class<*>, where: String? = null) {
	delete(clazz.tableName, where)
}

fun Array<Field>.fieldStr(): String {
	val fields = StringBuilder()
	forEach field@{ field ->
		field.isAccessible = true
		fields.append("${field.fieldName},")
	}
	fields.deleteCharAt(fields.length - 1)
	return fields.toString()
}

fun Map<Field, Boolean>.valueStr(value: Any): String {
	val values = StringBuilder()
	forEach field@{ (field, isSqlField) ->
		field.isAccessible = true
		val fieldValue = field.get(value) //?: return@field
		values.append(when {
			fieldValue == null -> "null"
			isSqlField -> (fieldValue as SQLHelper.SqlField<*>).sqlValue
			field.type == String::class.java -> "'${(fieldValue as String).replace("'", "''")}'"
			else -> fieldValue
		})
		values.append(',')
	}
	if (values.isNotEmpty()) values.deleteCharAt(values.length - 1)
	return values.toString()
}

fun Array<out Field>.sqlFieldMap(): Map<Field, Boolean> {
	val sqlFieldMap = java.util.HashMap<Field, Boolean>()
	forEach {
		sqlFieldMap[it] = it.type.interfaces.contains(SQLHelper.SqlField::class.java)
	}
	return sqlFieldMap
}

fun List<*>.valueStr(sqlFieldMap: Map<Field, Boolean>): String {
	val values = StringBuilder()
	forEach { value ->
		value ?: return@forEach
		values.append("(${sqlFieldMap.valueStr(value)}),")
	}
	if (values.isNotEmpty()) {
		values.deleteCharAt(values.length - 1)
	}
	return values.toString()
}

fun SQLHelper.insert(connection: Connection, sql: String, table: Class<*>) {
	val statement = connection.createStatement()
	try {
		statement.executeUpdate(sql)
	} catch (e: SQLiteException) {
		if (e.message == "[SQLITE_ERROR] SQL error or missing database (no such table: $table)") {
			createTable(table)
			statement.executeUpdate(sql)
		} else {
			e.printStackTrace()
		}
	}
	connection.commit()
	statement.closeOnCompletion()
}