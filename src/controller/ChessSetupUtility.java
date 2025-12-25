package controller;

import model.*;
import java.util.List; // Sử dụng Interface List để linh hoạt hơn
import java.util.concurrent.CopyOnWriteArrayList;

public class ChessSetupUtility {

    private static final int WHITE = GameController.WHITE;
    private static final int BLACK = GameController.BLACK;

    // Đổi ArrayList<Piece> thành List<Piece> để chấp nhận cả CopyOnWriteArrayList
    public static void setupStandardGame(List<Piece> pieces) {
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

    /** 1️⃣ Test: Vua vs Vua (Hòa ngay lập tức) */
    public static void testDraw_KingVsKing(List<Piece> pieces) {
        pieces.clear();
        pieces.add(new King(WHITE, 7, 4));
        pieces.add(new King(BLACK, 0, 4));
    }

    /** 2️⃣ Test: Vua + Tượng vs Vua (Hòa thiếu quân) */
    public static void testDraw_KingBishopVsKing(List<Piece> pieces) {
        pieces.clear();
        pieces.add(new King(WHITE, 7, 4));
        pieces.add(new Bishop(WHITE, 7, 2));
        pieces.add(new King(BLACK, 0, 4));
    }

    /** 3️⃣ Test: Vua + Mã vs Vua (Hòa thiếu quân) */
    public static void testDraw_KingKnightVsKing(List<Piece> pieces) {
        pieces.clear();
        pieces.add(new King(WHITE, 7, 4));
        pieces.add(new Knight(WHITE, 7, 1));
        pieces.add(new King(BLACK, 0, 4));
    }

    /** 4️⃣ Test: Vua + Tượng vs Vua + Tượng (CÙNG màu ô -> HÒA) */
    public static void testDraw_BishopsSameColor(List<Piece> pieces) {
        pieces.clear();
        pieces.add(new King(WHITE, 7, 4));
        pieces.add(new Bishop(WHITE, 7, 2));
        pieces.add(new King(BLACK, 0, 4));
        pieces.add(new Bishop(BLACK, 0, 5));
    }

    /** 5️⃣ Test: Vua + Tượng vs Vua + Tượng (KHÁC màu ô -> KHÔNG tự động hòa) */
    public static void testNoDraw_BishopsDifferentColor(List<Piece> pieces) {
        pieces.clear();
        pieces.add(new King(WHITE, 7, 4));
        pieces.add(new Bishop(WHITE, 7, 2));
        pieces.add(new King(BLACK, 0, 4));
        pieces.add(new Bishop(BLACK, 0, 2));
    }

    /** Test: Chiếu hết căn bản bằng Hậu */
    public static void testCheckmate_Queen(List<Piece> pieces) {
        pieces.clear();
        pieces.add(new King(WHITE, 7, 7));
        pieces.add(new Queen(BLACK, 6, 7));
        pieces.add(new Pawn(BLACK, 5, 6));
        pieces.add(new King(BLACK, 0, 0));
    }

    /** Test: Chiếu hết hàng cuối (Back Rank Mate) */
    public static void testCheckmate_BackRank(List<Piece> pieces) {
        pieces.clear();
        pieces.add(new King(WHITE, 7, 7));
        pieces.add(new Pawn(WHITE, 6, 5));
        pieces.add(new Pawn(WHITE, 6, 6));
        pieces.add(new Pawn(WHITE, 6, 7));
        pieces.add(new Rook(BLACK, 7, 0));
        pieces.add(new King(BLACK, 0, 0));
    }

    /** Test: Chiếu hết thắt nút (Smothered Mate) */
    public static void testCheckmate_Smothered(List<Piece> pieces) {
        pieces.clear();
        pieces.add(new King(WHITE, 7, 7));
        pieces.add(new Rook(WHITE, 7, 6));
        pieces.add(new Pawn(WHITE, 6, 7));
        pieces.add(new Pawn(WHITE, 6, 6));
        pieces.add(new Knight(BLACK, 5, 6));
        pieces.add(new King(BLACK, 0, 0));
    }

    /** Test: Thế cờ trong ảnh của bạn */
    public static void testCheckmate_ImageScenario(List<Piece> pieces) {
        pieces.clear();
        pieces.add(new King(WHITE, 2, 0));
        pieces.add(new Queen(BLACK, 7, 0));
        pieces.add(new King(BLACK, 1, 2));
        pieces.add(new Rook(BLACK, 3, 7));
    }
}