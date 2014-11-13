package lila.memo

import scala.concurrent.duration._

import spray.caching.{ LruCache, Cache }

final class MixedCacheMapDB[K, V](
    f: K => Fu[V],
    gbs: Double,
    awaitTime: FiniteDuration = 5.milliseconds,
    default: K => V) {

  private val asyncCache = AsyncCache(f, maxCapacity = 1000, timeToLive = 20 seconds)
  private val syncCache = org.mapdb.DBMaker.newCacheDirect[K, V](gbs)

  def get(k: K): V = Option(syncCache get k).fold {
    try {
      val v = asyncCache(k) await makeTimeout(awaitTime)
      syncCache.put(k, v)
      v
    }
    catch {
      case _: Exception => default(k)
    }
  }(identity)

  def invalidate(k: K) {
    println("invalidate " + k)
    println(syncCache get k)
    syncCache remove k
    println(syncCache get k)
  }
}

// object MixedCacheMapDB {

//   def apply[K, V](
//     f: K => Fu[V],
//     gbs: Double,
//     awaitTime: FiniteDuration = 5.milliseconds,
//     default: K => V): MixedCache[K, V] = {
//     val asyncCache = AsyncCache(f, maxCapacity = 1000, timeToLive = 20 seconds)
//     val syncCache = org.mapdb.DBMaker.newCacheDirect[K, V](gbs)
//       timeToLive,
//       (k: K) => asyncCache(k) await makeTimeout(awaitTime))
//     new MixedCache(syncCache, default)
//   }

//   def single[V](
//     f: => Fu[V],
//     timeToLive: Duration = Duration.Inf,
//     awaitTime: FiniteDuration = 5.milliseconds,
//     default: V): MixedCache[Boolean, V] = {
//     val asyncCache = AsyncCache.single(f, timeToLive = 3 minute)
//     val syncCache = Builder.cache[Boolean, V](
//       timeToLive,
//       (_: Boolean) => asyncCache(true) await makeTimeout(awaitTime))
//     new MixedCache(syncCache, _ => default)
//   }
// }
