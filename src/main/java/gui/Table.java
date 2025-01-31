package gui;

import javax.imageio.ImageIO;
import javax.swing.plaf.DimensionUIResource;

import org.checkerframework.checker.units.qual.t;

import static javax.swing.JFrame.setDefaultLookAndFeelDecorated;
//import static pgn.PGNUtilities.persistPGNFile;
//import static pgn.PGNUtilities.writeGameToPGNFile;
//import static pgn.FenUtilities.writeGameToFENFile;
import java.awt.Toolkit;
import java.awt.Dimension;

import engine.Player.MoveTransition;
import engine.Player.Player;
import engine.Player.ai.AlphaBetaWithMoveOrdering;
import engine.Player.ai.MiniMax;
import engine.Player.ai.MoveStrategy;
import engine.Player.ai.StandardBoardEvaluator;
import engine.Player.ai.StockAlphaBeta;
import engine.board.*;
import engine.board.Move.MoveFactory;
import engine.pieces.Piece;
import gui.Table.PlayerType;
//import pgn.FenUtilities;

import com.google.common.collect.Lists;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

@SuppressWarnings("unused")
public class Table {
    //Settings
    private final JFrame gameFrame;
    private final GameHistoryPanel gameHistoryPanel;
    private final TakenPiecesPanel takenPiecesPanel;
    private final DebugPanel debugPanel;
    private final BoardPanel boardPanel;
    private final MoveLog moveLog;
    private final GameSetup gameSetup;
    private Board chessBoard;
    private Move computerMove;
    private Piece sourceTile;
    private Piece humanMovedPiece;
    private BoardDirection boardDirection;
    private String pieceIconPath;
    private boolean highlightLegalMoves;
    private boolean useBook;
    private MyObservable observable;

    private Color lightTileColor = Color.decode("#FFFACD");
    private Color darkTileColor = Color.decode("#593E1A");
    private static String defaultPieceImagesPath = "src/main/resources/art/pieces/simple/";

    private static final Dimension OUTER_FRAME_DIMENSION = new DimensionUIResource(800, 800);
    private static final Dimension BOARD_PANEL_DIMENSION = new DimensionUIResource(800, 700);
    private static final Dimension TILE_PANEL_DIMENSION = new DimensionUIResource(20, 20);

    private static final Table INSTANCE = new Table();

    public Table() {
        //Create and set up the window
        this.gameFrame = new JFrame("Scorpion Chess");
        //Add menu bar
        final JMenuBar tableMenuBar = new JMenuBar();
        populateMenuBar(tableMenuBar);
        this.gameFrame.setJMenuBar(tableMenuBar);
        this.gameFrame.setLayout(new BorderLayout());

        //Create the chess board
        this.chessBoard = Board.createStandardBoard();
        this.boardDirection = BoardDirection.NORMAL;
        this.highlightLegalMoves = true;
        this.useBook = false;
        this.gameHistoryPanel = new GameHistoryPanel();
        this.takenPiecesPanel = new TakenPiecesPanel();
        this.debugPanel = new DebugPanel();
        this.boardPanel = new BoardPanel();
        this.moveLog = new MoveLog();
        this.observable = new MyObservable();
        this.observable.addObserver(new TableGameAIWatcher());
        this.gameSetup = new GameSetup(this.gameFrame, true);
        this.gameFrame.add(this.takenPiecesPanel, BorderLayout.WEST);
        this.gameFrame.add(this.boardPanel, BorderLayout.CENTER);
        this.gameFrame.add(this.gameHistoryPanel, BorderLayout.EAST);
        this.gameFrame.add(this.debugPanel, BorderLayout.SOUTH);
        setDefaultLookAndFeelDecorated(true);
        this.gameFrame.setSize(OUTER_FRAME_DIMENSION);
        this.gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.gameFrame.setResizable(false);
        this.gameFrame.setVisible(true);
    }

    public static Table get() {
        return INSTANCE;
    }

    private JFrame getGameFrame() {
        return this.gameFrame;
    }

    private DebugPanel getDebugPanel() {
        return this.debugPanel;
    }

    private GameSetup getGameSetup() {
        return this.gameSetup;
    }

