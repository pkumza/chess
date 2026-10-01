package cn.parentchess

import com.github.bhlangonijr.chesslib.*
import com.github.bhlangonijr.chesslib.move.Move

const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
fun Side.chinese() = if (this == Side.WHITE) "白方" else "黑方"
fun Piece.chinese(): String = when(pieceType) {
    PieceType.KING -> "王"; PieceType.QUEEN -> "后"; PieceType.ROOK -> "车"
    PieceType.BISHOP -> "象"; PieceType.KNIGHT -> "马"; PieceType.PAWN -> "兵"; else -> ""
}
fun Move.uci() = toString().lowercase()

data class GameRecord(val initialFen: String = START_FEN, val moves: List<String> = emptyList(), val result: String? = null)

class ChessGame(val initialFen: String = START_FEN) {
    val board = Board().apply { loadFromFen(initialFen) }
    val moves = mutableListOf<String>()
    var result: String? = null
    fun legal(): List<Move> = board.legalMoves()
    fun play(uci: String): Boolean {
        if (result != null) return false
        val move = legal().firstOrNull { it.uci() == uci.lowercase() } ?: return false
        check(board.doMove(move))
        moves.add(move.uci())
        result = automaticResult()
        return true
    }
    fun undo() {
        if (moves.isNotEmpty()) { board.undoMove(); moves.removeAt(moves.lastIndex) }
        result = automaticResult()
    }
    fun record() = GameRecord(initialFen, moves.toList(), result)
    fun automaticResult(): String? {
        val options = legal()
        if (options.isEmpty()) return if (board.isKingAttacked) "${board.sideToMove.flip().chinese()}获胜 · 将杀" else "和棋 · 逼和（没有合法走法）"
        if (materialDead(board)) return "和棋 · 子力不足，无法将杀"
        if (board.isRepetition(5)) return "和棋 · 五次重复局面"
        if (board.halfMoveCounter >= 150) return "和棋 · 75 回合未吃子或动兵"
        return null
    }
    fun claimReason(): String? = when {
        result != null -> null
        board.isRepetition(3) -> "三次重复局面"
        board.halfMoveCounter >= 100 -> "50 回合未吃子或动兵"
        else -> null
    }
    // A player may claim before an intended move that would create the draw condition.
    fun intendedClaims(): Map<String, String> {
        if (result != null) return emptyMap()
        val claims = linkedMapOf<String, String>()
        for (m in legal()) {
            board.doMove(m)
            val reason = when {
                board.isRepetition(3) -> "三次重复局面"
                board.halfMoveCounter >= 100 -> "50 回合未吃子或动兵"
                else -> null
            }
            board.undoMove()
            if (reason != null) claims[m.uci()] = reason
        }
        return claims
    }
    companion object {
        fun restore(record: GameRecord): ChessGame = ChessGame(record.initialFen).apply {
            record.moves.forEach { require(play(it)) { "存档含非法走子：$it" } }
            if (result == null) result = record.result
        }
        // Conservative exact material cases. In particular K+N vs K+N and K+NN vs K
        // are NOT dead: a legal cooperating mating sequence exists.
        fun materialDead(b: Board): Boolean {
            val pieces = Square.entries.take(64).map { it to b.getPiece(it) }
                .filter { it.second != Piece.NONE && it.second.pieceType != PieceType.KING }
            if (pieces.isEmpty()) return true
            if (pieces.size == 1) return pieces[0].second.pieceType in listOf(PieceType.BISHOP, PieceType.KNIGHT)
            if (pieces.all { it.second.pieceType == PieceType.BISHOP }) {
                return pieces.map { (sq, _) -> (sq.ordinal / 8 + sq.ordinal % 8) % 2 }.distinct().size == 1
            }
            return false
        }
    }
}
