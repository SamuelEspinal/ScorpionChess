package engine.bitBoard.moveGeneration.magics;

import java.util.List;

import engine.bitBoard.Coord;
import engine.bitBoard.helpers.BoardHelper;
import engine.bitBoard.moveGeneration.bitBoardsUtil.BitBoardUtility;

import java.util.ArrayList;

public class MagicHelper {
    
    public static long[] createAllBlockerBitboards(long movementMask) {
        // Create a list of the indices of the bits that are set in the movement mask
        List<Integer> moveSquareIndices = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            if (((movementMask >> i) & 1) == 1) {
                moveSquareIndices.add(i);
            }
        }

        // Calculate total number of different bitboards (one for each possible arrangement of pieces)
        int numPatterns = 1 << moveSquareIndices.size(); // 2^n
        long[] blockerBitboards = new long[numPatterns];

        // Create all bitboards
        for (int patternIndex = 0; patternIndex < numPatterns; patternIndex++) {
            for (int bitIndex = 0; bitIndex < moveSquareIndices.size(); bitIndex++) {
                int bit = (patternIndex >> bitIndex) & 1;
                blockerBitboards[patternIndex] |= (long) bit << moveSquareIndices.get(bitIndex);
            }
        }

        return blockerBitboards;
    }

    public static long createMovementMask(int squareIndex, boolean ortho) {
        long mask = 0;
        Coord[] directions = ortho ? BoardHelper.ROOK_DIRECTIONS : BoardHelper.BISHOP_DIRECTIONS;
        Coord startCoord = new Coord(squareIndex);

        for (Coord dir : directions) {
            for (int dst = 1; dst < 8; dst++) {
                Coord coord = startCoord.add(scale(dir, dst));
                Coord nextCoord = startCoord.add(scale(dir, dst + 1));

                if (nextCoord.isValidSquare()) {
                    BitBoardUtility.setSquare(mask, coord.getSquareIndex());
                } else {
                    break;
                }
            }
        }
        return mask;
    }

    public static long legalMoveBitboardFromBlockers(int startSquare, long blockerBitboard, boolean ortho) {
        long bitboard = 0;

        Coord[] directions = ortho ? BoardHelper.ROOK_DIRECTIONS : BoardHelper.BISHOP_DIRECTIONS;
        Coord startCoord = new Coord(startSquare);

        for (Coord dir : directions) {
            for (int dst = 1; dst < 8; dst++) {
                Coord coord = startCoord.add(scale(dir,dst));

                if (coord.isValidSquare()) {
                    BitBoardUtility.setSquare(bitboard, coord.getSquareIndex());
                    if (BitBoardUtility.containsSquare(blockerBitboard, coord.getSquareIndex())) {
                        break;
                    }
                } else {
                    break;
                }
            }
        }

        return bitboard;
    }

    //Helper method
    public static Coord scale(Coord coord, int factor) {
        return new Coord(coord.fileIndex * factor, coord.rankIndex * factor);
    }

}
