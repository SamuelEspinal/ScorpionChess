package engine.Player.ai;

import engine.Player.Player;
import engine.Player.ai.KingSafetyAnalyzer.KingDistance;
import engine.board.Board;
import engine.pieces.Piece;
import static engine.pieces.Piece.PieceType.BISHOP;

import engine.board.Move;

import com.google.common.annotations.VisibleForTesting;

//import java.util.*;


public final class StandardBoardEvaluator
        implements BoardEvaluator {

    //BONUSES
    private final static int CHECK_MATE_BONUS = 10000;
    private final static int CHECK_BONUS = 20;
    private final static int CASTLE_BONUS = 75;
    private final static int MOBILITY_MULTIPLIER = 5;
    private final static int ATTACK_MULTIPLIER = 1;
    private final static int TWO_BISHOPS_BONUS = 25;
    //private final static int COORDINATED_ATTACK_MULTIPLIER = 20;
    //private static final int TRADE_BONUS = 10;
    //private static final int TRADE_PENALTY = 40;
    //private static final int VULNERABLE_PENALTY = 2;

    private static final StandardBoardEvaluator INSTANCE = new StandardBoardEvaluator();

    StandardBoardEvaluator() {
    }

    public static StandardBoardEvaluator get() {
        return INSTANCE;
    }

    @Override
    public int evaluate(final Board board,
                        final int depth) {
        return score(board.whitePlayer(), depth) - score(board.blackPlayer(), depth);
    }

    public String evaluationDetails(final Board board, final int depth) {
        return
               ("White Mobility : " + mobility(board.whitePlayer()) + "\n") +
                "White kingThreats : " + kingThreats(board.whitePlayer(), depth) + "\n" +
                "White attacks : " + attacks(board.whitePlayer()) + "\n" +
                "White castle : " + castle(board.whitePlayer()) + "\n" +
                "White pieceEval : " + pieceEvaluations(board.whitePlayer()) + "\n" +
                "White pawnStructure : " + pawnStructure(board.whitePlayer()) + "\n" +
                "---------------------\n" +
                "Black Mobility : " + mobility(board.blackPlayer()) + "\n" +
                "Black kingThreats : " + kingThreats(board.blackPlayer(), depth) + "\n" +
                "Black attacks : " + attacks(board.blackPlayer()) + "\n" +
                "Black castle : " + castle(board.blackPlayer()) + "\n" +
                "Black pieceEval : " + pieceEvaluations(board.blackPlayer()) + "\n" +
                "Black pawnStructure : " + pawnStructure(board.blackPlayer()) + "\n\n" +
                "Final Score = " + evaluate(board, depth);
    }

    @VisibleForTesting
    private static int score(final Player player,
                             final int depth) {

        //GamePhase gamePhase = determineGamePhase(player);

        return  mobility(player) +
                kingThreats(player, depth) +
                attacks(player) +
                castle(player) +
                pieceEvaluations(player) + 
                pawnStructure(player) +
                kingSafety(player);

        /* switch (gamePhase) {
            case OPENING:
                score += pieceActivity(player);
                break;

            case MIDDLEGAME:
                //MID GAME NO BONUSES
                break;

            case ENDGAME:
                score += kingSafety(player); 
                score += pawnStructure(player);
                score += pieceActivity(player) / 2;
                break;
            default:
                throw new IllegalStateException("Unknown game phase: " + gamePhase); 
        return score;*/
    }

    private static int attacks(final Player player) {
        int attackScore = 0;
        //int materialDifference = calculateMaterialBalance(player); // Get current material balance
        
        for (final Move move : player.getLegalMoves()) {
            if (move.isAttack()) {
                final Piece movedPiece = move.getMovedPiece();
                final Piece attackedPiece = move.getAttackedPiece();
    
                if (movedPiece.getPieceValue() <= attackedPiece.getPieceValue()) {
                    attackScore ++;
                }
    
               /*  if (materialDifference > 0) {
                    attackScore += TRADE_BONUS;
                } else if (materialDifference < 0) {
                    attackScore -= TRADE_PENALTY;
                } */
            }
        }

        // Penalize for vulnerable pieces
        /* for (Piece piece : player.getActivePieces()) {
            for (Move opponentMove : player.getOpponent().getLegalMoves()) {
                if (opponentMove.getAttackedPiece() == piece) {
                    attackScore -= piece.getPieceValue() * VULNERABLE_PENALTY;
                }
            }
        } */
        return attackScore * ATTACK_MULTIPLIER;
    }

    private static int pieceEvaluations(final Player player) {
        int pieceValuationScore = 0;
        int numBishops = 0;
        for (final Piece piece : player.getActivePieces()) {
            pieceValuationScore += piece.getPieceValue() + piece.locationBonus();
            if(piece.getPieceType() == BISHOP) {
                numBishops++;
            }
        }
        return pieceValuationScore + (numBishops == 2 ? TWO_BISHOPS_BONUS : 0);
    }

    private static int mobility(final Player player) {
        return MOBILITY_MULTIPLIER * mobilityRatio(player);
    }

    private static int mobilityRatio(final Player player) {
        return (int)((player.getLegalMoves().size() * 10.0f) / player.getOpponent().getLegalMoves().size());
    }

    private static int kingThreats(final Player player,
                                   final int depth) {
        return player.getOpponent().isInCheckMate() ? CHECK_MATE_BONUS * depthBonus(depth) : check(player);
    }

    private static int check(final Player player) {
        return player.getOpponent().isInCheck() ? CHECK_BONUS : 0;
    }

    private static int depthBonus(final int depth) {
        return depth == 0 ? 1 : 100 * depth;
    }

    private static int castle(final Player player) {
        return player.isCastled() ? CASTLE_BONUS : 0;
    }

    private static int pawnStructure(final Player player) {
        return PawnStructureAnalyzer.get().pawnStructureScore(player);
    }

    private static int kingSafety(final Player player) {
        final KingDistance kingDistance = KingSafetyAnalyzer.get().calculateKingTropism(player);
        return ((kingDistance.getEnemyPiece().getPieceValue() / 100) * kingDistance.getDistance());
    }

/*  private static int rookStructure(final Player player, final Board board) {
        return RookStructureAnalyzer.getInstance().rookStructureScore(player, board);
    } */

    /* private static int pieceActivity(final Player player) {
        int activityScore = 0;
        for (final Piece piece : player.getActivePieces()) {
            activityScore += evaluatePieceCentralizationBonus(piece.getPiecePosition());
        }
        return activityScore;
    }

    private static int evaluatePieceCentralizationBonus(final int position) {
        int bonus = 0;
    
        int[][] centerProximityBonus = {
            {1, 2, 3, 4, 4, 3, 2, 1}, // Row 1 (a1-h1)
            {2, 3, 4, 6, 6, 4, 3, 2}, // Row 2 (a2-h2)
            {3, 4, 6, 8, 8, 6, 4, 3}, // Row 3 (a3-h3)
            {4, 6, 8,10,10, 8, 6, 4}, // Row 4 (a4-h4)
            {4, 6, 8,10,10, 8, 6, 4}, // Row 5 (a5-h5)
            {3, 4, 6, 8, 8, 6, 4, 3}, // Row 6 (a6-h6)
            {2, 3, 4, 6, 6, 4, 3, 2}, // Row 7 (a7-h7)
            {1, 2, 3, 4, 4, 3, 2, 1}  // Row 8 (a8-h8)
        };
    
        int row = position / 8;
        int col = position % 8;
    
        bonus += centerProximityBonus[row][col];
    
        return bonus;
    }

    private static int pieceCoordination(final Player player, final Board board) {
        int coordinationScore = 0;
        
        //MOVED TO ROOK STRUCTURE ANALYZER
        //coordinationScore += connectedRooksBonus(player);
        
        coordinationScore += attackCoordinationBonus(player, board);
        
        return coordinationScore;
    }

    private static int attackCoordinationBonus(final Player player, final Board board) {
        int attackCoordination = 0;
        Map<Integer, Integer> squareAttackCount = new HashMap<>();
        
        for (final Piece piece : player.getActivePieces()) {
            for (final Move move : piece.calculateLegalMoves(board)) {
                if (move.isAttack()) { 
                    int destination = move.getDestinationCoordinate();
                    squareAttackCount.put(destination, squareAttackCount.getOrDefault(destination, 0) + 1);
                }
            }
        }
        for (int attacksOnSquare : squareAttackCount.values()) {
            if (attacksOnSquare > 1) {
                attackCoordination += (attacksOnSquare - 1) * COORDINATED_ATTACK_MULTIPLIER;
            }
        }
        
        return attackCoordination;
    }

    private static int calculateMaterialBalance(final Player player) {
        int playerMaterial = getTotalMaterialValue(player);
        int opponentMaterial = getTotalMaterialValue(player.getOpponent());
        
        return playerMaterial - opponentMaterial;
    }

    private static int getTotalMaterialValue(final Player player) {
        int materialValue = 0;
        for (Piece piece : player.getActivePieces()) {
            materialValue += piece.getPieceType().getPieceValue();
        }
        return materialValue;
    }


    //KEEP THIS AS LAST METHOD
    private static GamePhase determineGamePhase(final Player player) {
        int totalPieces = player.getActivePieces().size() + player.getOpponent().getActivePieces().size();
    
        if (totalPieces > 24) {
            return GamePhase.OPENING;
        } else if (totalPieces > 16) {
            return GamePhase.MIDDLEGAME;
        } else {
            return GamePhase.ENDGAME;
        }
    }
    
    private enum GamePhase {
        OPENING,
        MIDDLEGAME,
        ENDGAME
    } */
}
    
