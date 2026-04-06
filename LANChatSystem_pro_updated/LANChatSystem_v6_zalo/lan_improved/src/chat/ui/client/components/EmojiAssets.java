package chat.ui.client.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.List;

public final class EmojiAssets {

    public record EmojiEntry(String value, String style, String tooltip) {}
    public record EmojiGroup(String title, List<EmojiEntry> entries) {}

    private static final List<EmojiGroup> PICKER_GROUPS = List.of(
        new EmojiGroup("Vui", List.of(
            emoji("\uD83D\uDE00", "smile", "Cuoi"),
            emoji("\uD83D\uDE01", "grin", "Cuoi rang ro"),
            emoji("\uD83D\uDE02", "joy", "Cuoi ra nuoc mat"),
            emoji("\uD83E\uDD23", "lol", "Cuoi lan"),
            emoji("\uD83D\uDE0A", "blush", "De thuong"),
            emoji("\uD83D\uDE0E", "cool", "Ngau"),
            emoji("\uD83D\uDE1B", "tongue", "Le luoi"),
            emoji("\uD83D\uDE1C", "wink_tongue", "Nhay mat"),
            emoji("\uD83E\uDD70", "hearts", "Dang yeu"),
            emoji("\uD83D\uDE18", "kiss", "Hon gio"),
            emoji("\uD83E\uDD17", "hug", "Om"),
            emoji("\uD83E\uDD73", "party", "An mung"),
            emoji("\u2728", "sparkles", "Lung linh"),
            emoji("\u2B50", "star", "Noi bat")
        )),
        new EmojiGroup("Bu\u1ed3n", List.of(
            emoji("\uD83E\uDD79", "plead", "Nhin toi nghiep"),
            emoji("\uD83D\uDE22", "cry", "Khoc"),
            emoji("\uD83D\uDE2D", "cry_loud", "Khoc lon"),
            emoji("\uD83E\uDD7A", "tear_smile", "Cam dong"),
            emoji("\uD83D\uDE15", "sad", "Buon"),
            emoji("\uD83D\uDE30", "sweat", "Cang thang"),
            emoji("\uD83D\uDE05", "sweat_smile", "Ngai"),
            emoji("\uD83D\uDE34", "sleep", "Ngu"),
            emoji("\uD83D\uDE2A", "sleepy", "Buon ngu"),
            emoji("\uD83D\uDE0C", "relief", "Tho phao"),
            emoji("\uD83E\uDD2D", "peek", "Nhin len"),
            emoji("\uD83D\uDE33", "flushed", "Bat ngo")
        )),
        new EmojiGroup("Y\u00eau th\u00edch", List.of(
            emoji("\u2764\uFE0F", "heart", "Tim"),
            emoji("\uD83D\uDE0D", "heart_eyes", "Me man"),
            emoji("\uD83D\uDC95", "heart_orbit", "Tim bay"),
            emoji("\uD83D\uDD25", "fire", "Chat"),
            emoji("\uD83D\uDC4D", "thumb", "Thich"),
            emoji("\uD83D\uDE4F", "pray", "Cam on"),
            emoji("\uD83D\uDC4F", "clap", "Vo tay"),
            emoji("\uD83D\uDCA1", "bulb", "Y tuong"),
            emoji("\uD83C\uDF81", "gift", "Qua"),
            emoji("\uD83D\uDE0E", "cool", "Tuyet"),
            emoji("\uD83E\uDD17", "hug", "Than thien"),
            emoji("\uD83D\uDE18", "kiss", "Thuong")
        )),
        new EmojiGroup("Kh\u00e1c", List.of(
            emoji("\uD83D\uDE21", "angry", "Tuc gian"),
            emoji("\uD83D\uDE24", "huff", "Bat binh"),
            emoji("\uD83D\uDE44", "roll", "Chan"),
            emoji("\uD83E\uDD10", "zip", "Im lang"),
            emoji("\uD83E\uDD14", "think", "Suy nghi"),
            emoji("\uD83D\uDE16", "scrunch", "Kho chiu"),
            emoji("\uD83D\uDCAF", "hundred", "100 diem"),
            emoji("\uD83D\uDE80", "rocket", "Tang toc"),
            emoji("\uD83C\uDF89", "confetti", "Chuc mung"),
            emoji("\u2728", "sparkles", "Toa sang"),
            emoji("\uD83D\uDCA1", "bulb", "Nhay so"),
            emoji("\u2B50", "star", "Danh dau")
        ))
    );

