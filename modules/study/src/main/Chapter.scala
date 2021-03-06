package lila.study

import chess.format.pgn.{ Glyph, Tag, Tags }
import chess.variant.Variant
import chess.{ Color, Centis }
import org.joda.time.DateTime

import chess.opening.{ FullOpening, FullOpeningDB }
import lila.tree.Node.{ Shapes, Comment, Gamebook }
import lila.user.User

case class Chapter(
    _id: Chapter.Id,
    studyId: Study.Id,
    name: Chapter.Name,
    setup: Chapter.Setup,
    root: Node.Root,
    tags: Tags,
    order: Int,
    ownerId: User.ID,
    conceal: Option[Chapter.Ply] = None,
    practice: Option[Boolean] = None,
    gamebook: Option[Boolean] = None,
    description: Option[String] = None,
    relay: Option[Chapter.Relay] = None,
    createdAt: DateTime
) extends Chapter.Like {

  def updateRoot(f: Node.Root => Option[Node.Root]) =
    f(root) map { newRoot =>
      copy(root = newRoot)
    }

  def addNode(node: Node, path: Path, newRelay: Option[Chapter.Relay] = None): Option[Chapter] =
    updateRoot {
      _.withChildren(_.addNodeAt(node, path))
    } map {
      _.copy(relay = newRelay orElse relay)
    }

  def setShapes(shapes: Shapes, path: Path): Option[Chapter] =
    updateRoot(_.setShapesAt(shapes, path))

  def setComment(comment: Comment, path: Path): Option[Chapter] =
    updateRoot(_.setCommentAt(comment, path))

  def setGamebook(gamebook: Gamebook, path: Path): Option[Chapter] =
    updateRoot(_.setGamebookAt(gamebook, path))

  def deleteComment(commentId: Comment.Id, path: Path): Option[Chapter] =
    updateRoot(_.deleteCommentAt(commentId, path))

  def toggleGlyph(glyph: Glyph, path: Path): Option[Chapter] =
    updateRoot(_.toggleGlyphAt(glyph, path))

  def setClock(clock: Option[Centis], path: Path): Option[Chapter] =
    updateRoot(_.setClockAt(clock, path))

  def opening: Option[FullOpening] =
    if (!Variant.openingSensibleVariants(setup.variant)) none
    else FullOpeningDB searchInFens root.mainline.map(_.fen)

  def isEmptyInitial = order == 1 && root.children.nodes.isEmpty

  def cloneFor(study: Study) = copy(
    _id = Chapter.makeId,
    studyId = study.id,
    ownerId = study.ownerId,
    createdAt = DateTime.now
  )

  def metadata = Chapter.Metadata(_id = _id, name = name, setup = setup)

  def isPractice = ~practice
  def isGamebook = ~gamebook
  def isConceal = conceal.isDefined

  def withoutChildren = copy(root = root.withoutChildren)

  def relayAndTags = relay map { Chapter.RelayAndTags(id, _, tags) }
}

object Chapter {

  case class Id(value: String) extends AnyVal with StringValue
  implicit val idIso = lila.common.Iso.string[Id](Id.apply, _.value)

  case class Name(value: String) extends AnyVal with StringValue
  implicit val nameIso = lila.common.Iso.string[Name](Name.apply, _.value)

  sealed trait Like {
    val _id: Chapter.Id
    val name: Chapter.Name
    val setup: Chapter.Setup
    def id = _id

    def initialPosition = Position.Ref(id, Path.root)
  }

  case class Setup(
      gameId: Option[lila.game.Game.ID],
      variant: Variant,
      orientation: Color,
      fromFen: Option[Boolean] = None
  ) {
    def isFromFen = ~fromFen
  }

  case class Relay(
      index: Int, // game index in the source URL
      path: Path,
      lastMoveAt: DateTime
  ) {
    def secondsSinceLastMove: Int = (nowSeconds - lastMoveAt.getSeconds).toInt
  }

  case class RelayAndTags(id: Id, relay: Relay, tags: Tags) {

    def looksAlive =
      tags.resultColor.isEmpty &&
        relay.lastMoveAt.isAfter {
          DateTime.now.minusMinutes {
            tags.clockConfig.fold(40)(_.limitInMinutes.toInt / 2 atLeast 15 atMost 60)
          }
        }

    def looksOver = !looksAlive
  }

  case class Metadata(
      _id: Chapter.Id,
      name: Chapter.Name,
      setup: Chapter.Setup
  ) extends Like

  case class IdName(id: Id, name: Name)

  case class Ply(value: Int) extends AnyVal with Ordered[Ply] {
    def compare(that: Ply) = value - that.value
  }

  def defaultName(order: Int) = Name(s"Chapter $order")

  private val defaultNamePattern = """^Chapter \d+$""".r.pattern
  def isDefaultName(n: Name) = n.value.isEmpty || defaultNamePattern.matcher(n.value).matches

  def fixName(n: Name) = Name(n.value.trim take 80)

  val idSize = 8

  def makeId = Id(scala.util.Random.alphanumeric take idSize mkString)

  def make(studyId: Study.Id, name: Name, setup: Setup, root: Node.Root, tags: Tags, order: Int, ownerId: User.ID, practice: Boolean, gamebook: Boolean, conceal: Option[Ply], relay: Option[Relay] = None) = Chapter(
    _id = makeId,
    studyId = studyId,
    name = fixName(name),
    setup = setup,
    root = root,
    tags = tags,
    order = order,
    ownerId = ownerId,
    practice = practice option true,
    gamebook = gamebook option true,
    conceal = conceal,
    relay = relay,
    createdAt = DateTime.now
  )
}
