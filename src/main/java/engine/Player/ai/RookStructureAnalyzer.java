package engine.Player.ai;

import static engine.pieces.Piece.PieceType.ROOK;

import java.util.Collection;
import java.util.stream.Collectors;

import engine.Player.Player;
import engine.pieces.Piece;
import engine.board.Board;

public final class RookStructureAnalyzer {

    private static final RookStructureAnalyzer INSTANCE = new RookStructureAnalyzer();
    private static final int OPEN_COLUMN_ROOK_BONUS = 45;
    private static final int SEMI_OPEN_COLUMN_ROOK_BONUS = 20;
    private static final int CONNECTED_ROOK_BONUS = 20;
    private static final int NO_BONUS = 0;

    private RookStructureAnalyzer() {
    }

    public static RookStructureAnalyzer getInstance() {
        return INSTANCE;
    }

    public int rookStructureScore(final Player player, final Board board) {
        final Collection<Piece> playerRooks = getPlayerRooks(player);

        if (playerRooks.isEmpty()) {
            return NO_BONUS; // no rooks, no bonus
        }

        final int[] piecesOnColumns = getPiecesOnColumns(board);
        return calculateRookBonus(playerRooks, piecesOnColumns);
    }

    private Collection<Piece> getPlayerRooks(final Player player) {
        return player.getActivePieces().stream()
                .filter(piece -> piece.getPieceType() == ROOK)
                .collect(Collectors.toList());
    }

    private int[] getPiecesOnColumns(final Board board) {
        final int[] piecesOnColumns = new int[8]; // 8 columns

        for (final Piece piece : board.getAllPieces()) {
            int column = piece.getPiecePosition() % 8;
            piecesOnColumns[column]++;
        }
        return piecesOnColumns;
    }

    private int calculateRookBonus(final Collection<Piece> rooks, final int[] piecesOnColumns) {
        int bonus = NO_BONUS;

        for (final Piece rook : rooks) {
            int column = rook.getPiecePosition() % 8;
            if (piecesOnColumns[column] == 1) { // Rook is alone on the file
                bonus += OPEN_COLUMN_ROOK_BONUS;
            } else if (piecesOnColumns[column] == 2) { // Semi-open file (only one opposing piece)
                bonus += SEMI_OPEN_COLUMN_ROOK_BONUS;
            }
            bonus += calculateConnectedRookBonus(rooks, rook);
        }
        return bonus;
    }

    private int calculateConnectedRookBonus(final Collection<Piece> rooks, final Piece rook) {
        for (final Piece otherRook : rooks) {
            if (otherRook != rook) {
                if (isConnectedHorizontally(rook, otherRook) || isConnectedVertically(rook, otherRook)) {
                    return CONNECTED_ROOK_BONUS;
                }
            }
        }
        return NO_BONUS;
    }

    private boolean isConnectedHorizontally(final Piece rook1, final Piece rook2) {
        return rook1.getPiecePosition() / 8 == rook2.getPiecePosition() / 8; 
    }

    private boolean isConnectedVertically(final Piece rook1, final Piece rook2) {
        return rook1.getPiecePosition() % 8 == rook2.getPiecePosition() % 8;
    }
}

