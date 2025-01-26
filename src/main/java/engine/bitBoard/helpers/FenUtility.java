package engine.bitBoard.helpers;

import java.util.ArrayList;
import java.util.List;

import engine.bitBoard.Coord;
import engine.bitBoard.bitBoard;
import engine.bitBoard.bitMove;
import engine.bitBoard.bitPiece;

import com.google.common.collect.ImmutableList;

public class FenUtility {
    public static final String START_POSITION_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    // Load position from FEN string
    public static PositionInfo positionFromFen(String fen) {
        PositionInfo loadedPositionInfo = new PositionInfo(fen);
        return loadedPositionInfo;
    }

    /**
     * Get the FEN string of the current position.
     * When alwaysIncludeEPSquare is true, the en passant square will be included
     * in the FEN string even if no enemy pawn is in a position to capture it.
     */
    public static String currentFen(bitBoard board, boolean alwaysIncludeEPSquare) {
       String fen = "";
        for (int rank = 7; rank >= 0; rank--) {
            int numEmptyFiles = 0;
            for (int file = 0; file < 8; file++) {
                int i = rank * 8 + file;
                int piece = board.square[i];
                if (piece != 0) {
                    if (numEmptyFiles != 0) {
                        fen += numEmptyFiles;
                        numEmptyFiles = 0;
                    }
                    boolean isBlack = bitPiece.isColor(piece, bitPiece.BLACK);
                    int pieceType = bitPiece.pieceType(piece);
                    String pieceChar = " ";
                    switch (pieceType) {
                        case bitPiece.ROOK -> pieceChar = "R";
                        case bitPiece.KNIGHT -> pieceChar = "N";
                        case bitPiece.BISHOP -> pieceChar = "B";
                        case bitPiece.QUEEN -> pieceChar = "Q";
                        case bitPiece.KING -> pieceChar = "K";
                        case bitPiece.PAWN -> pieceChar = "P";
                    }
                    fen += (isBlack) ? pieceChar.toLowerCase() : pieceChar;
                } else {
                    numEmptyFiles++;
                }
            }
            if (numEmptyFiles != 0) {
                fen += numEmptyFiles;
            }
            if (rank != 0) {
                fen += '/';
            }
        }

        // Side to move
        fen += (' ');
        fen += (board.isWhiteToMove) ? 'w' : 'b';

        // Castling
        boolean whiteKingside = (board.currentGameState.castlingRights & 1) == 1;
        boolean whiteQueenside = (board.currentGameState.castlingRights >> 1 & 1) == 1;
        boolean blackKingside = (board.currentGameState.castlingRights >> 2 & 1) == 1;
        boolean blackQueenside = (board.currentGameState.castlingRights >> 3 & 1) == 1;
        fen += (' ');
        fen += (whiteKingside) ? "K" : "";
        fen += (whiteQueenside) ? "Q" : "";
        fen += (blackKingside) ? "k" : "";
        fen += (blackQueenside) ? "q" : "";
        fen += ((board.currentGameState.castlingRights) == 0) ? "-" : "";

        // En passant
        fen += (' ');
        int epFileIndex = board.currentGameState.enPassantFile - 1;
        int epRankIndex = (board.isWhiteToMove) ? 5 : 2;

        boolean isEnPassant = epFileIndex != -1;
        boolean includeEP = alwaysIncludeEPSquare || enPassantCanBeCaptured(epFileIndex, epRankIndex, board);
        if (isEnPassant && includeEP) {
            fen += BoardHelper.squareNameFromCoordinate(epFileIndex, epRankIndex);
        } else {
            fen += ('-');
        }

        // 50 move counter
        fen += (' ');
        fen += (board.currentGameState.fiftyMoveCounter);

        // Full-move count (should be one at start, and increase after each move by black)
        fen += (' ');
        fen += (board.plyCount / 2) + 1;

        return fen;
    }

    static boolean enPassantCanBeCaptured(int epFileIndex, int epRankIndex, bitBoard board) {
        Coord captureFromA = new Coord(epFileIndex - 1, epRankIndex + (board.isWhiteToMove ? -1 : 1));
        Coord captureFromB = new Coord(epFileIndex + 1, epRankIndex + (board.isWhiteToMove ? -1 : 1));
        int epCaptureSquare = new Coord(epFileIndex, epRankIndex).getSquareIndex();
        int friendlyPawn = bitPiece.makePiece(bitPiece.PAWN, board.moveColour);

        return canCapture(captureFromA, friendlyPawn, epCaptureSquare, board) || canCapture(captureFromB, friendlyPawn, epCaptureSquare, board);
    }

