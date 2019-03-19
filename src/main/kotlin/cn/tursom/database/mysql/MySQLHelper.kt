package cn.tursom.database.mysql

import cn.tursom.database.*
import java.sql.Connection
import java.sql.DriverManager
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/*
 * MySQLHelper，SQLite辅助使用类
 * 实现创建表格、查询、插入和更新功能
 */
@Suppress("unused", "SqlNoDataSourceInspection", "SqlDialectInspection")
class MySQLHelper(val connection: Connection) : SQLHelper {
	init {
		connection.autoCommit = false
	}
	
	constructor(url: String, user: String, password: String)
		: this(DriverManager.getConnection("jdbc:mysql://$url?characterEncoding=utf-8", user, password))
	
	private val basename: String by lazy {
		val resultSet = connection.createStatement().executeQuery("SELECT database()")
		resultSet.next()
		resultSet.getString(1)
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
	
	/**
	 * 根据提供的class对象自动化创建表格
	 */
	@ExperimentalUnsignedTypes
	override fun <T> createTable(table: String, keys: Class<T>) {
		createTable(table, keys, null, "InnoDB", "utf8")
	}
	
	/**
	 * 根据提供的class对象自动化创建表格
	 * 但是有诸多缺陷，所以不是很建议使用
	 */
	@ExperimentalUnsignedTypes
	fun <T> createTable(table: String, keys: Class<T>, primaryKey: String?, engine: String = "InnoDB", charset: String = "utf8") {
		val statement = connection.createStatement()
		statement.executeUpdate(createTableStr(table, keys, engine, charset))
		connection.commit()
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
		deleteTable(table)
	}
	
	/**
	 * 查询
	 * @param adapter 用于保存查询结果的数据类，由SQLAdapter继承而来
	 * @param table 表名
	 * @param column 查询字段
	 * @param where 指定从一个表或多个表中获取数据的条件,Pair左边为字段名，右边为限定的值
	 * @param maxCount 最大查询数量
	 */
	override fun <T : Any> select(
		adapter: SQLAdapter<T>,
		table: String,
		column: Array<String>,
		where: Array<Where>,
		maxCount: Int?
	) {
		val whereStringBuilder = StringBuilder()
		where.forEach {
			whereStringBuilder.append(it.sqlStr)
			whereStringBuilder.append(',')
		}
		whereStringBuilder.deleteCharAt(whereStringBuilder.length - 1)
		
		val columnStringBuilder = StringBuilder()
		column.forEach {
			columnStringBuilder.append("$it,")
		}
		columnStringBuilder.deleteCharAt(whereStringBuilder.length - 1)
		select(adapter, table, columnStringBuilder.toString(), whereStringBuilder.toString(), maxCount)
	}
	
	override fun <T : Any> select(
		adapter: SQLAdapter<T>,
		table: String,
		column: String,
		where: String?,
		maxCount: Int?
	) {
		val statement = connection.createStatement()
		adapter.adapt(
			statement.executeQuery("SELECT $column FROM $table${if (where != null) " WHERE $where" else ""
			}${if (maxCount != null) " limit 0,$maxCount" else ""};")
		)
		statement.closeOnCompletion()
	}
	
	/**
	 * 查询
	 * adapter: 用于保存查询结果的数据类，由SQLAdapter继承而来
	 * table: 表名
	 * name: 查询字段
	 * where: 指定从一个表或多个表中获取数据的条件,Pair左边为字段名，右边为限定的值
	 */
	inline fun <reified T : Any> select(
		table: String,
		name: Array<String> = arrayOf("*"),
		where: Array<Where>
	): SQLAdapter<T> {
		val adapter = SQLAdapter(T::class.java)
		select(adapter, table, name, where)
		return adapter
	}
	
	inline fun <reified T : Any> select(
		table: String, name: String = "*",
		where: String? = null
	): SQLAdapter<T> {
		val adapter = SQLAdapter(T::class.java)
		select(adapter, table, name, where)
		return adapter
	}
	
	override fun update(table: String, set: Map<String, String>, where: Array<Where>) {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
	
	override fun commit() {
		connection.commit()
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
	override fun <T : Any> update(table: String, value: T, where: Array<Where>) {
		val whereArray = ArrayList<String>()
		where.forEach {
			whereArray.add(it.sqlStr)
		}
		update(table, value, whereArray.toTypedArray())
	}
	
	override fun delete(table: String, where: String) {
		val statement = connection.createStatement()
		statement.executeUpdate("DELETE FROM `$table` WHERE $where;")
		connection.commit()
		statement.closeOnCompletion()
	}
	
	override fun delete(table: String, where: Array<Where>) {
		val whereArray = StringBuilder()
		where.forEach {
			whereArray.append("${it.sqlStr},")
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
		
		fun <T> createTableStr(table: String, keys: Class<T>, engine: String = "InnoDB", charset: String = "utf8"): String {
			val fieldSet = keys.declaredFields
			val valueStrBuilder = StringBuilder()
			valueStrBuilder.append("CREATE TABLE IF NOT EXISTS `$table`(")
			val primaryKeySet = HashSet<String>()
			fieldSet.forEach {
				when (it.type) {
					Byte::class.java -> valueStrBuilder.append("`${it.name}` TINYINT")
					Char::class.java -> valueStrBuilder.append("`${it.name}` TINYINT UNSIGNED")
					//UByte::class.java -> valueStrBuilder.append("`${it.name}` TINYINT UNSIGNED")
					Short::class.java -> valueStrBuilder.append("`${it.name}` SMALLINT")
					//UShort::class.java -> valueStrBuilder.append("`${it.name}` SMALLINT UNSIGNED")
					Int::class.java -> valueStrBuilder.append("`${it.name}` INT")
					//UInt::class.java -> valueStrBuilder.append("`${it.name}` INT UNSIGNED")
					Long::class.java -> valueStrBuilder.append("`${it.name}` BIGINT")
					//ULong::class.java -> valueStrBuilder.append("`${it.name}` BIGINT UNSIGNED")
					Float::class.java -> valueStrBuilder.append("`${it.name}` FLOAT")
					Double::class.java -> valueStrBuilder.append("`${it.name}` DOUBLE")
					String::class.java -> valueStrBuilder.append("`${it.name}` VARCHAR")
					else -> {
						if (it.type.interfaces.contains(SqlField::class.java)) {
							valueStrBuilder.append("`${it.name}` ${it.type.getAnnotation(SqlFieldType::class.java)
								?.name ?: it.type.name.split('.').last()}")
						} else {
							return@forEach
						}
					}
				}
				
				//检查是否可以为空
				it.getAnnotation(NotNullField::class.java)?.let {
					valueStrBuilder.append(" NOT NULL")
				}
				it.getAnnotation(AutoIncrement::class.java)?.let {
					valueStrBuilder.append(" AUTO_INCREMENT")
				}
				it.getAnnotation(PrimaryKey::class.java)?.run {
					primaryKeySet.add(it.name)
				}
				it.getAnnotation(Unique::class.java)?.let {
					valueStrBuilder.append(" UNIQUE")
				}
				val annotation = it.getAnnotation(ExtraAttribute::class.java) ?: run {
					valueStrBuilder.append(",")
					return@forEach
				}
				valueStrBuilder.append(" ${annotation.attributes},")
			}
			if (primaryKeySet.isNotEmpty()) {
				valueStrBuilder.append("PRIMARY KEY(")
				primaryKeySet.forEach {
					valueStrBuilder.append("`$it`,")
				}
				valueStrBuilder.deleteCharAt(valueStrBuilder.length - 1)
				valueStrBuilder.append(")")
			} else {
				valueStrBuilder.deleteCharAt(valueStrBuilder.length - 1)
			}
			valueStrBuilder.append(")ENGINE=$engine DEFAULT CHARSET=$charset;")
			return valueStrBuilder.toString()
		}
	}
}