package lila.relation

import lila.memo.MixedCacheMapDB
import ornicar.scalalib.Zero

private[relation] final class Cached {

  private def cacheUp[K, V: Zero](f: K => Fu[V], gbs: Double): MixedCacheMapDB[K, V] =
    new MixedCacheMapDB[K, V](f = f, default = _ => zero[V], gbs = gbs)

  private[relation] val followers = cacheUp(RelationRepo.followers, 0.2)
  private[relation] val following = cacheUp(RelationRepo.following, 0.2)
  private[relation] val blockers = cacheUp(RelationRepo.blockers, 0.2)
  private[relation] val blocking = cacheUp(RelationRepo.blocking, 0.2)
  private[relation] val relation = cacheUp(findRelation, 0.4)

  private def findRelation(pair: (String, String)): Fu[Option[Relation]] = fuccess {
    pair match {
      case (u1, u2) =>
        if (following.get(u1)(u2)) true.some
        else if (blocking.get(u1)(u2)) true.some
        else none
    }
  }

  private[relation] def invalidate(u1: ID, u2: ID) {
    List(followers, following, blockers, blocking) foreach { cache =>
      List(u1, u2) foreach cache.invalidate
    }
    relation.invalidate(u1 -> u2)
  }
}
