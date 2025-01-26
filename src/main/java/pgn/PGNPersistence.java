package pgn;

import engine.Player.Player;
import engine.board.Board;
import engine.board.Move;

public interface PGNPersistence {

    void persistGame(Game game);

    Move getNextBestMove(Board board, Player player, String gameText);

}
