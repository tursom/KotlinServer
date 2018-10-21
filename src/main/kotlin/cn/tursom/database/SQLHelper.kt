package cn.tursom.database

/**
 * MySQLHelper，SQLite辅助使用类
 * 实现创建表格、查询、插入和更新功能
 */

interface SQLHelper {
	/**
	 * 创建表格
	 * @param table: 表格名
	 * @param keys: 属性列表
	 */
	fun createTable(table: String, keys: Array<String>)
	
	/**
	 * 根据提供的class对象自动化创建表格
	 * 但是有诸多缺陷，所以不是很建议使用
	 */
	fun <T> createTable(table: String, keys: Class<T>)
	
	/**
	 * 删除表格
	 */
	fun deleteTable(table: String)
	
	/**
	 * 删除表格
	 */
	fun dropTable(table: String)
	
	/**
	 * 查询
	 * @param adapter 用于保存查询结果的数据类，由SQLAdapter继承而来
	 * @param table 表名
	 * @param name 查询字段
	 * @param where 指定从一个表或多个表中获取数据的条件,Pair左边为字段名，右边为限定的值
	 * @param maxCount 最大查询数量
	 */
	fun <T : Any> select(
		adapter: SQLAdapter<T>, table: String,
		name: Array<String> = arrayOf("*"), where: Map<String, String>?, maxCount: Int? = null)
	
	/**
	 * 查询
	 * @param adapter 用于保存查询结果的数据类，由SQLAdapter继承而来
	 * @param table 表名
	 * @param name 查询字段
	 * @param where 指定从一个表或多个表中获取数据的条件,Pair左边为字段名，右边为限定的值
	 * @param maxCount 最大查询数量
	 */
	fun <T : Any> select(
		adapter: SQLAdapter<T>, table: String,
		where: Pair<String, String>, maxCount: Int? = null, name: Array<String> = arrayOf("*"))
	
	fun <T : Any> select(
		adapter: SQLAdapter<T>, table: String, name: String = "*", where: String? = null, maxCount: Int? = null
	)
	
	fun <T : Any> reverseSelect(
		adapter: SQLAdapter<T>, table: String,
		name: Array<String> = arrayOf("*"), where: Map<String, String>? = null, index: String, maxCount: Int? = null) {
	}
	
	fun <T : Any> reverseSelect(
		adapter: SQLAdapter<T>, table: String,
		name: Array<String> = arrayOf("*"), where: Pair<String, String>, index: String, maxCount: Int? = null)
	
	fun <T : Any> reverseSelect(
		adapter: SQLAdapter<T>, table: String, name: String = "*", where: String? = null, index: String, maxCount: Int? = null
	)
	
	/**
	 * 插入
	 * @param table 表名
	 * @param value 值
	 */
	fun <T : Any> insert(table: String, value: T)
	
	fun insert(table: String, column: Map<String, String>)
	
	fun insert(table: String, column: String, values: String)
	
	fun update(
		table: String,
		set: Map<String, String> = mapOf(),
		where: Map<String, String> = mapOf())
	
	fun <T : Any> update(table: String, value: T, where: Map<String, String>)
	
	fun delete(table: String, where: String)
	
	fun delete(table: String, where: Map<String, String>)
	
	fun delete(table: String, where: Pair<String, String>)
	
	fun commit()
	
	fun close()
}