package lila.tournament
package simul

import lila.tournament.{ Score => AbstractScore }
import lila.tournament.{ ScoringSystem => AbstractScoringSystem }

object ScoringSystem extends AbstractScoringSystem {

  case class Score(win: Option[Boolean]) extends AbstractScore {
    val value = win match {
      case Some(true)  => 2
      case Some(false) => 0
      case None        => 1
    }
  }

  case class Sheet(scores: List[Score]) extends ScoreSheet {
    val total = scores.foldLeft(0)(_ + _.value)
  }

  override def rank(tour: Tournament, players: Players): RankedPlayers = {
    players.foldLeft(Nil: RankedPlayers) {
      case (Nil, p)                  => (1, p) :: Nil
      case (list@((r0, p0) :: _), p) => ((p0.score == p.score).fold(r0, list.size + 1), p) :: list
    }.reverse
  }

  override def scoreSheet(tour: Tournament, user: String) = Sheet {
    val filtered = tour userPairings user filter (_.finished) reverse
    val nexts = (filtered drop 1 map Some.apply) :+ None
    filtered.zip(nexts).foldLeft(List[Score]()) {
      case (scores, (p, n)) => (p.winner match {
        case None if p.quickDraw    => Score(Some(false))
        case None                   => Score(None)
        case Some(w) if (user == w) => Score(Some(true))
        case _                      => Score(Some(false))
      }) :: scores
    }
  }
}
