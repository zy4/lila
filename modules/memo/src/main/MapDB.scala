package lila.memo

import java.io.File
import org.mapdb.DBMaker

object MapDB {

  val memory = DBMaker.newMemoryDirectDB.make
  val persistent = DBMaker.newFileDB(new File("local/mapdb")).mmapFileEnable.make

  // trait Cache[K, V] {
  //   def get(key: K): Option[V]
  //   def put(key: K, value: V): Unit
  // }

//   private val cacheDirect = DBMaker.newCacheDirect(8)

//   def cache[K, V](prefix: String) = new Cache[K, V] {

//     private def makeKey(k: K) = s"$prefix:$k"

//     def get(key: K) = Option(cacheDirect get makeKey(key))

//     def put(key: K, value: V) { cacheDirect.put(makeKey(key), value) }
//   }
}
