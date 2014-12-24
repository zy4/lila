package lila.tournament
package simul

import lila.tournament.{ PairingSystem => AbstractPairingSystem }

object PairingSystem extends AbstractPairingSystem {
  type P = (String, String)

  def createPairings(tour: Tournament, users: List[String]): Fu[(Pairings, Events)] =
    fuccess {
      if (tour.pairings.nonEmpty) (Nil, Nil)
      else {
        for {
          white <- users
          black <- users
          if white != black
        } yield Pairing(white, black)
      }.distinct -> Nil
    }
}
