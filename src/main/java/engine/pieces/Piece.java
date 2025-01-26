package engine.pieces;
import java.util.Collection;

import engine.Alliance;
import engine.board.Board;
import engine.board.Move;

public abstract class Piece {
    
    public static final String Knight = null;
    protected final PieceType pieceType;
    protected final int piecePosition;
    protected final Alliance pieceAlliance;
    protected final boolean isFirstMove;
    private final int cachedHashCode;

    Piece(final PieceType pieceType, 
          final Alliance pieceAlliance, 
          final int piecePosition,
          final boolean isFirstMove) {

        this.pieceType = pieceType;
        this.pieceAlliance = pieceAlliance;
        this.piecePosition = piecePosition;
        this.isFirstMove = isFirstMove;
        this.cachedHashCode = computeHashCode();
    }

    private int computeHashCode() {
        int result = pieceType.hashCode();
        result = 31 * result + pieceAlliance.hashCode();
        result = 31 * result + piecePosition;
        result = 31 * result + (isFirstMove ? 1 : 0);
        return result;
    }

    @Override
    public boolean equals(final Object other) {
        if(this == other) {
            return true;
        }
        if(!(other instanceof Piece)) {
            return false;
        }
        final Piece otherPiece = (Piece) other;
        return piecePosition == otherPiece.getPiecePosition() && 
               pieceType == otherPiece.getPieceType() && 
               pieceAlliance == otherPiece.getPieceAlliance() &&
               isFirstMove == otherPiece.isFirstMove();
    }

    @Override
    public int hashCode() {
        return this.cachedHashCode;
    }

    public int getPiecePosition() {
        return this.piecePosition;
    }

    public Alliance getPieceAlliance() {
        return this.pieceAlliance;
    }

    public boolean isFirstMove() {
        return this.isFirstMove;
    }

    public PieceType getPieceType() {  
        return this.pieceType;
    }

    public int getPieceValue() {
        return this.pieceType.getPieceValue();
    }

    public abstract int locationBonus();

    public abstract Piece movePiece(final Move move);

    public abstract Collection<Move> calculateLegalMoves(final Board board);

    public enum PieceType {
       
        PAWN(100, "P"),
        KNIGHT(300, "N"), 
        BISHOP(300, "B"),
        ROOK(500, "R"),
        QUEEN(900, "Q"),
        KING(10000, "K");

        private int pieceValue;
        private String pieceName;

        public int getPieceValue() {
            return this.pieceValue;
        }

        @Override
        public String toString() {
            return this.pieceName;
        }

        PieceType(final int pieceValue, final String pieceName) {
            this.pieceValue = pieceValue;
            this.pieceName = pieceName;
        }
    }
}
