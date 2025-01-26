package engine.Player.ai;

import engine.board.Board;
import engine.board.Move;

public interface MoveStrategy {

    long getNumBoardsEvaluated();
    
    Move execute(Board board);
}
