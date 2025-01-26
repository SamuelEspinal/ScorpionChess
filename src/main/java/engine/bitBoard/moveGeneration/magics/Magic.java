package engine.bitBoard.moveGeneration.magics;

import java.math.BigInteger;

//https://www.chessprogramming.org/Magic_Bitboards
public class Magic {
    
    static int[] rookShifts = PrecomputedMagics.RookShifts;
    static BigInteger[] rookMagics = PrecomputedMagics.RookMagics;  // Change to BigInteger
    static int[] bishopShifts = PrecomputedMagics.BishopShifts;
    static BigInteger[] bishopMagics = PrecomputedMagics.BishopMagics; // Change to BigInteger

    //Rook and bishop mask bitboards for each origin square
    //A mask is simply the legal moves available to the piece from the origin square
    //(on an empty board), except that the moves stop 1 square before the edge of the board.
    public static final long[] rookMasks = new long[64];
    public static final long[] bishopMasks = new long[64];

    public static final long[][] rookAttacks = new long[64][];
    public static final long[][] bishopAttacks = new long[64][];

    public static long GetSliderAttacks(int square, long blockers, boolean ortho) {
        return ortho ? getRookAttacks(square, blockers) : getBishopAttacks(square, blockers);
    }

    public static long getRookAttacks(int square, long blockers) {
        // Use BigInteger for calculations
        BigInteger key = BigInteger.valueOf(blockers & rookMasks[square]).multiply(rookMagics[square]).shiftRight(rookShifts[square]);
        return rookAttacks[square][key.intValue()]; // Convert BigInteger to int
    }

    public static long getBishopAttacks(int square, long blockers) {
        // Use BigInteger for calculations
        BigInteger key = BigInteger.valueOf(blockers & bishopMasks[square]).multiply(bishopMagics[square]).shiftRight(bishopShifts[square]);
        return bishopAttacks[square][key.intValue()]; // Convert BigInteger to int
    }

    static {
        // Initialize masks and attacks as before
        for (int squareIndex = 0; squareIndex < 64; squareIndex++) {
            rookMasks[squareIndex] = MagicHelper.createMovementMask(squareIndex, true);
            bishopMasks[squareIndex] = MagicHelper.createMovementMask(squareIndex, false);
        }

        for (int i = 0; i < 64; i++) {
            rookAttacks[i] = createTable(i, true, rookMagics[i], rookShifts[i]);
            bishopAttacks[i] = createTable(i, false, bishopMagics[i], bishopShifts[i]);
        }
    }

    private static long[] createTable(int square, boolean rook, BigInteger magic, int leftShift) {
        int numBits = 64 - leftShift;
        int lookupSize = 1 << numBits;
        long[] table = new long[lookupSize];

        long movementMask = MagicHelper.createMovementMask(square, rook);
        long[] blockerPatterns = MagicHelper.createAllBlockerBitboards(movementMask);

        for (long pattern : blockerPatterns) {
            BigInteger index = BigInteger.valueOf(pattern).multiply(magic).shiftRight(leftShift);
            long moves = MagicHelper.legalMoveBitboardFromBlockers(square, pattern, rook);
            table[index.intValue()] = moves; // Convert BigInteger to int
        }
        return table;
    }
}
