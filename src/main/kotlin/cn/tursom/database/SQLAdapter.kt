package cn.tursom.database

import sun.misc.Unsafe
import java.lang.reflect.Field
import java.sql.ResultSet
import java.sql.SQLException
import java.util.ArrayList
import kotlin.collections.HashSet
import kotlin.collections.contains
import kotlin.collections.forEach

open class SQLAdapter<T : Any>(private val clazz: Class<T>) : ArrayList<T>() {
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
	open fun adaptOnce(resultSet: ResultSet, fieldSet: HashSet<Field>) {
		//绕过构造函数获取变量0
		val t = unsafe.allocateInstance(clazz) as T
		fieldSet.forEach {
			it.isAccessible = true
			// 这里是获取bean属性的类型
			val beanType = it.type
			val value: Any?
			try {
				value = resultSet.getObject(it.name)
			} catch (e: SQLException) {
				e.printStackTrace()
				return@forEach
			}
			if (value != null) {
				val dbType = value.javaClass // 这里是获取数据库字段的类型
				//让我们把数据喂进去
				it.set(t, if (beanType == java.lang.Float::class.java) {
					if (dbType == java.lang.Double::class.java) {
						(value as Double).toFloat()
					} else {
						//检查是否可以为空
						if (it.getAnnotation(NotNullField::class.java) != null) {
							value.toString().toFloat()
						} else {
							value.toString().toFloatOrNull()
						}
					}
				} else if (beanType == java.lang.String::class.java && dbType != java.lang.String::class.java) {
					value.toString()
				} else if (beanType == java.lang.Boolean::class.java) {
					if (it.getAnnotation(NotNullField::class.java) != null) {
						value.toString().toBoolean()
					} else {
						try {
							value.toString().toBoolean()
						} catch (e: Exception) {
							null
						}
					}
				} else if (beanType.interfaces.contains(SqlField::class.java)) {
					val field = beanType.newInstance() as SqlField<*>
					field.adapt(value)
					field
				} else {
					value
				})
			}
		}
		add(t)
	}
	
	companion object {
		//利用Unsafe绕过构造函数获取变量
		private val unsafe by lazy {
			val field = Unsafe::class.java.getDeclaredField("theUnsafe")
			//允许通过反射设置属性的值
			field.isAccessible = true
			field.get(null) as Unsafe
		}
	}
}