package lila.puzzle

import play.api.libs.json._

import lila.common.PimpedJson._
import lila.game.Game
import lila.puzzle._

object JsonView {

  def apply(
    puzzle: Puzzle,
    userInfos: Option[UserInfos],
    mode: String,
    animationDuration: scala.concurrent.duration.Duration,
    pref: lila.pref.Pref,
    isMobileApi: Boolean,
    round: Option[Round] = None,
    win: Option[Boolean] = None,
    voted: Option[Boolean]): Fu[JsObject] =
    (!isMobileApi ?? GameJson(puzzle.gameId, puzzle.initialPly).map(_.some)) map { gameJson =>
      Json.obj(
        "game" -> gameJson,
        "puzzle" -> Json.obj(
          "id" -> puzzle.id,
          "rating" -> puzzle.perf.intRating,
          "attempts" -> puzzle.attempts,
          "fen" -> puzzle.fen,
          "color" -> puzzle.color.name,
          "initialMove" -> puzzle.initialMove,
          "initialPly" -> puzzle.initialPly,
          "gameId" -> puzzle.gameId,
          "lines" -> lila.puzzle.Line.toJson(puzzle.lines),
          "enabled" -> puzzle.enabled,
          "vote" -> puzzle.vote.sum
        ),
        "pref" -> Json.obj(
          "coords" -> pref.coords,
          "rookCastle" -> pref.rookCastle
        ),
        "chessground" -> Json.obj(
          "highlight" -> Json.obj(
            "lastMove" -> pref.highlight,
            "check" -> pref.highlight
          ),
          "movable" -> Json.obj(
            "showDests" -> pref.destination
          ),
          "draggable" -> Json.obj(
            "showGhost" -> pref.highlight
          ),
          "premovable" -> Json.obj(
            "showDests" -> pref.destination
          )
        ),
        "animation" -> Json.obj(
          "duration" -> pref.animationFactor * animationDuration.toMillis
        ),
        "mode" -> mode,
        "round" -> round.map { a =>
          Json.obj(
            "ratingDiff" -> a.ratingDiff,
            "win" -> a.win
          )
        },
        "win" -> win,
        "voted" -> voted,
        "user" -> userInfos.map { i =>
          Json.obj(
            "rating" -> i.user.perfs.puzzle.intRating,
            "history" -> isMobileApi.option(i.history.map(_.rating)), // for mobile BC
            "recent" -> i.history.map { r =>
              Json.arr(r.puzzleId, r.ratingDiff, r.rating)
            }
          ).noNull
        },
        "difficulty" -> isMobileApi.option {
          Json.obj(
            "choices" -> Json.arr(
              Json.arr(2, "Normal")
            ),
            "current" -> 2
          )
        }).noNull
    }
}