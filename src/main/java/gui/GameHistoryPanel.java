package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import engine.board.*;

import gui.Table.MoveLog;

public class GameHistoryPanel extends JPanel{
    
    private final DataModel model;
    private final JScrollPane scrollPane;
    private static final Dimension HISTORY_PANEL_DIMENSION = new Dimension(100, 400);

    GameHistoryPanel() {

        this.setLayout(new BorderLayout());
        this.model = new DataModel();
        final JTable table = new JTable(model);
        table.setRowHeight(15);
        this.scrollPane = new JScrollPane(table);
        scrollPane.setColumnHeaderView(table.getTableHeader());
        scrollPane.setPreferredSize(HISTORY_PANEL_DIMENSION);
        this.add(scrollPane, BorderLayout.CENTER);
        this.setVisible(true);
    }

    void redo(final Board board, 
              final MoveLog moveHistory) {
        int currentRow = 0;
        this.model.clear();
        for(final Move move : moveHistory.getMoves()) {
            final String moveText = move.toString();
            if(move.getMovedPiece().getPieceAlliance().isWhite()) {
                this.model.setValueAt(moveText, currentRow, 0);
            } else if(move.getMovedPiece().getPieceAlliance().isBlack()) {
                this.model.setValueAt(moveText, currentRow, 1);
                currentRow++;
            }
        }

        if(moveHistory.getMoves().size() > 0) {
            final Move lastMove = moveHistory.getMoves().get(moveHistory.size() - 1);
            final String moveText = lastMove.toString();
            if(lastMove.getMovedPiece().getPieceAlliance().isWhite()) {
                this.model.setValueAt(moveText + calculateCheckAndCheckMateHash(board), currentRow, 0);
            } else if(lastMove.getMovedPiece().getPieceAlliance().isBlack()) {
                this.model.setValueAt(moveText + calculateCheckAndCheckMateHash(board), currentRow -1 , 1);
            }
        }

        final JScrollBar vertical = this.scrollPane.getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
    }

    private String calculateCheckAndCheckMateHash(final Board board) {
        if(board.currentPlayer().isInCheckMate()) {
            return "#";
        } else if(board.currentPlayer().isInCheck()) {
            return "+";
        }
        return "";
    }

    private static class DataModel extends DefaultTableModel {

        private final List<Row> values;
        private static final String[] NAMES = {"White", "Black"};

        DataModel() {
            this.values = new java.util.ArrayList<>();
        }

        public void clear() {
            this.values.clear();
            setRowCount(0);
        }

        @Override
        public int getRowCount() {
            if(this.values == null) {
                return 0;
            }
            return this.values.size();
        }

        @Override
        public int getColumnCount() {
            return NAMES.length;
        }

        @Override
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            final Row CurrentRow = this.values.get(rowIndex);
            if(columnIndex == 0) {
                return CurrentRow.getWhiteMove();
            } else if(columnIndex == 1) {
                return CurrentRow.getBlackMove();
            }
            return null;
        }

        @Override
        public void setValueAt(final Object aValue, 
                               final int rowIndex, 
                               final int columnIndex) {
            final Row currentRow;
            if(this.values.size() <= rowIndex) {
                currentRow = new Row();
                this.values.add(currentRow);
            } else {
                currentRow = this.values.get(rowIndex);
            }
            if(columnIndex == 0) {
                currentRow.setWhiteMove((String)aValue);
                fireTableRowsInserted(rowIndex, rowIndex);
            } else if(columnIndex == 1) {
                currentRow.setBlackMove((String)aValue);
                fireTableRowsUpdated(rowIndex, columnIndex);
            }
            
        }

        @Override
        public Class<?> getColumnClass(final int columnIndex) {
            return Move.class;
        }

        @Override
        public String getColumnName(final int column) { 
            return NAMES[column];
        }
    }

    private static class Row {
        private String whiteMove;
        private String blackMove;

        Row() {

        }

        public String getWhiteMove() {
            return this.whiteMove;
        }        

        public String getBlackMove() {  
            return this.blackMove;
        }

        public void setWhiteMove(final String move) {
            this.whiteMove = move;
        }

        public void setBlackMove(final String move) {
            this.blackMove = move;
        }
    }
}
