package cn.tursom.database.sqlite

import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement

/*
 * 继承
 */
class BaseWriteTest(val base: String, val id: Int, val table: String) : Thread() {
	val c = DriverManager.getConnection("jdbc:sqlite:$base")
	val stmt = c.createStatement()
	override fun run() {
		synchronized(commit) {
			var sql = "create table if not exists test (name)"
			stmt.executeUpdate(sql)
			for (n in 1..10) {
				sql = "insert into $table (name) values(\"$id:$n\");"
				stmt.executeUpdate(sql)
			}
			stmt.close()
			c.close()
		}
	}
	
	@Synchronized
	fun close() {
		stmt.close()
		c.close()
	}
	
	companion object {
		var commit = 0
	}
}

fun main(args: Array<String>) {
	//Class.forName("org.sqlite.JDBC")
	val tableName = "test"
	val c: Connection?
	try {
		c = DriverManager.getConnection("jdbc:sqlite:test.db")
		try {
			val statement = c.createStatement()
			statement.executeUpdate("create table if not EXISTS test (name)")
			statement.close()
			for (n in 1..10) {
				BaseWriteTest("test.db", n, tableName).start()
			}
			c.close()
		} catch (e: Exception) {
			System.err.println(e.javaClass.name + ": " + e.message)
			System.exit(0)
		}
		
	} catch (e: Exception) {
		System.err.println(e.javaClass.name + ": " + e.message)
		System.exit(0)
	}
}

fun test() {
	var c: Connection?
	var stmt: Statement?
	try {
		//Class.forName("org.sqlite.JDBC")
		c = DriverManager.getConnection("jdbc:sqlite:test.db")
		println("Opened database successfully")
		
		stmt = c!!.createStatement()
		val sql = "CREATE TABLE if not exists COMPANY " +
				"(ID INT PRIMARY KEY     NOT NULL," +
				" NAME           TEXT    NOT NULL, " +
				" AGE            INT     NOT NULL, " +
				" ADDRESS        CHAR(50), " +
				" SALARY         REAL)"
		stmt!!.executeUpdate(sql)
		println("Table created successfully")
	} catch (e: Exception) {
		System.err.println(e.javaClass.name + ": " + e.message)
		System.exit(0)
	}
	
	try {
		Class.forName("org.sqlite.JDBC")
		c = DriverManager.getConnection("jdbc:sqlite:test.db")
		c.autoCommit = false
		println("Opened database successfully")
		
		stmt = c.createStatement()
		val rs = stmt.executeQuery("SELECT * FROM COMPANY;")
		while (rs.next()) {
			println("ID = ${rs.getInt("id")}")
			println("NAME = ${rs.getString("name")}")
			println("AGE = ${rs.getInt("age")}")
			println("ADDRESS = ${rs.getString("address")}")
			println("SALARY = ${rs.getFloat("salary")}")
			println()
		}
		rs.close()
		stmt.close()
		c.close()
	} catch (e: Exception) {
		System.err.println(e.javaClass.name + ": " + e.message)
		System.exit(0)
	}
	
	println("Operation done successfully")
}