    private static boolean canCapture(Coord from, int friendlyPawn, int epCaptureSquare, bitBoard bitBoard) {

        boolean isPawnOnSquare = bitBoard.square[from.getSquareIndex()] == friendlyPawn;
        if (from.isValidSquare() && isPawnOnSquare) {

            bitMove move = new bitMove(from.getSquareIndex(), epCaptureSquare, bitMove.EnPassantCaptureFlag);
            bitBoard.makeMove(move, false);
            bitBoard.makeNullMove();
            boolean wasLegalMove = !bitBoard.calculateInCheckState();

            bitBoard.unmakeNullMove();
            bitBoard.unmakeMove(move, false);
            return wasLegalMove;
        }
        return false;
    }

    public static String flipFen(String fen) {
        String flippedFen = "";
        String[] sections = fen.split(" ");

        String[] fenRanks = sections[0].split("/");

        for (int i = fenRanks.length - 1; i >= 0; i--) {
            String rank = fenRanks[i];
            for (char c : rank.toCharArray()) {
                flippedFen += invertCase(c);
            }
            if (i != 0) {
                flippedFen += '/';
            }
        }

        flippedFen += (" ") + (sections[1].charAt(0) == 'w' ? 'b' : 'w');
        String castlingRights = sections[2];
        String flippedRights = "";
        for (char c : "kqKQ".toCharArray()) {
            if (castlingRights.indexOf(c) >= 0) {
                flippedRights += (invertCase(c));
            }
        }
        flippedFen += (" ") + (flippedRights.length() == 0 ? "-" : flippedRights);

        String ep = sections[3];
        String flippedEp = (ep.charAt(0) + "");
        if (ep.length() > 1) {
            flippedEp += (ep.charAt(1) == '6' ? '3' : '6');
        }
        flippedFen += (" ") + (flippedEp);
        flippedFen += (" ") + (sections[4]) + (" ") + (sections[5]);

        return flippedFen;
    }

    private static char invertCase(char c) {
        return Character.isLowerCase(c) ? Character.toUpperCase(c) : Character.toLowerCase(c);
    }

    public static class PositionInfo {
        public final String fen;
        public final List<Integer> squares;

        // Castling rights
        public final boolean whiteCastleKingside;
        public final boolean whiteCastleQueenside;
        public final boolean blackCastleKingside;
        public final boolean blackCastleQueenside;
        // En passant file (1 is a-file, 8 is h-file, 0 means none)
        public int epFile;
        public final boolean whiteToMove;
        // Number of half-moves since last capture or pawn advance
        // (starts at 0 and increments after each player's move)
        public int fiftyMovePlyCount;
        // Total number of moves played in the game
        // (starts at 1 and increments after black's move)
        public int moveCount;

        public PositionInfo(String fen) {
            this.fen = fen;
            int[] squarePieces = new int[64];

            String[] sections = fen.split(" ");

            int file = 0;
            int rank = 7;

            for (char symbol : sections[0].toCharArray()) {
                if (symbol == '/') {
                    file = 0;
                    rank--;
                } else {
                    if (Character.isDigit(symbol)) {
                        file += (int) Character.getNumericValue(symbol);
                    } else {
                        int pieceColour = Character.isUpperCase(symbol) ? bitPiece.WHITE : bitPiece.BLACK;
                        int pieceType = 
                        switch (Character.toLowerCase(symbol)) {
                            case 'k' -> pieceType = bitPiece.KING;
                            case 'p' -> pieceType = bitPiece.PAWN;
                            case 'n' -> pieceType = bitPiece.KNIGHT;
                            case 'b' -> pieceType = bitPiece.BISHOP;
                            case 'r' -> pieceType = bitPiece.ROOK;
                            case 'q' -> pieceType = bitPiece.QUEEN;
                            default -> pieceType = bitPiece.NONE;
                        };

                        squarePieces[rank * 8 + file] = pieceType | pieceColour;
                        file++;
                    }
                }
            }

            List<Integer> squaresList = new ArrayList<>();
            for (int piece : squarePieces) {
                squaresList.add(piece);
            }
            squares = ImmutableList.copyOf(squaresList);

            whiteToMove = (sections[1].equals("w"));

            String castlingRights = sections[2];
            whiteCastleKingside = castlingRights.indexOf('K') >= 0;
            whiteCastleQueenside = castlingRights.indexOf('Q') >= 0;
            blackCastleKingside = castlingRights.indexOf('k') >= 0;
            blackCastleQueenside = castlingRights.indexOf('q') >= 0;

            // Default values
            epFile = 0;
            fiftyMovePlyCount = 0;
            moveCount = 0;

            if (sections.length > 3) {
                String enPassantFileName = sections[3].substring(0, 1);
                if (BoardHelper.FILE_NAMES.contains(enPassantFileName)) {
                    epFile = BoardHelper.FILE_NAMES.indexOf(enPassantFileName) + 1;
                }
            }

            // Half-move clock
            if (sections.length > 4) {
                fiftyMovePlyCount = Integer.parseInt(sections[4]);
            }
            // Full move number
            if (sections.length > 5) {
                moveCount = Integer.parseInt(sections[5]);
            }
        }
    }
}
