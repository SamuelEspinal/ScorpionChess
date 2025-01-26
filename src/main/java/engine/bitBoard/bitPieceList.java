package engine.bitBoard;

public class bitPieceList {
    
     // Indices of squares occupied by given piece type (only elements up to numPieces are valid)
     private int[] occupiedSquares;
    
     // Map from index of a square to the index in occupiedSquares array where that square is stored
     private int[] map;
     private int numPieces;
 
     public bitPieceList(int maxPieceCount) {
         occupiedSquares = new int[maxPieceCount]; // Store the positions of the pieces
         map = new int[64];  // A board has 64 squares
         numPieces = 0;  // Initially no pieces
     }
 
     // Overloaded constructor: Default maxPieceCount to 16 if not provided
     public bitPieceList() {
         this(16);  // Calls the above constructor with default value
     }
 
     // Return the number of pieces currently in the list
     public int getCount() {
         return numPieces;
     }
 
     // Add a piece at a given square
     public void addPieceAtSquare(int square) {
         occupiedSquares[numPieces] = square;  // Add the square to the array of occupied squares
         map[square] = numPieces;  // Map the square to the index in the occupiedSquares array
         numPieces++;  // Increment the number of pieces
     }
 
     // Remove a piece from a given square
     public void removePieceAtSquare(int square) {
         int pieceIndex = map[square];  // Find the index of the square in the occupiedSquares array
         occupiedSquares[pieceIndex] = occupiedSquares[numPieces - 1];  // Replace the removed piece with the last piece in the array
         map[occupiedSquares[pieceIndex]] = pieceIndex;  // Update the map to reflect the change
         numPieces--;  // Decrease the number of pieces
     }
 
     // Move a piece from one square to another
     public void movePiece(int startSquare, int targetSquare) {
         int pieceIndex = map[startSquare];  // Get the index of the piece being moved
         occupiedSquares[pieceIndex] = targetSquare;  // Update its position to the target square
         map[targetSquare] = pieceIndex;  // Update the map with the new position
     }
 
     public int get(int index) {
         return occupiedSquares[index];
     }
}
