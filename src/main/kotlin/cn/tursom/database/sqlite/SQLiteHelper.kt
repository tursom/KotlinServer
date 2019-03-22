package cn.tursom.database.sqlite

import cn.tursom.database.*
import cn.tursom.tools.simplifyPath
import org.sqlite.SQLiteException
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.lang.reflect.Field


/**
 * MySQLHelper，SQLite辅助使用类
 * 实现创建表格、查询、插入和更新功能
 */

@Suppress("SqlDialectInspection", "SqlNoDataSourceInspection")
class SQLiteHelper
/**
 * 创建名为 base.db 的数据库连接
 */
(base: String) : SQLHelper {
	private val connection: Connection
	private val path = File(base).absolutePath.simplifyPath()
	
	init {
		synchronized(connectionMap) {
			connection = connectionMap[path] ?: {
				val connection = DriverManager.getConnection("jdbc:sqlite:$base") ?: throw CantConnectDataBase()
				connectionMap[path] = connection
				connection.autoCommit = false
				connection
			}()
			connectionCount[path] = connectionCount[path] ?: 0 + 1
		}
	}
	
	override fun equals(other: Any?): Boolean =
		if (other is SQLiteHelper) {
			connection == other.connection
		} else {
			false
		}
	
	/**
	 * 创建表格
	 * @param table: 表格名
	 * @param keys: 属性列表
	 */
	override fun createTable(table: String, keys: Iterable<String>) {
		val sql = "CREATE TABLE if not exists $table (${keys.fieldStr()})"
		val statement = connection.createStatement()
		statement.executeUpdate(sql)
		commit()
	}
	
	/**
	 * 根据提供的class对象自动化创建表格
	 * 但是有诸多缺陷，所以不是很建议使用
	 */
	override fun createTable(fields: Class<*>) {
		val sql = createTableStr(fields)
		val statement = connection.createStatement()
		statement.executeUpdate(sql)
		commit()
	}
	
	/**
	 * 删除表格
	 */
	override fun deleteTable(table: String) {
		val statement = connection.createStatement()
		statement.executeUpdate("DROP TABLE if exists $table")
		commit()
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
	): SQLAdapter<T> =
		select(adapter, fields?.fieldStr()?:"*", where.whereStr(), order?.fieldName, reverse, maxCount)
	
	
	override fun <T : Any> select(
		adapter: SQLAdapter<T>, fields: String, where: String?, order: String?, reverse: Boolean, maxCount: Int?
	): SQLAdapter<T> {
		val sql = "SELECT $fields FROM ${adapter.clazz.tableName
		}${if (where != null) " WHERE $where" else ""
		}${if (order != null) " ORDER BY $order ${if (reverse) "DESC" else "ASC"}" else ""
		}${if (maxCount != null) " limit 0,$maxCount" else ""
		};"
		
		val statement = connection.createStatement()
		try {
			adapter.adapt(
				statement.executeQuery(sql)
			)
		} catch (e: SQLiteException) {
			if (e.message != "[SQLITE_ERROR] SQL error or missing database (no such table: ${adapter.clazz.tableName})") throw e
		}
		statement.closeOnCompletion()
		return adapter
	}
	
	private fun insert(connection: Connection, sql: String, table: Class<*>) {
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
		} finally {
			connection.commit()
			statement.closeOnCompletion()
		}
	}
	
	override fun <T : Any> insert(value: T) {
		val clazz = value.javaClass
		val fields = clazz.declaredFields
		val column = fields.fieldStr()
		val valueStr = fields.valueStr(value) ?: return
		val sql = "INSERT INTO ${value.tableName} ($column) VALUES ($valueStr);"
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
	
	override fun insert(table: String, fields: String, values: String) {
		val sql = "INSERT INTO $table ($fields) VALUES $values;"
		val statement = connection.createStatement()
		try {
			statement.executeUpdate(sql)
			commit()
		} finally {
			statement.closeOnCompletion()
		}
	}
	
	override fun <T : Any> update(
		value: T, where: Iterable<SQLHelper.Where>
	) {
		val set = StringBuilder()
		value.javaClass.declaredFields.forEach {
			it.isAccessible = true
			it.get(value)?.let { value ->
				set.append("${it.fieldName}=${value.fieldValue},")
			}
		}
		if (set.isNotEmpty()) {
			set.delete(set.length - 1, set.length)
		}
		
		val sql = "UPDATE ${value.tableName} SET $set WHERE ${where.whereStr()};"
		
		val statement = connection.createStatement()
		statement.executeUpdate(sql)
		commit()
		statement.closeOnCompletion()
	}
	
	override fun delete(table: String, where: String?) {
		val statement = connection.createStatement()
		statement.executeUpdate("DELETE FROM $table${if (where?.isNotEmpty() == true) " WHERE $where" else ""};")
		commit()
		statement.closeOnCompletion()
	}
	
	override fun delete(table: String, where: Iterable<SQLHelper.Where>) {
		delete(table, where.whereStr())
	}
	
	override fun commit() {
		synchronized(connection) {
			connection.commit()
		}
	}
	
	override fun close() {
		synchronized(connectionMap) {
			connectionCount[path] = connectionCount[path] ?: 1 - 1
			if (connectionCount[path] == 0) {
				connectionCount.remove(path)
				connectionMap.remove(path)
				connection.close()
			}
		}
	}
	
	override fun hashCode(): Int {
		var result = connection.hashCode()
		result = 31 * result + path.hashCode()
		return result
	}
	
	class CantConnectDataBase(s: String? = null) : SQLException(s)
	
	companion object {
		private val connectionMap by lazy {
			Class.forName("org.sqlite.JDBC")
			HashMap<String, Connection>()
		}
		private var connectionCount = HashMap<String, Int>()
		
		@Suppress("NestedLambdaShadowedImplicitParameter")
		fun <T> createTableStr(keys: Class<T>): String {
			val foreignKey = keys.getAnnotation(SQLHelper.ForeignKey::class.java)?.let {
				if (it.target.isNotEmpty()) it.target else null
			}
			val foreignKeyList = ArrayList<Pair<String, String>>()
			
			val valueStrBuilder = StringBuilder()
			valueStrBuilder.append("CREATE TABLE IF NOT EXISTS ${keys.tableName}(")
			keys.declaredFields.forEach {
				val fieldName = it.fieldName
				valueStrBuilder.append("$fieldName ${
				when (it.type) {
					java.lang.Byte::class.java -> "INTEGER"
					java.lang.Short::class.java -> "INTEGER"
					java.lang.Integer::class.java -> "INTEGER"
					java.lang.Long::class.java -> "INTEGER"
					java.lang.Float::class.java -> "REAL"
					java.lang.Double::class.java -> "REAL"
					java.lang.String::class.java -> it.getAnnotation<SQLHelper.TextLength>()?.let { "CHAR(${it.length})" }
						?: "TEXT"
					else -> {
						if (it.type.isSqlField) {
							it.type.getAnnotation<SQLHelper.FieldType>()?.name ?: it.type.name.split('.').last()
						} else {
							return@forEach
						}
					}
				}
				}")
				
				it.annotations.forEach {
					when (it) {
						//检查是否可以为空
						is SQLHelper.NotNull -> valueStrBuilder.append(" NOT NULL")
						is SQLHelper.AutoIncrement -> valueStrBuilder.append(" AUTO_INCREMENT")
						is SQLHelper.PrimaryKey -> valueStrBuilder.append(" PRIMARY KEY")
						is SQLHelper.Unique -> valueStrBuilder.append(" UNIQUE")
						is SQLHelper.Default -> valueStrBuilder.append(" DEFAULT ${it.default}")
						is SQLHelper.Check -> valueStrBuilder.append(" CHECK(${it.func})")
						is SQLHelper.ForeignKey ->
							foreignKeyList.add(fieldName to if (it.target.isNotEmpty()) it.target else fieldName)
					}
				}
				
				val annotation = it.getAnnotation(SQLHelper.ExtraAttribute::class.java) ?: run {
					valueStrBuilder.append(",")
					return@forEach
				}
				valueStrBuilder.append(" ${annotation.attributes},")
			}
			
			foreignKey?.let {
				val (source, target) = foreignKeyList.fieldStr()
				valueStrBuilder.append("FOREIGN KEY ($source) REFERENCES $it ($target),")
			}
			
			valueStrBuilder.deleteCharAt(valueStrBuilder.length - 1)
			valueStrBuilder.append(");")
			return valueStrBuilder.toString()
		}
	}
}
