package cn.parentchess

import com.github.bhlangonijr.chesslib.Side
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class ProgressionLessonsTest {
    @Test fun legacyLessonsCanBeCopiedIntoReviewWithoutMissingCompletionCrashingStartup() {
        val old = LessonCatalog.parse(File("src/main/assets/lessons.json").readText())
        assertEquals(100, old.size)
        old.forEach {
            val review = it.copy(chapter = "06 · 原题复习")
            assertEquals("checkmate", review.completion)
            assertEquals(it.id, review.id)
        }
    }
    private fun lessons() = LessonCatalog.parse(File("src/main/assets/progression-lessons.json").readText())

    @Test fun hintsFollowEveryAcceptedDefenceAndNeverMutateThePosition() {
        lessons().forEach { lesson ->
            lesson.lines.forEach { line ->
                var responseIndex = 1
                val attempt = LessonAttempt(lesson) { line[responseIndex].also { responseIndex += 2 } }
                line.filterIndexed { index, _ -> index % 2 == 0 }.forEach { uci ->
                    val before = attempt.game.board.fen
                    val observed = requireNotNull(attempt.hint(false))
                    assertNull(observed.move)
                    val revealed = requireNotNull(attempt.hint(true))
                    val next = requireNotNull(revealed.move)
                    assertTrue(next in attempt.game.legal())
                    assertTrue(lesson.lines.any { it.take(attempt.played.size + 1) == attempt.played + next.uci() })
                    assertEquals(before, attempt.game.board.fen)
                    assertTrue(attempt.move(uci))
                }
                assertNull(attempt.nextMove())
                assertNull(attempt.hint(true))
            }
        }
    }

    @Test fun originalSolutionsWorkForBothSidesAndFinishOnlyAtTheStatedEndpoint() {
        val lessons = lessons()
        assertTrue(lessons.any { ChessGame(it.fen).board.sideToMove == Side.BLACK })
        assertTrue(lessons.any { it.completion == "sourceLine" })
        assertEquals(lessons.size, lessons.map { it.id }.toSet().size)
        assertEquals((1..lessons.size).toList(), lessons.map { it.number })
        lessons.forEach { lesson ->
            val source = requireNotNull(lesson.source)
            val original = ChessGame(source.originalFen)
            assertTrue(original.play(source.originalMoves.first()))
            val imported = ChessGame(lesson.fen)
            // The libraries differ on recording an uncapturable en-passant square.
            assertEquals(original.board.fen.split(" ").filterIndexed { i, _ -> i != 3 },
                imported.board.fen.split(" ").filterIndexed { i, _ -> i != 3 })
            assertEquals(original.legal().map { it.uci() }.toSet(), imported.legal().map { it.uci() }.toSet())
            val line = source.originalMoves.drop(1)
            assertTrue(line in lesson.lines)
            // Choose the corresponding official response by tracking accepted user plies.
            var responseIndex = 1
            val attempt = LessonAttempt(lesson) { choices ->
                line[responseIndex].also { assertTrue(it in choices); responseIndex += 2 }
            }
            line.filterIndexed { index, _ -> index % 2 == 0 }.forEachIndexed { index, uci ->
                assertFalse(attempt.complete)
                assertTrue("${lesson.id}: $uci", attempt.move(uci))
                assertEquals(index == line.size / 2, attempt.complete)
            }
            assertTrue(attempt.complete)
            if (lesson.mateIn > 0) assertTrue(attempt.game.board.isMated)
            assertFalse(attempt.move(line.first()))
        }
    }

    @Test fun incorrectLegalMovesDoNotChangeThePositionOrProgress() {
        lessons().filter { it.completion == "sourceLine" }.forEach { lesson ->
            val attempt = LessonAttempt(lesson)
            val firstMoves = lesson.lines.map { it.first() }.toSet()
            val wrong = attempt.game.legal().firstOrNull { it.uci() !in firstMoves } ?: return@forEach
            val before = attempt.game.board.fen
            assertFalse(attempt.move(wrong.uci()))
            assertEquals(before, attempt.game.board.fen)
            assertTrue(attempt.played.isEmpty())
            assertFalse(attempt.complete)
        }
    }
}
