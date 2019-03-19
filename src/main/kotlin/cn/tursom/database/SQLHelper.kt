package cn.tursom.database

import kotlin.reflect.KCallable

@MustBeDocumented
@Target(AnnotationTarget.FIELD)
annotation class ExtraAttribute(val attributes: String)

@MustBeDocumented
@Target(AnnotationTarget.FIELD)
annotation class NotNullField

@MustBeDocumented
@Target(AnnotationTarget.FIELD)
annotation class AutoIncrement

@MustBeDocumented
@Target(AnnotationTarget.FIELD)
annotation class PrimaryKey

@MustBeDocumented
@Target(AnnotationTarget.FIELD)
annotation class Unique

@MustBeDocumented
@Target(AnnotationTarget.FIELD)
annotation class TextLength(val length: Int)

@MustBeDocumented
@Target(AnnotationTarget.CLASS)
annotation class SqlFieldType(val name: String)

interface SqlField<T> {
	fun adapt(obj: Any)
	fun get(): T
	val sqlValue: String
}

interface Where {
	val sqlStr: String
}

class EqualWhere<T>(field: KCallable<T>, private val value: String) : Where {
	private val first: String = field.name
	override val sqlStr: String
		get() = "$first=$value"
}

/**
 * MySQLHelper，SQLite辅助使用类
 * 实现创建表格、查询、插入和更新功能
 */

interface SQLHelper {
	/**
	 * 创建表格
	 * @param table: 表格名
	 * @param keys: 属性列表
	 */
	fun createTable(table: String, keys: Array<String>)
	
	/**
	 * 根据提供的class对象自动化创建表格
	 * 但是有诸多缺陷，所以不是很建议使用
	 */
	fun <T> createTable(table: String, keys: Class<T>)
	
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
	 * @param table 表名
	 * @param column 查询字段
	 * @param where 指定从一个表或多个表中获取数据的条件,Pair左边为字段名，右边为限定的值
	 * @param maxCount 最大查询数量
	 */
	fun <T : Any> select(
		adapter: SQLAdapter<T>,
		table: String,
		column: Array<String> = arrayOf("*"),
		where: Array<Where>,
		maxCount: Int? = null
	)
	
	/**
	 * 用于支持灵活查询
	 */
	fun <T : Any> select(
		adapter: SQLAdapter<T>,
		table: String,
		column: String = "*",
		where: String? = null,
		maxCount: Int? = null
	)
	
	/**
	 * 插入
	 * @param table 表名
	 * @param value 值
	 */
	fun <T : Any> insert(table: String, value: T)
	
	fun insert(table: String, column: Map<String, String>)
	
	fun insert(table: String, column: String, values: String)
	
	fun update(
		table: String,
		set: Map<String, String> = mapOf(),
		where: Array<Where> = arrayOf())
	
	fun <T : Any> update(table: String, value: T, where: Array<Where>)
	
	fun delete(table: String, where: String)
	
	fun delete(table: String, where: Array<Where>)
	
	fun commit()
	
	fun close()
}