    private Board getGameBoard() {
        return this.chessBoard;
    }

    private MoveLog getMoveLog() {
        return this.moveLog;
    }

    private BoardPanel getBoardPanel() {
        return this.boardPanel;
    }

    private GameHistoryPanel getGameHistoryPanel() {
        return this.gameHistoryPanel;
    }

    private TakenPiecesPanel getTakenPiecesPanel() {
        return this.takenPiecesPanel;
    }

    private boolean getUseBook() {
        return this.useBook;
    }

    public void show() {
        Table.get().getMoveLog().clear();
        Table.get().getGameHistoryPanel().redo(chessBoard, Table.get().getMoveLog());
        Table.get().getTakenPiecesPanel().redo(Table.get().getMoveLog());
        Table.get().getBoardPanel().drawBoard(Table.get().getGameBoard());
        Table.get().getDebugPanel().redo();
    }

    private void populateMenuBar(final JMenuBar tableMenuBar) {
        tableMenuBar.add(createFileMenu());
        tableMenuBar.add(createPreferencesMenu());
        tableMenuBar.add(createOptionsMenu());
    }

    private static void center(final JFrame frame) {
        final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        final int w = frame.getSize().width;
        final int h = frame.getSize().height;
        final int x = (dim.width - w) / 2;
        final int y = (dim.height - h) / 2;
        frame.setLocation(x, y);
    }

