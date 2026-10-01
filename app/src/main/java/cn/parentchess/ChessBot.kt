package cn.parentchess

import com.github.bhlangonijr.chesslib.*
import com.github.bhlangonijr.chesslib.move.Move
import java.util.concurrent.CancellationException
import kotlin.math.abs
import kotlin.random.Random

class ChessBot(private val random: Random = Random.Default) {
    private var deadline = 0L
    private var cancel: () -> Boolean = { false }
    private fun checkTime() { if (cancel() || System.nanoTime() >= deadline) throw CancellationException() }
    fun choose(source: Board, level: Int, budgetMs: Long = 650, cancelled: () -> Boolean = { false }): Move? {
        val b = source.clone()
        val legal = b.legalMoves()
        if (legal.isEmpty() || ChessGame.materialDead(b) || b.isRepetition(5) || b.halfMoveCounter >= 150) return null
        deadline = System.nanoTime() + budgetMs * 1_000_000
        cancel = cancelled
        var scores = legal.map { it to 0 }
        for (depth in 1..if (level == 0) 2 else 4) {
            try {
                val iteration = legal.map { m ->
                    checkTime(); b.doMove(m)
                    val score = try { -search(b, depth - 1, -1000000, 1000000, 1) } finally { b.undoMove() }
                    m to score
                }
                scores = iteration
            } catch (_: CancellationException) { break }
        }
        if (cancelled()) return null
        val best = scores.maxOf { it.second }
        val candidates = if (level == 0) scores.filter { it.second >= best - 180 } else scores.filter { it.second == best }
        return candidates.random(random).first
    }
    private fun search(b: Board, depth: Int, low: Int, high: Int, ply: Int): Int {
        checkTime()
        val moves = b.legalMoves()
        if (moves.isEmpty()) return if (b.isKingAttacked) -100000 + ply else 0
        if (ChessGame.materialDead(b) || b.isRepetition(3) || b.halfMoveCounter >= 100) return 0
        if (depth == 0) return evaluate(b)
        var alpha = low
        for (m in moves.sortedByDescending { value(b.getPiece(it.to).pieceType) + value(it.promotion.pieceType) }) {
            b.doMove(m)
            val score = try { -search(b, depth - 1, -high, -alpha, ply + 1) } finally { b.undoMove() }
            if (score >= high) return high
            if (score > alpha) alpha = score
        }
        return alpha
    }
    private fun value(t: PieceType?) = when(t) {
        PieceType.PAWN -> 100; PieceType.KNIGHT -> 320; PieceType.BISHOP -> 330
        PieceType.ROOK -> 500; PieceType.QUEEN -> 900; else -> 0
    }
    private fun evaluate(b: Board): Int {
        var score = 0
        for (sq in Square.entries.take(64)) {
            val p = b.getPiece(sq)
            if (p == Piece.NONE) continue
            val rank = if (p.pieceSide == Side.WHITE) sq.ordinal / 8 else 7 - sq.ordinal / 8
            val center = (7 - abs(2 * (sq.ordinal % 8) - 7) - abs(2 * (sq.ordinal / 8) - 7)) * 3
            val position = when(p.pieceType) {
                PieceType.PAWN -> rank * 9
                PieceType.KNIGHT, PieceType.BISHOP -> center * 3
                PieceType.KING -> if (b.squareAttackedBy(sq, p.pieceSide.flip()) != 0L) -45 else 0
                else -> center
            }
            score += (value(p.pieceType) + position) * if (p.pieceSide == b.sideToMove) 1 else -1
        }
        return score
    }
}
