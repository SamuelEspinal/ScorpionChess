package engine.bitBoard.moveGeneration.bitBoardsUtil;

public class BitBoardUtility {
    public static final long FILE_A = 0x101010101010101L;

    public static final long RANK_1 = 0xFFL;
    public static final long RANK_2 = RANK_1 << 8;
    public static final long RANK_3 = RANK_2 << 8;
    public static final long RANK_4 = RANK_3 << 8;
    public static final long RANK_5 = RANK_4 << 8;
    public static final long RANK_6 = RANK_5 << 8;
    public static final long RANK_7 = RANK_6 << 8;
    public static final long RANK_8 = RANK_7 << 8;

    public static final long NOT_A_FILE = ~FILE_A;
    public static final long NOT_H_FILE = ~(FILE_A << 7);

    public static final long[] KNIGHT_ATTACKS = new long[64];
    public static final long[] KING_MOVES = new long[64];
    public static final long[] WHITE_PAWN_ATTACKS = new long[64];
    public static final long[] BLACK_PAWN_ATTACKS = new long[64];

    // Get index of least significant set bit in given 64bit value. Also clears the bit to zero.
    public static int popLSB(long[] bitboard) {
        int i = Long.numberOfTrailingZeros(bitboard[0]);
        bitboard[0] &= bitboard[0] - 1;
        return i;
    }

    public static void setSquare(long bitboard, int squareIndex) {
        bitboard |= 1L << squareIndex;
    }

    public static void clearSquare(long bitboard, int squareIndex) {
        bitboard &= ~(1L << squareIndex);
    }

    public static void toggleSquare(long bitboard, int squareIndex) {
        bitboard ^= 1L << squareIndex;
    }

    public static void toggleSquares(long bitboard, int squareA, int squareB) {
        bitboard ^= (1L << squareA) | (1L << squareB);
    }

    public static boolean containsSquare(long bitboard, int square) {
        return ((bitboard >> square) & 1) != 0;
    }

    public static long pawnAttacks(long pawnBitboard, boolean isWhite) {
        // Pawn attacks are calculated like so: (example given with white to move)

			// The first half of the attacks are calculated by shifting all pawns north-east: northEastAttacks = pawnBitboard << 9
			// Note that pawns on the h file will be wrapped around to the a file, so then mask out the a file: northEastAttacks &= notAFile
			// (Any pawns that were originally on the a file will have been shifted to the b file, so a file should be empty).

			// The other half of the attacks are calculated by shifting all pawns north-west. This time the h file must be masked out.
			// Combine the two halves to get a bitboard with all the pawn attacks: northEastAttacks | northWestAttacks
        if (isWhite) {
            return ((pawnBitboard << 9) & NOT_A_FILE) | ((pawnBitboard << 7) & NOT_H_FILE);
        }
        return ((pawnBitboard >> 7) & NOT_A_FILE) | ((pawnBitboard >> 9) & NOT_H_FILE);
    }

    public static long shift(long bitboard, int numSquaresToShift) {
        if (numSquaresToShift > 0) {
            return bitboard << numSquaresToShift;
        } else {
            return bitboard >> -numSquaresToShift;
        }
    }

    static {
        (new BitBoardUtility()).init();
    }

    public void init() {
        int[][] orthoDir = { { -1, 0 }, { 0, 1 }, { 1, 0 }, { 0, -1 } };
        int[][] diagDir = { { -1, -1 }, { -1, 1 }, { 1, 1 }, { 1, -1 } };
        int[][] knightJumps = { { -2, -1 }, { -2, 1 }, { -1, 2 }, { 1, 2 }, { 2, 1 }, { 2, -1 }, { 1, -2 }, { -1, -2 } };

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                processSquare(x, y, orthoDir, diagDir, knightJumps);
            }
        }
    }

    private void processSquare(int x, int y, int[][] orthoDir, int[][] diagDir, int[][] knightJumps) {
        int squareIndex = y * 8 + x;

        for (int dirIndex = 0; dirIndex < 4; dirIndex++) {

            // Orthogonal and diagonal directions
            for (int dst = 1; dst < 8; dst++) {
                int orthoX = x + orthoDir[dirIndex][0] * dst;
                int orthoY = y + orthoDir[dirIndex][1] * dst;
                int diagX = x + diagDir[dirIndex][0] * dst;
                int diagY = y + diagDir[dirIndex][1] * dst;

                if (validSquareIndex(orthoX, orthoY)) {
                    int orthoTargetIndex = orthoY * 8 + orthoX;
                    if (dst == 1) {
                        KING_MOVES[squareIndex] |= 1L << orthoTargetIndex;
                    }
                }

                if (validSquareIndex(diagX, diagY)) {
                    int diagTargetIndex = diagY * 8 + diagX;
                    if (dst == 1) {
                        KING_MOVES[squareIndex] |= 1L << diagTargetIndex;
                    }
                }
            }

            // Knight jumps
            for (int i = 0; i < knightJumps.length; i++) {
                int knightX = x + knightJumps[i][0];
                int knightY = y + knightJumps[i][1];
                if (validSquareIndex(knightX, knightY)) {
                    int knightTargetSquare = knightY * 8 + knightX;
                    KNIGHT_ATTACKS[squareIndex] |= 1L << knightTargetSquare;
                }
            }

            // Pawn attacks
            if (validSquareIndex(x + 1, y + 1)) {
                int whitePawnRight = (y + 1) * 8 + (x + 1);
                WHITE_PAWN_ATTACKS[squareIndex] |= 1L << whitePawnRight;
            }
            if (validSquareIndex(x - 1, y + 1)) {
                int whitePawnLeft = (y + 1) * 8 + (x - 1);
                WHITE_PAWN_ATTACKS[squareIndex] |= 1L << whitePawnLeft;
            }

            if (validSquareIndex(x + 1, y - 1)) {
                int blackPawnAttackRight = (y - 1) * 8 + (x + 1);
                BLACK_PAWN_ATTACKS[squareIndex] |= 1L << blackPawnAttackRight;
            }
            if (validSquareIndex(x - 1, y - 1)) {
                int blackPawnAttackLeft = (y - 1) * 8 + (x - 1);
                BLACK_PAWN_ATTACKS[squareIndex] |= 1L << blackPawnAttackLeft;
            }
        }
    }

    private boolean validSquareIndex(int x, int y) {
        return x >= 0 && x < 8 && y >= 0 && y < 8;
    }
}
