package engine.bitBoard;

import java.util.Random;
import java.nio.ByteBuffer;

public class Zobrist {
    // Random numbers are generated for each aspect of the game state, and are used for calculating the hash:

    // piece type, colour, square index
    public static final long[][] piecesArray = new long[bitPiece.MAX_PIECE_INDEX + 1][64];
    // Each player has 4 possible castling right states: none, queenside, kingside, both.
    // So, taking both sides into account, there are 16 possible states.
    public static final long[] castlingRights = new long[16];
    // En passant file (0 = no ep).
    //  Rank does not need to be specified since side to move is included in key
    public static final long[] enPassantFile = new long[9];
    public static final long sideToMove;

    static {
        final int seed = 29426028;
        Random rng = new Random(seed);

        for (int squareIndex = 0; squareIndex < 64; squareIndex++) {
            for (int bitPiece : bitPiece.PIECE_INDICES) {
                piecesArray[bitPiece][squareIndex] = randomUnsigned64BitNumber(rng);
            }
        }

        for (int i = 0; i < castlingRights.length; i++) {
            castlingRights[i] = randomUnsigned64BitNumber(rng);
        }

        for (int i = 0; i < enPassantFile.length; i++) {
            enPassantFile[i] = (i == 0) ? 0 : randomUnsigned64BitNumber(rng);
        }

        sideToMove = randomUnsigned64BitNumber(rng);
    }

    // Calculate zobrist key from current board position.
    // NOTE: this function is slow and should only be used when the board is initially set up from FEN.
    // During search, the key should be updated incrementally instead.
    public static long calculateZobristKey(bitBoard board) {
        long zobristKey = 0;

        for (int squareIndex = 0; squareIndex < 64; squareIndex++) {
            int piece = board.square[squareIndex];

            if (bitPiece.pieceType(piece) != bitPiece.NONE) {
                zobristKey ^= piecesArray[piece][squareIndex];
            }
        }

        zobristKey ^= enPassantFile[board.currentGameState.getEnPassantFile()];

        if (board.moveColour == bitPiece.BLACK) {
            zobristKey ^= sideToMove;
        }

        zobristKey ^= castlingRights[board.currentGameState.getCastlingRights()];

        return zobristKey;
    }

    private static long randomUnsigned64BitNumber(Random rng) {
        byte[] buffer = new byte[8];
        rng.nextBytes(buffer);
        // Use ByteBuffer to convert the byte array into a long value
        return ByteBuffer.wrap(buffer).getLong();
    }
}

