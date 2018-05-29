package cn.tursom.database.mysql

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

/*
 * SQLHelper，SQLite辅助使用类
 * 实现创建表格、查询、插入和更新功能
 */
class SQLHelper(private val connection: Connection) {
	init {
		Class.forName("com.mysql.jdbc.Driver")
		connection.autoCommit = false
	}

	private val basename: String
		get() {
			val resultSet = connection.createStatement().executeQuery("SELECT database()")
			resultSet.next()
			return resultSet.getString(1)
		}

	constructor(base: String, user: String, pass: String)
			: this(DriverManager.getConnection("jdbc:mysql:$base", user, pass) ?: throw CantConnectDataBase())


	/*
	 * 创建表格
	 * table: 表格名
	 * keys: 属性列表
	 */
	fun createTable(table: String, keys: Array<String>) {
		val statement = connection.createStatement()
		statement.executeUpdate("CREATE TABLE if not exists `$table` ( ${toColumn(keys)} ) ENGINE = InnoDB DEFAULT CHARSET=utf8;")
		connection.commit()
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
			where: Array<Pair<String, String>>): SQLAdapter<T> {
		val adapter = SQLAdapter(T::class.java)
		select(adapter, table, toColumn(name), toWhere(where))
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

	/*
	 * 插入
	 * table: 表名
	 * column:
	 */
	fun insert(table: String, column: Array<Pair<String, String>>) {
		val columns = toKeys(column)
		insert(table, columns.first, columns.second)
	}

	fun insert(table: String, column: String, values: String) {
		val statement = connection.createStatement()
		statement.executeUpdate("INSERT INTO $table ($column) VALUES ($values)")
		connection.commit()
		statement.closeOnCompletion()
	}

	fun update(
			table: String,
			set: Array<Pair<String, String>>,
			where: Array<Pair<String, String>>) {
		val statement = connection.createStatement()
		statement.executeUpdate("UPDATE $table SET ${toWhere(set)} WHERE ${toWhere(where)};")
		connection.commit()
		statement.closeOnCompletion()
	}

	private fun toKeys(columns: Array<Pair<String, String>>): Pair<String, String> {
		val column = StringBuffer()
		val value = StringBuffer()
		columns.forEach {
			if (it.first.isNotEmpty() && it.second.isNotEmpty()) {
				column.append("${it.first},")
				value.append("\"${it.second}\",")
			}
		}
		column.delete(column.length - 1, column.length)
		value.delete(value.length - 1, value.length)
		return Pair(column.toString(), value.toString())
	}

	fun toColumn(column: Array<String>): String {
		val stringBuffer = StringBuffer()
		column.forEach {
			if (it.isNotEmpty())
				stringBuffer.append("$it,")
		}
		stringBuffer.delete(stringBuffer.length - 1, stringBuffer.length)
		return stringBuffer.toString()
	}

	fun toWhere(where: Array<Pair<String, String>>): String {
		val stringBuffer = StringBuffer()
		where.forEach {
			if (it.first.isNotEmpty() && it.second.isNotEmpty())
				stringBuffer.append("${it.first}=\"${it.second}\" AND ")
		}
		if (stringBuffer.isNotEmpty())
			stringBuffer.delete(stringBuffer.length - 5, stringBuffer.length)
		return stringBuffer.toString()
	}

	fun close() {
		connection.close()
	}

	class CantConnectDataBase(s: String? = null) : SQLException(s)
}