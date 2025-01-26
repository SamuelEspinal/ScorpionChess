package engine.bitBoard;

public class bitMove {
    // 16-bit move value
    private final short moveValue;

    // Flags
    public static final int NoFlag = 0b0000;
    public static final int EnPassantCaptureFlag = 0b0001;
    public static final int CastleFlag = 0b0010;
    public static final int PawnTwoUpFlag = 0b0011;

    public static final int PromoteToQueenFlag = 0b0100;
    public static final int PromoteToKnightFlag = 0b0101;
    public static final int PromoteToRookFlag = 0b0110;
    public static final int PromoteToBishopFlag = 0b0111;

    // Masks
    private static final short startSquareMask = 0b0000000000111111;
    private static final short targetSquareMask = 0b0000111111000000;
    private static final short flagMask = (short) 0b1111000000000000;

    public bitMove() {
        this.moveValue = 0;
    }
    // Constructor using short move value
    public bitMove(short moveValue) {
        this.moveValue = moveValue;
    }

    // Constructor using start square and target square
    public bitMove(int startSquare, int targetSquare) {
        this.moveValue = (short) (startSquare | (targetSquare << 6));
    }

    // Constructor using start square, target square, and flag
    public bitMove(int startSquare, int targetSquare, int flag) {
        this.moveValue = (short) (startSquare | (targetSquare << 6) | (flag << 12));
    }

    // Getter for moveValue
    public short getValue() {
        return moveValue;
    }

    // Check if move is null
    public boolean isNull() {
        return moveValue == 0;
    }

    // Get start square
    public int getStartSquare() {
        return moveValue & startSquareMask;
    }

    // Get target square
    public int getTargetSquare() {
        return (moveValue & targetSquareMask) >> 6;
    }

    // Check if the move is a promotion
    public boolean isPromotion() {
        return getMoveFlag() >= PromoteToQueenFlag;
    }

    // Get move flag
    public int getMoveFlag() {
        return (moveValue & flagMask) >> 12;
    }

    // Get the promotion piece type
    public int getPromotionPieceType() {
        switch (getMoveFlag()) {
            case PromoteToRookFlag:
                return bitPiece.ROOK;
            case PromoteToKnightFlag:
                return bitPiece.KNIGHT;
            case PromoteToBishopFlag:
                return bitPiece.BISHOP;
            case PromoteToQueenFlag:
                return bitPiece.QUEEN;
            default:
                return bitPiece.NONE;
        }
    }

    // Null move (equivalent to `Move.NullMove` in C#)
    public static bitMove nullMove() {
        return new bitMove((short) 0);
    }

    // Check if two moves are the same
    public static boolean sameMove(bitMove a, bitMove b) {
        return a.moveValue == b.moveValue;
    }
}
