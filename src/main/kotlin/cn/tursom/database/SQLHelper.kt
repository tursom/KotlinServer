package cn.tursom.database

import jdk.nashorn.internal.objects.annotations.Where
import java.lang.reflect.Field

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
	fun createTable(table: String, keys: List<String>)
	
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
		column: List<String> = listOf("*"),
		where: List<Where>,
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
		where: List<Where> = listOf())
	
	fun <T : Any> update(table: String, value: T, where: List<Where>)
	
	fun delete(table: String, where: String)
	
	fun delete(table: String, where: List<Where>)
	
	fun commit()
	
	fun close()
	
	
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
}

val Field.fieldName: String
	get() = getAnnotation(SQLHelper.FieldName::class.java)?.name ?: name

val Field.fieldType: String?
	get() = getAnnotation(SQLHelper.FieldType::class.java)?.name ?: when (type) {
		Byte::class.java -> "TINYINT"
		Short::class.java -> "SMALLINT"
		Int::class.java -> "INT"
		Long::class.java -> "BIGINT"
		Float::class.java -> "FLOAT"
		Double::class.java -> "DOUBLE"
		String::class.java -> getAnnotation(SQLHelper.TextLength::class.java)?.let { "CHAR(${it.length})" } ?: "TEXT"
		else -> if (type.interfaces.contains(SQLHelper.SqlField::class.java)) {
			type.getAnnotation(SQLHelper.FieldType::class.java)?.name ?: type.name.split('.').last()
		} else {
			null
		}
	}