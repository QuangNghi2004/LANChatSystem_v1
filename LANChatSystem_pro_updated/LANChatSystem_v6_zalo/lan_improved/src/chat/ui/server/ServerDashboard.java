package chat.ui.server;

import chat.connection.ClientConnection;
import chat.connection.ServerListener;
import chat.protocol.Protocol;
import chat.server.ServerMain;
import chat.util.Config;
import chat.util.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Server Dashboard — giao diện Admin hiện đại dark theme.
 */
public class ServerDashboard extends JFrame {

    private final ServerMain     server   = new ServerMain();
    private final ServerListener listener = new ServerListener(server);

    private JTextArea         logArea;
    private JTable            userTable;
    private DefaultTableModel userModel;
    private JButton           btnStart, btnStop;
    private JTextField        broadcastField;
    private JLabel            statusDot;
    private JLabel            statusText;
    private JLabel            counterLabel;

    // Stat cards
    private JLabel cardOnlineVal, cardMsgVal, cardUptimeVal, cardPortVal;
    private long   startTime = 0;
    private int    msgCount  = 0;

    public ServerDashboard() {
        super("LAN Chat — Server Dashboard");
        Logger.setCallback(msg -> SwingUtilities.invokeLater(() -> {
            if (logArea != null) {
                String ts = new SimpleDateFormat("HH:mm:ss").format(new Date());
                logArea.append("[" + ts + "] " + msg + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
                msgCount++;
                if (cardMsgVal != null) cardMsgVal.setText(String.valueOf(msgCount));
            }
        }));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(Config.SERVER_W + 80, Config.SERVER_H + 80);
        setMinimumSize(new Dimension(900, 580));
        setLocationRelativeTo(null);
        getContentPane().setBackground(Config.SRV_BG);
        buildUI();
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { confirmExit(); }
        });