    //TODO implement files menu
    private JMenu createFileMenu() {
        final JMenu filesMenu = new JMenu("File");
        filesMenu.setMnemonic(KeyEvent.VK_F);

        final JMenuItem openPGN = new JMenuItem("Load PGN File", KeyEvent.VK_O);
        openPGN.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int option = chooser.showOpenDialog(Table.get().getGameFrame());
            if (option == JFileChooser.APPROVE_OPTION) {
                //loadPGNFile(chooser.getSelectedFile());
            }
        });
        //filesMenu.add(openPGN);

        final JMenuItem openFEN = new JMenuItem("Load FEN File", KeyEvent.VK_F);
        openFEN.addActionListener(e -> {
            String fenString = JOptionPane.showInputDialog("Input FEN");
            if(fenString != null) {
                undoAllMoves();
                //chessBoard = FenUtilities.createGameFromFEN(fenString);
                Table.get().getBoardPanel().drawBoard(chessBoard);
            }
        });
        filesMenu.add(openFEN);

        final JMenuItem saveToFEN = new JMenuItem("Export Game as Fen", KeyEvent.VK_S);
        saveToFEN.addActionListener(e -> {
            final JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileFilter() {
                @Override
                public String getDescription() {
                    return ".fen";
                }
                @Override
                public boolean accept(final File file) {
                    return file.isDirectory() || file.getName().toLowerCase().endsWith("fen");
                }
            });
            final int option = chooser.showSaveDialog(Table.get().getGameFrame());
            if (option == JFileChooser.APPROVE_OPTION) {
                saveFENFile(chooser.getSelectedFile());
            }
        });
        filesMenu.add(saveToFEN);


        final JMenuItem saveToPGN = new JMenuItem("Save Game", KeyEvent.VK_S);
        saveToPGN.addActionListener(e -> {
            final JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileFilter() {
                @Override
                public String getDescription() {
                    return ".pgn";
                }
                @Override
                public boolean accept(final File file) {
                    return file.isDirectory() || file.getName().toLowerCase().endsWith("pgn");
                }
            });
            final int option = chooser.showSaveDialog(Table.get().getGameFrame());
            if (option == JFileChooser.APPROVE_OPTION) {
                //savePGNFile(chooser.getSelectedFile());
            }
        });
        //filesMenu.add(saveToPGN);

        final JMenuItem exitMenuItem = new JMenuItem("Exit", KeyEvent.VK_X);
        exitMenuItem.addActionListener(e -> {
            Table.get().getGameFrame().dispose();
            System.exit(0);
        });
        filesMenu.add(exitMenuItem);

        return filesMenu;
    }

    private JMenu createOptionsMenu() {

        final JMenu optionsMenu = new JMenu("Options");
        optionsMenu.setMnemonic(KeyEvent.VK_O);

        final JMenuItem resetMenuItem = new JMenuItem("New Game", KeyEvent.VK_P);
        resetMenuItem.addActionListener(e -> undoAllMoves());
        optionsMenu.add(resetMenuItem);

        final JMenuItem evaluateBoardMenuItem = new JMenuItem("Evaluate Board", KeyEvent.VK_E);
        evaluateBoardMenuItem.addActionListener(e -> System.out.println(StandardBoardEvaluator.get().evaluationDetails(chessBoard, gameSetup.getSearchDepth())));
        optionsMenu.add(evaluateBoardMenuItem);

        final JMenuItem escapeAnalysis = new JMenuItem("Escape Analysis Score", KeyEvent.VK_S);
        escapeAnalysis.addActionListener(e -> {
            final Move lastMove = moveLog.getMoves().get(moveLog.size() - 1);
            if(lastMove != null) {
                System.out.println(MoveUtils.exchangeScore(lastMove));
            }

        });
        optionsMenu.add(escapeAnalysis);

        final JMenuItem legalMovesMenuItem = new JMenuItem("Current State", KeyEvent.VK_L);
        legalMovesMenuItem.addActionListener(e -> {
            System.out.println(chessBoard.getWhitePieces());
            System.out.println(chessBoard.getBlackPieces());
            System.out.println(playerInfo(chessBoard.currentPlayer()));
            System.out.println(playerInfo(chessBoard.currentPlayer().getOpponent()));
        });
        optionsMenu.add(legalMovesMenuItem);

        final JMenuItem undoMoveMenuItem = new JMenuItem("Undo last move", KeyEvent.VK_M);
        undoMoveMenuItem.addActionListener(e -> {
            if(Table.get().getMoveLog().size() > 0) {
                undoLastMove();
            }
        });
        optionsMenu.add(undoMoveMenuItem);

        final JMenuItem setupGameMenuItem = new JMenuItem("Setup Game", KeyEvent.VK_S);
        setupGameMenuItem.addActionListener(e -> {
            Table.get().getGameSetup().promptUser();
            Table.get().setupUpdate(Table.get().getGameSetup());
        });
        optionsMenu.add(setupGameMenuItem);

        return optionsMenu;
    }

    private JMenu createPreferencesMenu() {

        //Main menu for preferences
        final JMenu preferencesMenu = new JMenu("Preferences");

        //Submenu for choosing colors
        final JMenu colorChooserSubMenu = new JMenu("Choose Colors");
        colorChooserSubMenu.setMnemonic(KeyEvent.VK_S);

        final JMenuItem chooseDarkMenuItem = new JMenuItem("Choose Dark Tile Color");
        colorChooserSubMenu.add(chooseDarkMenuItem);

        final JMenuItem chooseLightMenuItem = new JMenuItem("Choose Light Tile Color");
        colorChooserSubMenu.add(chooseLightMenuItem);

        final JMenuItem chooseLegalHighlightMenuItem = new JMenuItem(
            "Choose Legal Move Highlight Color");
        colorChooserSubMenu.add(chooseLegalHighlightMenuItem);

        preferencesMenu.add(colorChooserSubMenu);

        chooseDarkMenuItem.addActionListener(e -> {
            final Color colorChoice = JColorChooser.showDialog(Table.get().getGameFrame(), "Choose Dark Tile Color",
                    Table.get().getGameFrame().getBackground());
            if (colorChoice != null) {
                Table.get().getBoardPanel().setTileDarkColor(chessBoard, colorChoice);
            }
        });

        chooseLightMenuItem.addActionListener(e -> {
            final Color colorChoice = JColorChooser.showDialog(Table.get().getGameFrame(), "Choose Light Tile Color",
                    Table.get().getGameFrame().getBackground());
            if (colorChoice != null) {
                Table.get().getBoardPanel().setTileLightColor(chessBoard, colorChoice);
            }
        });

        //Submenu for choosing chess piece images
        final JMenu chessMenChoiceSubMenu = new JMenu("Choose Chess Piece Image Set");

        final JMenuItem classicPieceMenuItem = new JMenuItem("Original Pieces");
        chessMenChoiceSubMenu.add(classicPieceMenuItem);

        final JMenuItem standardPieceMenuItem = new JMenuItem("Standard Pieces");
        chessMenChoiceSubMenu.add(standardPieceMenuItem);

        classicPieceMenuItem.addActionListener(e -> {
            pieceIconPath = "src/main/resources/art/pieces/simple/";
            Table.get().getBoardPanel().drawBoard(chessBoard);
        });

        //TODO: add standard pieces art from noelle
//        classicPieceMenuItem.addActionListener(e -> {
//            System.out.println("Standard Piece Selected");
//            pieceIconPath = "src/main/resources/art/pieces/standard/";
//            Table.get().getBoardPanel().drawBoard(chessBoard);
//        });

        preferencesMenu.add(chessMenChoiceSubMenu);

        //Submenu for choosing board orientation
        final JMenuItem flipBoardMenu = new JMenuItem("Flip Board");
        flipBoardMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boardDirection = boardDirection.opposite();
                boardPanel.drawBoard(chessBoard);
            }
        });
        preferencesMenu.add(flipBoardMenu);

        //Submenu for choosing legal move highlight
        final JCheckBoxMenuItem legalMoveHighlighterCheckbox = new JCheckBoxMenuItem("Highlight Legal Moves", true);

        legalMoveHighlighterCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                highlightLegalMoves = legalMoveHighlighterCheckbox.isSelected();
            }
        });
        preferencesMenu.add(legalMoveHighlighterCheckbox);

        //Submenu for choosing use book moves vs AI
        final JCheckBoxMenuItem cbUseBookMoves = new JCheckBoxMenuItem(
            "Use Book Moves", false);

        cbUseBookMoves.addActionListener(e -> useBook = cbUseBookMoves.isSelected());

        preferencesMenu.add(cbUseBookMoves);
        
        return preferencesMenu;
    }
    
    private static String playerInfo(final Player player) {
        return ("Player is: " +player.getAlliance() + "\nlegal moves (" +player.getLegalMoves().size()+ ") = " +player.getLegalMoves() + "\ninCheck = " +
                player.isInCheck() + "\nisInCheckMate = " +player.isInCheckMate() +
                "\nisCastled = " +player.isCastled())+ "\n";
    }
    
    private void updateGameBoard(final Board board) {
        this.chessBoard = board;
    }

    private void updateComputerMove(final Move move) {
        this.computerMove = move;
    }

     
    private void undoAllMoves() {
        for(int i = Table.get().getMoveLog().size() - 1; i >= 0; i--) {
            final Move lastMove = Table.get().getMoveLog().removeMove(Table.get().getMoveLog().size() - 1);
            this.chessBoard = this.chessBoard.currentPlayer().unMakeMove(lastMove).getToBoard();
        }
        this.computerMove = null;
        Table.get().getMoveLog().clear();
        Table.get().getGameHistoryPanel().redo(chessBoard, Table.get().getMoveLog());
        Table.get().getTakenPiecesPanel().redo(Table.get().getMoveLog());
        Table.get().getBoardPanel().drawBoard(chessBoard);
        Table.get().getDebugPanel().redo();
    }

   /*  private static void loadPGNFile(final File pgnFile) {
        try {
            persistPGNFile(pgnFile);
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
    } */

    /* private static void savePGNFile(final File pgnFile) {
        try {
            writeGameToPGNFile(pgnFile, Table.get().getMoveLog());
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
    } */

    private static void saveFENFile(final File fenFile) {
        try {
            //FenUtilities.saveFENToFile(fenFile, Table.INSTANCE.getGameBoard());
        }
        catch (final Exception e) {
            e.printStackTrace();
        }
    }
    
    private void undoLastMove() {
        final Move lastMove = Table.get().getMoveLog().removeMove(Table.get().getMoveLog().size() - 1);
        this.chessBoard = this.chessBoard.currentPlayer().unMakeMove(lastMove).getToBoard();
        this.computerMove = null;
        Table.get().getMoveLog().removeMove(lastMove);
        Table.get().getGameHistoryPanel().redo(chessBoard, Table.get().getMoveLog());
        Table.get().getTakenPiecesPanel().redo(Table.get().getMoveLog());
        Table.get().getBoardPanel().drawBoard(chessBoard);
        Table.get().getDebugPanel().redo();
    }

    private void moveMadeUpdate(final PlayerType playerType) {
        observable.notifyObservers(playerType);
    }

    private void setupUpdate(final GameSetup gameSetup) {
        observable.notifyObservers(gameSetup);
    }

    private static class TableGameAIWatcher implements Observer {

        @Override
        public void update(final MyObservable o,
                           final Object arg) {

            if (Table.get().getGameSetup().isAIPlayer(Table.get().getGameBoard().currentPlayer()) &&
                !Table.get().getGameBoard().currentPlayer().isInCheckMate() &&
                !Table.get().getGameBoard().currentPlayer().isInStaleMate()) {
                System.out.println(Table.get().getGameBoard().currentPlayer() + " is set to AI, thinking....");
                final AIThinkTank thinkTank = new AIThinkTank();
                thinkTank.execute();
            }

            if (Table.get().getGameBoard().currentPlayer().isInCheckMate()) {
                JOptionPane.showMessageDialog(Table.get().getBoardPanel(),
                        "Game Over: Player " + Table.get().getGameBoard().currentPlayer() + " is in checkmate!", "Game Over",
                        JOptionPane.INFORMATION_MESSAGE);
            }

            if (Table.get().getGameBoard().currentPlayer().isInStaleMate()) {
                JOptionPane.showMessageDialog(Table.get().getBoardPanel(),
                        "Game Over: Player " + Table.get().getGameBoard().currentPlayer() + " is in stalemate!", "Game Over",
                        JOptionPane.INFORMATION_MESSAGE);
            }

        }

    }

    enum PlayerType {
        HUMAN,
        COMPUTER
    }

    private static class AIThinkTank extends SwingWorker<Move, String> {

        private AIThinkTank() {
        }

        @Override
        protected Move doInBackground() {
            final Move bestMove;
            final int quiescenceFactor = 4;
            //GARBAGE DONT USE THIS GUY BELOW
            //NEEDS WORK HES NOT SMART YET
            final MoveStrategy strategy = new AlphaBetaWithMoveOrdering(Table.get().getGameSetup().getSearchDepth(), quiescenceFactor);
            
            //THIS GUY CHILL
            //final MoveStrategy miniMax = new MiniMax(Table.get().getGameSetup().getSearchDepth());

            //TRYING TO USE THIS GUY
            //final MoveStrategy StockAlphaBeta = new StockAlphaBeta(Table.get().getGameSetup().getSearchDepth());
            //strategy.addObserver(Table.get().getDebugPanel());
            bestMove = strategy.execute(Table.get().getGameBoard());
            return bestMove;
        }

        @Override
        public void done() {
            try {
                final Move bestMove = get();
                Table.get().updateComputerMove(bestMove);
                Table.get().updateGameBoard(Table.get().getGameBoard().currentPlayer().makeMove(bestMove).getToBoard());
                Table.get().getMoveLog().addMove(bestMove);
                Table.get().getGameHistoryPanel().redo(Table.get().getGameBoard(), Table.get().getMoveLog());
                Table.get().getTakenPiecesPanel().redo(Table.get().getMoveLog());
                Table.get().getBoardPanel().drawBoard(Table.get().getGameBoard());
                Table.get().getDebugPanel().redo();
                Table.get().moveMadeUpdate(PlayerType.COMPUTER);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class BoardPanel extends JPanel {

        final List<TilePanel> boardTiles;

        BoardPanel() {
            super(new GridLayout(8,8));
            this.boardTiles = new ArrayList<>();
            for (int i = 0; i < BoardUtils.NUM_TILES; i++) {
                final TilePanel tilePanel = new TilePanel(this, i);
                this.boardTiles.add(tilePanel);
                add(tilePanel);
            }
            setPreferredSize(BOARD_PANEL_DIMENSION);
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            setBackground(Color.decode("#8B4726"));
            validate();
        }

        void drawBoard(final Board board) {
            removeAll();
            for (final TilePanel boardTile : boardDirection.traverse(boardTiles)) {
                boardTile.drawTile(board);
                add(boardTile);
            }
            validate();
            repaint();
        }

        void setTileDarkColor(final Board board,
                              final Color darkColor) {
            for (final TilePanel boardTile : boardTiles) {
                boardTile.setDarkTileColor(darkColor);
            }
            drawBoard(board);
        }

        void setTileLightColor(final Board board,
                                      final Color lightColor) {
            for (final TilePanel boardTile : boardTiles) {
                boardTile.setLightTileColor(lightColor);
            }
            drawBoard(board);
        }

    }

    public enum BoardDirection {
        NORMAL {
            @Override
            List<TilePanel> traverse(final List<TilePanel> boardTiles) {
                return boardTiles;
            }

            @Override
            BoardDirection opposite() {
                return FLIPPED;
            }
        },
        FLIPPED {
            @Override
            List<TilePanel> traverse(final List<TilePanel> boardTiles) {
                return Lists.reverse(boardTiles);
            }
            @Override
            BoardDirection opposite() {
                return NORMAL;
            }
        };

        abstract List<TilePanel> traverse(final List<TilePanel> boardTiles);
        abstract BoardDirection opposite();
    }

    public static class MoveLog {
        private final List<Move> moveLog;

        MoveLog() {
            this.moveLog = new ArrayList<>();
        }
        public List<Move> getMoves() {
            return this.moveLog;
        }
        public void addMove(final Move move) {
            this.moveLog.add(move);
        }
        public int size() {
            return this.moveLog.size();
        }
        void clear() {
            this.moveLog.clear();
        }
        Move removeMove(int index) {
            return this.moveLog.remove(index);
        }
        boolean removeMove(final Move move) {
            return this.moveLog.remove(move);
        }
    }

      private class TilePanel extends JPanel {

        private final int tileId;

        TilePanel(final BoardPanel boardPanel,
                  final int tileId) {
            super(new GridBagLayout());
            this.tileId = tileId;
            setPreferredSize(TILE_PANEL_DIMENSION);
            assignTileColor();
            assignTilePieceIcon(chessBoard);
            highlightTileBorder(chessBoard);
            addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(final MouseEvent event) {

                    if(Table.get().getGameSetup().isAIPlayer(Table.get().getGameBoard().currentPlayer()) ||
                       BoardUtils.isEndGame(Table.get().getGameBoard())) {
                        return;
                    }

                    if (isRightMouseButton(event)) {
                        sourceTile = null;
                        humanMovedPiece = null;
                    } else if (isleftMouseButton(event)) {
                        if (sourceTile == null) {
                            sourceTile = chessBoard.getPiece(tileId);
                            humanMovedPiece = sourceTile;
                            if (humanMovedPiece == null) {
                                sourceTile = null;
                            }
                        } else {
                            final Move move = MoveFactory.createMove(chessBoard, sourceTile.getPiecePosition(),
                                    tileId);
                            final MoveTransition transition = chessBoard.currentPlayer().makeMove(move);
                            if (transition.getMoveStatus().isDone()) {
                                chessBoard = transition.getToBoard();
                                moveLog.addMove(move);
                            }
                            sourceTile = null;
                            humanMovedPiece = null;
                        }
                    }
                    SwingUtilities.invokeLater(() -> {
                        gameHistoryPanel.redo(chessBoard, moveLog);
                        takenPiecesPanel.redo(moveLog);
                        if(gameSetup.isAIPlayer(chessBoard.currentPlayer())) {
                            Table.get().moveMadeUpdate(PlayerType.HUMAN);
                        }
                        boardPanel.drawBoard(chessBoard);
                        debugPanel.redo();
                    });
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                }

                @Override
                public void mouseEntered(final MouseEvent e) {
                }

                @Override
                public void mouseReleased(final MouseEvent e) {
                }

                @Override
                public void mousePressed(final MouseEvent e) {
                }

                private boolean isRightMouseButton(MouseEvent e) {
                    return SwingUtilities.isRightMouseButton(e);

                }

                private boolean isleftMouseButton(MouseEvent e) {
                    return SwingUtilities.isLeftMouseButton(e);
                }
            });
            validate();
        }

        public void drawTile(final Board board) {
            assignTileColor();
            assignTilePieceIcon(board);
            highlightTileBorder(board);
            highlightLegals(board);
            highlightAIMove();
            validate();
            repaint();
        }

        private void assignTilePieceIcon(final Board board) {
            this.removeAll();
            if (board.getPiece(this.tileId) != null) {
                try {
                    String imagePath = "art/pieces/simple/" + 
                        board.getPiece(this.tileId).getPieceAlliance().toString().substring(0, 1) +
                        board.getPiece(this.tileId).toString() + ".gif";
        
                    InputStream imageStream = getClass().getClassLoader().getResourceAsStream(imagePath);
                    if (imageStream == null) {
                        throw new IOException("Image not found: " + imagePath);
                    }
        
                    final BufferedImage image = ImageIO.read(imageStream);
                    add(new JLabel(new ImageIcon(image)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        void setLightTileColor(final Color color) {
            lightTileColor = color;
        }

        void setDarkTileColor(final Color color) {
            darkTileColor = color;
        }

        private void highlightLegals(final Board board) {
            if (highlightLegalMoves) {
                for (final Move move : pieceLegalMoves(board)) {
                    if (move.getDestinationCoordinate() == this.tileId) {
                        try {
                            InputStream imageStream = getClass().getClassLoader().getResourceAsStream("art/misc/green_dot.png");
                            if (imageStream == null) {
                                throw new IOException("Image not found: art/misc/green_dot.png");
                            }
        
                            add(new JLabel(new ImageIcon(ImageIO.read(imageStream))));
                        } catch (Exception error) {
                            error.printStackTrace();
                        }
                    }
                }
            }
        }

        private void highlightAIMove() {
            if(computerMove != null) {
                if(this.tileId == computerMove.getCurrentCoordinate()) {
                    setBackground(Color.pink);
                } else if(this.tileId == computerMove.getDestinationCoordinate()) {
                    setBackground(Color.red);
                }
            }
        }

        private void highlightTileBorder(final Board board) {
            if(humanMovedPiece != null &&
               humanMovedPiece.getPieceAlliance() == board.currentPlayer().getAlliance() &&
               humanMovedPiece.getPiecePosition() == this.tileId) {
                setBorder(BorderFactory.createLineBorder(Color.cyan));
            } else {
                setBorder(BorderFactory.createLineBorder(Color.GRAY));
            }
        }

        private Collection<Move> pieceLegalMoves(final Board board) {
            if(humanMovedPiece != null && humanMovedPiece.getPieceAlliance() == board.currentPlayer().getAlliance()) {
                return humanMovedPiece.calculateLegalMoves(board);
            }
            return Collections.emptyList();
        }

        private void assignTileColor() {
            if (BoardUtils.INSTANCE.FIRST_ROW.get(this.tileId) ||
                BoardUtils.INSTANCE.THIRD_ROW.get(this.tileId) ||
                BoardUtils.INSTANCE.FIFTH_ROW.get(this.tileId) ||
                BoardUtils.INSTANCE.SEVENTH_ROW.get(this.tileId)) {
                setBackground(this.tileId % 2 == 0 ? lightTileColor : darkTileColor);
            } else if(BoardUtils.INSTANCE.SECOND_ROW.get(this.tileId) ||
                      BoardUtils.INSTANCE.FOURTH_ROW.get(this.tileId) ||
                      BoardUtils.INSTANCE.SIXTH_ROW.get(this.tileId)  ||
                      BoardUtils.INSTANCE.EIGHTH_ROW.get(this.tileId)) {
                setBackground(this.tileId % 2 != 0 ? lightTileColor : darkTileColor);
            }
        }       
    }

    protected class MyObservable {
        private final List<Observer> observers = new ArrayList<>();
    
        public void addObserver(Observer observer) {
            observers.add(observer);
        }
    
        public void removeObserver(Observer observer) {
            observers.remove(observer);
        }
    
        public void notifyObservers(Object arg) {
            for (Observer observer : observers) {
                observer.update(this, arg);
            }
        }
    }
    
    protected interface Observer {
        void update(MyObservable o, Object arg);
    }
}


