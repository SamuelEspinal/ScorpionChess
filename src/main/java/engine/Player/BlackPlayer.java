package engine.Player;

import java.util.*;

import engine.Alliance;
import engine.board.Board;
import engine.board.BoardUtils;
import engine.board.Move;
import engine.board.Move.KingSideCastleMove;
import engine.board.Move.QueenSideCastleMove;
import engine.pieces.Piece;
import engine.pieces.Rook;

import static engine.pieces.Piece.PieceType.ROOK;

public class BlackPlayer extends Player {

    public BlackPlayer(final Board board, 
                       final Collection<Move> whiteStandardLegalMoves,
                       final Collection<Move> blackStandardLegalMoves) {
        super(board, blackStandardLegalMoves,  whiteStandardLegalMoves);
    }

    @Override
    public Collection<Piece> getActivePieces() {
        return this.board.getBlackPieces();
    }

    @Override
    public Alliance getAlliance() {
        return Alliance.BLACK;
    }

    @Override
    public Player getOpponent() {
        return this.board.whitePlayer();
    }

    @Override
    protected Collection<Move> calculateKingCastles(final Collection<Move> playerLegals, 
                                                    final Collection<Move> opponentLegals) {
        
        final List<Move> kingCastles = new ArrayList<>();
        
        if(this.playerKing.isFirstMove() && !this.isInCheck()) {
            //Blacks King side castle
            if(this.board.getPiece(5) == null && this.board.getPiece(6) == null) {
                final Piece kingSideRook = this.board.getPiece(7);
                if(kingSideRook != null && kingSideRook.isFirstMove()) {
                    if(Player.calculateAttacksOnTile(5, opponentLegals).isEmpty() &&
                       Player.calculateAttacksOnTile(6, opponentLegals).isEmpty() &&
                       kingSideRook.getPieceType() == ROOK) {
                        
                        if (!BoardUtils.isKingPawnTrap(this.board, this.playerKing, 12)) {
                        kingCastles.add(
                                new KingSideCastleMove(this.board, this.playerKing, 6, 
                                                      (Rook) kingSideRook, kingSideRook.getPiecePosition(), 
                                                      5));
                        }
                    }
                }
            }
            //Blacks Queen side castle
            if (this.board.getPiece(1) == null && this.board.getPiece(2) == null &&
                    this.board.getPiece(3) == null) {
                final Piece queenSideRook = this.board.getPiece(0);
                if (queenSideRook != null && queenSideRook.isFirstMove() &&
                        Player.calculateAttacksOnTile(2, opponentLegals).isEmpty() &&
                        Player.calculateAttacksOnTile(3, opponentLegals).isEmpty() &&
                        queenSideRook.getPieceType() == ROOK) {
                    if (!BoardUtils.isKingPawnTrap(this.board, this.playerKing, 12)) {
                        kingCastles.add(
                                new QueenSideCastleMove(this.board, this.playerKing, 2, (Rook) queenSideRook, queenSideRook.getPiecePosition(), 3));
                    }
                }
            }
        }
        return Collections.unmodifiableList(kingCastles);
    }
}
