/* package engine.bitBoard.moveGeneration;

import java.util.Arrays;

import engine.bitBoard.bitBoard;
import engine.bitBoard.bitMove;
import engine.bitBoard.bitPiece;
import engine.bitBoard.helpers.BoardHelper;
import engine.bitBoard.moveGeneration.bitBoardsUtil.BitBoardUtility;
import engine.bitBoard.moveGeneration.bitBoardsUtil.Bits;
import engine.bitBoard.moveGeneration.magics.Magic;
import engine.board.Move;
import engine.pieces.Piece;

public class MoveGenerator {

    public static final int MaxMoves = 218;

    public enum PromotionMode { All, QueenOnly, QueenAndKnight }

    public PromotionMode promotionsToGenerate = PromotionMode.All;

    // ---- Instance variables ----
    boolean isWhiteToMove;
    int friendlyColour;
    int opponentColour;
    int friendlyKingSquare;
    int friendlyIndex;
    int enemyIndex;

    boolean inCheck;
    boolean inDoubleCheck;

    // If in check, this bitboard contains squares in line from checking piece up to king
    // If not in check, all bits are set to 1
    long checkRayBitmask;

    long pinRays;
    long notPinRays;
    long opponentAttackMapNoPawns;
    public long opponentAttackMap;
    public long opponentPawnAttackMap;
    long opponentSlidingAttackMap;

    boolean generateQuietMoves;
    bitBoard board;
    int currMoveIndex;

    long enemyPieces;
    long friendlyPieces;
    long allPieces;
    long emptySquares;
    long emptyOrEnemySquares;
    long moveTypeMask;

    public bitMove[] GenerateMoves(bitBoard board, boolean capturesOnly) {
        bitMove[] moves = new bitMove[MaxMoves];
        GenerateMoves(board, moves, capturesOnly);
        return moves;
    }

    // Generates list of legal moves in current position.
    // Quiet moves (non captures) can optionally be excluded. This is used in quiescence search.
    public int GenerateMoves(bitBoard board, bitMove[] moves, boolean capturesOnly) {
        this.board = board;
        generateQuietMoves = !capturesOnly;

        Init();

        GenerateKingMoves(moves);

        // Only king moves are valid in a double check position, so can return early.
        if (!inDoubleCheck) {
            GenerateSlidingMoves(moves);
            GenerateKnightMoves(moves);
            GeneratePawnMoves(moves);
        }

        moves = Arrays.copyOfRange(moves, 0, currMoveIndex);

        return moves.length;
    }

    // Note, this will only return correct value after GenerateMoves() has been called in the current position
    public boolean InCheck() {
        return inCheck;
    }

    void Init() {
        // Reset state
        currMoveIndex = 0;
        inCheck = false;
        inDoubleCheck = false;
        checkRayBitmask = 0;
        pinRays = 0;

        // Store some info for convenience
        isWhiteToMove = board.moveColour == bitPiece.WHITE;
        friendlyColour = board.moveColour;
        opponentColour = board.opponentColour;
        friendlyKingSquare = board.kingSquare[board.moveColourIndex];
        friendlyIndex = board.moveColourIndex;
        enemyIndex = 1 - friendlyIndex;

        // Store some bitboards for convenience
        enemyPieces = board.colourBitboards[enemyIndex];
        friendlyPieces = board.colourBitboards[friendlyIndex];
        allPieces = board.allPiecesBitboard;
        emptySquares = ~allPieces;
        emptyOrEnemySquares = emptySquares | enemyPieces;
        moveTypeMask = generateQuietMoves ? ~0L : enemyPieces;

        CalculateAttackData();
    }

    void GenerateKingMoves(bitMove[] moves) {
        long legalMask = ~(opponentAttackMap | friendlyPieces);
        long kingMoves = BitBoardUtility.KING_MOVES[friendlyKingSquare] & legalMask & moveTypeMask;
        while (kingMoves != 0) {
            int targetSquare = BitBoardUtility.popLSB(kingMoves);
            moves[currMoveIndex++] = new bitMove(friendlyKingSquare, targetSquare);
        }

        // Castling
        if (!inCheck && generateQuietMoves) {
            long castleBlockers = opponentAttackMap | board.allPiecesBitboard;
            if (board.currentGameState.hasKingsideCastleRight(board.isWhiteToMove)) {
                long castleMask = board.isWhiteToMove ? Bits.WhiteKingsideMask : Bits.BlackKingsideMask;
                if ((castleMask & castleBlockers) == 0) {
                    int targetSquare = board.isWhiteToMove ? BoardHelper.G1 : BoardHelper.G8;
                    moves[currMoveIndex++] = new bitMove(friendlyKingSquare, targetSquare, bitMove.CastleFlag);
                }
            }
            if (board.currentGameState.hasQueensideCastleRight(board.isWhiteToMove)) {
                long castleMask = board.isWhiteToMove ? Bits.WhiteQueensideMask2 : Bits.BlackQueensideMask2;
                long castleBlockMask = board.isWhiteToMove ? Bits.WhiteQueensideMask : Bits.BlackQueensideMask;
                if ((castleMask & castleBlockers) == 0 && (castleBlockMask & board.allPiecesBitboard) == 0) {
                    int targetSquare = board.isWhiteToMove ? BoardHelper.C1 : BoardHelper.C8;
                    moves[currMoveIndex++] = new bitMove(friendlyKingSquare, targetSquare, bitMove.CastleFlag);
                }
            }
        }
    }

    void GenerateSlidingMoves(bitMove[] moves) {
        // Limit movement to empty or enemy squares, and must block check if king is in check.
        long moveMask = emptyOrEnemySquares & checkRayBitmask & moveTypeMask;

        long orthogonalSliders = board.friendlyOrthogonalSliders;
        long diagonalSliders = board.friendlyDiagonalSliders;

        // Pinned pieces cannot move if king is in
         if (inCheck) {
            orthogonalSliders &= ~pinRays;
            diagonalSliders &= ~pinRays;
        }

        // Ortho
        while (orthogonalSliders != 0) {
            int startSquare = BitBoardUtility.popLSB(orthogonalSliders);
            long moveSquares = Magic.getRookAttacks(startSquare, allPieces) & moveMask;

            // If piece is pinned, it can only move along the pin ray
            if (IsPinned(startSquare)) {
                moveSquares &= alignMask[startSquare][friendlyKingSquare];
            }

            while (moveSquares != 0) {
                int targetSquare = BitBoardUtility.popLSB(moveSquares);
                moves[currMoveIndex++] = new bitMove(startSquare, targetSquare);
            }
        }

        // Diag
        while (diagonalSliders != 0) {
            int startSquare = BitBoardUtility.popLSB(diagonalSliders);
            long moveSquares = Magic.getBishopAttacks(startSquare, allPieces) & moveMask;

            // If piece is pinned, it can only move along the pin ray
            if (IsPinned(startSquare)) {
                moveSquares &= alignMask[startSquare][friendlyKingSquare];
            }

            while (moveSquares != 0) {
                int targetSquare = BitBoardUtility.popLSB(moveSquares);
                moves[currMoveIndex++] = new bitMove(startSquare, targetSquare);
            }
        }
    }

    void GenerateKnightMoves(bitMove[] moves) {
        int friendlyKnightPiece = bitPiece.makePiece(bitPiece.KNIGHT, board.moveColour);
        // bitboard of all non-pinned knights
        long knights = board.pieceBitboards[friendlyKnightPiece] & notPinRays;
        long moveMask = emptyOrEnemySquares & checkRayBitmask & moveTypeMask;

        while (knights != 0) {
            int knightSquare = BitBoardUtility.popLSB(knights);
            long moveSquares = BitBoardUtility.KNIGHT_ATTACKS[knightSquare] & moveMask;

            while (moveSquares != 0) {
                int targetSquare = BitBoardUtility.popLSB(moveSquares);
                moves[currMoveIndex++] = new bitMove(knightSquare, targetSquare);
            }
        }
    }

    void GeneratePawnMoves(bitMove[] moves) {
        int pushDir = board.isWhiteToMove ? 1 : -1;
        int pushOffset = pushDir * 8;

        int friendlyPawnPiece = bitPiece.makePiece(bitPiece.PAWN, board.moveColour);
        long pawns = board.pieceBitboards[friendlyPawnPiece];

        long promotionRankMask = board.isWhiteToMove ? BitBoardUtility.RANK_8 : BitBoardUtility.RANK_1;

        long singlePush = (BitBoardUtility.shift(pawns, pushOffset)) & emptySquares;

        long pushPromotions = singlePush & promotionRankMask & checkRayBitmask;

        long captureEdgeFileMask = board.isWhiteToMove ? BitBoardUtility.NOT_A_FILE : BitBoardUtility.NOT_H_FILE;
        long captureEdgeFileMask2 = board.isWhiteToMove ? BitBoardUtility.NOT_H_FILE : BitBoardUtility.NOT_A_FILE;
        long captureA = BitBoardUtility.shift(pawns & captureEdgeFileMask, pushDir * 7) & enemyPieces;
        long captureB = BitBoardUtility.shift(pawns & captureEdgeFileMask2, pushDir * 9) & enemyPieces;

        long singlePushNoPromotions = singlePush & ~promotionRankMask & checkRayBitmask;

        long capturePromotionsA = captureA & promotionRankMask & checkRayBitmask;
        long capturePromotionsB = captureB & promotionRankMask & checkRayBitmask;

        captureA &= checkRayBitmask & ~promotionRankMask;
        captureB &= checkRayBitmask & ~promotionRankMask;

        // Single / double push
        if (generateQuietMoves) {
            // Generate single pawn pushes
            while (singlePushNoPromotions != 0) {
                int targetSquare = BitBoardUtility.popLSB(singlePushNoPromotions);
                int startSquare = targetSquare - pushOffset;
                moves[currMoveIndex++] = new bitMove(startSquare, targetSquare);
            }
        }

        // En passant and other pawn moves would be handled here
    }

    void CalculateAttackData() {
        // Calculate data about opponent's attacks (without king)
        opponentPawnAttackMap = AttackMaps.PawnAttacks(board.opponentColour, board.pieceBitboards[bitPiece.makePiece(bitPiece.PAWN, opponentColour)]);
        opponentAttackMapNoPawns = AttackMaps.AllAttacks(board, opponentColour, true);
        opponentSlidingAttackMap = AttackMaps.SlidingAttacks(board, opponentColour);
        opponentAttackMap = opponentPawnAttackMap | opponentAttackMapNoPawns;
    }

    boolean IsPinned(int square) {
        return (pinRays & BitBoardUtility.squareToBitboard(square)) != 0;
    }
}
 */