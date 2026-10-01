package cn.parentchess

import com.github.bhlangonijr.chesslib.*
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import kotlin.random.Random

class ChessTest {
    private fun game(fen: String) = ChessGame(fen)
    private fun legal(g: ChessGame) = g.legal().map { it.uci() }
    private fun perft(b: Board, depth: Int): Long {
        if (depth == 0) return 1
        return b.legalMoves().sumOf { b.doMove(it); val n = perft(b, depth - 1); b.undoMove(); n }
    }
    @Test fun startingPerft() { val b = Board(); assertEquals(20, b.legalMoves().size); assertEquals(400L, perft(b, 2)); assertEquals(8902L, perft(b, 3)) }
    @Test fun castlingAndPins() {
        val g = game("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
        assertTrue(legal(g).containsAll(listOf("e1g1", "e1c1")))
        assertTrue(g.play("e1g1")); assertEquals(Piece.WHITE_ROOK, g.board.getPiece(Square.F1)); g.undo(); assertTrue("e1g1" in legal(g))
        assertFalse("e1g1" in legal(game("k4r2/8/8/8/8/8/8/4K2R w K - 0 1")))
        assertFalse("e2d2" in legal(game("4r2k/8/8/8/8/8/4R3/4K3 w - - 0 1")))
        assertFalse("e1e2" in legal(game("8/8/8/8/8/4k3/8/4K3 w - - 0 1")))
    }
    @Test fun enPassant() {
        val g = game("7k/8/8/3pP3/8/8/8/K7 w - d6 0 1")
        assertTrue(g.play("e5d6")); assertEquals(Piece.NONE, g.board.getPiece(Square.D5))
        val pinned = game("4r2k/8/8/3pP3/8/8/8/4K3 w - d6 0 1")
        assertFalse("e5d6" in legal(pinned))
    }
    @Test fun allPromotions() { val g = game("k7/4P3/2K5/8/8/8/8/8 w - - 0 1"); assertEquals(setOf("e7e8q", "e7e8r", "e7e8b", "e7e8n"), legal(g).filter { it.startsWith("e7e8") }.toSet()) }
    @Test fun terminalAndMaterial() {
        assertTrue(game("7k/6Q1/5K2/8/8/8/8/8 b - - 0 1").automaticResult()!!.contains("将杀"))
        assertTrue(game("7k/5K2/6Q1/8/8/8/8/8 b - - 0 1").automaticResult()!!.contains("逼和"))
        assertTrue(ChessGame.materialDead(game("7k/8/8/8/8/8/8/KB6 w - - 0 1").board))
        assertFalse(ChessGame.materialDead(game("6nk/8/8/8/8/8/8/KN6 w - - 0 1").board))
        assertFalse(ChessGame.materialDead(game("7k/8/8/8/8/8/8/KNN5 w - - 0 1").board))
        assertTrue(ChessGame.materialDead(game("7k/8/8/8/8/4b3/8/K1B5 w - - 0 1").board))
    }
    @Test fun repetitionClaimsAndRestore() {
        val g = ChessGame()
        val cycle = listOf("g1f3", "g8f6", "f3g1", "f6g8")
        repeat(2) { cycle.forEach { assertTrue(g.play(it)) } }
        assertNotNull(g.claimReason()); assertNull(g.result)
        val serialized = Gson().toJson(g.record())
        val restored = ChessGame.restore(Gson().fromJson(serialized, GameRecord::class.java))
        assertEquals(g.board.fen, restored.board.fen); assertNotNull(restored.claimReason())
        repeat(2) { cycle.forEach { assertTrue(restored.play(it)) } }
        assertTrue(restored.result!!.contains("五次"))
        restored.undo(); assertNull(restored.result)
    }
    @Test fun fiftySeventyFiveAndMatePriority() {
        val g = game("7k/8/8/8/8/8/8/KR6 w - - 99 1")
        assertNull(g.claimReason()); assertTrue(g.intendedClaims().isNotEmpty()); assertEquals(99, g.board.halfMoveCounter)
        assertTrue(g.play("b1b2")); assertNotNull(g.claimReason()); assertNull(g.result)
        val h = game("7k/8/8/8/8/8/8/KR6 w - - 149 1"); assertTrue(h.play("b1b2")); assertTrue(h.result!!.contains("75"))
        val mate = game("7k/8/5KQ1/8/8/8/8/8 w - - 149 1"); assertTrue(mate.play("g6g7")); assertTrue(mate.result!!.contains("将杀"))
    }
    @Test fun intendedRepetitionClaimDoesNotChangeBoard() {
        val g = ChessGame(); listOf("g1f3","g8f6","f3g1","f6g8","g1f3","g8f6","f3g1").forEach { g.play(it) }
        val before = g.board.fen; val history = g.board.history.toList()
        assertNotNull(g.intendedClaims()["f6g8"]); assertEquals(before, g.board.fen); assertEquals(history, g.board.history.toList())
    }
    @Test fun botLegalAndCancellation() {
        for (level in 0..1) {
            val g = ChessGame()
            repeat(8) { val m = ChessBot(Random(42)).choose(g.board, level, 60)!!; assertTrue(m.uci() in legal(g)); assertTrue(g.play(m.uci())) }
        }
        assertNull(ChessBot().choose(Board(), 1, 50) { true })
        assertNull(ChessBot().choose(game("7k/6Q1/5K2/8/8/8/8/8 b - - 0 1").board, 1))
    }
    @Test fun everyLessonAndBranch() {
        val lessons = LessonCatalog.parse(File("src/main/assets/lessons.json").readText())
        assertEquals(100, lessons.size); assertEquals(100, lessons.map { it.id }.toSet().size)
        lessons.forEach { l ->
            assertTrue(l.hints.size >= 2)
            if (l.question != null) {
                val a = LessonAttempt(l); assertFalse(a.answer("错误")); assertTrue(a.answer(l.answer!!)); assertTrue(a.complete)
            } else {
                assertTrue(l.lines.isNotEmpty())
                l.lines.forEach { line ->
                    val a = LessonAttempt(l) { replies -> line[1].also { assertTrue(it in replies) } }
                    val wrong = a.game.legal().map { it.uci() }.firstOrNull { move -> l.lines.none { it.first() == move } }
                    if (wrong != null) { val before = a.game.board.fen; assertFalse(a.move(wrong)); assertEquals(before, a.game.board.fen) }
                    for (i in line.indices step 2) assertTrue("${l.id}: ${line[i]}", a.move(line[i]))
                    assertTrue(a.complete)
                    if (l.goal.contains("将杀")) assertTrue(a.game.board.isMated)
                }
                if (l.mateIn == 1) {
                    val g = ChessGame(l.fen)
                    val mates = g.legal().filter { m -> g.board.doMove(m); val mate = g.board.isMated; g.board.undoMove(); mate }.map { it.uci() }.toSet()
                    assertEquals(mates, l.lines.map { it.first() }.toSet())
                }
            }
        }
    }
}