        // Refresh every 2s
        new Timer(2000, e -> {
            refreshTable();
            if (startTime > 0 && cardUptimeVal != null) {
                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                long h = elapsed/3600, m = (elapsed%3600)/60, s = elapsed%60;
                cardUptimeVal.setText(String.format("%02d:%02d:%02d", h, m, s));
            }
        }).start();
    }

    // ── Build UI ──────────────────────────────────────────────────────────
    private void buildUI() {
        setLayout(new BorderLayout(0, 0));
        add(buildHeader(),  BorderLayout.NORTH);
        add(buildMain(),    BorderLayout.CENTER);
        add(buildFooter(),  BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 0));
        header.setBackground(Config.SRV_PANEL);
        header.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, Config.SRV_BORDER),
            new EmptyBorder(14, 20, 14, 20)));

        // Logo + title
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        JLabel logo = new JLabel("[SRV]");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JPanel titleGroup = new JPanel();
        titleGroup.setLayout(new BoxLayout(titleGroup, BoxLayout.Y_AXIS));
        titleGroup.setOpaque(false);

        JLabel title = new JLabel("Server Dashboard");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Config.SRV_TEXT);

        JLabel subtitle = new JLabel("LAN Chat System v3 - Admin");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitle.setForeground(Config.SRV_TEXT_MUTED);

        titleGroup.add(title);
        titleGroup.add(subtitle);

        left.add(logo);
        left.add(titleGroup);

        // Status indicator + controls
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        // Status pill
        JPanel statusPill = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Config.SRV_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        statusPill.setOpaque(false);
        statusPill.setBorder(new EmptyBorder(6, 12, 6, 14));

        statusDot = new JLabel("-");
        statusDot.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusDot.setForeground(Config.SRV_TEXT_MUTED);

        statusText = new JLabel("Chưa khởi động");
        statusText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusText.setForeground(Config.SRV_TEXT_MUTED);

        statusPill.add(statusDot);
        statusPill.add(statusText);

        // Counter badge
        counterLabel = new JLabel("0 online") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Config.SRV_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        counterLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        counterLabel.setForeground(Config.SRV_TEXT_MUTED);
        counterLabel.setOpaque(false);
        counterLabel.setBorder(new EmptyBorder(6, 14, 6, 14));
        counterLabel.setHorizontalAlignment(SwingConstants.CENTER);

        btnStart = srvBtn("[>] Khoi dong", Config.SRV_GREEN);
        btnStop  = srvBtn("[||] Dung",      Config.SRV_RED);
        btnStop.setEnabled(false);
        btnStart.addActionListener(e -> startServer());
        btnStop .addActionListener(e -> stopServer());

        right.add(statusPill);
        right.add(counterLabel);
        right.add(Box.createHorizontalStrut(4));
        right.add(btnStart);
        right.add(btnStop);

        header.add(left,  BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildMain() {
        JPanel main = new JPanel(new BorderLayout(0, 12));
        main.setBackground(Config.SRV_BG);
        main.setBorder(new EmptyBorder(16, 16, 0, 16));

        // ── Stat cards row
        main.add(buildStatCards(), BorderLayout.NORTH);

        // ── Content area (Log + Users)
        main.add(buildContent(), BorderLayout.CENTER);

        return main;
    }

    private JPanel buildStatCards() {
        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 0));
        cards.setBackground(Config.SRV_BG);
        cards.setBorder(new EmptyBorder(0, 0, 12, 0));

        cardOnlineVal  = new JLabel("0");
        cardMsgVal     = new JLabel("0");
        cardUptimeVal  = new JLabel("00:00:00");
        cardPortVal    = new JLabel(String.valueOf(Config.SERVER_PORT));

        cards.add(statCard("Người dùng", cardOnlineVal,  "USR", Config.SRV_ACCENT));
        cards.add(statCard("Logs",        cardMsgVal,    "LOG", Config.SRV_GREEN));
        cards.add(statCard("Uptime",      cardUptimeVal, "UPT", Config.SRV_YELLOW));
        cards.add(statCard("Port",        cardPortVal,   "PRT", new Color(167, 139, 250)));

        return cards;
    }

    private JPanel statCard(String label, JLabel valueLabel, String icon, Color accent) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Config.SRV_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                // Left accent bar
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setLayout(new BorderLayout(10, 0));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(14, 16, 14, 14));

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        iconLabel.setForeground(accent);
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setPreferredSize(new Dimension(36, 36));
        iconLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accent, 1, true),
            new EmptyBorder(2, 4, 2, 4)));
        iconLabel.setVerticalAlignment(SwingConstants.CENTER);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(Config.SRV_TEXT);

        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        labelComp.setForeground(Config.SRV_TEXT_MUTED);

        textPanel.add(valueLabel);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(labelComp);

        card.add(iconLabel,  BorderLayout.WEST);
        card.add(textPanel,  BorderLayout.CENTER);

        return card;
    }

    private JSplitPane buildContent() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setBackground(Config.SRV_BG);
        split.setBorder(null);
        split.setDividerSize(8);
        split.setDividerLocation(580);
        split.setResizeWeight(0.6);

        // ── Log panel
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBackground(Config.SRV_CARD);
        logPanel.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 1, 1, 1, Config.SRV_BORDER),
            new EmptyBorder(0, 0, 0, 0)));

        JPanel logHeader = cardHeader("[LOG]  System Log", true);
        JButton btnClear = tinyBtn("Xóa log");
        btnClear.addActionListener(e -> { logArea.setText(""); msgCount = 0; if (cardMsgVal!=null) cardMsgVal.setText("0"); });
        logHeader.add(btnClear, BorderLayout.EAST);
        logPanel.add(logHeader, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(Config.SRV_LOG_BG);
        logArea.setForeground(Config.SRV_LOG_TEXT);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setMargin(new Insets(10, 12, 10, 12));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setCaretColor(Config.SRV_GREEN);

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(null);
        logScroll.getVerticalScrollBar().setPreferredSize(new Dimension(5, 0));
        logScroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            protected void configureScrollBarColors() {
                thumbColor = Config.SRV_BORDER;
                trackColor = Config.SRV_LOG_BG;
            }
            protected JButton createDecreaseButton(int o) { return zBtn(); }
            protected JButton createIncreaseButton(int o) { return zBtn(); }
            private JButton zBtn() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); return b; }
        });
        logPanel.add(logScroll, BorderLayout.CENTER);

        // ── User table panel
        JPanel userPanel = new JPanel(new BorderLayout());
        userPanel.setBackground(Config.SRV_CARD);
        userPanel.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 1, 1, 1, Config.SRV_BORDER),
            new EmptyBorder(0, 0, 0, 0)));

        JPanel userHeader = cardHeader("[USR]  Nguoi dung Online", false);
        userPanel.add(userHeader, BorderLayout.NORTH);

        String[] cols = {"#", "Tên đăng nhập", "Địa chỉ IP", "Vai trò"};
        userModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        userTable = new JTable(userModel);
        styleTable(userTable);
        userTable.getColumnModel().getColumn(0).setMaxWidth(40);
        userTable.getColumnModel().getColumn(1).setPreferredWidth(110);
        userTable.getColumnModel().getColumn(2).setPreferredWidth(110);
        userTable.getColumnModel().getColumn(3).setPreferredWidth(70);

        JScrollPane tableScroll = new JScrollPane(userTable);
        tableScroll.setBorder(null);
        tableScroll.getViewport().setBackground(Config.SRV_PANEL);
        tableScroll.getVerticalScrollBar().setPreferredSize(new Dimension(5, 0));
        userPanel.add(tableScroll, BorderLayout.CENTER);

        // Admin action buttons below table
        userPanel.add(buildUserActions(), BorderLayout.SOUTH);

        split.setLeftComponent(logPanel);
        split.setRightComponent(userPanel);
        return split;
    }

    private JPanel buildUserActions() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        p.setBackground(Config.SRV_CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, Config.SRV_BORDER),
            new EmptyBorder(8, 10, 10, 10)));

        JButton btnKick  = actionBtn("[!] Kick",  Config.SRV_YELLOW);
        JButton btnBan   = actionBtn("[X] Ban",   Config.SRV_RED);
        JButton btnUnban = actionBtn("[V] Unban", Config.SRV_GREEN);

        btnKick.addActionListener(e -> kickSelected());
        btnBan.addActionListener(e -> banSelected());
        btnUnban.addActionListener(e -> unbanSelected());

        p.add(btnKick);
        p.add(btnBan);
        p.add(btnUnban);
        return p;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setBackground(Config.SRV_PANEL);
        footer.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, Config.SRV_BORDER),
            new EmptyBorder(10, 20, 12, 20)));

        JLabel bcLbl = new JLabel("[BC]");
        bcLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));

        broadcastField = new JTextField();
        broadcastField.setFont(Config.FONT_NORMAL);
        broadcastField.setBackground(Config.SRV_CARD);
        broadcastField.setForeground(Config.SRV_TEXT);
        broadcastField.setCaretColor(Config.SRV_ACCENT);
        broadcastField.putClientProperty("JTextField.placeholderText", "Gửi thông báo đến tất cả người dùng...");
        broadcastField.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 1, 1, 1, Config.SRV_BORDER),
            new EmptyBorder(8, 12, 8, 12)));
        broadcastField.addActionListener(e -> sendBroadcast());

        JButton btnBC = srvBtn("Gửi thông báo", Config.SRV_ACCENT);
        btnBC.addActionListener(e -> sendBroadcast());

        footer.add(bcLbl,          BorderLayout.WEST);
        footer.add(broadcastField, BorderLayout.CENTER);
        footer.add(btnBC,          BorderLayout.EAST);
        return footer;
    }

    // ── Server lifecycle ──────────────────────────────────────────────────
    private void startServer() {
        try {
            listener.start();
            btnStart.setEnabled(false); btnStop.setEnabled(true);
            statusDot.setForeground(Config.SRV_GREEN);
            statusText.setText("Đang chạy  ·  Port " + Config.SERVER_PORT);
            statusText.setForeground(Config.SRV_GREEN);
            startTime = System.currentTimeMillis();
            Logger.info("[OK] Server khoi dong thanh cong — Port " + Config.SERVER_PORT);
        } catch (Exception e) {
            Logger.error("[ERR] Khong the khoi dong: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Lỗi khởi động:\n" + e.getMessage(),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopServer() {
        listener.stop();
        btnStart.setEnabled(true); btnStop.setEnabled(false);
        statusDot.setForeground(Config.SRV_TEXT_MUTED);
        statusText.setText("Đã dừng");
        statusText.setForeground(Config.SRV_TEXT_MUTED);
        startTime = 0;
        userModel.setRowCount(0);
        counterLabel.setText("0 online");
        counterLabel.setForeground(Config.SRV_TEXT_MUTED);
        if (cardOnlineVal != null) cardOnlineVal.setText("0");
        if (cardUptimeVal != null) cardUptimeVal.setText("00:00:00");
        Logger.info("[STOP] Server da dung.");
    }

    // ── Admin actions ─────────────────────────────────────────────────────
    private void kickSelected() {
        String u = selectedUser(); if (u == null) return;
        int r = JOptionPane.showConfirmDialog(this,
            "Kick người dùng \"" + u + "\"?", "Xác nhận",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) return;
        ClientConnection cc = server.getClientManager().getSession(u);
        if (cc != null) cc.send(Protocol.build(Protocol.KICKED, "Bạn đã bị kick"));
        Logger.info("[KICK] Admin kick: " + u);
    }

    private void banSelected() {
        String u = selectedUser(); if (u == null) return;
        int r = JOptionPane.showConfirmDialog(this,
            "Ban tài khoản \"" + u + "\"?\nHành động này sẽ cấm tài khoản.",
            "Xác nhận Ban", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) return;
        server.getUserDAO().ban(u, "admin", "Admin ban");
        ClientConnection cc = server.getClientManager().getSession(u);
        if (cc != null) cc.send(Protocol.build(Protocol.BANNED, "Tài khoản bị cấm"));
        Logger.info("[BAN] Admin ban: " + u);
    }

    private void unbanSelected() {
        String u = selectedUser();
        if (u == null) {
            String input = JOptionPane.showInputDialog(this, "Username cần Unban:");
            if (input != null && !input.isBlank()) {
                server.getUserDAO().unban(input.trim());
                Logger.info("[UNBAN] " + input.trim());
                JOptionPane.showMessageDialog(this, "[OK] Da unban: " + input.trim());
            }
            return;
        }
        server.getUserDAO().unban(u);
        Logger.info("[UNBAN] Admin unban: " + u);
        JOptionPane.showMessageDialog(this, "[OK] Da unban: " + u);
    }

    private String selectedUser() {
        int r = userTable.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một người dùng từ danh sách!");
            return null;
        }
        return (String) userModel.getValueAt(r, 1);
    }

    private void sendBroadcast() {
        String msg = broadcastField.getText().trim();
        if (msg.isEmpty()) return;
        String ts = new SimpleDateFormat("HH:mm:ss").format(new Date());
        server.getClientManager().broadcastAll(Protocol.build(Protocol.BROADCAST, msg, ts));
        Logger.info("[BC] Broadcast: " + msg);
        broadcastField.setText("");
    }

    // ── Refresh table ──────────────────────────────────────────────────────
    private void refreshTable() {
        if (!listener.isRunning()) return;
        Map<String, ClientConnection> sessions = server.getClientManager().getAllSessions();
        userModel.setRowCount(0);
        int i = 1;
        for (Map.Entry<String, ClientConnection> e : sessions.entrySet()) {
            String role = server.getUserDAO().getRole(e.getKey());
            userModel.addRow(new Object[]{i++, e.getKey(), e.getValue().getIp(), role});
        }
        int count = sessions.size();
        counterLabel.setText(count + " online");
        counterLabel.setForeground(count > 0 ? Config.SRV_GREEN : Config.SRV_TEXT_MUTED);
        if (cardOnlineVal != null) cardOnlineVal.setText(String.valueOf(count));
    }

    private void confirmExit() {
        int r = JOptionPane.showConfirmDialog(this,
            "Dừng server và thoát?", "Xác nhận thoát", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) { stopServer(); System.exit(0); }
    }

    // ── UI Helpers ─────────────────────────────────────────────────────────
    private JPanel cardHeader(String title, boolean hasClearBtn) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Config.SRV_PANEL);
        p.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, Config.SRV_BORDER),
            new EmptyBorder(9, 14, 9, 10)));
        JLabel l = new JLabel(title);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(Config.SRV_TEXT);
        p.add(l, BorderLayout.WEST);
        return p;
    }

    private JButton srvBtn(String text, Color accent) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = isEnabled()
                    ? (getModel().isRollover() ? accent.brighter() : accent.darker())
                    : Config.SRV_BORDER;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(8, 18, 8, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton actionBtn(String text, Color accent) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 11));
        b.setForeground(accent);
        b.setBackground(Config.SRV_CARD);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accent.darker(), 1, true),
            new EmptyBorder(5, 12, 5, 12)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 25)); }
            public void mouseExited(MouseEvent e)  { b.setBackground(Config.SRV_CARD); }
        });
        return b;
    }

    private JButton tinyBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        b.setForeground(Config.SRV_TEXT_MUTED);
        b.setBackground(Config.SRV_BORDER);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(4, 10, 4, 10));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void styleTable(JTable t) {
        t.setBackground(Config.SRV_PANEL);
        t.setForeground(Config.SRV_TEXT);
        t.setFont(Config.FONT_NORMAL);
        t.setRowHeight(34);
        t.setGridColor(Config.SRV_BORDER);
        t.setSelectionBackground(new Color(Config.SRV_ACCENT.getRed(),
            Config.SRV_ACCENT.getGreen(), Config.SRV_ACCENT.getBlue(), 80));
        t.setSelectionForeground(Config.SRV_TEXT);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setFillsViewportHeight(true);

        JTableHeader th = t.getTableHeader();
        th.setBackground(Config.SRV_PANEL);
        th.setForeground(Config.SRV_TEXT_MUTED);
        th.setFont(new Font("Segoe UI", Font.BOLD, 11));
        th.setBorder(new MatteBorder(0, 0, 1, 0, Config.SRV_BORDER));
        th.setReorderingAllowed(false);

        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object val,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(
                    table, val, isSelected, hasFocus, row, col);
                c.setBackground(isSelected
                    ? new Color(0, 122, 255, 50)
                    : row % 2 == 0 ? Config.SRV_PANEL : Config.SRV_CARD);
                c.setForeground(Config.SRV_TEXT);
                c.setBorder(new EmptyBorder(0, 12, 0, 8));
                c.setOpaque(true);

                // Role column coloring
                if (col == 3 && val != null) {
                    String role = val.toString().toLowerCase();
                    if (role.contains("admin")) c.setForeground(Config.SRV_YELLOW);
                }
                return c;
            }
        });
    }
}
