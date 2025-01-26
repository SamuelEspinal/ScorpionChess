package engine.bitBoard.helpers;

import engine.bitBoard.Coord;
import engine.bitBoard.bitBoard;
import engine.bitBoard.bitPiece;

public final class BoardHelper {
    
    public static final Coord[] ROOK_DIRECTIONS = {
        new Coord(-1, 0), new Coord(1, 0), new Coord(0, 1), new Coord(0, -1)
    };
    
    public static final Coord[] BISHOP_DIRECTIONS = {
        new Coord(-1, 1), new Coord(1, 1), new Coord(1, -1), new Coord(-1, -1)
    };

    public static final String FILE_NAMES = "abcdefgh";
    public static final String RANK_NAMES = "12345678";

    public static final int A1 = 0;
    public static final int B1 = 1;
    public static final int C1 = 2;
    public static final int D1 = 3;
    public static final int E1 = 4;
    public static final int F1 = 5;
    public static final int G1 = 6;
    public static final int H1 = 7;

    public static final int A8 = 56;
    public static final int B8 = 57;
    public static final int C8 = 58;
    public static final int D8 = 59;
    public static final int E8 = 60;
    public static final int F8 = 61;
    public static final int G8 = 62;
    public static final int H8 = 63;

    // Rank (0 to 7) of square 
    public static int rankIndex(int squareIndex) {
        return squareIndex >> 3;
    }

    // File (0 to 7) of square 
    public static int fileIndex(int squareIndex) {
        return squareIndex & 0b000111;
    }

    public static int indexFromCoord(int fileIndex, int rankIndex) {
        return rankIndex * 8 + fileIndex;
    }

    public static int indexFromCoord(Coord coord) {
        return indexFromCoord(coord.getFileIndex(), coord.getRankIndex());
    }

    public static Coord coordFromIndex(int squareIndex) {
        return new Coord(fileIndex(squareIndex), rankIndex(squareIndex));
    }

    public static boolean isLightSquare(int fileIndex, int rankIndex) {
        return (fileIndex + rankIndex) % 2 != 0;
    }

    public static boolean isLightSquare(int squareIndex) {
        return isLightSquare(fileIndex(squareIndex), rankIndex(squareIndex));
    }

    public static String squareNameFromCoordinate(int fileIndex, int rankIndex) {
        return FILE_NAMES.charAt(fileIndex) + "" + (rankIndex + 1);
    }

    public static String squareNameFromIndex(int squareIndex) {
        return squareNameFromCoordinate(coordFromIndex(squareIndex));
    }

    public static String squareNameFromCoordinate(Coord coord) {
        return squareNameFromCoordinate(coord.getFileIndex(), coord.getRankIndex());
    }

    public static int squareIndexFromName(String name) {
        char fileName = name.charAt(0);
        char rankName = name.charAt(1);
        int fileIndex = FILE_NAMES.indexOf(fileName);
        int rankIndex = RANK_NAMES.indexOf(rankName);
        return indexFromCoord(fileIndex, rankIndex);
    }

    public static boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x < 8 && y >= 0 && y < 8;
    }

    /**
     * Creates an ASCII diagram of the current position.
     */
    public static String createDiagram(bitBoard board, boolean blackAtTop, boolean includeFen, boolean includeZobristKey) {
        StringBuilder result = new StringBuilder();
        int lastMoveSquare = board.allGameMoves.size() > 0 ? 
            board.allGameMoves.get(board.allGameMoves.size() - 1).getTargetSquare() : -1;

        for (int y = 0; y < 8; y++) {
            int rankIndex = blackAtTop ? 7 - y : y;
            result.append("+---+---+---+---+---+---+---+---+\n");

            for (int x = 0; x < 8; x++) {
                int fileIndex = blackAtTop ? x : 7 - x;
                int squareIndex = indexFromCoord(fileIndex, rankIndex);
                boolean highlight = squareIndex == lastMoveSquare;
                int piece = board.square[squareIndex];

                if (highlight) {
                    result.append("|(").append(bitPiece.getSymbol(piece)).append(")");
                } else {
                    result.append("| ").append(bitPiece.getSymbol(piece)).append(" ");
                }

                if (x == 7) {
                    // Show rank number
                    result.append("| ").append(rankIndex + 1).append("\n");
                }
            }

            if (y == 7) {
                // Show file names
                result.append("+---+---+---+---+---+---+---+---+\n");
                String fileNames = "  a   b   c   d   e   f   g   h  ";
                String fileNamesRev = "  h   g   f   e   d   c   b   a  ";
                result.append(blackAtTop ? fileNames : fileNamesRev).append("\n");

                if (includeFen) {
                    result.append("Fen         : ").append(FenUtility.currentFen(board, true)).append("\n");
                }
                if (includeZobristKey) {
                    result.append("Zobrist Key : ").append(board.zobristKey).append("\n");
                }
            }
        }

        return result.toString();
    }
}

