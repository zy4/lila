package lila.bookmark

import scala.concurrent.duration._

private[bookmark] final class Cached {

  private[bookmark] val gameIdsCache = new lila.memo.MixedCacheMapDB[String, Set[String]](
    (userId: String) => BookmarkRepo gameIdsByUserId userId map (_.toSet),
    gbs = 0.5,
    default = _ => Set.empty,
    awaitTime = 10.milliseconds)

  def gameIds(userId: String) = gameIdsCache get userId

  def bookmarked(gameId: String, userId: String): Boolean =
    gameIds(userId) contains gameId

  def count(userId: String): Int = gameIds(userId).size
}
