package cn.tursom.database.mongodb

import com.mongodb.MongoClient
import com.mongodb.MongoCredential
import com.mongodb.client.model.Filters
import org.bson.Document
import org.junit.Test

class MongoHelperTest {
	@Test
	fun testConnection() {
		//MongoCredential.createScramSha1Credential()三个参数分别为 用户名 数据库名称 密码
		val credential = MongoCredential.createScramSha1Credential("tursom", "test", "test123".toCharArray())
		//通过连接认证获取MongoDB连接
		val mongoClient = MongoClient("127.0.0.1", 27017)
		
		// 连接到数据库
		val mongoDatabase = mongoClient.getDatabase("test")
		val collection = mongoDatabase.getCollection("testCollection")
		collection.deleteMany(Filters.eq("title","MongoDb"))
		val document = Document("title", "MongoDb")
		document["1"] = "2"
		collection.insertMany(listOf(document))
		collection.find(Filters.eq("title","MongoDb")).forEach {
			println(it)
		}
	}
}