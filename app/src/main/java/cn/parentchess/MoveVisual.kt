package cn.parentchess

import com.github.bhlangonijr.chesslib.*
import com.github.bhlangonijr.chesslib.move.Move
import kotlin.math.abs

data class PieceTravel(val piece: Piece, val from: Square, val to: Square)
data class MoveVisual(val id: Long, val move: String, val before: List<Piece>, val travels: List<PieceTravel>, val capturedSquare: Square?, val retry: Boolean = false, val blocked: Boolean = false) {
    val capture get() = capturedSquare != null
    companion object {
        /** Preview only: the lesson position and progress are never changed. */
        fun rejected(id: Long, board: Board, move: Move): MoveVisual =
            if (move in board.legalMoves()) create(id, board, move).copy(retry = true)
            else MoveVisual(id, move.uci(), Square.entries.take(64).map { board.getPiece(it) },
                listOf(PieceTravel(board.getPiece(move.from), move.from, move.to)), null, retry = true, blocked = true)

        /** Called before applying a legal move; also handles castling and en passant. */
        fun create(id: Long, board: Board, move: Move): MoveVisual {
            val piece = board.getPiece(move.from)
            val before = Square.entries.take(64).map { board.getPiece(it) }
            val captured = when {
                board.getPiece(move.to) != Piece.NONE -> move.to
                piece.pieceType == PieceType.PAWN && move.from.ordinal % 8 != move.to.ordinal % 8 -> Square.entries[move.to.ordinal + if (piece.pieceSide == Side.WHITE) -8 else 8]
                else -> null
            }
            val travels = mutableListOf(PieceTravel(piece, move.from, move.to))
            if (piece.pieceType == PieceType.KING && abs(move.from.ordinal - move.to.ordinal) == 2) {
                val rank = move.from.ordinal / 8 * 8
                val short = move.to.ordinal > move.from.ordinal
                val rookFrom = Square.entries[rank + if (short) 7 else 0]
                val rookTo = Square.entries[rank + if (short) 5 else 3]
                travels += PieceTravel(board.getPiece(rookFrom), rookFrom, rookTo)
            }
            return MoveVisual(id, move.uci(), before, travels, captured)
        }
    }
}
