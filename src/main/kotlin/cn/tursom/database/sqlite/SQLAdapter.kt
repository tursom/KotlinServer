package cn.tursom.database.sqlite

import sun.misc.Unsafe
import java.lang.reflect.Field
import java.sql.ResultSet
import java.sql.SQLException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashSet

/**
 * SQLite查询结果储存类
 */
open class SQLAdapter<T : Any>(private val clazz: Class<T>) : ArrayList<T>() {
	//获取Unsafe
	private val field: Field by lazy {
		val field = Unsafe::class.java.getDeclaredField("theUnsafe")
		//允许通过反射设置属性的值
		field.isAccessible = true
		field
	}
	//利用Unsafe绕过构造函数获取变量
	private val unsafe = field.get(null) as Unsafe
	
	open fun adapt(resultSet: ResultSet) {
		clear() //清空已储存的数据
		try {
			val fieldSet = HashSet<Field>()
			if (resultSet.next()) {
				clazz.declaredFields.forEach {
					try {
						resultSet.getObject(it.name)
						fieldSet.add(it)
					} catch (e: SQLException) {
					}
				}
				adaptOnce(resultSet, fieldSet)
			}
			// 遍历ResultSet
			while (resultSet.next()) {
				adaptOnce(resultSet, fieldSet)
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}
	
	@Suppress("UNCHECKED_CAST")
	private fun adaptOnce(resultSet: ResultSet, fieldSet: HashSet<Field>) {
		//绕过构造函数获取变量0
		val t = unsafe.allocateInstance(clazz) as T
		fieldSet.forEach {
			it.isAccessible = true
			// 这里是获取bean属性的类型
			val beanType = it.type
			// 根据 rs 列名 ，组装javaBean里边的其中一个set方法，object 就是数据库第一行第一列的数据了
			var value: Any?
			try {
				value = resultSet.getObject(it.name)
			} catch (e: SQLException) {
				e.printStackTrace()
				return@forEach
			}
			if (value != null) {
				val dbType = value.javaClass // 这里是获取数据库字段的类型
				// 处理类型不匹配问题
				if (beanType == java.util.Date::class.java) {
					if (dbType == java.sql.Timestamp::class.java) {
						value = java.util.Date((value as java.sql.Timestamp).time)
					} else if (dbType == String::class.java) {
						value = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").parse(value as String)
					}
				} else if (beanType == java.lang.Float::class.java && dbType == java.lang.Double::class.java) {
					value = (value as Double).toFloat()
				} else if (beanType == java.lang.String::class.java && dbType != java.lang.String::class.java) {
					value = value.toString()
				} else if (beanType == java.lang.Boolean::class.java && dbType == java.lang.String::class.java) {
					value = value.toString().toBoolean()
				}
				it.set(t, value)
			}
		}
		add(t)
	}
}
