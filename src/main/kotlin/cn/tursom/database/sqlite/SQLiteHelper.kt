package cn.tursom.database.sqlite

import cn.tursom.database.*
import org.sqlite.SQLiteException
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import cn.tursom.tools.simplifyPath
import java.lang.reflect.Field

@MustBeDocumented
@Target(AnnotationTarget.FIELD)
annotation class Default(val default: String)

@MustBeDocumented
@Target(AnnotationTarget.FIELD)
annotation class Check(val func: String)

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
		println(path)
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
	override fun createTable(table: String, keys: List<String>) {
		val sql = "CREATE TABLE if not exists $table (${toColumn(keys)})"
		println(sql)
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
		println(sql)
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
	 * @param column 查询字段
	 * @param where 指定从一个表或多个表中获取数据的条件,Pair左边为字段名，右边为限定的值
	 * @param maxCount 最大查询数量
	 */
	override fun <T : Any> select(
		adapter: SQLAdapter<T>,
		column: List<String>,
		where: List<SQLHelper.Where>,
		maxCount: Int?
	): SQLAdapter<T> =
		select(adapter, toColumn(column), toWhere(where), maxCount)
	
	
	override fun <T : Any> select(
		adapter: SQLAdapter<T>, column: String, where: String?, maxCount: Int?
	): SQLAdapter<T> {
		val statement = connection.createStatement()
		try {
			adapter.adapt(
				statement.executeQuery("SELECT $column FROM ${adapter.clazz.tableName} ${if (where != null) "WHERE $where" else ""
				} ${if (maxCount != null) "limit 0,$maxCount" else ""} ;")
			)
		} catch (e: SQLiteException) {
			if (e.message != "[SQLITE_ERROR] SQL error or missing database (no such table: ${adapter.clazz.tableName})") throw e
		}
		statement.closeOnCompletion()
		return adapter
	}
	
	fun <T : Any> reverseSelect(
		adapter: SQLAdapter<T>,
		table: String,
		name: List<String>,
		where: List<SQLHelper.Where>?,
		index: String,
		maxCount: Int?
	): SQLAdapter<T> =
		if (where != null) {
			reverseSelect(adapter, table, toColumn(name), toWhere(where), index, maxCount)
		} else {
			reverseSelect(adapter, table, toColumn(name), null, index, maxCount)
		}
	
	fun <T : Any> reverseSelect(
		adapter: SQLAdapter<T>,
		table: String,
		name: String,
		where: String?,
		index: String,
		maxCount: Int?
	): SQLAdapter<T> {
		val statement = connection.createStatement()
		try {
			@Suppress("SqlResolve", "SqlIdentifier")
			adapter.adapt(
				statement.executeQuery("SELECT $name FROM $table ${if (where == null) "WHERE $where" else ""
				} ORDER BY $index DESC limit 0,${maxCount ?: Int.MAX_VALUE};")
			)
		} catch (e: SQLiteException) {
			if (e.message != "[SQLITE_ERROR] SQL error or missing database (no such table: $table)") throw e
		}
		statement.closeOnCompletion()
		return adapter
	}
	
	override fun <T : Any> insert(value: T) {
		val table = value.tableName
		
		val (column, values) = value.javaClass.declaredFields.columnAndValue(value)
		
		try {
			insert(table, column, values)
		} catch (e: SQLiteException) {
			if (e.message == "[SQLITE_ERROR] SQL error or missing database (no such table: $table)") {
				createTable(value.javaClass)
				insert(table, column, values)
			} else {
				e.printStackTrace()
			}
		}
	}
	
	override fun insert(valueList: List<*>) {
		val statement = connection.createStatement()
		val first = valueList.firstOrNull() ?: return
		val table = first.tableName
		val field = first.javaClass.declaredFields
		valueList.forEach { value ->
			value ?: return@forEach
			val (column, values) = field.columnAndValue(value)
			val sql = "INSERT INTO $table ($column) VALUES ($values)"
			try {
				statement.executeUpdate(sql)
			} catch (e: SQLiteException) {
				if (e.message == "[SQLITE_ERROR] SQL error or missing database (no such table: $table)") {
					createTable(value.javaClass)
					statement.executeUpdate(sql)
				} else {
					e.printStackTrace()
				}
			}
		}
		commit()
		statement.closeOnCompletion()
	}
	
	override fun insert(table: String, column: String, values: String) {
		val statement = connection.createStatement()
		val sql = "INSERT INTO $table ($column) VALUES ($values)"
		statement.executeUpdate(sql)
		commit()
		statement.closeOnCompletion()
	}
	
	override fun <T : Any> update(
		value: T, where: List<SQLHelper.Where>
	) {
		val set = StringBuilder()
		value.javaClass.declaredFields.forEach {
			it.isAccessible = true
			it.get(value)?.let { value ->
				set.append("${it.name}=${value.fieldValue},")
			}
		}
		if (set.isNotEmpty()) {
			set.delete(set.length - 1, set.length)
		}
		
		val sql = "UPDATE ${value.tableName} SET $set WHERE ${toWhere(where)};"
		
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
	
	override fun delete(table: String, where: List<SQLHelper.Where>) {
		delete(table, toWhere(where))
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
	
	private fun toColumn(column: List<String>): String {
		val stringBuilder = StringBuilder()
		column.forEach {
			if (it.isNotEmpty())
				stringBuilder.append("$it,")
		}
		stringBuilder.delete(stringBuilder.length - 1, stringBuilder.length)
		return stringBuilder.toString()
	}
	
	private fun toWhere(where: List<SQLHelper.Where>): String {
		val stringBuilder = StringBuilder()
		where.forEach {
			stringBuilder.append("${it.sqlStr} AND ")
		}
		if (stringBuilder.isNotEmpty())
			stringBuilder.delete(stringBuilder.length - 5, stringBuilder.length)
		return stringBuilder.toString()
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
		
		private fun Array<Field>.columnAndValue(value: Any): Pair<String, String> {
			val column = StringBuilder()
			val values = StringBuilder()
			forEach field@{ field ->
				field.isAccessible = true
				val fieldValue = field.get(value) ?: return@field
				values.append(if (field.type.interfaces.contains(SQLHelper.SqlField::class.java)) {
					(fieldValue as SQLHelper.SqlField<*>).sqlValue
				} else {
					fieldValue.toString()
				}.replace("'", "''"))
				values.append(',')
				column.append("${field.name},")
			}
			column.deleteCharAt(column.length - 1)
			values.deleteCharAt(values.length - 1)
			return column.toString() to values.toString()
		}
		
		@Suppress("NestedLambdaShadowedImplicitParameter")
		fun <T> createTableStr(keys: Class<T>): String {
			val fieldSet = keys.declaredFields
			val valueStrBuilder = StringBuilder()
			valueStrBuilder.append("CREATE TABLE IF NOT EXISTS ${keys.tableName}(")
			fieldSet.forEach {
				valueStrBuilder.append("${it.name} ${
				when (it.type) {
					Byte::class.java -> "INTEGER"
					Char::class.java -> "INTEGER"
					Short::class.java -> "INTEGER"
					Int::class.java -> "INTEGER"
					Long::class.java -> "INTEGER"
					Float::class.java -> "REAL"
					Double::class.java -> "REAL"
					String::class.java -> it.getAnnotation<SQLHelper.TextLength>()?.let { "CHAR(${it.length})" } ?: "TEXT"
					else -> {
						if (it.type.interfaces.contains(SQLHelper.SqlField::class.java)) {
							it.type.getAnnotation<SQLHelper.FieldType>()?.name ?: it.type.name.split('.').last()
						} else {
							return@forEach
						}
					}
				}
				}")
				
				//检查是否可以为空
				it.getAnnotation(SQLHelper.NotNull::class.java)?.let {
					valueStrBuilder.append(" NOT NULL")
				}
				it.getAnnotation(SQLHelper.AutoIncrement::class.java)?.let {
					valueStrBuilder.append(" AUTO_INCREMENT")
				}
				it.getAnnotation(SQLHelper.PrimaryKey::class.java)?.run {
					valueStrBuilder.append(" PRIMARY KEY")
				}
				it.getAnnotation(SQLHelper.Unique::class.java)?.let {
					valueStrBuilder.append(" UNIQUE")
				}
				it.getAnnotation(Default::class.java)?.let {
					valueStrBuilder.append(" DEFAULT ${it.default}")
				}
				it.getAnnotation(Check::class.java)?.let {
					valueStrBuilder.append(" CHECK(${it.func})")
				}
				
				val annotation = it.getAnnotation(SQLHelper.ExtraAttribute::class.java) ?: run {
					valueStrBuilder.append(",")
					return@forEach
				}
				valueStrBuilder.append(" ${annotation.attributes},")
			}
			valueStrBuilder.deleteCharAt(valueStrBuilder.length - 1)
			valueStrBuilder.append(");")
			return valueStrBuilder.toString()
		}
	}
}
