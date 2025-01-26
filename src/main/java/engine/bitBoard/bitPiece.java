package engine.bitBoard;

public class bitPiece {
    
    // Piece Types
    public static final int NONE = 0;
    public static final int PAWN = 1;
    public static final int KNIGHT = 2;
    public static final int BISHOP = 3;
    public static final int ROOK = 4;
    public static final int QUEEN = 5;
    public static final int KING = 6;

    // Piece Colors
    public static final int WHITE = 0;
    public static final int BLACK = 8;

    // Pieces
    public static final int WHITE_PAWN = PAWN | WHITE;      // 1
    public static final int WHITE_KNIGHT = KNIGHT | WHITE;  // 2
    public static final int WHITE_BISHOP = BISHOP | WHITE;  // 3
    public static final int WHITE_ROOK = ROOK | WHITE;      // 4
    public static final int WHITE_QUEEN = QUEEN | WHITE;    // 5
    public static final int WHITE_KING = KING | WHITE;      // 6

    public static final int BLACK_PAWN = PAWN | BLACK;      // 9
    public static final int BLACK_KNIGHT = KNIGHT | BLACK;  // 10
    public static final int BLACK_BISHOP = BISHOP | BLACK;  // 11
    public static final int BLACK_ROOK = ROOK | BLACK;      // 12
    public static final int BLACK_QUEEN = QUEEN | BLACK;    // 13
    public static final int BLACK_KING = KING | BLACK;      // 14

    public static final int MAX_PIECE_INDEX = BLACK_KING;

    // Bit Masks
    private static final int TYPE_MASK = 0b0111;
    private static final int COLOR_MASK = 0b1000;

    // Piece array
    public static final int[] PIECE_INDICES = {
        WHITE_PAWN, WHITE_KNIGHT, WHITE_BISHOP, WHITE_ROOK, WHITE_QUEEN, WHITE_KING,
        BLACK_PAWN, BLACK_KNIGHT, BLACK_BISHOP, BLACK_ROOK, BLACK_QUEEN, BLACK_KING
    };

    // Create a piece from type and color
    public static int makePiece(int pieceType, int pieceColor) {
        return pieceType | pieceColor;
    }

    public static int makePiece(int pieceType, boolean isWhite) {
        return makePiece(pieceType, isWhite ? WHITE : BLACK);
    }

    // Check if the piece matches a given color
    public static boolean isColor(int piece, int color) {
        return (piece & COLOR_MASK) == color && piece != NONE;
    }

    // Check if the piece is white
    public static boolean isWhite(int piece) {
        return isColor(piece, WHITE);
    }

    // Get piece color
    public static int pieceColor(int piece) {
        return piece & COLOR_MASK;
    }

    // Get piece type (without color)
    public static int pieceType(int piece) {
        return piece & TYPE_MASK;
    }

    // Check if the piece is a Rook or Queen
    public static boolean isOrthogonalSlider(int piece) {
        int type = pieceType(piece);
        return type == QUEEN || type == ROOK;
    }

    // Check if the piece is a Bishop or Queen
    public static boolean isDiagonalSlider(int piece) {
        int type = pieceType(piece);
        return type == QUEEN || type == BISHOP;
    }

    // Check if the piece is a sliding piece (Rook, Bishop, or Queen)
    public static boolean isSlidingPiece(int piece) {
        int type = pieceType(piece);
        return type == QUEEN || type == BISHOP || type == ROOK;
    }

    // Get the symbol for a piece
    public static char getSymbol(int piece) {
        int type = pieceType(piece);
        char symbol = switch (type) {
            case ROOK -> 'R';
            case KNIGHT -> 'N';
            case BISHOP -> 'B';
            case QUEEN -> 'Q';
            case KING -> 'K';
            case PAWN -> 'P';
            default -> ' ';
        };
        return isWhite(piece) ? symbol : Character.toLowerCase(symbol);
    }

    // Get the piece type from a symbol
    public static int getPieceTypeFromSymbol(char symbol) {
        symbol = Character.toUpperCase(symbol);
        return switch (symbol) {
            case 'R' -> ROOK;
            case 'N' -> KNIGHT;
            case 'B' -> BISHOP;
            case 'Q' -> QUEEN;
            case 'K' -> KING;
            case 'P' -> PAWN;
            default -> NONE;
        };
    }
}
