package cn.tursom.database.mysql

import sun.misc.Unsafe
import java.lang.reflect.Field
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

@Suppress("UNCHECKED_CAST")
open class SQLAdapter<T : Any>(private val clazz: Class<T>) : ArrayList<T>() {
	
	open fun adapt(resultSet: ResultSet) {
		clear()
		val field: Field = Unsafe::class.java.getDeclaredField("theUnsafe")
		field.isAccessible = true
		val unsafe = field.get(null) as Unsafe
		try {
			// 遍历ResultSet
			while (resultSet.next()) {
				//绕过构造函数获取变量
				val t = unsafe.allocateInstance(clazz) as T
				
				clazz.declaredFields.forEach {
					it.isAccessible = true
					// 这里是获取bean属性的类型
					val beanType = it.type
					// 根据 rs 列名 ，组装javaBean里边的其中一个set方法，object 就是数据库第一行第一列的数据了
					var value: Any?
					try {
						value = resultSet.getObject(it.name)
					} catch (e: SQLException) {
						if (e.message == "no such column: '${it.name}'") {
							return@forEach
						} else {
							e.printStackTrace()
							return@forEach
						}
					}
					if (value != null) {
						val dbType = value.javaClass // 这里是获取数据库字段的类型
						//处理类型不匹配问题
						when (beanType) {
//							java.util.Date::class.java ->
//								when (dbType) {
//									String::class.java -> value = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").parse(value as String)
//								}
							java.lang.Float::class.java ->
								if (dbType == java.lang.Double::class.java)
									value = (value as Double).toFloat()
							SQLHelper.SQLCommand::class.java -> value = SQLHelper.SQLCommand(value.toString())
						}
					}
					it.set(t, value)
				}
				add(t)
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}
}