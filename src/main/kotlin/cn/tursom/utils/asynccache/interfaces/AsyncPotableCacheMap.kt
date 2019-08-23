package cn.tursom.utils.asynccache.interfaces

import cn.tursom.utils.datastruct.async.interfaces.AsyncPotableMap

interface AsyncPotableCacheMap<K, V> : AsyncCacheMap<K, V>, AsyncPotableMap<K, V>