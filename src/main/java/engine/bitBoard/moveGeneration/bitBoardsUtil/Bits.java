package engine.bitBoard.moveGeneration.bitBoardsUtil;

import engine.bitBoard.helpers.BoardHelper;

public class Bits {
    
    public static final long FileA = 0x101010101010101L;

    public static final long WhiteKingsideMask = 1L << BoardHelper.F1 | 1L << BoardHelper.G1;
    public static final long BlackKingsideMask = 1L << BoardHelper.F8 | 1L << BoardHelper.G8;

    public static final long WhiteQueensideMask2 = 1L << BoardHelper.D1 | 1L << BoardHelper.C1;
    public static final long BlackQueensideMask2 = 1L << BoardHelper.D8 | 1L << BoardHelper.C8;

    public static final long WhiteQueensideMask = WhiteQueensideMask2 | 1L << BoardHelper.B1;
    public static final long BlackQueensideMask = BlackQueensideMask2 | 1L << BoardHelper.B8;

    public static final long[] WhitePassedPawnMask = new long[64];
    public static final long[] BlackPassedPawnMask = new long[64];

    //A pawn on 'e4' for example, is considered supporrted by anyy pawn on 
    // squares d3, d4, f3, f4....
    public static final long[] WhitePawnSupportMask = new long[64];
    public static final long[] BlackPawnSupportMask = new long[64];

    public static final long[] FileMask = new long[8];
    public static final long[] AdjacentFileMask = new long[8];

    public static final long[] KingSafetyMask = new long[64];

    // Mask of 'forward' square. For example, from e4 the forward squares for white are: [e5, e6, e7....]
    public static final long[] WhiteForwardFileMask = new long[64];
    public static final long[] BlackForwardFileMask = new long[64];

    //Mask of three consecutive files centeres at a given file index
    // For example, given file '3' the mask would contain files [2,3,4]
    // notice that for edge files, such as 0, it would contain files [0,1,2]
    public static final long[] TripleFileMask = new long[8];

    static {

        for(int i = 0; i < 8; i++) {
        
            FileMask[i] = FileA << i;
            long left = i > 0 ? FileA << (i - 1) : 0;
            long right = i < 7 ? FileA << (i + 1) : 0;
            AdjacentFileMask[i] = left | right;
        }

        for(int i = 0; i < 8; i++) {
            int clampedFile = MathUtils.clamp(i, 1, 6);
            TripleFileMask[i] = FileMask[clampedFile] | AdjacentFileMask[clampedFile];
        }

        for(int square = 0; square < 64; square++) {
            int file = BoardHelper.fileIndex(square);
            int rank = BoardHelper.rankIndex(square);
            long adjacentFiles = FileA << Math.max(0, file -1) | FileA << Math.min(7, file + 1);

            //Passed pawn mask
            long whiteForwardMask = ~(Long.MAX_VALUE >> (64 - 8* (rank + 1)));
            long blackForwardMask = ((1L << 8 * rank) - 1);

            WhitePassedPawnMask[square] = (FileA << file | adjacentFiles) & whiteForwardMask;
            BlackPassedPawnMask[square] = (FileA << file | adjacentFiles) & blackForwardMask;

            //Pawn support mask
            long adjacent = (1L << (square - 1) | 1L << (square + 1)) & adjacentFiles;
            WhitePawnSupportMask[square] = adjacent | BitBoardUtility.shift(adjacent, -8);
            BlackPawnSupportMask[square] = adjacent | BitBoardUtility.shift(adjacent, 8);

            WhiteForwardFileMask[square] = whiteForwardMask & FileMask[file];
            BlackForwardFileMask[square] = blackForwardMask & FileMask[file];
        }
        
        for(int i = 0; i < 64; i++) {
            KingSafetyMask[i] = BitBoardUtility.KING_MOVES[i] | (1L << i);
        }
    }

    //Helper methods
    public class MathUtils{
        public static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(value, max));
        }
    }

}
