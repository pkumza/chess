package cn.parentchess

import com.github.bhlangonijr.chesslib.*
import org.junit.Assert.*
import org.junit.Test

class MoveVisualTest {
    @Test fun rejectedCapturePreviewsWithoutChangingLessonAndCanBeRetried() {
        val lesson = LessonCatalog.parse(java.io.File("src/main/assets/lessons.json").readText())
            .first { it.question == null && it.lines.isNotEmpty() }
        val attempt = LessonAttempt(lesson)
        val wrong = attempt.game.legal().first { move -> lesson.lines.none { it.first() == move.uci() } }
        val before = attempt.game.board.fen
        assertFalse(attempt.move(wrong.uci()))
        val preview = MoveVisual.rejected(8, attempt.game.board, wrong)
        assertTrue(preview.retry); assertFalse(preview.blocked)
        assertEquals(wrong.to, preview.travels.first().to)
        assertEquals(before, attempt.game.board.fen)
        assertTrue(attempt.played.isEmpty()); assertFalse(attempt.complete)
        assertTrue(attempt.move(lesson.lines.first().first()))

        val captureBoard = ChessGame("7k/8/8/3p4/4P3/8/8/K7 w - - 0 1").board
        val capture = MoveVisual.rejected(9, captureBoard, com.github.bhlangonijr.chesslib.move.Move(Square.E4, Square.D5))
        assertTrue(capture.retry); assertTrue(capture.capture)
        assertEquals(Piece.BLACK_PAWN, captureBoard.getPiece(Square.D5))
    }
    @Test fun blockedAttemptDoesNotInventCaptureOrMoveTheBoard() {
        val board = ChessGame().board
        val before = board.fen
        val f = MoveVisual.rejected(10, board, com.github.bhlangonijr.chesslib.move.Move(Square.E2, Square.F4))
        assertTrue(f.retry); assertTrue(f.blocked); assertFalse(f.capture)
        assertEquals(before, board.fen)
        assertEquals(listOf(PieceTravel(Piece.WHITE_PAWN, Square.E2, Square.F4)), f.travels)
    }
    private fun frame(fen: String, uci: String): MoveVisual {
        val game = ChessGame(fen)
        val before = game.board.fen
        val frame = MoveVisual.create(7,game.board,game.legal().first { it.uci() == uci })
        assertEquals(before,game.board.fen)
        assertEquals(64,frame.before.size)
        return frame
    }
    @Test fun ordinaryMoveHasNoCaptureAndKeepsOriginalPiece() {
        val f=frame(START_FEN,"e2e4")
        assertFalse(f.capture);assertEquals(listOf(PieceTravel(Piece.WHITE_PAWN,Square.E2,Square.E4)),f.travels)
    }
    @Test fun captureRetainsVictimUntilContact() {
        val f=frame("7k/8/8/3p4/4P3/8/8/K7 w - - 0 1","e4d5")
        assertTrue(f.capture);assertEquals(Square.D5,f.capturedSquare);assertEquals(Piece.BLACK_PAWN,f.before[Square.D5.ordinal])
    }
    @Test fun enPassantShattersPawnBehindDestinationForBothSides() {
        val w=frame("7k/8/8/3pP3/8/8/8/K7 w - d6 0 1","e5d6")
        assertEquals(Square.D5,w.capturedSquare);assertEquals(Square.D6,w.travels.single().to)
        val b=frame("7k/8/8/8/3Pp3/8/8/K7 b - d3 0 1","e4d3")
        assertEquals(Square.D4,b.capturedSquare)
    }
    @Test fun castlingAnimatesKingAndRookTogether() {
        val w=frame("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","e1g1")
        assertEquals(2,w.travels.size);assertEquals(PieceTravel(Piece.WHITE_ROOK,Square.H1,Square.F1),w.travels[1]);assertFalse(w.capture)
        val b=frame("r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 0 1","e8c8")
        assertEquals(PieceTravel(Piece.BLACK_ROOK,Square.A8,Square.D8),b.travels[1])
    }
    @Test fun promotionMovesPawnBeforeReplacingWithChosenPiece() {
        val f=frame("k6r/6P1/2K5/8/8/8/8/8 w - - 0 1","g7h8n")
        assertEquals(Piece.WHITE_PAWN,f.travels.single().piece);assertTrue(f.capture)
        assertEquals(Piece.BLACK_ROOK,f.before[Square.H8.ordinal])
    }
}
