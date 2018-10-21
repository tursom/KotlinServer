package cn.tursom.database.mysql

import cn.tursom.database.SQLAdapter
import cn.tursom.database.SQLHelper
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import kotlin.collections.ArrayList
import java.text.SimpleDateFormat
import java.text.DateFormat


/*
 * MySQLHelper，SQLite辅助使用类
 * 实现创建表格、查询、插入和更新功能
 */
class MySQLHelper(val connection: Connection) : SQLHelper {
	/**
	 * 根据提供的class对象自动化创建表格
	 * 但是有诸多缺陷，所以不是很建议使用
	 */
	override fun <T> createTable(table: String, keys: Class<T>) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
	
	/**
	 * 删除表格
	 */
	override fun deleteTable(table: String) {
		val statement = connection.createStatement()
		statement.executeUpdate("DROP TABLE if exists $table ENGINE = InnoDB DEFAULT CHARSET=utf8;")
		connection.commit()
	}
	
	/**
	 * 删除表格
	 */
	override fun dropTable(table: String) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
	
	/**
	 * 查询
	 * @param adapter 用于保存查询结果的数据类，由SQLAdapter继承而来
	 * @param table 表名
	 * @param name 查询字段
	 * @param where 指定从一个表或多个表中获取数据的条件,Pair左边为字段名，右边为限定的值
	 * @param maxCount 最大查询数量
	 */
	override fun <T : Any> select(adapter: SQLAdapter<T>, table: String, name: Array<String>, where: Map<String, String>?, maxCount: Int?) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
	
	/**
	 * 查询
	 * @param adapter 用于保存查询结果的数据类，由SQLAdapter继承而来
	 * @param table 表名
	 * @param name 查询字段
	 * @param where 指定从一个表或多个表中获取数据的条件,Pair左边为字段名，右边为限定的值
	 * @param maxCount 最大查询数量
	 */
	override fun <T : Any> select(adapter: SQLAdapter<T>, table: String, where: Pair<String, String>, maxCount: Int?, name: Array<String>) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
	
	override fun <T : Any> select(adapter: SQLAdapter<T>, table: String, name: String, where: String?, maxCount: Int?) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
	
	override fun <T : Any> reverseSelect(adapter: SQLAdapter<T>, table: String, name: Array<String>, where: Pair<String, String>, index: String, maxCount: Int?) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
	
	override fun <T : Any> reverseSelect(adapter: SQLAdapter<T>, table: String, name: String, where: String?, index: String, maxCount: Int?) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
	
	override fun update(table: String, set: Map<String, String>, where: Map<String, String>) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
	
	override fun delete(table: String, where: Pair<String, String>) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
	