    private static final List<EmojiEntry> QUICK_REACTIONS = List.of(
        emoji("\uD83D\uDC4D", "thumb", "Thich"),
        emoji("\u2764\uFE0F", "heart", "Tim"),
        emoji("\uD83D\uDE02", "joy", "Cuoi"),
        emoji("\uD83E\uDD79", "plead", "Thuong"),
        emoji("\uD83D\uDE2D", "cry_loud", "Khoc"),
        emoji("\uD83D\uDE21", "angry", "Gian")
    );

    private EmojiAssets() {}

    public static List<EmojiGroup> pickerGroups() {
        return PICKER_GROUPS;
    }

    public static List<EmojiEntry> quickReactions() {
        return QUICK_REACTIONS;
    }

    public static Icon createIcon(String style, int size) {
        return new ImageIcon(render(style, size));
    }

    private static EmojiEntry emoji(String value, String style, String tooltip) {
        return new EmojiEntry(value, style, tooltip);
    }

    private static BufferedImage render(String style, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        switch (style) {
            case "thumb" -> drawThumb(g, size);
            case "heart" -> drawHeart(g, size);
            case "heart_orbit" -> {
                drawBaseFace(g, size);
                drawEyes(g, size, 0);
                drawSmile(g, size, true);
                drawHeartsAround(g, size);
            }
            case "fire" -> drawFire(g, size);
            case "party", "confetti" -> drawParty(g, size);
            case "hundred" -> drawHundred(g, size);
            case "pray" -> drawPray(g, size);
            case "clap" -> drawClap(g, size);
            case "sparkles" -> drawSparkles(g, size);
            case "bulb" -> drawBulb(g, size);
            case "gift" -> drawGift(g, size);
            case "rocket" -> drawRocket(g, size);
            case "star" -> drawStarBadge(g, size);
            default -> drawFace(g, size, style);
        }

        g.dispose();
        return img;
    }

    private static void drawFace(Graphics2D g, int s, String style) {
        drawBaseFace(g, s);
        switch (style) {
            case "smile" -> { drawEyes(g, s, 0); drawSmile(g, s, false); }
            case "plead" -> { drawBigEyes(g, s); drawSmallMouth(g, s); }
            case "heart_eyes" -> { drawHeartEyes(g, s); drawSmile(g, s, false); }
            case "joy" -> { drawClosedLaughEyes(g, s); drawOpenLaugh(g, s); drawTears(g, s, true, true); }
            case "cool" -> { drawSunglasses(g, s); drawSmile(g, s, false); }
            case "cry_loud" -> { drawSadEyes(g, s); drawCryMouth(g, s); drawTears(g, s, true, true); }
            case "blush" -> { drawEyes(g, s, 0); drawSmile(g, s, true); drawBlush(g, s); }
            case "scrunch" -> { drawXEyes(g, s); drawWobbleMouth(g, s); }
            case "sleep" -> { drawSleepEyes(g, s); drawSmallMouth(g, s); drawZ(g, s); }
            case "cry" -> { drawSadEyes(g, s); drawSadMouth(g, s); drawTears(g, s, false, true); }
            case "angry" -> { drawAngryEyes(g, s); drawSadMouth(g, s); }
            case "tongue" -> { drawEyes(g, s, 0); drawTongueMouth(g, s); }
            case "grin" -> { drawEyes(g, s, 0); drawGrin(g, s); }
            case "flushed" -> { drawBigEyes(g, s); drawSmallMouth(g, s); drawBlush(g, s); }
            case "sad" -> { drawEyes(g, s, 0); drawSadMouth(g, s); }
            case "hearts" -> { drawEyes(g, s, 0); drawSmile(g, s, true); drawHeartsAround(g, s); }
            case "tear_smile" -> { drawEyes(g, s, 0); drawSmile(g, s, true); drawTears(g, s, false, true); }
            case "kiss" -> { drawEyes(g, s, 0); drawKissMouth(g, s); drawHeartNear(g, s); }
            case "hug" -> { drawEyes(g, s, 0); drawSmile(g, s, true); drawHands(g, s); }
            case "relief" -> { drawSleepEyes(g, s); drawSmile(g, s, true); }
            case "sleepy" -> { drawSleepEyes(g, s); drawDropMouth(g, s); }
            case "peek" -> { drawPeekEyes(g, s); drawSmallMouth(g, s); }
            case "think" -> { drawThinkingEyes(g, s); drawThinkingHand(g, s); }
            case "wink_tongue" -> { drawWink(g, s); drawTongueMouth(g, s); }
            case "roll" -> { drawRollEyes(g, s); drawFlatMouth(g, s); }
            case "sweat" -> { drawBigEyes(g, s); drawSmallMouth(g, s); drawSweat(g, s); }
            case "lol" -> { drawClosedLaughEyes(g, s); drawOpenLaugh(g, s); }
            case "zip" -> { drawEyes(g, s, 0); drawZipMouth(g, s); }
            case "huff" -> { drawAngryEyes(g, s); drawHuff(g, s); }
            case "sweat_smile" -> { drawEyes(g, s, 0); drawSmile(g, s, true); drawSweat(g, s); }
            default -> { drawEyes(g, s, 0); drawSmile(g, s, false); }
        }
    }

