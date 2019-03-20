package cn.tursom.database

import java.io.Closeable
import java.lang.reflect.Field

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
	fun <T> createTable(fields: Class<T>)
	
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
	 * @param column 查询字段
	 * @param where 指定从一个表或多个表中获取数据的条件,Pair左边为字段名，右边为限定的值
	 * @param maxCount 最大查询数量
	 */
	fun <T : Any> select(
		adapter: SQLAdapter<T>,
		column: List<String> = listOf("*"),
		where: List<Where>,
		maxCount: Int? = null
	): SQLAdapter<T>
	
	/**
	 * 用于支持灵活查询
	 */
	fun <T : Any> select(
		adapter: SQLAdapter<T>,
		column: String = "*",
		where: String? = null,
		maxCount: Int? = null
	): SQLAdapter<T>
	
	/**
	 * 插入
	 * @param value 值
	 */
	fun <T : Any> insert(value: T)
	
	fun insert(table: String, column: Map<String, String>)
	
	fun insert(table: String, column: String, values: String)
	
	fun update(
		table: String,
		set: Map<String, String> = mapOf(),
		where: List<Where> = listOf())
	
	fun <T : Any> update(value: T, where: List<Where>)
	
	fun delete(table: String, where: String)
	
	fun delete(table: String, where: List<Where>)
	
	fun commit()
	
	interface SqlField<T> {
		fun adapt(obj: Any)
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
	@Target(AnnotationTarget.FIELD)
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
	column: List<String> = listOf("*"),
	where: List<SQLHelper.Where>,
	maxCount: Int? = null
): SQLAdapter<T> = select(SQLAdapter(T::class.java), column, where, maxCount)

/**
 * 用于支持灵活查询
 */
inline fun <reified T : Any> SQLHelper.select(
	column: String = "*",
	where: String? = null,
	maxCount: Int? = null
): SQLAdapter<T> = select(SQLAdapter(T::class.java), column, where, maxCount)

fun <T : Any> SQLHelper.select(
	clazz: Class<T>,
	column: List<String> = listOf("*"),
	where: List<SQLHelper.Where>,
	maxCount: Int? = null
): SQLAdapter<T> = select(SQLAdapter(clazz), column, where, maxCount)

/**
 * 用于支持灵活查询
 */
fun <T : Any> SQLHelper.select(
	clazz: Class<T>,
	column: String = "*",
	where: String? = null,
	maxCount: Int? = null
): SQLAdapter<T> = select(SQLAdapter(clazz), column, where, maxCount)