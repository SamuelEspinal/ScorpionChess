//import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import engine.Alliance;
import engine.Player.MoveTransition;
import engine.board.Board;
import engine.board.Board.Builder;
import engine.board.BoardUtils;
import engine.board.Move;
import engine.board.Move.MoveFactory;
import engine.pieces.Bishop;
import engine.pieces.King;
import engine.pieces.Rook;

public class TestPlayer {

//    @Test
//    public void testSimpleEvaluation() {
//        final Board board = Board.createStandardBoard();
//        final MoveTransition t1 = board.currentPlayer()
//                .makeMove(MoveFactory.createMove(board, BoardUtils.INSTANCE.getCoordinateAtPosition("e2"),
//                                BoardUtils.INSTANCE.getCoordinateAtPosition("e4")));
//        assertTrue(t1.getMoveStatus().isDone());
//        final MoveTransition t2 = t1.getToBoard()
//                .currentPlayer()
//                .makeMove(MoveFactory.createMove(t1.getToBoard(), BoardUtils.INSTANCE.getCoordinateAtPosition("e7"),
//                        BoardUtils.INSTANCE.getCoordinateAtPosition("e5")));
//        assertTrue(t2.getMoveStatus().isDone());
//        assertEquals(StandardBoardEvaluator.get().evaluate(t2.getToBoard(), 0), 0);
//    }

    @Test
    public void testBug() {
        final Board board = Board.createStandardBoard();
        final MoveTransition t1 = board.currentPlayer()
                .makeMove(MoveFactory.createMove(board, BoardUtils.INSTANCE.getCoordinateAtPosition("c2"),
                                BoardUtils.INSTANCE.getCoordinateAtPosition("c3")));
        assertTrue(t1.getMoveStatus().isDone());
        final MoveTransition t2 = t1.getToBoard()
                .currentPlayer()
                .makeMove(MoveFactory.createMove(t1.getToBoard(), BoardUtils.INSTANCE.getCoordinateAtPosition("b8"),
                        BoardUtils.INSTANCE.getCoordinateAtPosition("a6")));
        assertTrue(t2.getMoveStatus().isDone());
        final MoveTransition t3 = t2.getToBoard()
                .currentPlayer()
                .makeMove(MoveFactory.createMove(t2.getToBoard(), BoardUtils.INSTANCE.getCoordinateAtPosition("d1"),
                        BoardUtils.INSTANCE.getCoordinateAtPosition("a4")));
        assertTrue(t3.getMoveStatus().isDone());
        final MoveTransition t4 = t3.getToBoard()
                .currentPlayer()
                .makeMove(MoveFactory.createMove(t3.getToBoard(), BoardUtils.INSTANCE.getCoordinateAtPosition("d7"),
                        BoardUtils.INSTANCE.getCoordinateAtPosition("d6")));
        assertFalse(t4.getMoveStatus().isDone());
    }

    @Test
    public void testDiscoveredCheck() {
        final Builder builder = new Builder();
        // Black Layout
        builder.setPiece(new King(Alliance.BLACK, 4, false, false));
        builder.setPiece(new Rook(Alliance.BLACK, 24));
        // White Layout
        builder.setPiece(new Bishop(Alliance.WHITE, 44));
        builder.setPiece(new Rook(Alliance.WHITE, 52));
        builder.setPiece(new King(Alliance.WHITE, 58, false, false));
        // Set the current player
        builder.setMoveMaker(Alliance.WHITE);
        final Board board = builder.build();
        final MoveTransition t1 = board.currentPlayer()
                .makeMove(MoveFactory.createMove(board, BoardUtils.INSTANCE.getCoordinateAtPosition("e3"),
                                BoardUtils.INSTANCE.getCoordinateAtPosition("b6")));
        assertTrue(t1.getMoveStatus().isDone());
        assertTrue(t1.getToBoard().currentPlayer().isInCheck());
        final MoveTransition t2 = t1.getToBoard()
                .currentPlayer()
                .makeMove(MoveFactory.createMove(t1.getToBoard(), BoardUtils.INSTANCE.getCoordinateAtPosition("a5"),
                        BoardUtils.INSTANCE.getCoordinateAtPosition("b5")));
        assertFalse(t2.getMoveStatus().isDone());
        final MoveTransition t3 = t1.getToBoard()
                .currentPlayer()
                .makeMove(MoveFactory.createMove(t1.getToBoard(), BoardUtils.INSTANCE.getCoordinateAtPosition("a5"),
                        BoardUtils.INSTANCE.getCoordinateAtPosition("e5")));
        assertTrue(t3.getMoveStatus().isDone());
    }

//    @Test
//    public void testUnmakeMove() {
//        final Board board = Board.createStandardBoard();
//        final Move m1 = MoveFactory.createMove(board, BoardUtils.INSTANCE.getCoordinateAtPosition("e2"),
//                BoardUtils.INSTANCE.getCoordinateAtPosition("e4"));
//        final MoveTransition t1 = board.currentPlayer()
//                .makeMove(m1);
//        assertTrue(t1.getMoveStatus().isDone());
//        t1.getToBoard().currentPlayer().getOpponent().unMakeMove(m1);
//    }

    @Test
    public void testIllegalMove() {
        final Board board = Board.createStandardBoard();
        final Move m1 = MoveFactory.createMove(board, BoardUtils.INSTANCE.getCoordinateAtPosition("e2"),
                BoardUtils.INSTANCE.getCoordinateAtPosition("e6"));
        final MoveTransition t1 = board.currentPlayer()
                .makeMove(m1);
        assertFalse(t1.getMoveStatus().isDone());
    }

}