    private static void drawBaseFace(Graphics2D g, int s) {
        g.setPaint(new GradientPaint(0, 0, new Color(255, 244, 140), 0, s, new Color(255, 187, 48)));
        g.fillOval(4, 4, s - 8, s - 8);
        g.setColor(new Color(227, 149, 34));
        g.setStroke(new BasicStroke(1.5f));
        g.drawOval(4, 4, s - 8, s - 8);
    }

    private static void drawEyes(Graphics2D g, int s, int yOff) {
        g.setColor(new Color(64, 54, 48));
        g.fillOval(scale(12, s), scale(14 + yOff, s), scale(5, s), scale(7, s));
        g.fillOval(scale(24, s), scale(14 + yOff, s), scale(5, s), scale(7, s));
    }

    private static void drawBigEyes(Graphics2D g, int s) {
        g.setColor(Color.WHITE);
        g.fillOval(scale(10, s), scale(12, s), scale(8, s), scale(10, s));
        g.fillOval(scale(23, s), scale(12, s), scale(8, s), scale(10, s));
        g.setColor(new Color(64, 54, 48));
        g.fillOval(scale(13, s), scale(15, s), scale(4, s), scale(5, s));
        g.fillOval(scale(26, s), scale(15, s), scale(4, s), scale(5, s));
    }

