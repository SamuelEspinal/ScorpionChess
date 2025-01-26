package engine.bitBoard;

public final class bitGameState {
    final int capturedPieceType;
    public final int enPassantFile;
    public final int castlingRights;
    public final int fiftyMoveCounter;
    final long zobristKey;

    public static final int CLEAR_WHITE_KINGSIDE_MASK = 0b1110;
    public static final int CLEAR_WHITE_QUEENSIDE_MASK = 0b1101;
    public static final int CLEAR_BLACK_KINGSIDE_MASK = 0b1011;
    public static final int CLEAR_BLACK_QUEENSIDE_MASK = 0b0111;

    public bitGameState(int capturedPieceType, int enPassantFile, int castlingRights, int fiftyMoveCounter, long zobristKey) {
        this.capturedPieceType = capturedPieceType;
        this.enPassantFile = enPassantFile;
        this.castlingRights = castlingRights;
        this.fiftyMoveCounter = fiftyMoveCounter;
        this.zobristKey = zobristKey;
    }

    public int getCapturedPieceType() {
        return capturedPieceType;
    }

    public int getEnPassantFile() {
        return enPassantFile;
    }

    public int getCastlingRights() {
        return castlingRights;
    }

    public int getFiftyMoveCounter() {
        return fiftyMoveCounter;
    }

    public long getZobristKey() {
        return zobristKey;
    }

    public boolean hasKingsideCastleRight(boolean white) {
        int mask = white ? 1 : 4;
        return (castlingRights & mask) != 0;
    }

    public boolean hasQueensideCastleRight(boolean white) {
        int mask = white ? 2 : 8;
        return (castlingRights & mask) != 0;
    }
}

