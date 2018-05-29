package cn.tursom.database.sqlite

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement


class SpecialSQLHelper {
	
	private var c: Connection? = null
	private var stmt: Statement? = null
	
	init {
		c = null
		stmt = null
	}
	
	fun connectDb(name: String) {
		c = null
		try {
			Class.forName("org.sqlite.JDBC")
			c = DriverManager.getConnection("jdbc:sqlite:$name.db")
			c!!.close()
		} catch (e: Exception) {
			System.err.println(e.javaClass.name + ": " + e.message)
			System.exit(0)
		}
		
		println("Created database successfully")
	}
	
	fun createTable(nameDb: String, tableName: String, values: String) {
		c = null
		stmt = null
		try {
			Class.forName("org.sqlite.JDBC")
			c = DriverManager.getConnection("jdbc:sqlite:$nameDb.db")
			stmt = c!!.createStatement()
			
			val sql = "CREATE TABLE " + tableName.toUpperCase() + " " + values + ");"
			
			stmt!!.executeUpdate(sql)
			stmt!!.close()
			
			println("$tableName added")
			c!!.close()
		} catch (e: Exception) {
			System.err.println(e.javaClass.name + ": " + e.message)
			//System.exit(0);
		}
		
	}
	
	fun insert(nameDb: String, tableName: String, values: String) {
		c = null
		stmt = null
		try {
			Class.forName("org.sqlite.JDBC")
			c = DriverManager.getConnection("jdbc:sqlite:$nameDb.db")
			c!!.setAutoCommit(false)
			println("Opened database successfully")
			
			stmt = c!!.createStatement()
			
			val sql = "INSERT INTO " + tableName.toUpperCase() + " VALUES(" +
					values + ");"
			stmt!!.executeUpdate(sql)
			
			stmt!!.close()
			c!!.commit()
			c!!.close()
		} catch (e: Exception) {
			System.err.println(e.javaClass.name + ": " + e.message)
			System.exit(0)
		}
		
		println("Records created successfully")
	}
	
	
	fun select(nameDb: String, tableName: String, selection: String): ResultSet? {
		c = null
		stmt = null
		var temp: ResultSet? = null
		try {
			Class.forName("org.sqlite.JDBC")
			c = DriverManager.getConnection("jdbc:sqlite:$nameDb.db")
			c!!.setAutoCommit(false)
			println("Opened database successfully")
			
			stmt = c!!.createStatement()
			temp = stmt!!.executeQuery("SELECT $selection FROM $tableName;")
			stmt!!.close()
			c!!.close()
		} catch (e: Exception) {
			System.err.println(e.javaClass.name + ": " + e.message)
			System.exit(0)
		}
		
		println("Select completed successfully")
		return temp
	}
	
	fun update(dbName: String, tableName: String, id: String, colName: String, value: String) {
		c = null
		stmt = null
		try {
			Class.forName("org.sqlite.JDBC")
			c = DriverManager.getConnection("jdbc:sqlite:$dbName.db")
			c!!.setAutoCommit(false)
			println("Opened database successfully")
			
			stmt = c!!.createStatement()
			
			val sql = "UPDATE " + tableName.toUpperCase() + " set " +
					colName + " = " + value + "where ID=" + id + ";"
			stmt!!.executeUpdate(sql)
			
			stmt!!.close()
			c!!.commit()
			c!!.close()
		} catch (e: Exception) {
			System.err.println(e.javaClass.name + ": " + e.message)
			System.exit(0)
		}
		
		println("updated created successfully")
		
	}
	
	fun delete(dbName: String, tableName: String, id: String) {
		c = null
		stmt = null
		try {
			Class.forName("org.sqlite.JDBC")
			c = DriverManager.getConnection("jdbc:sqlite:$dbName.db")
			c!!.setAutoCommit(false)
			println("Opened database successfully")
			
			stmt = c!!.createStatement()
			
			val sql = "DELETE from " + tableName.toUpperCase() +
					"where ID=" + id + ";"
			stmt!!.executeUpdate(sql)
			
			stmt!!.close()
			c!!.commit()
			c!!.close()
		} catch (e: Exception) {
			System.err.println(e.javaClass.name + ": " + e.message)
			System.exit(0)
		}
		
		println("row deleted successfully")
	}
	
	
}