    private static void drawSleepEyes(Graphics2D g, int s) {
        g.setColor(new Color(64, 54, 48));
        g.setStroke(new BasicStroke(scaleF(2f, s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(scale(10, s), scale(14, s), scale(8, s), scale(6, s), 0, -180);
        g.drawArc(scale(22, s), scale(14, s), scale(8, s), scale(6, s), 0, -180);
    }

    private static void drawClosedLaughEyes(Graphics2D g, int s) {
        g.setColor(new Color(64, 54, 48));
        g.setStroke(new BasicStroke(scaleF(2.3f, s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(scale(10, s), scale(12, s), scale(8, s), scale(8, s), 180, 180);
        g.drawArc(scale(22, s), scale(12, s), scale(8, s), scale(8, s), 180, 180);
    }

    private static void drawSadEyes(Graphics2D g, int s) {
        g.setColor(new Color(64, 54, 48));
        g.fillOval(scale(12, s), scale(15, s), scale(5, s), scale(7, s));
        g.fillOval(scale(24, s), scale(15, s), scale(5, s), scale(7, s));
    }

    private static void drawAngryEyes(Graphics2D g, int s) {
        g.setColor(new Color(64, 54, 48));
        g.setStroke(new BasicStroke(scaleF(2f, s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(scale(10, s), scale(16, s), scale(17, s), scale(13, s));
        g.drawLine(scale(23, s), scale(13, s), scale(30, s), scale(16, s));
        g.fillOval(scale(12, s), scale(15, s), scale(4, s), scale(5, s));
        g.fillOval(scale(25, s), scale(15, s), scale(4, s), scale(5, s));
    }

    private static void drawWink(Graphics2D g, int s) {
        g.setColor(new Color(64, 54, 48));
        g.fillOval(scale(12, s), scale(15, s), scale(5, s), scale(7, s));
        g.setStroke(new BasicStroke(scaleF(2f, s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(scale(23, s), scale(18, s), scale(29, s), scale(18, s));
    }

    private static void drawThinkingEyes(Graphics2D g, int s) {
        g.setColor(new Color(64, 54, 48));
        g.setStroke(new BasicStroke(scaleF(2f, s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(scale(10, s), scale(13, s), scale(17, s), scale(15, s));
        g.fillOval(scale(12, s), scale(16, s), scale(4, s), scale(5, s));
        g.fillOval(scale(25, s), scale(15, s), scale(4, s), scale(6, s));
    }

    private static void drawPeekEyes(Graphics2D g, int s) {
        g.setColor(Color.WHITE);
        g.fillOval(scale(9, s), scale(12, s), scale(9, s), scale(10, s));
        g.fillOval(scale(22, s), scale(12, s), scale(9, s), scale(10, s));
        g.setColor(new Color(64, 54, 48));
        g.fillOval(scale(13, s), scale(15, s), scale(4, s), scale(5, s));
        g.fillOval(scale(26, s), scale(15, s), scale(4, s), scale(5, s));
    }

    private static void drawRollEyes(Graphics2D g, int s) {
        g.setColor(Color.WHITE);
        g.fillOval(scale(10, s), scale(13, s), scale(8, s), scale(9, s));
        g.fillOval(scale(23, s), scale(13, s), scale(8, s), scale(9, s));
        g.setColor(new Color(64, 54, 48));
        g.fillOval(scale(12, s), scale(12, s), scale(4, s), scale(4, s));
        g.fillOval(scale(25, s), scale(12, s), scale(4, s), scale(4, s));
    }

    private static void drawXEyes(Graphics2D g, int s) {
        g.setColor(new Color(64, 54, 48));
        g.setStroke(new BasicStroke(scaleF(2f, s)));
        g.drawLine(scale(11, s), scale(14, s), scale(17, s), scale(20, s));
        g.drawLine(scale(17, s), scale(14, s), scale(11, s), scale(20, s));
        g.drawLine(scale(23, s), scale(14, s), scale(29, s), scale(20, s));
        g.drawLine(scale(29, s), scale(14, s), scale(23, s), scale(20, s));
    }

    private static void drawHeartEyes(Graphics2D g, int s) {
        drawSmallHeart(g, scale(12, s), scale(13, s), scale(5, s));
        drawSmallHeart(g, scale(24, s), scale(13, s), scale(5, s));
    }

    private static void drawSunglasses(Graphics2D g, int s) {
        g.setColor(new Color(37, 47, 61));
        g.fillRoundRect(scale(9, s), scale(13, s), scale(9, s), scale(7, s), scale(3, s), scale(3, s));
        g.fillRoundRect(scale(21, s), scale(13, s), scale(9, s), scale(7, s), scale(3, s), scale(3, s));
        g.fillRect(scale(18, s), scale(15, s), scale(4, s), scale(2, s));
    }

    private static void drawSmile(Graphics2D g, int s, boolean small) {
        g.setColor(new Color(130, 69, 43));
        g.setStroke(new BasicStroke(scaleF(2.2f, s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(scale(11, s), scale(small ? 20 : 18, s), scale(18, s), scale(small ? 8 : 10, s), 200, 140);
    }

    private static void drawSmallMouth(Graphics2D g, int s) {
        g.setColor(new Color(130, 69, 43));
        g.fillOval(scale(18, s), scale(24, s), scale(4, s), scale(4, s));
    }

    private static void drawSadMouth(Graphics2D g, int s) {
        g.setColor(new Color(130, 69, 43));
        g.setStroke(new BasicStroke(scaleF(2.2f, s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(scale(12, s), scale(24, s), scale(16, s), scale(8, s), 20, 140);
    }

    private static void drawOpenLaugh(Graphics2D g, int s) {
        g.setColor(new Color(121, 44, 45));
        g.fillArc(scale(11, s), scale(21, s), scale(18, s), scale(12, s), 180, 180);
        g.setColor(Color.WHITE);
        g.fillRect(scale(13, s), scale(22, s), scale(14, s), scale(3, s));
    }

    private static void drawGrin(Graphics2D g, int s) {
        g.setColor(new Color(121, 44, 45));
        g.fillRoundRect(scale(11, s), scale(21, s), scale(18, s), scale(10, s), scale(6, s), scale(6, s));
        g.setColor(Color.WHITE);
        g.fillRoundRect(scale(12, s), scale(22, s), scale(16, s), scale(6, s), scale(4, s), scale(4, s));
        g.setColor(new Color(210, 210, 210));
        for (int i = 0; i < 4; i++) {
            int x = scale(15 + i * 3, s);
            g.drawLine(x, scale(22, s), x, scale(28, s));
        }
    }

    private static void drawTongueMouth(Graphics2D g, int s) {
        g.setColor(new Color(121, 44, 45));
        g.fillOval(scale(13, s), scale(21, s), scale(14, s), scale(10, s));
        g.setColor(new Color(237, 104, 126));
        g.fillOval(scale(16, s), scale(24, s), scale(8, s), scale(8, s));
    }

    private static void drawKissMouth(Graphics2D g, int s) {
        g.setColor(new Color(160, 72, 80));
        g.fillOval(scale(17, s), scale(22, s), scale(6, s), scale(6, s));
    }

    private static void drawCryMouth(Graphics2D g, int s) {
        g.setColor(new Color(121, 44, 45));
        g.fillOval(scale(15, s), scale(22, s), scale(10, s), scale(8, s));
    }

    private static void drawWobbleMouth(Graphics2D g, int s) {
        g.setColor(new Color(121, 44, 45));
        g.setStroke(new BasicStroke(scaleF(2f, s)));
        g.drawArc(scale(13, s), scale(22, s), scale(5, s), scale(5, s), 0, 180);
        g.drawArc(scale(18, s), scale(24, s), scale(5, s), scale(5, s), 180, 180);
        g.drawArc(scale(23, s), scale(22, s), scale(5, s), scale(5, s), 0, 180);
    }

    private static void drawFlatMouth(Graphics2D g, int s) {
        g.setColor(new Color(121, 44, 45));
        g.setStroke(new BasicStroke(scaleF(2f, s)));
        g.drawLine(scale(14, s), scale(26, s), scale(27, s), scale(26, s));
    }

    private static void drawZipMouth(Graphics2D g, int s) {
        g.setColor(new Color(120, 120, 120));
        g.fillRoundRect(scale(13, s), scale(23, s), scale(16, s), scale(4, s), scale(2, s), scale(2, s));
        g.setColor(new Color(86, 86, 86));
        for (int x = 15; x < 28; x += 3) {
            g.drawLine(scale(x, s), scale(23, s), scale(x, s), scale(27, s));
        }
    }

    private static void drawDropMouth(Graphics2D g, int s) {
        g.setColor(new Color(121, 44, 45));
        g.fillOval(scale(17, s), scale(22, s), scale(7, s), scale(9, s));
    }

    private static void drawHuff(Graphics2D g, int s) {
        g.setColor(new Color(121, 44, 45));
        g.fillOval(scale(15, s), scale(22, s), scale(10, s), scale(8, s));
        g.setColor(new Color(220, 240, 255));
        g.fillOval(scale(27, s), scale(17, s), scale(5, s), scale(5, s));
        g.fillOval(scale(30, s), scale(14, s), scale(4, s), scale(4, s));
    }

    private static void drawTears(Graphics2D g, int s, boolean left, boolean right) {
        g.setColor(new Color(68, 186, 255));
        if (left) g.fill(new Ellipse2D.Double(scale(7, s), scale(18, s), scale(5, s), scale(9, s)));
        if (right) g.fill(new Ellipse2D.Double(scale(30, s), scale(18, s), scale(5, s), scale(9, s)));
    }

    private static void drawSweat(Graphics2D g, int s) {
        g.setColor(new Color(68, 186, 255));
        g.fill(new Ellipse2D.Double(scale(28, s), scale(8, s), scale(6, s), scale(10, s)));
    }

    private static void drawBlush(Graphics2D g, int s) {
        g.setColor(new Color(255, 166, 176));
        g.fillOval(scale(8, s), scale(22, s), scale(6, s), scale(4, s));
        g.fillOval(scale(27, s), scale(22, s), scale(6, s), scale(4, s));
    }

    private static void drawHeartNear(Graphics2D g, int s) {
        drawSmallHeart(g, scale(27, s), scale(10, s), scale(4, s));
    }

    private static void drawHeartsAround(Graphics2D g, int s) {
        drawSmallHeart(g, scale(7, s), scale(9, s), scale(4, s));
        drawSmallHeart(g, scale(29, s), scale(8, s), scale(4, s));
    }

    private static void drawThinkingHand(Graphics2D g, int s) {
        g.setColor(new Color(236, 184, 122));
        g.fillRoundRect(scale(23, s), scale(24, s), scale(9, s), scale(5, s), scale(4, s), scale(4, s));
    }

    private static void drawHands(Graphics2D g, int s) {
        g.setColor(new Color(236, 184, 122));
        g.fillRoundRect(scale(7, s), scale(23, s), scale(8, s), scale(6, s), scale(4, s), scale(4, s));
        g.fillRoundRect(scale(26, s), scale(23, s), scale(8, s), scale(6, s), scale(4, s), scale(4, s));
    }

    private static void drawZ(Graphics2D g, int s) {
        g.setColor(new Color(94, 110, 255));
        Font font = new Font("Segoe UI", Font.BOLD, Math.max(9, s / 4));
        g.setFont(font);
        g.drawString("Z", scale(29, s), scale(11, s));
    }

    private static void drawSmallHeart(Graphics2D g, int x, int y, int r) {
        g.setColor(new Color(255, 77, 109));
        g.fillOval(x, y, r, r);
        g.fillOval(x + r - Math.max(1, r / 3), y, r, r);
        Polygon p = new Polygon();
        p.addPoint(x - 1, y + r - 1);
        p.addPoint(x + r + 1, y + r + Math.max(2, r));
        p.addPoint(x + r + Math.max(3, r + 1), y + r - 1);
        g.fillPolygon(p);
    }

    private static void drawHeart(Graphics2D g, int s) {
        g.setColor(new Color(255, 82, 112));
        g.fillOval(scale(10, s), scale(8, s), scale(10, s), scale(10, s));
        g.fillOval(scale(18, s), scale(8, s), scale(10, s), scale(10, s));
        Polygon p = new Polygon();
        p.addPoint(scale(7, s), scale(16, s));
        p.addPoint(scale(19, s), scale(33, s));
        p.addPoint(scale(31, s), scale(16, s));
        g.fillPolygon(p);
    }

    private static void drawFire(Graphics2D g, int s) {
        Polygon outer = new Polygon();
        outer.addPoint(scale(21, s), scale(4, s));
        outer.addPoint(scale(31, s), scale(18, s));
        outer.addPoint(scale(25, s), scale(34, s));
        outer.addPoint(scale(13, s), scale(34, s));
        outer.addPoint(scale(9, s), scale(21, s));
        g.setColor(new Color(255, 112, 54));
        g.fillPolygon(outer);

        Polygon inner = new Polygon();
        inner.addPoint(scale(21, s), scale(10, s));
        inner.addPoint(scale(26, s), scale(20, s));
        inner.addPoint(scale(22, s), scale(30, s));
        inner.addPoint(scale(16, s), scale(29, s));
        inner.addPoint(scale(15, s), scale(21, s));
        g.setColor(new Color(255, 212, 74));
        g.fillPolygon(inner);
    }

    private static void drawParty(Graphics2D g, int s) {
        g.setColor(new Color(255, 228, 134));
        g.fillOval(scale(4, s), scale(4, s), s - scale(8, s), s - scale(8, s));
        g.setColor(new Color(235, 191, 77));
        g.drawOval(scale(4, s), scale(4, s), s - scale(8, s), s - scale(8, s));

        Polygon cone = new Polygon();
        cone.addPoint(scale(14, s), scale(15, s));
        cone.addPoint(scale(30, s), scale(21, s));
        cone.addPoint(scale(18, s), scale(31, s));
        g.setColor(new Color(120, 118, 255));
        g.fillPolygon(cone);
        g.setColor(new Color(255, 255, 255, 180));
        g.drawLine(scale(19, s), scale(18, s), scale(24, s), scale(25, s));
        g.drawLine(scale(17, s), scale(22, s), scale(22, s), scale(28, s));

        drawConfettiDot(g, s, 10, 10, new Color(255, 92, 120));
        drawConfettiDot(g, s, 27, 11, new Color(76, 194, 255));
        drawConfettiDot(g, s, 29, 29, new Color(255, 168, 28));
        drawConfettiDot(g, s, 10, 27, new Color(96, 201, 128));
    }

    private static void drawHundred(Graphics2D g, int s) {
        g.setColor(new Color(255, 241, 241));
        g.fillOval(scale(4, s), scale(4, s), s - scale(8, s), s - scale(8, s));
        g.setColor(new Color(239, 96, 96));
        g.setStroke(new BasicStroke(scaleF(1.3f, s)));
        g.drawOval(scale(4, s), scale(4, s), s - scale(8, s), s - scale(8, s));
        Font font = new Font("Segoe UI", Font.BOLD, Math.max(11, s / 3));
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics(font);
        String text = "100";
        int x = (s - fm.stringWidth(text)) / 2;
        int y = (s + fm.getAscent() - fm.getDescent()) / 2 - scale(1, s);
        g.drawString(text, x, y);
        g.drawLine(x + scale(1, s), y + scale(2, s), x + fm.stringWidth(text) - scale(1, s), y - scale(2, s));
    }

    private static void drawPray(Graphics2D g, int s) {
        g.setColor(new Color(255, 236, 209));
        g.fillRoundRect(scale(13, s), scale(10, s), scale(8, s), scale(19, s), scale(7, s), scale(7, s));
        g.fillRoundRect(scale(21, s), scale(10, s), scale(8, s), scale(19, s), scale(7, s), scale(7, s));
        g.setColor(new Color(94, 156, 255));
        g.fillRoundRect(scale(13, s), scale(26, s), scale(8, s), scale(6, s), scale(3, s), scale(3, s));
        g.fillRoundRect(scale(21, s), scale(26, s), scale(8, s), scale(6, s), scale(3, s), scale(3, s));
        g.setColor(new Color(255, 216, 76));
        g.fillOval(scale(17, s), scale(6, s), scale(2, s), scale(2, s));
        g.fillOval(scale(22, s), scale(5, s), scale(2, s), scale(2, s));
    }

    private static void drawClap(Graphics2D g, int s) {
        g.setColor(new Color(255, 224, 188));
        g.fillRoundRect(scale(8, s), scale(14, s), scale(10, s), scale(14, s), scale(6, s), scale(6, s));
        g.fillRoundRect(scale(20, s), scale(10, s), scale(10, s), scale(16, s), scale(6, s), scale(6, s));
        g.setColor(new Color(255, 196, 138));
        g.fillRoundRect(scale(12, s), scale(9, s), scale(4, s), scale(8, s), scale(3, s), scale(3, s));
        g.fillRoundRect(scale(24, s), scale(6, s), scale(4, s), scale(10, s), scale(3, s), scale(3, s));
        g.setColor(new Color(255, 203, 66));
        g.drawLine(scale(9, s), scale(9, s), scale(12, s), scale(5, s));
        g.drawLine(scale(31, s), scale(8, s), scale(35, s), scale(4, s));
    }

    private static void drawSparkles(Graphics2D g, int s) {
        drawStar(g, scale(21, s), scale(13, s), scale(9, s), new Color(255, 214, 74));
        drawStar(g, scale(10, s), scale(25, s), scale(4, s), new Color(255, 234, 158));
        drawStar(g, scale(30, s), scale(28, s), scale(5, s), new Color(255, 232, 148));
    }

    private static void drawBulb(Graphics2D g, int s) {
        g.setColor(new Color(255, 230, 98));
        g.fillOval(scale(10, s), scale(7, s), scale(18, s), scale(18, s));
        g.setColor(new Color(233, 190, 34));
        g.drawOval(scale(10, s), scale(7, s), scale(18, s), scale(18, s));
        g.setColor(new Color(128, 136, 147));
        g.fillRoundRect(scale(15, s), scale(23, s), scale(8, s), scale(7, s), scale(2, s), scale(2, s));
        g.setColor(new Color(255, 243, 167));
        g.drawLine(scale(8, s), scale(12, s), scale(4, s), scale(10, s));
        g.drawLine(scale(31, s), scale(12, s), scale(35, s), scale(10, s));
    }

    private static void drawGift(Graphics2D g, int s) {
        g.setColor(new Color(255, 125, 125));
        g.fillRoundRect(scale(8, s), scale(13, s), scale(24, s), scale(20, s), scale(6, s), scale(6, s));
        g.setColor(new Color(255, 236, 230));
        g.fillRect(scale(18, s), scale(13, s), scale(4, s), scale(20, s));
        g.fillRect(scale(8, s), scale(21, s), scale(24, s), scale(4, s));
        g.setColor(new Color(255, 184, 197));
        g.fillOval(scale(14, s), scale(8, s), scale(7, s), scale(7, s));
        g.fillOval(scale(20, s), scale(8, s), scale(7, s), scale(7, s));
    }

    private static void drawRocket(Graphics2D g, int s) {
        Polygon body = new Polygon();
        body.addPoint(scale(21, s), scale(6, s));
        body.addPoint(scale(28, s), scale(18, s));
        body.addPoint(scale(21, s), scale(31, s));
        body.addPoint(scale(14, s), scale(18, s));
        g.setColor(new Color(236, 241, 247));
        g.fillPolygon(body);
        g.setColor(new Color(127, 140, 163));
        g.drawPolygon(body);
        g.setColor(new Color(102, 167, 255));
        g.fillOval(scale(18, s), scale(15, s), scale(6, s), scale(6, s));
        g.setColor(new Color(255, 92, 92));
        Polygon finLeft = new Polygon();
        finLeft.addPoint(scale(14, s), scale(22, s));
        finLeft.addPoint(scale(9, s), scale(28, s));
        finLeft.addPoint(scale(15, s), scale(27, s));
        g.fillPolygon(finLeft);
        Polygon finRight = new Polygon();
        finRight.addPoint(scale(28, s), scale(22, s));
        finRight.addPoint(scale(33, s), scale(28, s));
        finRight.addPoint(scale(27, s), scale(27, s));
        g.fillPolygon(finRight);
        Polygon flame = new Polygon();
        flame.addPoint(scale(21, s), scale(31, s));
        flame.addPoint(scale(24, s), scale(37, s));
        flame.addPoint(scale(18, s), scale(37, s));
        g.setColor(new Color(255, 176, 46));
        g.fillPolygon(flame);
    }

    private static void drawStarBadge(Graphics2D g, int s) {
        g.setColor(new Color(255, 246, 197));
        g.fillOval(scale(4, s), scale(4, s), s - scale(8, s), s - scale(8, s));
        g.setColor(new Color(234, 191, 52));
        g.drawOval(scale(4, s), scale(4, s), s - scale(8, s), s - scale(8, s));
        drawStar(g, scale(21, s), scale(21, s), scale(11, s), new Color(255, 200, 48));
    }

    private static void drawStar(Graphics2D g, int centerX, int centerY, int radius, Color color) {
        Polygon star = new Polygon();
        for (int i = 0; i < 10; i++) {
            double angle = Math.PI / 5 * i - Math.PI / 2;
            int r = (i % 2 == 0) ? radius : Math.max(2, radius / 2);
            star.addPoint(centerX + (int) (Math.cos(angle) * r), centerY + (int) (Math.sin(angle) * r));
        }
        g.setColor(color);
        g.fillPolygon(star);
    }

    private static void drawThumb(Graphics2D g, int s) {
        g.setColor(new Color(255, 222, 174));
        g.fillRoundRect(scale(10, s), scale(15, s), scale(12, s), scale(14, s), scale(5, s), scale(5, s));
        g.fillRoundRect(scale(18, s), scale(10, s), scale(7, s), scale(18, s), scale(4, s), scale(4, s));
        g.fillRoundRect(scale(22, s), scale(13, s), scale(8, s), scale(5, s), scale(3, s), scale(3, s));
        g.fillRoundRect(scale(22, s), scale(18, s), scale(7, s), scale(4, s), scale(3, s), scale(3, s));
        g.fillRoundRect(scale(22, s), scale(22, s), scale(6, s), scale(4, s), scale(3, s), scale(3, s));
        g.setColor(new Color(71, 120, 255));
        g.fillRoundRect(scale(8, s), scale(24, s), scale(5, s), scale(9, s), scale(2, s), scale(2, s));
    }

    private static void drawConfettiDot(Graphics2D g, int s, int x, int y, Color color) {
        g.setColor(color);
        g.fillOval(scale(x, s), scale(y, s), scale(4, s), scale(4, s));
    }

    private static int scale(int value, int size) {
        return Math.round(value * (size / 42f));
    }

    private static float scaleF(float value, int size) {
        return value * (size / 42f);
    }
}
