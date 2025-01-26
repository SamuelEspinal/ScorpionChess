/* package engine.Player.ai;

import java.util.*;
import com.google.common.collect.ComparisonChain;

import engine.Alliance;
import engine.Player.MoveTransition;
import engine.Player.Player;
import engine.board.Board;
import engine.board.BoardUtils;
import engine.board.Move;
import engine.board.Move.MoveFactory;

public class CombinedAlphaBetaWithIterativeDeepening implements MoveStrategy {

    private final BoardEvaluator evaluator;
    private final int searchDepth;
    private final MoveSorter moveSorter;
    private final int quiescenceFactor;
    private long boardsEvaluated;
    private long executionTime;
    private int quiescenceCount;
    private int cutOffsProduced;

    private final List<MoveObserver> observers = new ArrayList<>(); // List of observers

    private enum MoveSorter {
        SORT {
            @Override
            Collection<Move> sort(final Collection<Move> moves) {
                List<Move> sortedMoves = new ArrayList<>(moves);
                Collections.sort(sortedMoves, SMART_SORT);
                return sortedMoves;
            }
        };
    
        public static final Comparator<Move> SMART_SORT = new Comparator<Move>() {
            @Override
            public int compare(final Move move1, final Move move2) {
                return ComparisonChain.start()
                        .compareTrueFirst(BoardUtils.isThreatenedBoardImmediate(move1.getBoard()), BoardUtils.isThreatenedBoardImmediate(move2.getBoard()))
                        .compareTrueFirst(move1.isAttack(), move2.isAttack())
                        .compareTrueFirst(move1.isCastlingMove(), move2.isCastlingMove())
                        .compare(move2.getMovedPiece().getPieceValue(), move1.getMovedPiece().getPieceValue())
                        .result();
            }
        };
    
        abstract Collection<Move> sort(Collection<Move> moves);
    }

    public CombinedAlphaBetaWithIterativeDeepening(final int searchDepth, final int quiescenceFactor) {
        this.evaluator = StandardBoardEvaluator.get();
        this.searchDepth = searchDepth;
        this.quiescenceFactor = quiescenceFactor;
        this.moveSorter = MoveSorter.SORT;
        this.boardsEvaluated = 0;
        this.quiescenceCount = 0;
        this.cutOffsProduced = 0;
    }

    @Override
    public String toString() {
        return "AB+MO";
    }

    @Override
    public long getNumBoardsEvaluated() {
        return this.boardsEvaluated;
    }

    public void addObserver(MoveObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(MoveObserver observer) {
        observers.remove(observer);
    }
    
    private void notifyObservers(String message) {
        for (MoveObserver observer : observers) {
            observer.onMoveEvaluated(message);
        }
    }

    @Override
    public Move execute(final Board board) {
        final long startTime = System.currentTimeMillis();
        final Player currentPlayer = board.currentPlayer();
        final Alliance alliance = currentPlayer.getAlliance();
        Move bestMove = MoveFactory.getNullMove();
        int highestSeenValue = Integer.MIN_VALUE;
        int lowestSeenValue = Integer.MAX_VALUE;
        int currentValue;
        int moveCounter = 1;

        Collection<Move> legalMoves = this.moveSorter.sort(board.currentPlayer().getLegalMoves());
        final int numMoves = legalMoves.size();
        System.out.println(board.currentPlayer() + " THINKING with depth = " + this.searchDepth);
        System.out.println("\tOrdered moves! : " + legalMoves);

        MoveOrderingBuilder moveOrderingBuilder = new MoveOrderingBuilder();
        moveOrderingBuilder.setOrder(Ordering.DESC);

        for (final Move move : legalMoves) {
            final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
            this.quiescenceCount = 0;
            final String s;
            if (moveTransition.getMoveStatus().isDone()) {
                final long candidateMoveStartTime = System.nanoTime();
                currentValue = alliance.isWhite() ?
                        min(moveTransition.getToBoard(), this.searchDepth - 1, highestSeenValue, lowestSeenValue) :
                        max(moveTransition.getToBoard(), this.searchDepth - 1, highestSeenValue, lowestSeenValue);

                moveOrderingBuilder.addMoveOrderingRecord(move, currentValue);

                if (alliance.isWhite() && currentValue > highestSeenValue) {
                    highestSeenValue = currentValue;
                    bestMove = move;
                } else if (alliance.isBlack() && currentValue < lowestSeenValue) {
                    lowestSeenValue = currentValue;
                    bestMove = move;
                }
                final String quiescenceInfo = " [h: " + highestSeenValue + " l: " + lowestSeenValue + "] q: " + this.quiescenceCount;
                s = "\t" + toString() + "(" + this.searchDepth + "), m: (" + moveCounter + "/" + numMoves + ") " + move + ", best:  " + bestMove
                        + quiescenceInfo + ", t: " + calculateTimeTaken(candidateMoveStartTime, System.nanoTime());
            } else {
                s = "\t" + toString() + ", m: (" + moveCounter + "/" + numMoves + ") " + move + " is illegal, best: " + bestMove;
            }
            System.out.println(s);
            notifyObservers(s); 
            moveCounter++;
        }

        List<MoveScoreRecord> orderedMoves = moveOrderingBuilder.build();

        System.out.println("Ordered Moves: " + orderedMoves);

        this.executionTime = System.currentTimeMillis() - startTime;
        System.out.printf("%s SELECTS %s [#boards evaluated = %d, time taken = %d ms, eval rate = %.1f cutoffCount = %d prune percent = %.2f\n", 
                          board.currentPlayer(),
                          bestMove, this.boardsEvaluated, this.executionTime, 
                          (1000 * ((double) this.boardsEvaluated / this.executionTime)), 
                          this.cutOffsProduced, 
                          100 * ((double) this.cutOffsProduced / this.boardsEvaluated));
        return bestMove;
    }

    public int max(final Board board, final int depth, final int highest, final int lowest) {
        if (depth == 0 || BoardUtils.isEndGame(board)) {
            this.boardsEvaluated++;
            return this.evaluator.evaluate(board, depth);
        }
        int currentHighest = highest;
        for (final Move move : this.moveSorter.sort((board.currentPlayer().getLegalMoves()))) {
            final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
            if (moveTransition.getMoveStatus().isDone()) {
                currentHighest = Math.max(currentHighest, min(moveTransition.getToBoard(),
                        calculateQuiescenceDepth(board, move, depth), currentHighest, lowest));
                if (lowest <= currentHighest) {
                    System.out.println("Pruning at depth: " + depth + " with currentHighest: " + currentHighest + " and lowest: " + lowest);
                    this.cutOffsProduced++;
                    break;
                }
            }
        }
        return currentHighest;
    }

    public int min(final Board board, final int depth, final int highest, final int lowest) {
        if (depth == 0 || BoardUtils.isEndGame(board)) {
            this.boardsEvaluated++;
            return this.evaluator.evaluate(board, depth);
        }
        int currentLowest = lowest;
        for (final Move move : this.moveSorter.sort((board.currentPlayer().getLegalMoves()))) {
            final MoveTransition moveTransition = board.currentPlayer().makeMove(move);
            if (moveTransition.getMoveStatus().isDone()) {
                currentLowest = Math.min(currentLowest, max(moveTransition.getToBoard(),
                        calculateQuiescenceDepth(board, move, depth), highest, currentLowest));
                if (currentLowest <= highest) {
                    System.out.println("Pruning at depth: " + depth + " with currentHighest: " + currentLowest + " and highest: " + highest);
                    this.cutOffsProduced++;
                    break;
                }
            }
        }
        return currentLowest;
    }

    private int calculateQuiescenceDepth(final Board board, final Move move, final int depth) {
        return depth + this.quiescenceFactor;
    }

    private static String calculateTimeTaken(final long start, final long end) {
        final long timeTaken = (end - start) / 1000000;
        return timeTaken + " ms";
    }

    // Nested class for move ordering
    private static class MoveScoreRecord implements Comparable<MoveScoreRecord> {
        final Move move;
        final int score;

        MoveScoreRecord(final Move move, final int score) {
            this.move = move;
            this.score = score;
        }

        int getScore() {
            return this.score;
        }

        @Override
        public int compareTo(MoveScoreRecord o) {
            return Integer.compare(this.score, o.score);
        }

        @Override
        public String toString() {
            return this.move + " : " + this.score;
        }
    }

    // Enum for move ordering
    enum Ordering {
        ASC {
            @Override
            List<MoveScoreRecord> order(final List<MoveScoreRecord> moveScoreRecords) {
                Collections.sort(moveScoreRecords, Comparator.comparingInt(MoveScoreRecord::getScore));
                return moveScoreRecords;
            }
        },
        DESC {
            @Override
            List<MoveScoreRecord> order(final List<MoveScoreRecord> moveScoreRecords) {
                Collections.sort(moveScoreRecords, Comparator.comparingInt(MoveScoreRecord::getScore).reversed());
                return moveScoreRecords;
            }
        };

        abstract List<MoveScoreRecord> order(final List<MoveScoreRecord> moveScoreRecords);
    }

    // Builder class for move ordering
    private static class MoveOrderingBuilder {
        List<MoveScoreRecord> moveScoreRecords;
        Ordering ordering;

        MoveOrderingBuilder() {
            this.moveScoreRecords = new ArrayList<>();
            this.ordering = Ordering.ASC; // Default ordering
        }

        void addMoveOrderingRecord(final Move move, final int score) {
            this.moveScoreRecords.add(new MoveScoreRecord(move, score));
        }

        void setOrder(final Ordering ordering) {
            this.ordering = ordering;
        }

        List<MoveScoreRecord> build() {
            return this.ordering.order(this.moveScoreRecords);
        }
    }

    // Observer interface
    public interface MoveObserver {
        void onMoveEvaluated(String message);
    }
}
 */