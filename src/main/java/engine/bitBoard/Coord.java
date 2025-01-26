package engine.bitBoard;

import engine.bitBoard.helpers.BoardHelper;

public final class Coord implements Comparable<Coord> {
    public final int fileIndex;
    public final int rankIndex;

    // Constructor that takes file and rank indices
    public Coord(int fileIndex, int rankIndex) {
        this.fileIndex = fileIndex;
        this.rankIndex = rankIndex;
    }

    // Constructor that takes a square index and calculates file and rank
    public Coord(int squareIndex) {
        this.fileIndex = BoardHelper.fileIndex(squareIndex);
        this.rankIndex = BoardHelper.rankIndex(squareIndex);
    }

    // Method to determine if the square is a light square
    public boolean isLightSquare() {
        return (fileIndex + rankIndex) % 2 != 0;
    }

    // Method to compare this Coord with another Coord
    @Override
    public int compareTo(Coord other) {
        if (this.fileIndex == other.fileIndex && this.rankIndex == other.rankIndex) {
            return 0;
        }
        return 1; // or you could use Integer.compare(fileIndex, other.fileIndex)
    }

    // Operator overloading equivalent in Java (using methods)
    public Coord add(Coord other) {
        return new Coord(this.fileIndex + other.fileIndex, this.rankIndex + other.rankIndex);
    }

    public Coord subtract(Coord other) {
        return new Coord(this.fileIndex - other.fileIndex, this.rankIndex - other.rankIndex);
    }

    public Coord multiply(int m) {
        return new Coord(this.fileIndex * m, this.rankIndex * m);
    }

    // Checks if the Coord is a valid square on the board
    public boolean isValidSquare() {
        return fileIndex >= 0 && fileIndex < 8 && rankIndex >= 0 && rankIndex < 8;
    }

    // Getter for SquareIndex
    public int getSquareIndex() {
        return BoardHelper.indexFromCoord(this);
    }

    // Getters for fileIndex and rankIndex if needed
    public int getFileIndex() {
        return fileIndex;
    }

    public int getRankIndex() {
        return rankIndex;
    }
}
