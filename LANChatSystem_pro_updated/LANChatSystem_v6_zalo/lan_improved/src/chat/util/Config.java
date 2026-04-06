package chat.util;

import java.awt.Color;
import java.awt.Font;

/** Toàn bộ hằng số cấu hình — chỉnh tại đây để thay đổi toàn hệ thống. */
public class Config {
    // ── Mạng
    public static final String DEFAULT_HOST    = "127.0.0.1";
    public static final int    SERVER_PORT     = 5555;
    public static final int    RECONNECT_DELAY = 5000;

    // ── Database
    public static final String DB_HOST = "localhost";
    public static final int    DB_PORT = 3306;
    public static final String DB_NAME = "lanchat";
    public static final String DB_USER = "root";
    public static final String DB_PASS = "";

    // ── Admin mặc định
    public static final String ADMIN_USER = "admin";
    public static final String ADMIN_PASS = "admin123";

    // ── File
    public static final long   MAX_FILE_SIZE  = 10L * 1024 * 1024;
    public static final int    FILE_CHUNK     = 32 * 1024;
    public static final int    MAX_IMAGE_DIM  = 1200;

    // ── Kích thước cửa sổ
    public static final int CLIENT_W = 1020;
    public static final int CLIENT_H = 700;
    public static final int SERVER_W = 960;
    public static final int SERVER_H = 640;

    // ── Zalo-inspired Client Colors
    public static final Color ZALO_BLUE         = new Color(0, 120, 212);
    public static final Color ZALO_BLUE_DARK    = new Color(0, 99, 176);
    public static final Color ZALO_BLUE_LIGHT   = new Color(20, 140, 232);
    public static final Color ZALO_ICON_ACTIVE  = new Color(255, 255, 255);
    public static final Color ZALO_ICON_DIM     = new Color(180, 215, 255);

    // Chat area
    public static final Color CHAT_BG           = new Color(242, 246, 250);
    public static final Color CHAT_LIST_BG      = new Color(255, 255, 255);
    public static final Color CHAT_LIST_HOVER   = new Color(240, 245, 255);
    public static final Color CHAT_LIST_ACTIVE  = new Color(224, 237, 255);
    public static final Color CHAT_HEADER_BG   = new Color(255, 255, 255);

    // Bubbles
    public static final Color BUBBLE_SELF      = new Color(0, 120, 212);
    public static final Color BUBBLE_OTHER     = new Color(255, 255, 255);
    public static final Color BUBBLE_SELF_TEXT = new Color(255, 255, 255);
    public static final Color BUBBLE_OTHER_TEXT= new Color(30, 30, 30);

    // Text
    public static final Color TEXT_PRIMARY     = new Color(30, 30, 30);
    public static final Color TEXT_SECONDARY   = new Color(90, 90, 100);
    public static final Color TEXT_MUTED       = new Color(160, 160, 175);
    public static final Color TEXT_WHITE       = new Color(255, 255, 255);
    public static final Color TEXT_TIME        = new Color(140, 150, 165);

    // Input
    public static final Color INPUT_BG         = new Color(255, 255, 255);
    public static final Color INPUT_BORDER     = new Color(210, 215, 225);
    public static final Color INPUT_AREA_BG    = new Color(248, 250, 253);

    // Accent
    public static final Color ACCENT           = new Color(0, 120, 212);
    public static final Color ACCENT2          = new Color(39, 186, 89);
    public static final Color RED_ACCENT       = new Color(235, 64, 64);
    public static final Color YELLOW_ACCENT    = new Color(255, 195, 0);
    public static final Color ONLINE_DOT       = new Color(39, 186, 89);
    public static final Color DIVIDER          = new Color(225, 228, 235);

    // Server Dashboard
    public static final Color SRV_BG           = new Color(15, 18, 32);
    public static final Color SRV_PANEL        = new Color(24, 28, 46);
    public static final Color SRV_CARD         = new Color(32, 38, 60);
    public static final Color SRV_BORDER       = new Color(48, 56, 85);
    public static final Color SRV_ACCENT       = new Color(0, 122, 255);
    public static final Color SRV_GREEN        = new Color(52, 211, 153);
    public static final Color SRV_RED          = new Color(248, 113, 113);
    public static final Color SRV_YELLOW       = new Color(251, 191, 36);
    public static final Color SRV_TEXT         = new Color(220, 225, 240);
    public static final Color SRV_TEXT_MUTED   = new Color(130, 140, 170);
    public static final Color SRV_LOG_BG       = new Color(10, 13, 24);
    public static final Color SRV_LOG_TEXT     = new Color(155, 220, 155);

    // Legacy mappings
    public static final Color BG_DARK  = SRV_BG;
    public static final Color BG_PANEL = SRV_PANEL;
    public static final Color BG_INPUT = new Color(50, 50, 70);

    // ── Font
    public static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD,  16);
    public static final Font FONT_NORMAL  = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_MONO    = new Font("Consolas",  Font.PLAIN, 12);
    public static final Font FONT_BOLD    = new Font("Segoe UI", Font.BOLD,  13);
    public static final Font FONT_BOLD_LG = new Font("Segoe UI", Font.BOLD,  15);
}
