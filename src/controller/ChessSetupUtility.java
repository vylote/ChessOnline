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
    /** 1️⃣ Test: Vua vs Vua (Hòa ngay lập tức) */
    public static void testDraw_KingVsKing(ArrayList<Piece> pieces) {
        pieces.clear();
        pieces.add(new King(WHITE, 7, 4));
        pieces.add(new King(BLACK, 0, 4));
    }

    /** 2️⃣ Test: Vua + Tượng vs Vua (Hòa thiếu quân) */
    public static void testDraw_KingBishopVsKing(ArrayList<Piece> pieces) {
        pieces.clear();
        pieces.add(new King(WHITE, 7, 4));
        pieces.add(new Bishop(WHITE, 7, 2)); // Tượng trắng
        pieces.add(new King(BLACK, 0, 4));
    }

    /** 3️⃣ Test: Vua + Mã vs Vua (Hòa thiếu quân) */
    public static void testDraw_KingKnightVsKing(ArrayList<Piece> pieces) {
        pieces.clear();
        pieces.add(new King(WHITE, 7, 4));
        pieces.add(new Knight(WHITE, 7, 1)); // Mã trắng
        pieces.add(new King(BLACK, 0, 4));
    }

    /** 4️⃣ Test: Vua + Tượng vs Vua + Tượng (CÙNG màu ô -> HÒA) */
    public static void testDraw_BishopsSameColor(ArrayList<Piece> pieces) {
        pieces.clear();
        // Tượng Trắng ở C1 (7, 2) -> Ô tối (Dark square)
        pieces.add(new King(WHITE, 7, 4));
        pieces.add(new Bishop(WHITE, 7, 2));

        // Tượng Đen ở F8 (0, 5) -> Ô tối (Dark square)
        pieces.add(new King(BLACK, 0, 4));
        pieces.add(new Bishop(BLACK, 0, 5));
        // Vì 7+2=9 (lẻ) và 0+5=5 (lẻ) -> Theo logic bàn cờ của bạn,
        // hãy đảm bảo hàm isInsufficientMaterial check (row+col)%2 giống nhau.
    }

    /** 5️⃣ Test: Vua + Tượng vs Vua + Tượng (KHÁC màu ô -> KHÔNG tự động hòa) */
    public static void testNoDraw_BishopsDifferentColor(ArrayList<Piece> pieces) {
        pieces.clear();
        // Tượng Trắng ở C1 (7, 2) -> Ô tối
        pieces.add(new King(WHITE, 7, 4));
        pieces.add(new Bishop(WHITE, 7, 2));

        // Tượng Đen ở C8 (0, 2) -> Ô sáng (Light square)
        pieces.add(new King(BLACK, 0, 4));
        pieces.add(new Bishop(BLACK, 0, 2));
        // Trận đấu này sẽ tiếp tục cho đến khi có Stalemate hoặc luật khác.
    }

    /** Test: Chiếu hết căn bản bằng Hậu (Scholar's Mate - Tương tự) */
    public static void testCheckmate_Queen(ArrayList<Piece> pieces) {
        pieces.clear();
        // Vua trắng bị dồn vào góc
        pieces.add(new King(WHITE, 7, 7));
        // Hậu đen chiếu trực diện, được bảo vệ bởi Tốt đen
        pieces.add(new Queen(BLACK, 6, 7));
        pieces.add(new Pawn(BLACK, 5, 6));
        // Vua đen ở vị trí bất kỳ
        pieces.add(new King(BLACK, 0, 0));
    }

    /** Test: Chiếu hết hàng cuối (Back Rank Mate) - Lỗi phổ biến của người mới */
    public static void testCheckmate_BackRank(ArrayList<Piece> pieces) {
        pieces.clear();
        // Vua trắng bị chặn bởi chính quân Tốt của mình
        pieces.add(new King(WHITE, 7, 7));
        pieces.add(new Pawn(WHITE, 6, 5));
        pieces.add(new Pawn(WHITE, 6, 6));
        pieces.add(new Pawn(WHITE, 6, 7));

        // Xe đen xuống hàng cuối chiếu hết
        pieces.add(new Rook(BLACK, 7, 0));
        pieces.add(new King(BLACK, 0, 0));
    }

    /** Test: Chiếu hết thắt nút (Smothered Mate) - Vua bị vây bởi quân mình và bị Mã chiếu */
    public static void testCheckmate_Smothered(ArrayList<Piece> pieces) {
        pieces.clear();
        // Vua trắng bị kẹt cứng
        pieces.add(new King(WHITE, 7, 7));
        pieces.add(new Rook(WHITE, 7, 6));
        pieces.add(new Pawn(WHITE, 6, 7));
        pieces.add(new Pawn(WHITE, 6, 6));

        // Mã đen nhảy vào chiếu hết
        pieces.add(new Knight(BLACK, 5, 6));
        pieces.add(new King(BLACK, 0, 0));
    }
    /** Test: Thế cờ trong ảnh của bạn (Hậu và Vua Đen chiếu hết Vua Trắng) */
    public static void testCheckmate_ImageScenario(ArrayList<Piece> pieces) {
        pieces.clear();
        // Vua Trắng ở a6 (2, 0)
        pieces.add(new King(WHITE, 2, 0));
        // Hậu Đen ở a1 (7, 0) chiếu trực diện
        pieces.add(new Queen(BLACK, 7, 0));
        // Vua Đen ở c7 (1, 2) khóa các ô thoát b6, b7
        pieces.add(new King(BLACK, 1, 2));
        // Xe Đen ở h5 (3, 7) khóa hàng 5
        pieces.add(new Rook(BLACK, 3, 7));
    }
}