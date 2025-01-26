package engine.Player.ai;

import engine.board.Board;

public interface BoardEvaluator {
    
    int evaluate(Board board, int depth);

    
}
