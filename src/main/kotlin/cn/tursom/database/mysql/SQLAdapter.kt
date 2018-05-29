package cn.tursom.database.mysql

import java.lang.reflect.Field
import java.sql.ResultSet
import sun.misc.Unsafe

class SQLAdapter<T : Any>(private val clazz: Class<T>) : ArrayList<T>() {
	
	@Suppress("UNCHECKED_CAST")
	fun adapt(resultSet: ResultSet) {
		clear()
		var field: Field?
		try {
			// 取得ResultSet列名
			val rsmd = resultSet.metaData
			// 获取记录集中的列数
			val counts = rsmd.columnCount
			// 定义counts个String 变量
			val columnNames = arrayOfNulls<String>(counts)
			// 给每个变量赋值(字段名称全部转换成小写)
			for (i in 0 until counts) {
				columnNames[i] = rsmd.getColumnLabel(i + 1).toLowerCase()
			}
			// 遍历ResultSet
			while (resultSet.next()) {
				//绕过构造函数获取变量
				field = Unsafe::class.java.getDeclaredField("theUnsafe")
				field.isAccessible = true
				val unsafe = field.get(null) as Unsafe
				
				val t = unsafe.allocateInstance(clazz) as T
				// 反射, 从ResultSet绑定到JavaBean
				for (i in 0 until counts) {
					// 设置参数类型，此类型应该跟javaBean 里边的类型一样，而不是取数据库里边的类型
					field = clazz.getDeclaredField(columnNames[i])
					field.isAccessible = true
					// 这里是获取bean属性的类型
					val beanType = field!!.type
					// 根据 rs 列名 ，组装javaBean里边的其中一个set方法，object 就是数据库第一行第一列的数据了
					var value: Any? = resultSet.getObject(columnNames[i])
					if (value != null) {
						// 这里是获取数据库字段的类型
						val dbType = value.javaClass
						// 处理日期类型不匹配问题
						if (dbType == java.sql.Timestamp::class.java && beanType == java.util.Date::class.java) {
							value = java.util.Date(
									(value as java.sql.Timestamp).time)
						}
					}
					field.set(t, value)
				}
				add(t)
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}
}