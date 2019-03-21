package cn.tursom.database.mysql

import cn.tursom.database.*
import java.lang.reflect.Field
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLSyntaxErrorException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * MySQLHelper，SQLite辅助使用类
 * 实现创建表格、查询、插入和更新功能
 */
@Suppress("SqlNoDataSourceInspection", "SqlDialectInspection")
class MySQLHelper(
	@Suppress("MemberVisibilityCanBePrivate") val connection: Connection,
	base: String? = null
) : SQLHelper {
	
	@Suppress("MemberVisibilityCanBePrivate")
	var basename: String? = null
		get() = synchronized(this) {
			return field
		}
		set(value) = synchronized(this) {
			value?.let { base ->
				val statement = connection.createStatement()
				statement.executeQuery("USE $base")
				field = base
				statement.closeOnCompletion()
			}
		}
	
	init {
		connection.autoCommit = false
		basename = base
	}
	
	constructor(url: String, user: String, password: String, base: String? = null)
		: this(
		DriverManager.getConnection(
			"jdbc:mysql://$url?characterEncoding=utf-8",
			user,
			password
		),
		base
	)
	
	/*
	 * 创建表格
	 * table: 表格名
	 * keys: 属性列表
	 */
	override fun createTable(table: String, keys: List<String>) {
		val statement = connection.createStatement()
		statement.executeUpdate("CREATE TABLE if not exists `$table` ( ${toColumn(keys)} ) ENGINE = InnoDB DEFAULT CHARSET=utf8;")
		connection.commit()
	}
	
	/**
	 * 根据提供的class对象自动化创建表格
	 */
	override fun createTable(fields: Class<*>) {
		createTable(fields.tableName, fields, "InnoDB", "utf8")
	}
	
	/**
	 * 根据提供的class对象自动化创建表格
	 */
	@Suppress("MemberVisibilityCanBePrivate")
	fun createTable(table: String, keys: Class<*>, engine: String = "InnoDB", charset: String = "utf8") {
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
	 * @param fields 查询字段
	 * @param where 指定从一个表或多个表中获取数据的条件,Pair左边为字段名，右边为限定的值
	 * @param maxCount 最大查询数量
	 */
	override fun <T : Any> select(
		adapter: SQLAdapter<T>,
		fields: List<String>,
		where: List<SQLHelper.Where>,
		maxCount: Int?
	): SQLAdapter<T> = select(
		adapter = adapter,
		fields = if (fields.isEmpty()) "*" else {
			val columnStringBuilder = StringBuilder()
			fields.forEach {
				columnStringBuilder.append("$it,")
			}
			columnStringBuilder.deleteCharAt(columnStringBuilder.length - 1)
			columnStringBuilder.toString()
		},
		where = where.toWhere(),
		maxCount = maxCount
	)
	
	
	override fun <T : Any> select(
		adapter: SQLAdapter<T>,
		fields: String,
		where: String?,
		maxCount: Int?
	): SQLAdapter<T> {
		val statement = connection.createStatement()
		adapter.adapt(
			statement.executeQuery("SELECT $fields FROM ${adapter.clazz.tableName}${if (where != null) " WHERE $where" else ""
			}${if (maxCount != null) " limit 0,$maxCount" else ""};")
		)
		statement.closeOnCompletion()
		return adapter
	}
	
	/**
	 * 查询
	 * adapter: 用于保存查询结果的数据类，由SQLAdapter继承而来
	 * table: 表名
	 * name: 查询字段
	 * where: 指定从一个表或多个表中获取数据的条件,Pair左边为字段名，右边为限定的值
	 */
	inline fun <reified T : Any> select(
		name: List<String> = listOf("*"),
		where: List<SQLHelper.Where>
	): SQLAdapter<T> {
		val adapter = SQLAdapter(T::class.java)
		select(adapter, name, where)
		return adapter
	}
	
	inline fun <reified T : Any> select(
		name: String = "*",
		where: String? = null
	): SQLAdapter<T> {
		val adapter = SQLAdapter(T::class.java)
		select(adapter, name, where)
		return adapter
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
	 * @param value 用来存储数据的bean对象
	 * @param where SQL语句的一部分，用来限定查找的条件。每一条String储存一个条件
	 */
	override fun <T : Any> update(value: T, where: List<SQLHelper.Where>) {
		val sb = StringBuilder()
		value.javaClass.declaredFields.forEach {
			sb.append("${it.fieldName}=${getFieldValueByName(it.name, value)?.fieldValue ?: return@forEach},")
		}
		if (sb.isNotEmpty())
			sb.delete(sb.length - 1, sb.length)
		update(value.tableName, sb.toString(), where.toWhere()!!)
	}
	
	/**
	 * 更新数据库数据
	 * @param values 用来存储数据的bean对象与限定条件
	 */
	@Suppress("NestedLambdaShadowedImplicitParameter")
	fun <T : Any> updateArray(values: List<Pair<T, List<SQLHelper.Where>>>) {
		val table = values.first().first.tableName
		val statement = connection.createStatement()
		values.forEach {
			val sb = StringBuilder()
			val value = it.first
			value.javaClass.declaredFields.forEach {
				getFieldValueByName(it.name, value)?.let { instance ->
					sb.append(it.getAnnotation(SQLHelper.FieldName::class.java)?.name ?: it.name)
					sb.append("=")
					sb.append(when (instance) {
						is SQLHelper.SqlField<*> -> instance.sqlValue
						is String -> "'${instance.replace("'", "''")}'"
						else -> instance.toString()
					})
					sb.append(",")
				}
			}
			if (sb.isNotEmpty())
				sb.delete(sb.length - 1, sb.length)
			statement.executeUpdate("UPDATE $table SET $sb WHERE ${it.second.toWhere()};")
		}
		connection.commit()
		statement.closeOnCompletion()
	}
	
	private fun insert(connection: Connection, sql: String, table: Class<*>) {
		val statement = connection.createStatement()
		try {
			statement.executeUpdate(sql)
		} catch (e: SQLSyntaxErrorException) {
			if (e.message == "Table '$basename.${table.tableName}' doesn't exist") {
				createTable(table)
				statement.executeUpdate(sql)
			} else {
				e.printStackTrace()
			}
		}
		connection.commit()
		statement.closeOnCompletion()
	}
	
	override fun insert(table: String, fields: String, values: String) {
		val statement = connection.createStatement()
		statement.executeUpdate("INSERT INTO $table ($fields) VALUES $values;")
		connection.commit()
		statement.closeOnCompletion()
	}
	
	override fun <T : Any> insert(value: T) {
		val clazz = value.javaClass
		val fields = clazz.declaredFields
		val sql = "INSERT INTO ${value.tableName} (${fields.fieldStr()}) VALUES (${
		fields.sqlFieldMap().valueStr(value) ?: return});"
		insert(connection, sql, clazz)
	}
	
	override fun insert(valueList: List<*>) {
		val first = valueList.firstOrNull() ?: return
		val clazz = first.javaClass
		val field = clazz.declaredFields
		val values = valueList.valueStr(field.sqlFieldMap()) ?: return
		if (values.isEmpty()) return
		val sql = "INSERT INTO ${first.tableName} (${field.fieldStr()}) VALUES $values;"
		insert(connection, sql, clazz)
	}
	
	override fun delete(table: String, where: String?) {
		val statement = connection.createStatement()
		statement.executeUpdate("DELETE FROM `$table` WHERE $where;")
		connection.commit()
		statement.closeOnCompletion()
	}
	
	override fun delete(table: String, where: List<SQLHelper.Where>) {
		val whereArray = StringBuilder()
		where.forEach {
			whereArray.append("${it.sqlStr},")
		}
		if (whereArray.isNotEmpty())
			whereArray.delete(whereArray.length - 1, whereArray.length)
		delete(table, whereArray.toString())
	}
	
	override fun close() {
		connection.close()
	}
	
	override fun commit() {
		connection.commit()
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
	
	private fun List<SQLHelper.Where>.toWhere(): String? = if (isEmpty()) null else {
		val whereStringBuilder = StringBuilder()
		val iterator = iterator()
		whereStringBuilder.append(iterator.next().sqlStr)
		for (it in iterator) {
			whereStringBuilder.append(" and ")
			whereStringBuilder.append(it.sqlStr)
		}
		whereStringBuilder.toString()
	}
	
	companion object {
		init {
			Class.forName("com.mysql.cj.jdbc.Driver")
		}
		
		var dateFoemat: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
		
		fun <T> createTableStr(keys: Class<T>, engine: String = "InnoDB", charset: String = "utf8"): String =
			createTableStr(keys.tableName, keys, engine, charset)
		
		fun <T> createTableStr(table: String, keys: Class<T>, engine: String = "InnoDB", charset: String = "utf8"): String {
			val fieldSet = keys.declaredFields
			val valueStrBuilder = StringBuilder()
			valueStrBuilder.append("CREATE TABLE IF NOT EXISTS `$table`(")
			val primaryKeySet = HashSet<String>()
			fieldSet.forEach {
				valueStrBuilder.append("`${it.fieldName}` ${it.fieldType ?: return@forEach}")
				
				//检查是否可以为空
				it.getAnnotation(SQLHelper.NotNull::class.java)?.let {
					valueStrBuilder.append(" NOT NULL")
				}
				it.getAnnotation(SQLHelper.AutoIncrement::class.java)?.let {
					valueStrBuilder.append(" AUTO_INCREMENT")
				}
				it.getAnnotation(SQLHelper.PrimaryKey::class.java)?.run {
					primaryKeySet.add(it.fieldName)
				}
				it.getAnnotation(SQLHelper.Unique::class.java)?.let {
					valueStrBuilder.append(" UNIQUE")
				}
				val annotation = it.getAnnotation<SQLHelper.ExtraAttribute>() ?: run {
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
		
		private fun toColumn(column: List<String>): String {
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
		
		private val Field.fieldType: String?
			get() = getAnnotation(SQLHelper.FieldType::class.java)?.name ?: when (type) {
				java.lang.Byte::class.java -> "TINYINT"
				java.lang.Character::class.java -> "TINYINT"
				java.lang.Short::class.java -> "SMALLINT"
				java.lang.Integer::class.java -> "INT"
				java.lang.Long::class.java -> "BIGINT"
				java.lang.Float::class.java -> "FLOAT"
				java.lang.Double::class.java -> "DOUBLE"
				
				Byte::class.java -> "TINYINT"
				Char::class.java -> "TINYINT"
				Short::class.java -> "SMALLINT"
				Int::class.java -> "INT"
				Long::class.java -> "BIGINT"
				Float::class.java -> "FLOAT"
				Double::class.java -> "Double"
				
				java.lang.String::class.java -> getAnnotation(SQLHelper.TextLength::class.java)?.let { "CHAR(${it.length})" }
					?: "TEXT"
				else -> if (type.isSqlField) {
					type.getAnnotation(SQLHelper.FieldType::class.java)?.name ?: type.name.split('.').last()
				} else {
					null
				}
			}
	}
}