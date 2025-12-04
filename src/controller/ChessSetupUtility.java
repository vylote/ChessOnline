package controller;

import model.*;
import java.util.ArrayList;

public class ChessSetupUtility {

    private static final int WHITE = GameController.WHITE;
    private static final int BLACK = GameController.BLACK;

    public static void setupStandardGame(ArrayList<Piece> pieces) {
        pieces.clear();

        for (int c = 0; c < 8; c++) pieces.add(new Pawn(WHITE, 6, c));
        pieces.add(new Rook(WHITE, 7, 0));
        pieces.add(new Rook(WHITE, 7, 7));
        pieces.add(new Knight(WHITE, 7, 1));
        pieces.add(new Knight(WHITE, 7, 6));
        pieces.add(new Bishop(WHITE, 7, 2));
        pieces.add(new Bishop(WHITE, 7, 5));
        pieces.add(new King(WHITE, 7, 4));
        pieces.add(new Queen(WHITE, 7, 3));

        for (int c = 0; c < 8; c++) pieces.add(new Pawn(BLACK, 1, c));
        pieces.add(new Rook(BLACK, 0, 0));
        pieces.add(new Rook(BLACK, 0, 7));
        pieces.add(new Knight(BLACK, 0, 1));
        pieces.add(new Knight(BLACK, 0, 6));
        pieces.add(new Bishop(BLACK, 0, 2));
        pieces.add(new Bishop(BLACK, 0, 5));
        pieces.add(new King(BLACK, 0, 4));
        pieces.add(new Queen(BLACK, 0, 3));
    }

    // --- CÁC HÀM TEST THẾ CỜ (Tất cả đều là static và nhận tham số pieces) ---

    /** Test: K+B vs K+B CÙNG MÀU Ô (Sửa lỗi cho hòa thiếu vật chất) */
    public static void testDraw_KBVsKB_SameColor(ArrayList<Piece> pieces) {
        pieces.clear();
        // Tượng Trắng F1 (7, 5) -> Ô Sáng
        pieces.add(new King(WHITE, 7, 6));
        pieces.add(new Bishop(WHITE, 7, 5));
        // Tượng Đen E8 (0, 4) -> Ô Sáng (Sửa từ F8: 0, 5)
        pieces.add(new King(BLACK, 0, 6));
        pieces.add(new Bishop(BLACK, 0, 4));
        // LƯU Ý: Lượt đi phải được thiết lập trong GameController
    }

    /** Test: Chiếu hết tùy chỉnh */
    public static void testCustomCheckmate(ArrayList<Piece> pieces) {
        pieces.clear();
        pieces.add(new Rook(WHITE, 7, 1));
        pieces.add(new King(WHITE, 7, 6));
        pieces.add(new Pawn(WHITE, 6, 5));
        pieces.add(new Pawn(WHITE, 6, 6));
        pieces.add(new Pawn(WHITE, 6, 7));
        pieces.add(new King(BLACK, 0, 6));
        pieces.add(new Pawn(BLACK, 1, 5));
        pieces.add(new Pawn(BLACK, 1, 6));
        pieces.add(new Pawn(BLACK, 1, 7));
    }

    /** Test: Anastasia's Mate */
    public static void testAnastasiasMate(ArrayList<Piece> pieces) {
        pieces.clear();
        pieces.add(new Knight(WHITE, 1, 4));
        pieces.add(new Rook(WHITE, 5, 4));
        pieces.add(new King(WHITE, 7, 6));
        pieces.add(new King(BLACK, 1, 7));
        pieces.add(new Pawn(BLACK, 1, 6));
    }

    /** Test: Chiếu hết Tượng 1 */
    public static void testBishopCheckmate1(ArrayList<Piece> pieces) {
        pieces.clear();
        pieces.add(new King(WHITE, 7, 6));
        pieces.add(new Bishop(WHITE, 5, 6));
        pieces.add(new King(BLACK, 0, 7));
        pieces.add(new Pawn(BLACK, 1, 7));
        pieces.add(new Bishop(BLACK, 0, 6));
    }

    /** Test: Chiếu hết Mã */
    public static void testKnightCheckmate(ArrayList<Piece> pieces) {
        pieces.clear();
        pieces.add(new King(WHITE, 7, 2));
        pieces.add(new Rook(WHITE, 7, 6));
        pieces.add(new Knight(WHITE, 3, 4));
        pieces.add(new King(BLACK, 0, 7));
        pieces.add(new Pawn(BLACK, 1, 7));
    }
}