package cn.parentchess

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Square
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class SourcedLessonsTest {
    private fun lessons() = LessonCatalog.parse(File("src/main/assets/lessons.json").readText())
    private fun mates(board: Board): Set<String> = board.legalMoves().filter { move ->
        board.doMove(move)
        val mate = board.isMated
        board.undoMove()
        mate
    }.map { it.uci() }.toSet()

    @Test fun everyPositionMatchesItsOriginalSourceAndEverySourceLineEndsInMate() {
        val lessons = lessons()
        assertEquals((1..100).toList(), lessons.map { it.number })
        assertEquals(70, lessons.count { it.mateIn == 1 })
        assertEquals(30, lessons.count { it.mateIn == 2 })
        assertEquals(100, lessons.map { it.fen.substringBefore(' ') }.distinct().size)
        lessons.forEach { lesson ->
            val source = requireNotNull(lesson.source)
            assertEquals("lichess-${source.puzzleId}", lesson.id)
            assertEquals("https://lichess.org/training/${source.puzzleId}", source.url)
            assertTrue(source.gameUrl.startsWith("https://lichess.org/"))
            assertEquals("CC0-1.0", source.license)
            assertTrue("endgame" in source.themes)
            assertTrue(source.rating <= 1200 && source.popularity >= 90 && source.plays >= 1000)
            val original = ChessGame(source.originalFen)
            assertTrue(original.play(source.originalMoves.first()))
            val imported = ChessGame(lesson.fen)
            assertEquals(original.board.sideToMove, imported.board.sideToMove)
            Square.entries.take(64).forEach { assertEquals(original.board.getPiece(it), imported.board.getPiece(it)) }
            assertTrue(Square.entries.take(64).count { imported.board.getPiece(it) != Piece.NONE } in 4..7)
            assertTrue(source.originalMoves.drop(1) in lesson.lines)
            source.originalMoves.drop(1).forEach { assertTrue(original.play(it)) }
            assertTrue("${lesson.id}: original solution must mate", original.board.isMated)
        }
    }

    @Test fun allDefencesAndAllImmediateMatesAreCovered() {
        lessons().forEach { lesson ->
            val board = ChessGame(lesson.fen).board
            if (lesson.mateIn == 1) {
                assertEquals(mates(board), lesson.lines.map { it.single() }.toSet())
            } else {
                assertTrue(mates(board).isEmpty())
                val winning = mutableSetOf<String>()
                for (first in board.legalMoves()) {
                    board.doMove(first)
                    val replies = board.legalMoves()
                    val answers = replies.associate { reply ->
                        board.doMove(reply)
                        val finishes = mates(board)
                        board.undoMove()
                        reply.uci() to finishes
                    }
                    if (answers.isNotEmpty() && answers.values.all { it.isNotEmpty() }) {
                        winning += first.uci()
                        val recorded = lesson.lines.filter { it.first() == first.uci() }
                        assertEquals("${lesson.id}: no defence omitted", answers.keys, recorded.map { it[1] }.toSet())
                        answers.forEach { (reply, finishes) ->
                            assertEquals(finishes, recorded.filter { it[1] == reply }.map { it[2] }.toSet())
                        }
                    }
                    board.undoMove()
                }
                assertEquals("${lesson.id}: accept every forcing solution", winning, lesson.lines.map { it.first() }.toSet())
            }
        }
    }

    @Test fun obsoleteProgressIsPreservedButDoesNotCompleteNewPuzzles() {
        val lessons = lessons()
        val saved = (1..24).map { "lesson-%02d".format(it) }.toSet()
        assertEquals(0, LessonCatalog.completedCount(lessons, saved))
        assertEquals(1, LessonCatalog.completedCount(lessons, saved + lessons.first().id))
        assertEquals(24, saved.size)
    }

    @Test fun theFirstMoveOfATwoMovePuzzleIsNotSuccess() {
        lessons().filter { it.mateIn == 2 }.forEach { lesson ->
            val a = LessonAttempt(lesson)
            assertTrue(a.move(lesson.source!!.originalMoves[1]))
            assertFalse(a.complete)
            assertNull(a.game.result)
            assertEquals(2, a.played.size)
            val finish = lesson.lines.first { it.take(2) == a.played }[2]
            assertTrue(a.move(finish))
            assertTrue(a.complete)
            assertTrue(a.game.board.isMated)
        }
    }
}
