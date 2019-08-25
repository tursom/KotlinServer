package cn.tursom.utils.cache

import cn.tursom.database.async.AsyncSqlAdapter
import cn.tursom.database.async.AsyncSqlHelper
import cn.tursom.utils.cache.interfaces.AsyncPotableCacheMap
import cn.tursom.utils.asynclock.AsyncMutexLock
import cn.tursom.utils.background
import kotlinx.coroutines.delay
import java.lang.Exception
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import java.util.logging.Logger

@Suppress("CanBeParameter", "MemberVisibilityCanBePrivate", "SpellCheckingInspection")
class AsyncSqlStringCacheMap(
	val db: AsyncSqlHelper,
	val timeout: Long,
	val table: String,
	val updateDelay: Long = 60 * 1000,
	val prevCacheMap: AsyncPotableCacheMap<String, String> = DefaultAsyncPotableCacheMap(timeout),
	val logger: Logger? = null
) : AsyncPotableCacheMap<String, String> by prevCacheMap {
	private val updateRunerLock = AtomicBoolean(false)
	private val updateLock = AsyncMutexLock()
	private val updateList = ArrayList<StorageData>()


	override suspend fun get(key: String): String? {
		val memCache = prevCacheMap.get(key)
		return if (memCache != null) memCache
		else {
			val storage = db.select(AsyncSqlAdapter(StorageData::class.java), maxCount = 1)
			if (storage.isNotEmpty()) {
				val value = storage[0].value
				set(key, value)
				value
			} else null
		}
	}

	override suspend fun get(key: String, constructor: suspend () -> String): String {
		val memCache = get(key)
		return if (memCache != null) memCache
		else {
			val newValue = constructor()
			background { updateStorage(key, newValue) }
			newValue
		}
	}

	override suspend fun set(key: String, value: String): String? {
		prevCacheMap.set(key, value)
		updateStorage(key, value)
		return value
	}

	suspend fun updateStorage(key: String, value: String) {
		updateLock {
			updateList.add(StorageData(key, value))
		}
		if (updateRunerLock.compareAndSet(false, true)) background {
			delay(updateDelay)
			updateLock {
				try {
					val updated = db.replace(table, updateList)
					logger?.log(Level.INFO, "AsyncSqlStringCacheMap update $updated coloums")
				} catch (e: Exception) {
					System.err.println("AsyncSqlStringCacheMap cause an Exception on update database")
					e.printStackTrace()
				} finally {
					updateList.clear()
				}
			}
			updateRunerLock.set(false)
		}
	}
}

