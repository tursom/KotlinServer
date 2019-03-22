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
			"jdbc:mysql://$url?characterEncoding=utf-8&serverTimezone=UTC",
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
	override fun createTable(table: String, keys: Iterable<String>) {
		val statement = connection.createStatement()
		statement.executeUpdate("CREATE TABLE if not exists `$table` ( ${keys.fieldStr()} ) ENGINE = InnoDB DEFAULT CHARSET=utf8;")
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
		fields: Iterable<String>?,
		where: Iterable<SQLHelper.Where>,
		order: Field?,
		reverse: Boolean,
		maxCount: Int?
	): SQLAdapter<T> = select(
		adapter = adapter,
		fields = fields?.fieldStr() ?: "*",
		where = where.whereStr(),
		order = order?.fieldName,
		reverse = false,
		maxCount = maxCount
	)
	
	
	override fun <T : Any> select(
		adapter: SQLAdapter<T>,
		fields: String,
		where: String?,
		order: String?,
		reverse: Boolean,
		maxCount: Int?
	): SQLAdapter<T> {
		val sql = "SELECT $fields FROM ${adapter.clazz.tableName
		}${if (where != null) " WHERE $where" else ""
		}${if (order != null) " ORDER BY $order ${if (reverse) "DESC" else "ASC"}" else ""
		}${if (maxCount != null) " limit $maxCount" else ""
		};"
		println(sql)
		val statement = connection.createStatement()
		adapter.adapt(statement.executeQuery(sql))
		statement.closeOnCompletion()
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
	override fun <T : Any> update(value: T, where: Iterable<SQLHelper.Where>) {
		val sb = StringBuilder()
		value.javaClass.declaredFields.forEach {
			it.isAccessible = true
			sb.append("${it.fieldName}=${it.get(value)?.fieldValue ?: return@forEach},")
		}
		if (sb.isNotEmpty())
			sb.delete(sb.length - 1, sb.length)
		update(value.tableName, sb.toString(), where.whereStr())
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
				it.isAccessible = true
				it.get(value)?.let { instance ->
					sb.append(it.getAnnotation(SQLHelper.FieldName::class.java)?.name ?: it.name)
					sb.append("=")
					sb.append(instance.fieldValue)
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
		} finally {
			connection.commit()
			statement.closeOnCompletion()
		}
	}
	
	override fun insert(table: String, fields: String, values: String) {
		val statement = connection.createStatement()
		try {
			statement.executeUpdate("INSERT INTO $table ($fields) VALUES $values;")
			connection.commit()
		} finally {
			statement.closeOnCompletion()
		}
	}
	
	override fun <T : Any> insert(value: T) {
		val clazz = value.javaClass
		val fields = clazz.declaredFields
		val sql = "INSERT INTO ${value.tableName} (${fields.fieldStr()}) VALUES (${
		fields.valueStr(value) ?: return});"
		insert(connection, sql, clazz)
	}
	
	override fun insert(valueList: Iterable<*>) {
		val first = valueList.firstOrNull() ?: return
		val clazz = first.javaClass
		val field = clazz.declaredFields
		val values = valueList.valueStr(field) ?: return
		if (values.isEmpty()) return
		val sql = "INSERT INTO ${first.tableName} (${field.fieldStr()}) VALUES $values;"
		insert(connection, sql, clazz)
	}
	
	override fun delete(table: String, where: String?) {
		val statement = connection.createStatement()
		statement.executeUpdate("DELETE FROM `$table`${if (where != null) " WHERE $where" else ""};")
		connection.commit()
		statement.closeOnCompletion()
	}
	
	override fun delete(table: String, where: Iterable<SQLHelper.Where>) {
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
			val primaryKeySet = ArrayList<String>()
			
			val foreignKey = keys.getAnnotation(SQLHelper.ForeignKey::class.java)?.let {
				if (it.target.isNotEmpty()) it.target else null
			}
			val foreignKeyList = ArrayList<Pair<String, String>>()
			
			fieldSet.forEach {
				val fieldName = it.fieldName
				valueStrBuilder.append("`${fieldName}` ${it.fieldType ?: return@forEach}")
				
				it.annotations.forEach annotations@{ annotation ->
					when (annotation) {
						//检查是否可以为空
						is SQLHelper.NotNull -> valueStrBuilder.append(" NOT NULL")
						is SQLHelper.AutoIncrement -> valueStrBuilder.append(" AUTO_INCREMENT")
						is SQLHelper.PrimaryKey -> primaryKeySet.add(it.fieldName)
						is SQLHelper.Unique -> valueStrBuilder.append(" UNIQUE")
						is SQLHelper.Default -> valueStrBuilder.append(" DEFAULT ${annotation.default}")
						is SQLHelper.Check -> valueStrBuilder.append(" CHECK(${annotation.func})")
						is SQLHelper.ExtraAttribute -> valueStrBuilder.append(" ${annotation.attributes}")
						is SQLHelper.ForeignKey ->
							foreignKeyList.add(fieldName to if (annotation.target.isNotEmpty()) annotation.target else fieldName)
					}
				}
				valueStrBuilder.append(",")
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
			
			foreignKey?.let {
				val (source, target) = foreignKeyList.fieldStr()
				valueStrBuilder.append("FOREIGN KEY ($source) REFERENCES $it ($target),")
			}
			valueStrBuilder.append(")ENGINE=$engine DEFAULT CHARSET=$charset;")
			return valueStrBuilder.toString()
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
	}
}