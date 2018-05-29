package cn.tursom.database.mysql

import java.lang.reflect.Field
import java.sql.ResultSet
import java.util.*

/**
 * @说明：利用反射机制从ResultSet自动绑定到JavaBean；根据记录集自动调用javaBean里边的对应方法。
 *
 * @param <T>
</T> */
class SetBean<T : Any> {
	/**
	 * @param clazz
	 * 所要封装的javaBean
	 * @param rs
	 * 记录集
	 * @return ArrayList 数组里边装有 多个javaBean
	 * @throws Exception
	 */
	inline fun <reified T : Any> getList(rs: ResultSet): List<T>? {
		var field: Field?
		val lists = ArrayList<T>()
		try {
			// 取得ResultSet列名
			val rsmd = rs.metaData
			// 获取记录集中的列数
			val counts = rsmd.columnCount
			// 定义counts个String 变量
			val columnNames = arrayOfNulls<String>(counts)
			// 给每个变量赋值(字段名称全部转换成小写)
			for (i in 0 until counts) {
				columnNames[i] = rsmd.getColumnLabel(i + 1).toLowerCase()
			}
			// 变量ResultSet
			while (rs.next()) {
				val t = T::class.java.newInstance()
				// 反射, 从ResultSet绑定到JavaBean
				for (i in 0 until counts) {
					
					// 设置参数类型，此类型应该跟javaBean 里边的类型一样，而不是取数据库里边的类型
					field = T::class.java.getDeclaredField(columnNames[i])
					
					// 这里是获取bean属性的类型
					val beanType = field!!.type
					
					// 根据 rs 列名 ，组装javaBean里边的其中一个set方法，object 就是数据库第一行第一列的数据了
					var value: Any? = rs.getObject(columnNames[i])
					
					if (value != null) {
						
						// 这里是获取数据库字段的类型
						val dbType = value.javaClass
						
						// 处理日期类型不匹配问题
						if (dbType == java.sql.Timestamp::class.java && beanType == java.util.Date::class.java) {
							// value = new
							// java.util.Date(rs.getTimestamp(columnNames[i]).getTime());
							value = java.util.Date(
									(value as java.sql.Timestamp).time)
						}
						// 处理double类型不匹配问题
						if (dbType == java.math.BigDecimal::class.java && beanType == Double::class.javaPrimitiveType) {
							// value = rs.getDouble(columnNames[i]);
							value = value.toString().toDouble()
						}
						// 处理int类型不匹配问题
						if (dbType == java.math.BigDecimal::class.java && beanType == Int::class.javaPrimitiveType) {
							// value = rs.getInt(columnNames[i]);
							value = value.toString().toInt()
						}
					}
					
					val setMethodName = "set" + firstUpperCase(columnNames[i]!!)
					// 第一个参数是传进去的方法名称，第二个参数是 传进去的类型；
					val m = t.javaClass.getMethod(setMethodName, beanType)
					
					// 第二个参数是传给set方法数据；如果是get方法可以不写
					m.invoke(t, value)
				}
				lists.add(t)
			}
		} catch (e: Exception) {
			e.printStackTrace()
			return null
		}
		
		return lists
	}
	
	/**
	 * @param clazz
	 * bean类
	 * @param rs
	 * 结果集 (只有封装第一条结果)
	 * @return 封装了查询结果的bean对象
	 */
	fun getObj(clazz: Class<T>, rs: ResultSet): T? {
		var field: Field?
		var obj: T? = null
		try {
			// 取得ResultSet列名
			val rsmd = rs.metaData
			// 获取记录集中的列数
			val counts = rsmd.columnCount
			// 定义counts个String 变量
			val columnNames = arrayOfNulls<String>(counts)
			// 给每个变量赋值(字段名称全部转换成小写)
			for (i in 0 until counts) {
				columnNames[i] = rsmd.getColumnLabel(i + 1).toLowerCase()
			}
			// 变量ResultSet
			if (rs.next()) {
				val t = clazz.newInstance()
				// 反射, 从ResultSet绑定到JavaBean
				for (i in 0 until counts) {
					try {
						// 设置参数类型，此类型应该跟javaBean 里边的类型一样，而不是取数据库里边的类型
						field = clazz.getDeclaredField(columnNames[i])
					} catch (ex: Exception) {
						ex.printStackTrace()
						continue
					}
					
					// 这里是获取bean属性的类型
					val beanType = field!!.type
					
					// 根据 rs 列名 ，组装javaBean里边的其中一个set方法，object 就是数据库第一行第一列的数据了
					var value: Any? = rs.getObject(columnNames[i])
					
					if (value != null) {
						
						// 这里是获取数据库字段的类型
						val dbType = value.javaClass
						
						// 处理日期类型不匹配问题
						if (dbType == java.sql.Timestamp::class.java && beanType == java.util.Date::class.java) {
							// value = new
							// java.util.Date(rs.getTimestamp(columnNames[i]).getTime());
							value = java.util.Date(
									(value as java.sql.Timestamp).time)
						}
						// 处理double类型不匹配问题
						if (dbType == java.math.BigDecimal::class.java && beanType == Double::class.javaPrimitiveType) {
							// value = rs.getDouble(columnNames[i]);
							value = value.toString().toDouble()
						}
						// 处理int类型不匹配问题
						if (dbType == java.math.BigDecimal::class.java && beanType == Int::class.javaPrimitiveType) {
							// value = rs.getInt(columnNames[i]);
							value = value.toString().toInt()
						}
					}
					
					val setMethodName = "set" + firstUpperCase(columnNames[i]!!)
					// 第一个参数是传进去的方法名称，第二个参数是 传进去的类型；
					val m = t.javaClass.getMethod(setMethodName, beanType)
					
					// 第二个参数是传给set方法数据；如果是get方法可以不写
					m.invoke(t, value)
				}
				obj = t
			}
		} catch (e: Exception) {
			e.printStackTrace()
			return null
		}
		
		return obj
	}
	
	companion object {
		
		// 首写字母变大写
		fun firstUpperCase(old: String): String {
			return old.substring(0, 1).toUpperCase() + old.substring(1)
		}
	}
}