	override fun commit() {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
	
	init {
		connection.autoCommit = false
	}
	
	constructor(url: String, user: String, password: String)
		: this(DriverManager.getConnection("jdbc:mysql://$url?characterEncoding=utf-8", user, password))
	
	private val basename: String
		get() {
			val resultSet = connection.createStatement().executeQuery("SELECT database()")
			resultSet.next()
			return resultSet.getString(1)
		}
	
	/*
	 * 创建表格
	 * table: 表格名
	 * keys: 属性列表
	 */
	override fun createTable(table: String, keys: Array<String>) {
		val statement = connection.createStatement()
		statement.executeUpdate("CREATE TABLE if not exists `$table` ( ${toColumn(keys)} ) ENGINE = InnoDB DEFAULT CHARSET=utf8;")
		connection.commit()
	}
	
	fun createTable(table: String, keys: Map<String, String>) {
		val keysArray = ArrayList<String>()
		keys.forEach {
			keysArray.add("`${it.key}` ${it.value}")
		}
		createTable(table, keysArray.toTypedArray())
	}
	
	/*
	 * 查询
	 * adapter: 用于保存查询结果的数据类，由SQLAdapter继承而来
	 * table: 表名
	 * name: 查询字段
	 * where: 指定从一个表或多个表中获取数据的条件,Pair左边为字段名，右边为限定的值
	 */
	inline fun <reified T : Any> select(
		table: String, name: Array<String> = arrayOf("*"),
		where: Map<String, String>): SQLAdapter<T> {
		val adapter = SQLAdapter(T::class.java)
		select(adapter, table, name, where)
		return adapter
	}
	
	inline fun <reified T : Any> select(
		table: String, name: String = "*",
		where: String? = null): SQLAdapter<T> {
		val adapter = SQLAdapter(T::class.java)
		select(adapter, table, name, where)
		return adapter
	}
	
	fun <T : Any> select(
		adapter: SQLAdapter<T>, table: String,
		name: String = "*", where: String? = null
	) {
		val statement = connection.createStatement()
		val resultSet =
			if (where == null || where.isEmpty())
				statement.executeQuery("SELECT $name FROM $table ;")
			else
				statement.executeQuery("SELECT $name FROM $table WHERE $where;")
		adapter.adapt(resultSet)
		resultSet.close()
		statement.closeOnCompletion()
	}
	
	/**
	 * 插入
	 * @param table 表名
	 * @param column
	 */
	override fun insert(table: String, column: Map<String, String>) {
		val columns = toKeys(column)
		insert(table, columns.first, columns.second)
	}
	
	override fun insert(table: String, column: String, values: String) {
		val statement = connection.createStatement()
		statement.executeUpdate("INSERT INTO $table ($column) VALUES ($values);")
		connection.commit()
		statement.closeOnCompletion()
	}
	
	override fun <T : Any> insert(table: String, value: T) {
		val valueMap = HashMap<String, String>()
		value.javaClass.declaredFields.forEach {
			if (it.type == Date::class.java) {
				valueMap[it.name] = java.sql.Date((getFieldValueByName(it.name, value) as Date).time).toString()
			} else
				valueMap[it.name] = getFieldValueByName(it.name, value).toString()
		}
		
		val column = StringBuilder()
		val values = StringBuilder()
		value.javaClass.declaredFields.forEach {
			column.append("${it.name},")
			values.append(when (it.type) {
				String::class.java -> "'${getFieldValueByName(it.name, value).toString().replace("'", "''")}'"
				Date::class.java -> "'${dateFoemat.format(getFieldValueByName(it.name, value) as Date)}'"
				else -> getFieldValueByName(it.name, value).toString()
			})
			values.append(',')
		}
		if (column.isNotEmpty())
			column.delete(column.length - 1, column.length)
		if (values.isNotEmpty())
			values.delete(values.length - 1, values.length)
		insert(table, column.toString(), values.toString())
	}
	
	private fun update(
		table: String,
		set: String,
		where: String) {
		val statement = connection.createStatement()
		statement.executeUpdate("UPDATE $table SET $set WHERE $where;")
		connection.commit()
		statement.closeOnCompletion()
	}
	
	/**
	 * 更新数据库数据
	 * @param table 表名
	 * @param value 用来存储数据的bean对象
	 * @param where SQL语句的一部分，用来限定查找的条件。每一条String储存一个条件
	 */
	fun <T : Any> update(table: String, value: T, where: Array<String>) {
		val sb = StringBuilder()
		value.javaClass.declaredFields.forEach {
			sb.append(it.name)
			sb.append("=")
			sb.append(when (it.type) {
				String::class.java -> "'${getFieldValueByName(it.name, value).toString().replace("'", "''")}'"
				Date::class.java -> "'${dateFoemat.format(getFieldValueByName(it.name, value) as Date)}'"
				else -> getFieldValueByName(it.name, value).toString()
			})
			sb.append(",")
		}
		if (sb.isNotEmpty())
			sb.delete(sb.length - 1, sb.length)
		update(table, sb.toString(), toWhere(where))
	}
	
	/**
	 * 同上，但是where限定为=
	 */
	override fun <T : Any> update(table: String, value: T, where: Map<String, String>) {
		val whereArray = ArrayList<String>()
		where.forEach {
			when (it.value.javaClass) {
				String::class.java ->
					whereArray.add("${it.key}='${it.value.toString().replace("'", "''")}'")
				else ->
					whereArray.add("${it.key}=${it.value}")
			}
		}
		update(table, value, whereArray.toTypedArray())
	}
	
	override fun delete(table: String, where: String) {
		val statement = connection.createStatement()
		statement.executeUpdate("DELETE FROM `$table` WHERE $where;")
		connection.commit()
		statement.closeOnCompletion()
	}
	
	override fun delete(table: String, where: Map<String, String>) {
		val whereArray = StringBuilder()
		where.forEach {
			when (it.value.javaClass) {
				String::class.java ->
					whereArray.append("${it.key}='${it.value.toString().replace("'", "''")}',")
				else ->
					whereArray.append("${it.key}=${it.value},")
			}
		}
		if (whereArray.isNotEmpty())
			whereArray.delete(whereArray.length - 1, whereArray.length)
		delete(table, whereArray.toString())
	}
	
	fun <T : Any> insertArray(table: String, values: Array<T>) {
		val statement = connection.createStatement()
		
		values.forEach { valueObject ->
			val valueMap = HashMap<String, String>()
			valueObject.javaClass.declaredFields.forEach {
				if (it.type == Date::class.java) {
					valueMap[it.name] = java.sql.Date((getFieldValueByName(it.name, valueObject) as Date).time).toString()
				} else
					valueMap[it.name] = getFieldValueByName(it.name, valueObject).toString()
			}
			
			val column = StringBuilder()
			val value = StringBuilder()
			valueObject.javaClass.declaredFields.forEach {
				column.append("${it.name},")
				value.append(when (it.type) {
					String::class.java -> "'${getFieldValueByName(it.name, valueObject).toString().replace("'", "''")}'"
					Date::class.java -> "'${dateFoemat.format(getFieldValueByName(it.name, valueObject) as Date)}'"
					else -> getFieldValueByName(it.name, valueObject).toString()
				})
				value.append(',')
			}
			if (column.isNotEmpty())
				column.delete(column.length - 1, column.length)
			if (value.isNotEmpty())
				value.delete(value.length - 1, value.length)
			statement.executeUpdate("INSERT INTO $table ($column) VALUES ($value);")
		}
		
		connection.commit()
		statement.closeOnCompletion()
	}
	
	/**
	 * 更新数据库数据
	 * @param table 表名
	 * @param values 用来存储数据的bean对象与限定条件
	 */
	fun <T : Any> updateArray(table: String, values: Array<Pair<T, Array<String>>>) {
		val statement = connection.createStatement()
		values.forEach {
			val sb = StringBuilder()
			val value = it.first
			value.javaClass.declaredFields.forEach {
				sb.append(it.name)
				sb.append("=")
				sb.append(when (it.type) {
					String::class.java -> "'${getFieldValueByName(it.name, value).toString().replace("'", "''")}'"
					Date::class.java -> "'${dateFoemat.format(getFieldValueByName(it.name, value) as Date)}'"
					else -> getFieldValueByName(it.name, value).toString()
				})
				sb.append(",")
			}
			if (sb.isNotEmpty())
				sb.delete(sb.length - 1, sb.length)
			statement.executeUpdate("UPDATE $table SET $sb WHERE ${toWhere(it.second)};")
		}
		connection.commit()
		statement.closeOnCompletion()
	}
	
	private fun toKeys(columns: Map<String, String>): Pair<String, String> {
		val column = StringBuffer()
		val value = StringBuffer()
		columns.forEach {
			if (it.key.isNotEmpty() && it.value.isNotEmpty()) {
				column.append("${it.key},")
				value.append("'${it.value.replace("'", "''")}',")
			}
		}
		column.delete(column.length - 1, column.length)
		value.delete(value.length - 1, value.length)
		return Pair(column.toString(), value.toString())
	}
	
	private fun toColumn(column: Array<String>): String {
		val stringBuffer = StringBuffer()
		column.forEach {
			if (it.isNotEmpty())
				stringBuffer.append("$it,")
		}
		stringBuffer.delete(stringBuffer.length - 1, stringBuffer.length)
		return stringBuffer.toString()
	}
	
	private fun toWhere(where: Map<String, String>): String {
		val stringBuffer = StringBuffer()
		where.forEach {
			if (it.key.isNotEmpty() && it.value.isNotEmpty())
				stringBuffer.append("${it.key}='${it.value.replace("'", "''")}',")
		}
		if (stringBuffer.isNotEmpty())
			stringBuffer.delete(stringBuffer.length - 1, stringBuffer.length)
		return stringBuffer.toString()
	}
	
	private fun toWhere(where: Array<String>): String {
		val stringBuffer = StringBuffer()
		where.forEach {
			if (it.isNotEmpty())
				stringBuffer.append("$it,")
		}
		if (stringBuffer.isNotEmpty())
			stringBuffer.delete(stringBuffer.length - 1, stringBuffer.length)
		return stringBuffer.toString()
	}
	
	override fun close() {
		connection.close()
	}
	
	/**
	 * 根据属性名获取属性值
	 */
	private fun getFieldValueByName(fieldName: String, o: Any): Any? {
		return try {
			val firstLetter = fieldName.substring(0, 1).toUpperCase()
			val getter = "get" + firstLetter + fieldName.substring(1)
			val method = o.javaClass.getMethod(getter)
			method.invoke(o)
		} catch (e: Exception) {
			e.printStackTrace()
			null
		}
		
	}
	
	companion object {
		init {
			Class.forName("com.mysql.cj.jdbc.Driver")
		}
		
		var dateFoemat: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
		
		val typeMap = mapOf(
			Pair(Char::class.java, "TINYINT"),
			Pair(Short::class.java, "SMALLINT"),
			Pair(Int::class.java, "INT"),
			Pair(Float::class.java, "FLOAT"),
			Pair(Double::class.java, "DOUBLE"),
			Pair(Date::class.java, "TIMESTAMP ")
		)
	}
}