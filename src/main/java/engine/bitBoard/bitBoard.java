package engine.bitBoard;

import engine.bitBoard.helpers.BoardHelper;
import engine.bitBoard.helpers.FenUtility;
import engine.bitBoard.moveGeneration.bitBoardsUtil.BitBoardUtility;
import engine.bitBoard.moveGeneration.magics.Magic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class bitBoard {

    public static final int WhiteIndex = 0;
    public static final int BlackIndex = 1;

    // Stores piece code for each square on the board
    public final int[] square;
    // Square index of white and black king
    public int[] kingSquare;
    // Bitboard for each piece type and colour (white pawns, white knights, ... black pawns, etc.)
    public long[] pieceBitboards;
    // Bitboards for all pieces of either colour (all white pieces, all black pieces)
    public long[] colourBitboards;
    public long allPiecesBitboard;
    public long friendlyOrthogonalSliders;
    public long friendlyDiagonalSliders;
    public long enemyOrthogonalSliders;
    public long enemyDiagonalSliders;
    // Piece count excluding pawns and kings
    public int totalPieceCountWithoutPawnsAndKings;
    // Piece lists for different types of pieces
    public bitPieceList[] rooks;
    public bitPieceList[] bishops;
    public bitPieceList[] queens;
    public bitPieceList[] knights;
    public bitPieceList[] pawns;

    // Side to move info
    public boolean isWhiteToMove;
    public int moveColour = isWhiteToMove ? bitPiece.WHITE : bitPiece.BLACK;
    public int opponentColour =isWhiteToMove ? bitPiece.BLACK : bitPiece.WHITE;
    public int moveColourIndex = isWhiteToMove ? WhiteIndex : BlackIndex;
    public int opponentColourIndex = isWhiteToMove ? BlackIndex : WhiteIndex;
    // List of (hashed) positions since last pawn move or capture (for detecting repetitions)
    public Stack<Long> repetitionPositionHistory;

    // Total plies (half-moves) played in game
    public int plyCount;
    public bitGameState currentGameState;
    public int fiftyMoveCounter = currentGameState.fiftyMoveCounter;
    public long zobristKey = currentGameState.zobristKey;

    private FenUtility.PositionInfo startPositionInfo;
    public String currentFEN = FenUtility.currentFen(this, true);
    public String gameStartFEN = startPositionInfo.fen;

    public List<bitMove> allGameMoves;

    // Private fields
    private bitPieceList[] allPieceLists;
    private Stack<bitGameState> gameStateHistory;
    private boolean cachedInCheckValue;
    private boolean hasCachedInCheckValue;

    public bitBoard() {
        square = new int[64];
    }

    // Make a move on the board
    public void makeMove(bitMove move, boolean inSearch) {
        int startSquare = move.getStartSquare();
        int targetSquare = move.getTargetSquare();
        int moveFlag = move.getMoveFlag();
        boolean isPromotion = move.isPromotion();
        boolean isEnPassant = moveFlag == bitMove.EnPassantCaptureFlag;

        int movedPiece = square[startSquare];
        int movedPieceType = bitPiece.pieceType(movedPiece);
        int capturedPiece = isEnPassant ? bitPiece.makePiece(bitPiece.PAWN, opponentColour) : square[targetSquare];
        int capturedPieceType = bitPiece.pieceType(capturedPiece);

        int prevCastleState = currentGameState.castlingRights;
        int prevEnPassantFile = currentGameState.enPassantFile;
        long newZobristKey = currentGameState.zobristKey;
        int newCastlingRights = currentGameState.castlingRights;
        int newEnPassantFile = 0;

        // Update bitboard of moved piece (pawn promotion is a special case and is corrected later)
        movePiece(movedPiece, startSquare, targetSquare);

        // Handle captures
        if (capturedPieceType != bitPiece.NONE) {
            int captureSquare = targetSquare;

            if (isEnPassant) {
                captureSquare = targetSquare + (isWhiteToMove ? -8 : 8);
                square[captureSquare] = bitPiece.NONE;
            }
            if (capturedPieceType != bitPiece.PAWN) {
                totalPieceCountWithoutPawnsAndKings--;
            }

            // Remove captured piece from bitboards and piece list
            allPieceLists[capturedPiece].removePieceAtSquare(captureSquare);
            BitBoardUtility.toggleSquare(pieceBitboards[capturedPiece], captureSquare);
            BitBoardUtility.toggleSquare(colourBitboards[opponentColourIndex], captureSquare);
            newZobristKey ^= Zobrist.piecesArray[capturedPiece][captureSquare];
        }

        // Handle king moves
        if (movedPieceType == bitPiece.KING) {
            kingSquare[moveColourIndex] = targetSquare;
            newCastlingRights &= (isWhiteToMove) ? 0b1100 : 0b0011;

            // Handle castling
            if (moveFlag == bitMove.CastleFlag) {
                int rookPiece = bitPiece.makePiece(bitPiece.ROOK, moveColour);
                boolean kingside = targetSquare == BoardHelper.G1 || targetSquare == BoardHelper.G8;
                int castlingRookFromIndex = (kingside) ? targetSquare + 1 : targetSquare - 2;
                int castlingRookToIndex = (kingside) ? targetSquare - 1 : targetSquare + 1;

                //Update rook position
                BitBoardUtility.toggleSquares(pieceBitboards[rookPiece], castlingRookFromIndex, castlingRookToIndex);
                BitBoardUtility.toggleSquares(colourBitboards[moveColourIndex], castlingRookFromIndex, castlingRookToIndex);
                allPieceLists[rookPiece].movePiece(castlingRookFromIndex, castlingRookToIndex);
                square[castlingRookFromIndex] = bitPiece.NONE;
                square[castlingRookToIndex] = bitPiece.ROOK | moveColour;

                newZobristKey ^= Zobrist.piecesArray[rookPiece][castlingRookFromIndex];
                newZobristKey ^= Zobrist.piecesArray[rookPiece][castlingRookToIndex];
            }
        }

        // Handle promotion
        if (isPromotion) {
            totalPieceCountWithoutPawnsAndKings++;
            int promotionPieceType = move.getPromotionPieceType();
            int promotionPiece = bitPiece.makePiece(promotionPieceType, moveColour);

            // Remove pawn from promotion square and add promoted piece instead
            BitBoardUtility.toggleSquare(pieceBitboards[movedPiece], targetSquare);
            BitBoardUtility.toggleSquare(pieceBitboards[promotionPiece], targetSquare);
            allPieceLists[movedPiece].removePieceAtSquare(targetSquare);
            allPieceLists[promotionPiece].addPieceAtSquare(targetSquare);
            square[targetSquare] = promotionPiece;
        }

        // Pawn has moved two squares forward, mark en-passant flag
        if (moveFlag == bitMove.PawnTwoUpFlag) {
            int file = BoardHelper.fileIndex(startSquare) + 1;
            newEnPassantFile = file;
            newZobristKey ^= Zobrist.enPassantFile[file];
        }

        // Update castling rights
        if (prevCastleState != 0) {
            if (targetSquare == BoardHelper.H1 || startSquare == BoardHelper.H1) {
                newCastlingRights &= bitGameState.CLEAR_WHITE_KINGSIDE_MASK;
            } else if (targetSquare == BoardHelper.A1 || startSquare == BoardHelper.A1) {
                newCastlingRights &= bitGameState.CLEAR_WHITE_QUEENSIDE_MASK;
            }
            if (targetSquare == BoardHelper.H8 || startSquare == BoardHelper.H8) {
                newCastlingRights &= bitGameState.CLEAR_BLACK_KINGSIDE_MASK;
            } else if (targetSquare == BoardHelper.A8 || startSquare == BoardHelper.A8) {
                newCastlingRights &= bitGameState.CLEAR_BLACK_QUEENSIDE_MASK;
            }
        }

        // Update Zobrist key with new piece positions and side to move
        newZobristKey ^= Zobrist.sideToMove;
        newZobristKey ^= Zobrist.piecesArray[movedPiece][startSquare];
        newZobristKey ^= Zobrist.piecesArray[square[targetSquare]][targetSquare];
        newZobristKey ^= Zobrist.enPassantFile[prevEnPassantFile];

        if (newCastlingRights != prevCastleState) {
            newZobristKey ^= Zobrist.castlingRights[prevCastleState];  // remove old castling rights state
            newZobristKey ^= Zobrist.castlingRights[newCastlingRights]; // add new castling rights state
        }

        // Switch side to move
        isWhiteToMove = !isWhiteToMove;
        plyCount++;
        int newFiftyMoveCounter = currentGameState.fiftyMoveCounter + 1;

        allPiecesBitboard = colourBitboards[WhiteIndex] | colourBitboards[BlackIndex];
        updateSliderBitboards();

        if (movedPieceType == bitPiece.PAWN || capturedPieceType != bitPiece.NONE) {
            if (!inSearch) {
                repetitionPositionHistory.clear();
            }
            newFiftyMoveCounter = 0;
        }

        bitGameState newState = new bitGameState(capturedPieceType, newEnPassantFile, newCastlingRights, newFiftyMoveCounter, newZobristKey);
        gameStateHistory.push(newState);
        currentGameState = newState;
        hasCachedInCheckValue = false;

        if (!inSearch) {
            repetitionPositionHistory.push(newState.zobristKey);
            allGameMoves.add(move);
        }
    }

    // Unmake a move on the board (reverse the move)
    public void unmakeMove(bitMove move, boolean inSearch) {
        // Swap colour to move
        isWhiteToMove = !isWhiteToMove;

        boolean undoingWhiteMove = isWhiteToMove;

        // Get move info
        int movedFrom = move.getStartSquare();
        int movedTo = move.getTargetSquare();
        int moveFlag = move.getMoveFlag();

        boolean undoingEnPassant = moveFlag == bitMove.EnPassantCaptureFlag;
        boolean undoingPromotion = move.isPromotion();
        boolean undoingCapture = currentGameState.capturedPieceType != bitPiece.NONE;

        int movedPiece = undoingPromotion ? bitPiece.makePiece(bitPiece.PAWN, moveColour) : square[movedTo];
        int movedPieceType = bitPiece.pieceType(movedPiece);
        int capturedPieceType = currentGameState.capturedPieceType;

        // If undoing promotion, then remove piece from promotion square and replace with pawn
        if (undoingPromotion) {
            int promotedPiece = square[movedTo];
            int pawnPiece = bitPiece.makePiece(bitPiece.PAWN, moveColour);
            totalPieceCountWithoutPawnsAndKings--;

            allPieceLists[promotedPiece].removePieceAtSquare(movedTo);
            allPieceLists[movedPiece].addPieceAtSquare(movedTo);
            BitBoardUtility.toggleSquare(pieceBitboards[promotedPiece], movedTo);
            BitBoardUtility.toggleSquare(pieceBitboards[pawnPiece], movedTo);
        }

        movePiece(movedPiece, movedTo, movedFrom);

        // Undo capture
        if (undoingCapture) {
            int captureSquare = movedTo;
            int capturedPiece = bitPiece.makePiece(capturedPieceType, opponentColour);

            if (undoingEnPassant) {
                captureSquare = movedTo + (undoingWhiteMove ? -8 : 8);
            }
            if (capturedPieceType != bitPiece.PAWN) {
                totalPieceCountWithoutPawnsAndKings++;
            }

            // Add back captured piece
            BitBoardUtility.toggleSquare(pieceBitboards[capturedPiece], captureSquare);
            BitBoardUtility.toggleSquare(colourBitboards[opponentColourIndex], captureSquare);
            allPieceLists[capturedPiece].addPieceAtSquare(captureSquare);
            square[captureSquare] = capturedPiece;
        }

        // Update king
        if (movedPieceType == bitPiece.KING) {
            kingSquare[moveColourIndex] = movedFrom;

            // Undo castling
            if (moveFlag == bitMove.CastleFlag) {
                int rookPiece = bitPiece.makePiece(bitPiece.ROOK, moveColour);
                boolean kingside = movedTo == BoardHelper.G1 || movedTo == BoardHelper.G8;
                int rookSquareBeforeCastling = kingside ? movedTo + 1 : movedTo - 2;
                int rookSquareAfterCastling = kingside ? movedTo - 1 : movedTo + 1;

                // Undo castling by returning rook to original square
                BitBoardUtility.toggleSquares(pieceBitboards[rookPiece], rookSquareAfterCastling, rookSquareBeforeCastling);
                BitBoardUtility.toggleSquares(colourBitboards[moveColourIndex], rookSquareAfterCastling, rookSquareBeforeCastling);
                square[rookSquareAfterCastling] = bitPiece.NONE;
                square[rookSquareBeforeCastling] = rookPiece;
                allPieceLists[rookPiece].movePiece(rookSquareAfterCastling, rookSquareBeforeCastling);
            }
        }

        allPiecesBitboard = colourBitboards[WhiteIndex] | colourBitboards[BlackIndex];
        updateSliderBitboards();

        if (!inSearch && !repetitionPositionHistory.isEmpty()) {
            repetitionPositionHistory.pop();
        }
        if (!inSearch) {
            allGameMoves.remove(allGameMoves.size() - 1);
        }

        // Go back to previous state
        gameStateHistory.pop();
        currentGameState = gameStateHistory.peek();
        plyCount--;
        hasCachedInCheckValue = false;
    }

    public void makeNullMove() {
        isWhiteToMove = !isWhiteToMove;

        plyCount++;

        long newZobristKey = currentGameState.zobristKey;
        newZobristKey ^= Zobrist.sideToMove;
        newZobristKey ^= Zobrist.enPassantFile[currentGameState.enPassantFile];

        bitGameState newState = new bitGameState(bitPiece.NONE, 0, currentGameState.castlingRights, currentGameState.fiftyMoveCounter + 1, newZobristKey);
        currentGameState = newState;
        gameStateHistory.push(currentGameState);
        updateSliderBitboards();
        hasCachedInCheckValue = true;
        cachedInCheckValue = false;
    }

    public void unmakeNullMove() {
        isWhiteToMove = !isWhiteToMove;
        plyCount--;
        gameStateHistory.pop();
        currentGameState = gameStateHistory.peek();
        updateSliderBitboards();
        hasCachedInCheckValue = true;
        cachedInCheckValue = false;
    }

    public boolean isInCheck() {
        if(hasCachedInCheckValue) {
            return cachedInCheckValue;
        } else {
            cachedInCheckValue = calculateInCheckState();
            hasCachedInCheckValue = true;
            return cachedInCheckValue;
        }
    }

    public boolean calculateInCheckState() {
        int thisKingSquare = kingSquare[moveColourIndex];
        long blockers = allPiecesBitboard;

        if(enemyOrthogonalSliders != 0) {
            long rookAttacks = Magic.getRookAttacks(thisKingSquare, blockers);
            if((rookAttacks & enemyOrthogonalSliders) != 0) {
                return true;
            }
        }
        if(enemyDiagonalSliders != 0) {
            long bishopAttacks = Magic.getBishopAttacks(thisKingSquare, blockers);
            if((bishopAttacks & enemyDiagonalSliders) != 0) {
                return true;
            }
        }
        long enemyKnights = pieceBitboards[bitPiece.makePiece(bitPiece.KNIGHT, opponentColour)];
        if((BitBoardUtility.KNIGHT_ATTACKS[thisKingSquare] & enemyKnights) != 0) {
            return true;
        }
        long enemyPawns = pieceBitboards[bitPiece.makePiece(bitPiece.PAWN, opponentColour)];
        long pawnAttackMask = isWhiteToMove ? BitBoardUtility.WHITE_PAWN_ATTACKS[thisKingSquare] : BitBoardUtility.BLACK_PAWN_ATTACKS[thisKingSquare];
        if((pawnAttackMask & enemyPawns) != 0) {
            return true;
        }

        return false;
    }

    public void loadStartPosition() {
        loadPosition(FenUtility.START_POSITION_FEN);
    }

    public void loadPosition(String fen) {
        FenUtility.PositionInfo posInfo = FenUtility.positionFromFen(fen);
        loadPosition(posInfo);
    }

    public void loadPosition(FenUtility.PositionInfo posInfo) {
        startPositionInfo = posInfo;
        Initialize();

        for(int squareIndex = 0; squareIndex < 64; squareIndex++) {
            int piece = posInfo.squares.get(squareIndex);
            int pieceType = bitPiece.pieceType(piece);
            int colourIndex = bitPiece.isWhite(piece) ? WhiteIndex : BlackIndex;
            square[squareIndex] = piece;

            if(piece != bitPiece.NONE) {
                BitBoardUtility.setSquare(pieceBitboards[pieceType], squareIndex);
                BitBoardUtility.setSquare(colourBitboards[colourIndex], squareIndex);

                if(pieceType == bitPiece.KING) {
                    kingSquare[colourIndex] = squareIndex;
                } else {
                    allPieceLists[piece].addPieceAtSquare(squareIndex);
                }
                totalPieceCountWithoutPawnsAndKings += (pieceType == bitPiece.PAWN || pieceType == bitPiece.KING) ? 0 : 1;
            }
        }

        //side to move
        isWhiteToMove = posInfo.whiteToMove;

        //set extra bitboards
        allPiecesBitboard = colourBitboards[WhiteIndex] | colourBitboards[BlackIndex];
        updateSliderBitboards();

        //create gamestate
        int whiteCastle = ((posInfo.whiteCastleKingside ? 1 << 0 : 0) | ((posInfo.whiteCastleQueenside ? 1 << 1 : 0)));
        int blackCastle = ((posInfo.blackCastleKingside ? 1 << 2 : 0) | ((posInfo.blackCastleQueenside ? 1 << 3 : 0)));
        int castlingRights = whiteCastle | blackCastle;

        plyCount = (posInfo.moveCount - 1) * 2 + (isWhiteToMove ? 0 : 1);

        //Set gamestate (note: calculating zobrist key relies on current game state)
        currentGameState = new bitGameState(bitPiece.NONE, posInfo.epFile, castlingRights, posInfo.fiftyMovePlyCount, 0);
        long zobristKey = Zobrist.calculateZobristKey(this);
        currentGameState = new bitGameState(bitPiece.NONE, posInfo.epFile, castlingRights, posInfo.fiftyMovePlyCount, zobristKey);

        repetitionPositionHistory.push(zobristKey);

        gameStateHistory.push(currentGameState);
    }

    @Override
    public String toString() {
        return BoardHelper.createDiagram(this, true, true, true);
    }

    public static bitBoard createBoard(String fen /* = FenUtility.START_POSITION_FEN */)  {
        bitBoard board = new bitBoard();
        board.loadPosition(fen);
        return board;
    }

    public static bitBoard createBoard(bitBoard source) {
        bitBoard board = new bitBoard();
        board.loadPosition(source.startPositionInfo);
        for(int i = 0; i < source.allGameMoves.size(); i++) {
            board.makeMove(source.allGameMoves.get(i), false);
        }
        return board;
    }
        

    // Update piece lists / bitboards based on given move info.
		// Note that this does not account for the following things, which must be handled separately:
		// 1. Removal of a captured piece
		// 2. Movement of rook when castling
		// 3. Removal of pawn from 1st/8th rank during pawn promotion
		// 4. Addition of promoted piece during pawn promotion

    private void movePiece(int piece, int startSquare, int targetSquare) {
        // Move a piece from one square to another (update bitboards)
        BitBoardUtility.toggleSquares(pieceBitboards[piece], startSquare, targetSquare);
        BitBoardUtility.toggleSquares(colourBitboards[moveColourIndex], targetSquare, startSquare);

        allPieceLists[piece].movePiece(startSquare, targetSquare);
        square[startSquare ] = bitPiece.NONE;
        square[targetSquare] = piece;
    }

    void updateSliderBitboards() {
        int friendlyRook = bitPiece.makePiece(bitPiece.ROOK, moveColour);
        int friendlyQueen = bitPiece.makePiece(bitPiece.QUEEN, moveColour);
        int friendlyBishop = bitPiece.makePiece(bitPiece.BISHOP, moveColour);

        friendlyOrthogonalSliders = pieceBitboards[friendlyRook] | pieceBitboards[friendlyQueen];
        friendlyDiagonalSliders = pieceBitboards[friendlyBishop] | pieceBitboards[friendlyQueen];

        int enemyRook = bitPiece.makePiece(bitPiece.ROOK, opponentColour);
        int enemyQueen = bitPiece.makePiece(bitPiece.QUEEN, opponentColour);
        int enemyBishop = bitPiece.makePiece(bitPiece.BISHOP, opponentColour);

        enemyOrthogonalSliders = pieceBitboards[enemyRook] | pieceBitboards[enemyQueen];
        enemyDiagonalSliders = pieceBitboards[enemyBishop] | pieceBitboards[enemyQueen];
    }

    void Initialize() {
        allGameMoves = new ArrayList<bitMove>();
        kingSquare = new int[2];
        Arrays.fill(square, 0);
        
        repetitionPositionHistory = new Stack<Long>();
        gameStateHistory = new Stack<bitGameState>();

        currentGameState = new bitGameState(bitPiece.NONE, 0, 0, 0, 0);
        plyCount = 0;

        knights = new bitPieceList[]{new bitPieceList(10), new bitPieceList(10)};
        pawns = new bitPieceList[]{new bitPieceList(8), new bitPieceList(8)};
        rooks = new bitPieceList[]{new bitPieceList(10), new bitPieceList(10)};
        bishops = new bitPieceList[]{new bitPieceList(10), new bitPieceList(10)};
        queens = new bitPieceList[]{new bitPieceList(9), new bitPieceList(9)};

        allPieceLists = new bitPieceList[bitPiece.MAX_PIECE_INDEX + 1];
        allPieceLists[bitPiece.WHITE_PAWN] = pawns[WhiteIndex];
        allPieceLists[bitPiece.WHITE_KNIGHT] = knights[WhiteIndex];
        allPieceLists[bitPiece.WHITE_BISHOP] = bishops[WhiteIndex];
        allPieceLists[bitPiece.WHITE_ROOK] = rooks[WhiteIndex];
        allPieceLists[bitPiece.WHITE_QUEEN] = queens[WhiteIndex];
        allPieceLists[bitPiece.WHITE_KING] = new bitPieceList(1);

        allPieceLists[bitPiece.BLACK_PAWN] = pawns[BlackIndex];
        allPieceLists[bitPiece.BLACK_KNIGHT] = knights[BlackIndex];
        allPieceLists[bitPiece.BLACK_BISHOP] = bishops[BlackIndex];
        allPieceLists[bitPiece.BLACK_ROOK] = rooks[BlackIndex];
        allPieceLists[bitPiece.BLACK_QUEEN] = queens[BlackIndex];
        allPieceLists[bitPiece.BLACK_KING] = new bitPieceList(1);

        totalPieceCountWithoutPawnsAndKings = 0;

        //Init bitboards
        pieceBitboards = new long[bitPiece.MAX_PIECE_INDEX + 1];
        colourBitboards = new long[2];
        allPiecesBitboard = 0;
    }
}
