package chat.ui.client;

import chat.client.ClientMain;
import chat.client.managers.FileManager;
import chat.client.managers.SessionManager;
import chat.protocol.Protocol;
import chat.ui.client.components.*;
import chat.ui.client.dialogs.LoginDialog;
import chat.ui.client.panels.*;
import chat.util.Config;
import chat.util.SoundUtil;
import chat.util.TransferMessageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Cửa sổ chat chính — giao diện kiểu Zalo.
 * Layout: [NavRail 60px] [ConvList 280px] [ChatArea]
 */
public class ClientWindow extends JFrame {

    private final ClientMain     client;
    private final SessionManager session = SessionManager.getInstance();

    // ── State
    private final Map<String, ChatPanel>  panels = new LinkedHashMap<>();
    private final Map<String, Integer>    unread = new HashMap<>();
    private final DefaultListModel<String> convModel = new DefaultListModel<>();
    private EmojiPicker emojiPicker;

    // ── UI components
    private JList<String>  convList;
    private JPanel         chatArea;
    private CardLayout     cardLayout;
    private JLabel         statusLabel;
    private JLabel         chatTitleLabel;
    private JLabel         chatSubLabel;
    private JTextField     inputField;
    private JLabel         quoteLabel;
    private JPanel         quotePanelWrapper;
    private JPanel         attachmentPreviewWrapper;
    private JLabel         attachmentPreviewLabel;
    private JButton        attachmentClearButton;
    private JButton        btnSend;
    private JPanel         navRail;
    private JButton        btnConnect;
    private JPanel         chatHeader;  // reference to update add-member button
    private String         currentKey = null;
    private File           pendingAttachment;

    // ── NavRail buttons
    private JButton navBtnChat, navBtnContacts, navBtnAdmin;

    // ════════════════════════════════════════════════════════════════════
    public ClientWindow() {
        super("LAN Chat");
        client = new ClientMain(
            null,
            received -> SwingUtilities.invokeLater(() -> {
                // Dam bao nguoi gui hien trong danh sach cuoc tro chuyen
                if (!convModel.contains(received.from())) {
                    convModel.addElement(received.from());
                }
                getOrCreate(received.from()).addMessage(
                    received.from(),
                    received.messageText(),
                    nowTs(),
                    null,
                    null);
                bump(received.from());
                SoundUtil.ting();
            }),
            sent -> SwingUtilities.invokeLater(() -> {
                getOrCreate(sent.to()).addMessage(
                    session.getUsername(),
                    sent.messageText(),
                    nowTs(),
                    "sent",
                    null);
                if (!sent.to().equals(currentKey)) bump(sent.to());
            }),
            (key, msg) -> SwingUtilities.invokeLater(() -> showSystem(key, msg))
        );
        client.setOnDispatch(p -> SwingUtilities.invokeLater(() -> dispatch(p)));
        client.setOnReconnect(() -> SwingUtilities.invokeLater(() ->
            statusLabel.setText("Đang kết nối lại...")));

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(Config.CLIENT_W, Config.CLIENT_H);
        setMinimumSize(new Dimension(820, 560));
        setLocationRelativeTo(null);
        getContentPane().setBackground(Config.CHAT_LIST_BG);
        buildUI();
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { confirmExit(); }
        });
    }

    // ── Login ──────────────────────────────────────────────────────────────
    public void showLogin() {
        LoginDialog dlg = new LoginDialog(this);
        dlg.setVisible(true);
        if (!dlg.isConfirmed()) { System.exit(0); return; }

        boolean isReg = dlg.isRegister();
        session.login(dlg.getUsername(), "user", dlg.getHost());
        setTitle("LAN Chat — " + dlg.getUsername());
        statusLabel.setText("Đang kết nối...");

        String cmd = isReg ? Protocol.REGISTER : Protocol.LOGIN;
        client.conn.connect(dlg.getHost(),
            Protocol.build(cmd, dlg.getUsername(), dlg.getPassword()),
            err -> SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Lỗi kết nối");
                JOptionPane.showMessageDialog(this,
                    "Không kết nối được!\n" + err, "Lỗi", JOptionPane.ERROR_MESSAGE);
                showLogin();
            }));

        client.setOnDispatch(p -> SwingUtilities.invokeLater(() -> {
            if (isReg && Protocol.AUTH_OK.equals(p[0])) {
                client.conn.closeQuiet();
                String u = session.getUsername(), h = session.getServerHost();
                JOptionPane.showMessageDialog(this,
                    "✅ Đăng ký \"" + u + "\" thành công!\nVui lòng đăng nhập.",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
                LoginDialog dlg2 = new LoginDialog(this);
                dlg2.prefillLogin(u, h);
                dlg2.setVisible(true);
                if (!dlg2.isConfirmed()) { System.exit(0); return; }
                session.login(dlg2.getUsername(), "user", dlg2.getHost());
                setTitle("LAN Chat — " + dlg2.getUsername());
                client.setOnDispatch(pp -> SwingUtilities.invokeLater(() -> dispatch(pp)));
                client.conn.connect(dlg2.getHost(),
                    Protocol.build(Protocol.LOGIN, dlg2.getUsername(), dlg2.getPassword()),
                    e -> SwingUtilities.invokeLater(() -> { statusLabel.setText("Lỗi: " + e); showLogin(); }));
            } else {
                dispatch(p);
            }
        }));

        setVisible(true);
    }

    // ── Dispatch ──────────────────────────────────────────────────────────
    private void dispatch(String[] p) {
        switch (p[0]) {
            case Protocol.AUTH_OK       -> onAuthOk(p);
            case Protocol.AUTH_FAIL     -> onAuthFail(p);
            case Protocol.USER_LIST     -> onUserList(p);
            case Protocol.GROUP_LIST    -> onGroupList(p);
            case Protocol.USER_JOIN     -> onUserJoin(p);
            case Protocol.USER_LEAVE    -> onUserLeave(p);
            case Protocol.MSG           -> onBroadcast(p);
            case Protocol.PM            -> onPM(p);
            case Protocol.GROUP_MSG     -> onGroupMsg(p);
            case Protocol.BROADCAST     -> onAdminBroadcast(p);
            case Protocol.DELIVERED     -> onDelivered(p);
            case Protocol.READ          -> onRead(p);
            case Protocol.GROUP_CREATED -> onGroupCreated(p);
            case Protocol.INVITE_GROUP  -> onInvite(p);
            case Protocol.HISTORY_RESP  -> onHistory(p);
            case Protocol.KICKED        -> onKicked(p);
            case Protocol.BANNED        -> onKicked(p);
            case Protocol.FILE_OFFER    -> onFileOffer(p);
            case Protocol.FILE_REJECT   -> {
                if (p.length >= 4) showSystem(p[1], "❌ " + p[1] + " từ chối file: " + p[3]);
            }
            case Protocol.OP_FAIL        -> onOpFail(p);
            case Protocol.OP_OK          -> onOpOk(p);
            case Protocol.USER_CREATED   -> onUserCreated(p);
            case Protocol.ROLE_ASSIGNED  -> onRoleAssigned(p);
            case Protocol.CONV_INFO_RESP -> onConvInfoResp(p);
        }
    }

    // ── Handlers ──────────────────────────────────────────────────────────
    private void onAuthOk(String[] p) {
        String role = p.length > 2 ? p[2] : "user";
        session.login(session.getUsername(), role, session.getServerHost());
        String roleDisplay = switch (role) {
            case "admin"        -> " 🛡️ [Quản trị viên]";
            case "truong_phong" -> " 👔 [Trưởng phòng]";
            case "pho_phong"    -> " 👔 [Phó phòng]";
            case "nhan_vien"    -> " 👤 [Nhân viên]";
            default             -> "";
        };
        statusLabel.setText("🟢 " + session.getUsername() + roleDisplay);
        getOrCreate("Lobby");
        btnConnect.setText("Ngắt kết nối");
        updateNavUser();
    }

    private void onAuthFail(String[] p) {
        String msg = p.length > 1 ? p[1] : "Xác thực thất bại";
        // Nếu đã đăng nhập (ví dụ lỗi đổi mật khẩu), chỉ hiện thông báo, không ngắt kết nối
        if (session.isLoggedIn()) {
            JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        client.conn.closeQuiet();
        JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
        showLogin();
    }

    private void onUserList(String[] p) {
        if (p.length < 2 || p[1].isBlank()) return;
        convModel.clear();
        Arrays.stream(p[1].split(",")).filter(u -> !u.isBlank()).forEach(convModel::addElement);
    }

    private void onGroupList(String[] p) {
        if (p.length < 2 || p[1].isBlank()) return;
        for (String e : p[1].split(";")) {
            if (e.isBlank()) continue;
            String g = "#" + e.split(":")[0];
            if (!convModel.contains(g)) convModel.addElement(g);
        }
    }

    private void onUserJoin(String[] p) {
        if (p.length < 2) return;
        if (!convModel.contains(p[1])) convModel.addElement(p[1]);
        showSystem("Lobby", "👋 " + p[1] + " đã tham gia");
        SoundUtil.ting();
    }

    private void onUserLeave(String[] p) {
        if (p.length < 2) return;
        convModel.removeElement(p[1]);
        showSystem("Lobby", "👋 " + p[1] + " ngắt kết nối");
    }

    private void onBroadcast(String[] p) {
        if (p.length < 4) return;
        getOrCreate("Lobby").addMessage(p[1], p[2], p[3], null, null);
        bump("Lobby");
        SoundUtil.ting();
    }

    private void onPM(String[] p) {
        if (p.length < 4) return;
        getOrCreate(p[1]).addMessage(p[1], p[2], p[3], null, null);
        bump(p[1]);
        client.conn.send(Protocol.build(Protocol.READ, p[1]));
        SoundUtil.ting();
    }

    private void onGroupMsg(String[] p) {
        if (p.length < 5) return;
        String key = "#" + p[1];
        getOrCreate(key).addMessage(p[2], p[3], p[4], null, null);
        bump(key);
        if (!"System".equals(p[2])) SoundUtil.ting();
    }

    private void onAdminBroadcast(String[] p) {
        JOptionPane.showMessageDialog(this,
            (p.length > 1 ? p[1] : "") + (p.length > 2 ? "\n\n[" + p[2] + "]" : ""),
            "📢 Thông báo Admin", JOptionPane.INFORMATION_MESSAGE);
        SoundUtil.ting();
    }

    private void onOpFail(String[] p) {
        String msg = p.length > 1 ? p[1] : "Thao tac that bai";
        JOptionPane.showMessageDialog(this, msg, "Loi", JOptionPane.ERROR_MESSAGE);
    }

    private void onOpOk(String[] p) {
        String msg = p.length > 1 ? p[1] : "Thao tac thanh cong";
        JOptionPane.showMessageDialog(this, msg, "Thanh cong", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Xu ly phan hoi thong tin cuoc tro chuyen tu server.
     * Format: CONV_INFO_RESP || target || isGroup || fileCount || imageCount || extra
     * Ket hop voi so dem realtime cua ChatPanel de hien thi chinh xac nhat.
     */
    private void onConvInfoResp(String[] p) {
        if (p.length < 6) return;
        String  target    = p[1];
        boolean isGroup   = "true".equalsIgnoreCase(p[2]);
        int     fileCount = 0, imgCount = 0;
        try { fileCount = Integer.parseInt(p[3]); } catch (NumberFormatException ignored) {}
        try { imgCount  = Integer.parseInt(p[4]); } catch (NumberFormatException ignored) {}
        String extra = p[5];

        // Ket hop voi so dem realtime tu ChatPanel (hien tai phien dang nhap)
        String panelKey = isGroup ? "#" + target : target;
        ChatPanel cp = panels.get(panelKey);
        if (cp != null) {
            fileCount = Math.max(fileCount, cp.getLocalFileCount());
            imgCount  = Math.max(imgCount,  cp.getLocalImageCount());
        }

        chat.ui.client.dialogs.GroupInfoDialog dlg =
            new chat.ui.client.dialogs.GroupInfoDialog(
                this, target, isGroup, fileCount, imgCount, extra);
        dlg.setVisible(true);
    }

    private void onUserCreated(String[] p) {
        // p[1]=username, p[2]=role, p[3]=roleDisplay
        String username    = p.length > 1 ? p[1] : "?";
        String roleDisplay = p.length > 3 ? p[3] : (p.length > 2 ? p[2] : "");
        JOptionPane.showMessageDialog(this,
            "✅ Đã tạo tài khoản thành công!\n\nTên đăng nhập: " + username
                + "\nChức vụ: " + roleDisplay,
            "Tạo tài khoản", JOptionPane.INFORMATION_MESSAGE);
    }

    private void onRoleAssigned(String[] p) {
        // p[1]=username, p[2]=role, p[3]=roleDisplay
        String username    = p.length > 1 ? p[1] : "?";
        String roleDisplay = p.length > 3 ? p[3] : (p.length > 2 ? p[2] : "");
        JOptionPane.showMessageDialog(this,
            "✅ Đã phân quyền thành công!\n\nTài khoản: " + username
                + "\nChức vụ mới: " + roleDisplay,
            "Phân quyền", JOptionPane.INFORMATION_MESSAGE);
    }

    private void onDelivered(String[] p) {
        if (p.length < 2) return;
        ChatPanel cp = panels.get(p[1]);
        if (cp != null) cp.updateStatus(null, "delivered");
    }

    private void onRead(String[] p) {
        if (p.length < 2) return;
        ChatPanel cp = panels.get(p[1]);
        if (cp != null) cp.updateStatus(null, "read");
    }

    private void onGroupCreated(String[] p) {
        if (p.length < 2) return;
        String g = "#" + p[1];
        if (!convModel.contains(g)) convModel.addElement(g);
    }

    private void onInvite(String[] p) {
        if (p.length < 3) return;
        int r = JOptionPane.showConfirmDialog(this,
            p[2] + " mời bạn vào nhóm \"" + p[1] + "\"\nTham gia?",
            "Lời mời nhóm", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) {
            client.conn.send(Protocol.build(Protocol.JOIN_GROUP, p[1]));
            getOrCreate("#" + p[1]);
        }
    }

    private void onHistory(String[] p) {
        if (p.length < 3) return;
        ChatPanel cp = panels.get(p[1]);
        if (cp == null) return;
        for (String e : p[2].split(";")) {
            String[] pts = e.split(":", 3);
            if (pts.length == 3) cp.addMessage(pts[0], decodeHistoryContent(pts[1]), pts[2], null, null);
        }
    }

    private void onKicked(String[] p) {
        JOptionPane.showMessageDialog(this,
            p.length > 1 ? p[1] : "Bạn đã bị ngắt kết nối",
            "Thông báo", JOptionPane.WARNING_MESSAGE);
        System.exit(0);
    }

    private void onFileOffer(String[] p) {
        if (p.length < 5) return;
        String from = p[1], fname = p[2];
        long size = 0;
        try { size = Long.parseLong(p[3]); } catch (NumberFormatException ignored) {}
        client.fileMgr.prepareIncoming(from, fname, size, p[4]);
    }

    // ── Gửi tin nhắn ──────────────────────────────────────────────────────
    private void sendMessage() {
        String text = inputField.getText().trim();
        if ((text.isEmpty() && pendingAttachment == null) || currentKey == null) return;

        String ts  = new SimpleDateFormat("HH:mm").format(new Date());
        ChatPanel cp = panels.get(currentKey);
        String quoted = cp != null ? cp.getQuotedText() : null;

        if (!text.isEmpty()) {
            if ("Lobby".equals(currentKey))
                client.msgMgr.sendBroadcast(text);
            else if (currentKey.startsWith("#"))
                client.msgMgr.sendGroupMsg(currentKey.substring(1), text);
            else
                client.msgMgr.sendPM(currentKey, text);

            if (cp != null) {
                cp.addMessage(session.getUsername(), text, ts, "sent", quoted);
                cp.clearQuote();
            }
        }

        if (pendingAttachment != null) {
            client.fileMgr.offerFile(pendingAttachment, currentKey);
        }

        clearQuoteBar();
        clearAttachmentPreview();
        inputField.setText("");
        inputField.requestFocus();
    }

    // ── Build UI ───────────────────────────────────────────────────────────
    private void buildUI() {
        setLayout(new BorderLayout());

        // Main layout: NavRail | ConvList | ChatArea
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Config.CHAT_LIST_BG);

        // Left side: NavRail (60px) + ConvList (280px)
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(348, 0));

        navRail = buildNavRail();
        JPanel convPanel = buildConvPanel();

        leftPanel.add(navRail, BorderLayout.WEST);
        leftPanel.add(convPanel, BorderLayout.CENTER);

        // Right: Chat area
        JPanel rightPanel = buildRightPanel();

        // Separator line
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setForeground(Config.DIVIDER);
        sep.setBackground(Config.DIVIDER);

        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(rightPanel, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel buildNavRail() {
        // NavRail với gradient xanh đậm — đồng bộ LoginDialog
        JPanel rail = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(10, 20, 48),
                                                      0, getHeight(), new Color(0, 58, 112));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Vòng trang trí nhỏ
                g2.setColor(new Color(64, 158, 255, 18));
                g2.fillOval(-30, getHeight() / 3, 120, 120);
                g2.setColor(new Color(255, 255, 255, 6));
                g2.fillOval(10, -20, 80, 80);
                g2.dispose();
            }
        };
        rail.setLayout(new BoxLayout(rail, BoxLayout.Y_AXIS));
        rail.setOpaque(false);
        rail.setPreferredSize(new Dimension(68, 0));

        // Logo icon (LC trong vòng tròn accent)
        JComponent logoIcon = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Hào quang
                g2.setColor(new Color(64, 158, 255, 40));
                g2.fillOval(2, 2, 38, 38);
                // Nền tròn
                g2.setColor(new Color(64, 158, 255, 200));
                g2.fillOval(6, 6, 30, 30);
                // Ký tự
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                String t = "LC";
                g2.drawString(t, (42 - fm.stringWidth(t)) / 2, (42 + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(42, 42); }
        };
        logoIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoIcon.setMaximumSize(new Dimension(42, 42));

        JPanel logoBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        logoBox.setOpaque(false);
        logoBox.setBorder(new EmptyBorder(14, 0, 16, 0));
        logoBox.add(logoIcon);

        // Divider line
        JPanel divider1 = new JPanel();
        divider1.setOpaque(false);
        divider1.setPreferredSize(new Dimension(40, 1));
        divider1.setMaximumSize(new Dimension(40, 1));
        divider1.setBorder(javax.swing.BorderFactory.createMatteBorder(
            1, 0, 0, 0, new Color(255, 255, 255, 30)));
        divider1.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Nav buttons
        navBtnChat     = navBtn("💬", "Trò chuyện", true);
        navBtnContacts = navBtn("👥", "Danh bạ", false);
        navBtnAdmin    = navBtn("🛡️", "Quản trị", false);

        navBtnChat.addActionListener(e -> switchNav(navBtnChat));
        navBtnContacts.addActionListener(e -> switchNav(navBtnContacts));
        navBtnAdmin.addActionListener(e -> {
            if (session.isAdmin()) switchNav(navBtnAdmin);
            else JOptionPane.showMessageDialog(this,
                "Chỉ Admin mới có quyền truy cập trang quản trị.",
                "Không có quyền", JOptionPane.WARNING_MESSAGE);
        });
        navBtnAdmin.setVisible(false);

        rail.add(logoBox);
        rail.add(divider1);
        rail.add(Box.createVerticalStrut(10));
        rail.add(navBtnChat);
        rail.add(Box.createVerticalStrut(2));
        rail.add(navBtnContacts);
        rail.add(Box.createVerticalStrut(2));
        rail.add(navBtnAdmin);
        rail.add(Box.createGlue());

        // ── Nút Đổi mật khẩu (icon khóa) ──
        JButton btnChangePwd = navActionBtn("🔑", "Đổi mật khẩu");
        btnChangePwd.addActionListener(e -> {
            if (session.isLoggedIn()) openChangePassword();
        });

        // ── User avatar / disconnect ──
        btnConnect = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Ring ngoài
                g2.setColor(new Color(64, 158, 255, 60));
                g2.fillOval(0, 0, getWidth(), getHeight());
                // Nền avatar
                g2.setColor(new Color(0, 45, 95));
                g2.fillOval(3, 3, getWidth()-6, getHeight()-6);
                // Initials
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                String t = session.isLoggedIn()
                    ? session.getUsername().substring(0, Math.min(2, session.getUsername().length())).toUpperCase()
                    : "?";
                g2.drawString(t, (getWidth()-fm.stringWidth(t))/2, (getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        btnConnect.setPreferredSize(new Dimension(40, 40));
        btnConnect.setMaximumSize(new Dimension(40, 40));
        btnConnect.setContentAreaFilled(false);
        btnConnect.setBorderPainted(false);
        btnConnect.setFocusPainted(false);
        btnConnect.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnConnect.setToolTipText("Tài khoản — Click phải để xem tùy chọn");
        btnConnect.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnConnect.addActionListener(e -> {
            if (client.conn.isConnected()) {
                showStyledUserPopup(btnConnect);
            } else showLogin();
        });
        btnConnect.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showStyledUserPopup(btnConnect);
            }
            @Override public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showStyledUserPopup(btnConnect);
            }
        });

        JPanel bottomBox = new JPanel();
        bottomBox.setLayout(new BoxLayout(bottomBox, BoxLayout.Y_AXIS));
        bottomBox.setOpaque(false);
        bottomBox.setBorder(new EmptyBorder(0, 0, 14, 0));

        // Divider trước bottom
        JPanel divider2 = new JPanel();
        divider2.setOpaque(false);
        divider2.setPreferredSize(new Dimension(40, 1));
        divider2.setMaximumSize(new Dimension(40, 1));
        divider2.setBorder(javax.swing.BorderFactory.createMatteBorder(
            1, 0, 0, 0, new Color(255, 255, 255, 30)));
        divider2.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel pwdBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
        pwdBox.setOpaque(false);
        pwdBox.add(btnChangePwd);

        JPanel userBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        userBox.setOpaque(false);
        userBox.setBorder(new EmptyBorder(2, 0, 0, 0));
        userBox.add(btnConnect);

        bottomBox.add(divider2);
        bottomBox.add(pwdBox);
        bottomBox.add(userBox);

        rail.add(bottomBox);
        return rail;
    }

    /** Hiện popup người dùng có thiết kế đồng bộ */
    private void showStyledUserPopup(Component source) {
        if (!session.isLoggedIn()) return;
        JPopupMenu menu = new JPopupMenu() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
            }
        };
        menu.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(210, 220, 240), 1, true),
            new EmptyBorder(4, 0, 4, 0)));
        menu.setBackground(Color.WHITE);
        menu.setOpaque(false);

        JMenuItem miUser = new JMenuItem("👤  " + session.getUsername());
        miUser.setFont(new Font("Segoe UI", Font.BOLD, 13));
        miUser.setForeground(new Color(20, 28, 48));
        miUser.setEnabled(false);
        miUser.setBackground(Color.WHITE);
        menu.add(miUser);
        menu.addSeparator();

        JMenuItem miPass = new JMenuItem("🔑  Đổi mật khẩu");
        miPass.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        miPass.setForeground(new Color(20, 28, 48));
        miPass.addActionListener(ev -> openChangePassword());
        menu.add(miPass);

        menu.addSeparator();
        JMenuItem miLogout = new JMenuItem("🚪  Ngắt kết nối");
        miLogout.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        miLogout.setForeground(new Color(200, 40, 40));
        miLogout.addActionListener(ev -> disconnect());
        menu.add(miLogout);

        menu.show(source, source.getWidth() + 4, 0);
    }

    /** Nút action nhỏ trong NavRail (icon + tooltip) */
    private JButton navActionBtn(String icon, String tooltip) {
        JButton b = new JButton(icon) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 30));
                    g2.fillRoundRect(4, 2, getWidth()-8, getHeight()-4, 8, 8);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        b.setForeground(new Color(180, 215, 255));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(44, 38));
        b.setMaximumSize(new Dimension(44, 38));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setToolTipText(tooltip);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setForeground(Color.WHITE); }
            public void mouseExited(MouseEvent e)  { b.setForeground(new Color(180, 215, 255)); }
        });
        return b;
    }

    private JButton navBtn(String icon, String tooltip, boolean active) {
        JButton b = new JButton(icon) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean sel = Boolean.TRUE.equals(getClientProperty("active"));
                if (sel) {
                    g2.setColor(new Color(64, 158, 255, 55));
                    g2.fillRoundRect(6, 3, getWidth()-12, getHeight()-6, 12, 12);
                    g2.setColor(new Color(64, 158, 255));
                    g2.fillRoundRect(0, (getHeight()-20)/2, 3, 20, 3, 3);
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 18));
                    g2.fillRoundRect(6, 3, getWidth()-12, getHeight()-6, 12, 12);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 21));
        b.setForeground(active ? Color.WHITE : new Color(170, 210, 255));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(68, 52));
        b.setMaximumSize(new Dimension(68, 52));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setToolTipText(tooltip);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty("active", active);
        return b;
    }

    private void switchNav(JButton clicked) {
        for (JButton nb : new JButton[]{navBtnChat, navBtnContacts, navBtnAdmin}) {
            if (nb == null) continue;
            boolean sel = nb == clicked;
            nb.putClientProperty("active", sel);
            nb.setForeground(sel ? Color.WHITE : new Color(170, 210, 255));
            nb.repaint();
        }
        // Nếu click Admin, hiện admin panel trong chatArea
        if (clicked == navBtnAdmin && session.isAdmin()) {
            showAdminPanel();
        }
    }

    private void updateNavUser() {
        btnConnect.repaint();
        if (navBtnAdmin != null) {
            navBtnAdmin.setVisible(session.isAdmin());
        }
        if (navRail != null) navRail.repaint();
    }

    private JPanel buildConvPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Config.CHAT_LIST_BG);
        panel.setBorder(new MatteBorder(0, 0, 0, 1, Config.DIVIDER));

        // ── Header — styled to match overall dark accent theme
        JPanel header = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(250, 252, 255));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, new Color(220, 228, 240)),
            new EmptyBorder(14, 16, 14, 14)));

        statusLabel = new JLabel("Chưa kết nối");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        statusLabel.setForeground(new Color(20, 28, 48));

        JPanel headerBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        headerBtns.setOpaque(false);

        JButton btnNewGroup = compactIconBtn("➕", "Tạo nhóm (Admin)");
        btnNewGroup.addActionListener(e -> {
            if (!session.isAdmin()) {
                JOptionPane.showMessageDialog(this,
                    "Chỉ Admin mới có quyền tạo nhóm.",
                    "Không có quyền", JOptionPane.WARNING_MESSAGE);
                return;
            }
            createGroup();
        });
        headerBtns.add(btnNewGroup);

        header.add(statusLabel, BorderLayout.WEST);
        header.add(headerBtns, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        // ── Search bar
        JPanel searchBox = new JPanel(new BorderLayout(6, 0));
        searchBox.setBackground(Config.CHAT_LIST_BG);
        searchBox.setBorder(new EmptyBorder(8, 10, 6, 10));

        JTextField searchField = new JTextField();
        searchField.setFont(Config.FONT_NORMAL);
        searchField.setBackground(new Color(237, 242, 252));
        searchField.setForeground(new Color(90, 105, 140));
        searchField.setCaretColor(new Color(20, 28, 48));
        searchField.putClientProperty("JTextField.placeholderText", "Tìm kiếm...");
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Config.DIVIDER, 1, true),
            new EmptyBorder(6, 32, 6, 10)));
        searchBox.add(searchField, BorderLayout.CENTER);
        panel.add(searchBox, BorderLayout.AFTER_LAST_LINE);

        // ── Conversation list
        convList = new JList<>(convModel);
        convList.setBackground(Config.CHAT_LIST_BG);
        convList.setSelectionBackground(new Color(0,0,0,0));
        convList.setSelectionForeground(Config.TEXT_PRIMARY);
        convList.setFixedCellHeight(68);
        convList.setCellRenderer(new ZaloConvRenderer());
        convList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                String v = convList.getSelectedValue();
                if (v != null) openChat(v);
            }
        });

        JScrollPane scroll = new JScrollPane(convList);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Config.CHAT_LIST_BG);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(4, 0));
        scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            protected void configureScrollBarColors() {
                thumbColor = Config.DIVIDER;
                trackColor = Config.CHAT_LIST_BG;
            }
            protected JButton createDecreaseButton(int o) { return zeroBtn(); }
            protected JButton createIncreaseButton(int o) { return zeroBtn(); }
            private JButton zeroBtn() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0,0));
                return b;
            }
        });

        panel.add(searchBox, BorderLayout.NORTH);
        // Re-arrange layout
        panel.remove(header);
        panel.remove(searchBox);
        JPanel topSection = new JPanel(new BorderLayout());
        topSection.add(header, BorderLayout.NORTH);
        topSection.add(searchBox, BorderLayout.CENTER);
        panel.add(topSection, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildRightPanel() {
        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(Config.CHAT_BG);

        // ── Chat header — gradient subtle top strip
        chatHeader = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Subtle top accent line
                GradientPaint lineGp = new GradientPaint(
                    0, 0, new Color(64, 158, 255, 120),
                    getWidth(), 0, new Color(0, 58, 112, 0));
                g2.setPaint(lineGp);
                g2.fillRect(0, 0, getWidth(), 2);
                g2.dispose();
            }
        };
        chatHeader.setOpaque(false);
        chatHeader.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, new Color(220, 228, 240)),
            new EmptyBorder(12, 18, 12, 16)));
        chatHeader.setPreferredSize(new Dimension(0, 62));

        JPanel titleGroup = new JPanel(new BorderLayout(0, 2));
        titleGroup.setOpaque(false);
        chatTitleLabel = new JLabel("Chọn cuộc trò chuyện");
        chatTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        chatTitleLabel.setForeground(new Color(20, 28, 48));
        chatSubLabel = new JLabel("Không có tin nhắn nào");
        chatSubLabel.setFont(Config.FONT_SMALL);
        chatSubLabel.setForeground(Config.TEXT_MUTED);
        titleGroup.add(chatTitleLabel, BorderLayout.NORTH);
        titleGroup.add(chatSubLabel, BorderLayout.SOUTH);

        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        headerActions.setOpaque(false);
        JButton btnAddMember = compactIconBtn("👤➕", "Thêm thành viên (Admin)");
        btnAddMember.setToolTipText("Thêm thành viên vào nhóm");
        btnAddMember.addActionListener(e -> addMemberToGroup());
        btnAddMember.setVisible(false); // shown when a group is open and user is admin
        JButton btnInfo = compactIconBtn("i", "Thong tin cuoc tro chuyen");
        btnInfo.addActionListener(e -> openConvInfo());
        headerActions.add(btnAddMember);
        headerActions.add(btnInfo);

        // Store reference so openChat can show/hide
        chatHeader.putClientProperty("btnAddMember", btnAddMember);

        chatHeader.add(titleGroup, BorderLayout.CENTER);
        chatHeader.add(headerActions, BorderLayout.EAST);
        right.add(chatHeader, BorderLayout.NORTH);

        // ── Card layout for chat panels
        cardLayout = new CardLayout();
        chatArea = new JPanel(cardLayout);
        chatArea.setBackground(Config.CHAT_BG);

        // Welcome screen
        JPanel welcome = buildWelcomeScreen();
        chatArea.add(welcome, "__welcome__");

        right.add(chatArea, BorderLayout.CENTER);

        // ── Input area
        right.add(buildInputArea(), BorderLayout.SOUTH);

        return right;
    }

    private JPanel buildWelcomeScreen() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Config.CHAT_BG);
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        JLabel icon = new JLabel("💬");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 60));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("LAN Chat");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Config.ZALO_BLUE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Chọn một cuộc trò chuyện để bắt đầu");
        sub.setFont(Config.FONT_NORMAL);
        sub.setForeground(Config.TEXT_MUTED);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(icon);
        inner.add(Box.createVerticalStrut(12));
        inner.add(title);
        inner.add(Box.createVerticalStrut(6));
        inner.add(sub);

        p.add(inner);
        return p;
    }

    private JPanel buildInputArea() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(Config.INPUT_AREA_BG);
        outer.setBorder(new MatteBorder(1, 0, 0, 0, Config.DIVIDER));

        // Quote strip
        quotePanelWrapper = new JPanel(new BorderLayout());
        quotePanelWrapper.setBackground(new Color(232, 240, 255));
        quotePanelWrapper.setBorder(new MatteBorder(0, 3, 0, 0, Config.ZALO_BLUE));
        quotePanelWrapper.setVisible(false);

        quoteLabel = new JLabel(" ");
        quoteLabel.setFont(Config.FONT_SMALL);
        quoteLabel.setForeground(Config.ZALO_BLUE);
        quoteLabel.setBorder(new EmptyBorder(5, 10, 5, 10));

        JButton btnClearQuote = new JButton("✕");
        btnClearQuote.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        btnClearQuote.setForeground(Config.TEXT_MUTED);
        btnClearQuote.setContentAreaFilled(false);
        btnClearQuote.setBorderPainted(false);
        btnClearQuote.setFocusPainted(false);
        btnClearQuote.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClearQuote.addActionListener(e -> clearQuoteBar());

        quotePanelWrapper.add(quoteLabel, BorderLayout.CENTER);
        quotePanelWrapper.add(btnClearQuote, BorderLayout.EAST);

        attachmentPreviewWrapper = new JPanel(new BorderLayout(8, 0));
        attachmentPreviewWrapper.setBackground(new Color(245, 248, 252));
        attachmentPreviewWrapper.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 3, 0, 0, new Color(146, 186, 232)),
            new EmptyBorder(8, 10, 8, 10)));
        attachmentPreviewWrapper.setVisible(false);

        attachmentPreviewLabel = new JLabel(" ");
        attachmentPreviewLabel.setFont(Config.FONT_SMALL);
        attachmentPreviewLabel.setForeground(Config.TEXT_PRIMARY);

        attachmentClearButton = new JButton("✕");
        attachmentClearButton.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        attachmentClearButton.setForeground(Config.TEXT_MUTED);
        attachmentClearButton.setContentAreaFilled(false);
        attachmentClearButton.setBorderPainted(false);
        attachmentClearButton.setFocusPainted(false);
        attachmentClearButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        attachmentClearButton.addActionListener(e -> clearAttachmentPreview());

        attachmentPreviewWrapper.add(attachmentPreviewLabel, BorderLayout.CENTER);
        attachmentPreviewWrapper.add(attachmentClearButton, BorderLayout.EAST);

        // Input row
        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setBackground(Config.INPUT_AREA_BG);
        inputRow.setBorder(new EmptyBorder(8, 12, 10, 12));

        // Left action buttons
        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        leftBtns.setOpaque(false);

        emojiPicker = new EmojiPicker(this, emoji -> {
            int pos = inputField.getCaretPosition();
            String current = inputField.getText();
            inputField.setText(current.substring(0, pos) + emoji + current.substring(pos));
            inputField.requestFocusInWindow();
            inputField.setCaretPosition(pos + emoji.length());
        });

        JButton bEmoji = inputIconBtn("😊");
        JButton bImage = inputIconBtn("🖼️");
        JButton bFile  = inputIconBtn("📎");
        bEmoji.setToolTipText("Emoji");
        bImage.setToolTipText("Gửi ảnh");
        bFile.setToolTipText("Gửi file");
        bEmoji.addActionListener(e -> emojiPicker.showAt(bEmoji));
        bImage.addActionListener(e -> pickImage());
        bFile.addActionListener(e -> pickFile());

        leftBtns.add(bEmoji);
        leftBtns.add(bImage);
        leftBtns.add(bFile);

        // Text input
        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        inputField.setBackground(Color.WHITE);
        inputField.setForeground(Config.TEXT_PRIMARY);
        inputField.setCaretColor(Config.ZALO_BLUE);
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Config.INPUT_BORDER, 1, true),
            new EmptyBorder(8, 14, 8, 14)));
        inputField.addActionListener(e -> sendMessage());
        inputField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                inputField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Config.ZALO_BLUE, 1, true),
                    new EmptyBorder(8, 14, 8, 14)));
            }
            public void focusLost(FocusEvent e) {
                inputField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Config.INPUT_BORDER, 1, true),
                    new EmptyBorder(8, 14, 8, 14)));
            }
        });

        // Send button — đồng bộ với login button style
        btnSend = new JButton("Gửi") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c1 = getModel().isRollover() ? new Color(85, 175, 255) : new Color(64, 158, 255);
                Color c2 = getModel().isRollover() ? new Color(30, 110, 215) : new Color(8, 96, 205);
                GradientPaint gp = new GradientPaint(0, 0, c1, 0, getHeight(), c2);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillRoundRect(0, 0, getWidth(), getHeight()/2, 12, 12);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                String t = getText();
                g2.drawString(t, (getWidth()-fm.stringWidth(t))/2, (getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {}
        };
        btnSend.setContentAreaFilled(false);
        btnSend.setBorderPainted(false);
        btnSend.setFocusPainted(false);
        btnSend.setPreferredSize(new Dimension(72, 36));
        btnSend.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSend.addActionListener(e -> sendMessage());

        inputRow.add(leftBtns, BorderLayout.WEST);
        inputRow.add(inputField, BorderLayout.CENTER);
        inputRow.add(btnSend, BorderLayout.EAST);

        outer.add(quotePanelWrapper, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(attachmentPreviewWrapper, BorderLayout.NORTH);
        center.add(inputRow, BorderLayout.CENTER);
        outer.add(center, BorderLayout.CENTER);

        TransferHandler dropHandler = new TransferHandler() {
            @Override public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override public boolean importData(TransferSupport support) {
                if (!canImport(support)) return false;
                try {
                    @SuppressWarnings("unchecked")
                    java.util.List<File> files = (java.util.List<File>) support.getTransferable()
                        .getTransferData(DataFlavor.javaFileListFlavor);
                    if (files == null || files.isEmpty()) return false;
                    queueAttachment(files.get(0));
                    return true;
                } catch (Exception ignored) {
                    return false;
                }
            }
        };
        outer.setTransferHandler(dropHandler);
        inputField.setTransferHandler(dropHandler);
        return outer;
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private ChatPanel getOrCreate(String key) {
        if (!panels.containsKey(key)) {
            ChatPanel cp = new ChatPanel(
                session.getUsername(),
                this::openAttachment,
                this::saveAttachmentDirect,
                fm -> client.fileMgr.resolveCachedFile(fm.transferId(), fm.fileName()));
            cp.setOnQuote((sender, text) -> showQuoteBar(sender, text));
            panels.put(key, cp);
            chatArea.add(cp, key);
            if (!key.equals("Lobby") && !key.startsWith("#") && session.isLoggedIn())
                client.msgMgr.requestHistory(key);
        }
        return panels.get(key);
    }

    private void openChat(String key) {
        if (key == null || key.equals(session.getUsername())) return;
        getOrCreate(key);
        currentKey = key;
        cardLayout.show(chatArea, key);
        chatTitleLabel.setText(key.startsWith("#") ? key : key);
        chatSubLabel.setText(key.startsWith("#") ? "Nhóm" : "Trực tuyến");
        unread.put(key, 0);
        convList.repaint();

        // Hiện/ẩn nút Thêm thành viên: chỉ admin, chỉ khi đang trong nhóm
        if (chatHeader != null) {
            Object btn = chatHeader.getClientProperty("btnAddMember");
            if (btn instanceof JButton addBtn) {
                addBtn.setVisible(session.isAdmin() && key.startsWith("#"));
            }
        }
    }

    private void bump(String key) {
        if (key.equals(currentKey)) return;
        int n = unread.getOrDefault(key, 0) + 1;
        unread.put(key, n);
        convList.repaint();
    }

    private void showSystem(String key, String msg) { getOrCreate(key).addSystemMsg(msg); }

    private void createGroup() {
        if (!session.isAdmin()) {
            JOptionPane.showMessageDialog(this,
                "Chỉ Admin mới có quyền tạo nhóm.",
                "Không có quyền", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String name = JOptionPane.showInputDialog(this, "Tên nhóm mới:");
        if (name == null || name.isBlank()) return;
        client.conn.send(Protocol.build(Protocol.CREATE_GROUP, name.trim()));
        getOrCreate("#" + name.trim());
    }

    private void addMemberToGroup() {
        if (!session.isAdmin()) {
            JOptionPane.showMessageDialog(this,
                "Chỉ Admin mới có quyền thêm thành viên vào nhóm.",
                "Không có quyền", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (currentKey == null || !currentKey.startsWith("#")) {
            JOptionPane.showMessageDialog(this,
                "Hãy chọn một nhóm chat trước.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String groupName = currentKey.substring(1);
        String username  = JOptionPane.showInputDialog(this,
            "Nhập tên đăng nhập cần thêm vào nhóm \"" + groupName + "\":");
        if (username == null || username.isBlank()) return;
        client.conn.send(Protocol.build(Protocol.ADD_MEMBER, groupName, username.trim()));
    }

    /** Mo dialog thong tin cuoc tro chuyen — gui yeu cau len server. */
    private void openConvInfo() {
        if (currentKey == null || currentKey.equals("__admin__")) return;
        if ("Lobby".equals(currentKey)) {
            JOptionPane.showMessageDialog(this,
                "Khong the xem thong tin cua Lobby.",
                "Thong bao", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        boolean isGroup = currentKey.startsWith("#");
        String target   = isGroup ? currentKey.substring(1) : currentKey;
        client.conn.send(Protocol.build(Protocol.CONV_INFO_REQ, target, String.valueOf(isGroup)));
    }

    /** Mo dialog doi mat khau (user thuong). */
    private void openChangePassword() {
        if (!session.isLoggedIn()) return;
        chat.ui.client.dialogs.ChangePasswordDialog dlg =
            new chat.ui.client.dialogs.ChangePasswordDialog(
                this,
                raw -> client.conn.send(raw));
        dlg.setVisible(true);
    }

    /** Mo dialog doi mat khau cho user bat ky (admin). */
    private void openAdminChangePassword(String targetUser) {
        chat.ui.client.dialogs.ChangePasswordDialog dlg =
            new chat.ui.client.dialogs.ChangePasswordDialog(
                this,
                raw -> client.conn.send(raw),
                true, targetUser);
        dlg.setVisible(true);
    }

    /** Hien thi bang dieu khien Admin trong khu vuc chat chinh. */
    private void showAdminPanel() {
        // Tạo panel nếu chưa có
        if (!panels.containsKey("__admin__")) {
            chatArea.add(buildAdminPanel(), "__admin__");
        }
        currentKey = "__admin__";
        cardLayout.show(chatArea, "__admin__");
        chatTitleLabel.setText("🛡️ Quản trị viên");
        chatSubLabel.setText("Quản lý tài khoản & phân quyền");
        if (chatHeader != null) {
            Object btn = chatHeader.getClientProperty("btnAddMember");
            if (btn instanceof JButton addBtn) addBtn.setVisible(false);
        }
    }

    /** Xây dựng Admin Panel — tạo tài khoản, phân quyền, thêm thành viên nhóm. */
    private JPanel buildAdminPanel() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Config.CHAT_BG);
        root.setBorder(new EmptyBorder(24, 32, 24, 32));

        // ── Tiêu đề
        JLabel title = new JLabel("🛡️  Bảng Quản Trị");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Config.ZALO_BLUE);
        title.setBorder(new EmptyBorder(0, 0, 20, 0));
        root.add(title, BorderLayout.NORTH);

        // Admin cards - viet ben duoi

        // ── Card 1: Tạo tài khoản ──
        JPanel cardCreate = adminCard("➕  Tạo tài khoản mới");
        JTextField fUser = adminField("Tên đăng nhập *");
        JPasswordField fPass = new JPasswordField();
        styleAdminField(fPass, "Mật khẩu *");
        JTextField fFull = adminField("Họ và tên");
        String[] roles = {"nhan_vien", "pho_phong", "truong_phong"};
        String[] roleLabels = {"Nhân viên", "Phó phòng", "Trưởng phòng"};
        JComboBox<String> cbRole = new JComboBox<>(roleLabels);
        cbRole.setFont(Config.FONT_NORMAL);
        cbRole.setBackground(Color.WHITE);
        cbRole.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Config.INPUT_BORDER, 1, true),
            new EmptyBorder(4, 8, 4, 8)));
        JButton btnCreate = adminActionBtn("Tạo tài khoản", Config.ZALO_BLUE);
        btnCreate.addActionListener(e -> {
            String u = fUser.getText().trim();
            String p = new String(fPass.getPassword()).trim();
            String f = fFull.getText().trim();
            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.",
                    "Thiếu thông tin", JOptionPane.WARNING_MESSAGE); return;
            }
            String selectedRole = roles[cbRole.getSelectedIndex()];
            client.conn.send(Protocol.build(Protocol.CREATE_USER, u, p, selectedRole,
                f.isEmpty() ? u : f));
            fUser.setText(""); fPass.setText(""); fFull.setText("");
        });

        cardCreate.add(adminLabel("Tên đăng nhập"));
        cardCreate.add(fUser);
        cardCreate.add(Box.createVerticalStrut(8));
        cardCreate.add(adminLabel("Mật khẩu"));
        cardCreate.add(fPass);
        cardCreate.add(Box.createVerticalStrut(8));
        cardCreate.add(adminLabel("Họ và tên"));
        cardCreate.add(fFull);
        cardCreate.add(Box.createVerticalStrut(8));
        cardCreate.add(adminLabel("Chức vụ"));
        cardCreate.add(cbRole);
        cardCreate.add(Box.createVerticalStrut(16));
        cardCreate.add(btnCreate);
        cardCreate.add(Box.createVerticalGlue());

        // ── Card 2: Phân quyền ──
        JPanel cardRole = adminCard("🎖️  Phân quyền tài khoản");
        JTextField fRoleUser = adminField("Tên đăng nhập");
        String[] rolesAll = {"nhan_vien", "pho_phong", "truong_phong"};
        String[] roleLabelsAll = {"Nhân viên", "Phó phòng", "Trưởng phòng"};
        JComboBox<String> cbNewRole = new JComboBox<>(roleLabelsAll);
        cbNewRole.setFont(Config.FONT_NORMAL);
        cbNewRole.setBackground(Color.WHITE);
        cbNewRole.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Config.INPUT_BORDER, 1, true),
            new EmptyBorder(4, 8, 4, 8)));
        JButton btnAssign = adminActionBtn("Phân quyền", new Color(46, 160, 67));
        btnAssign.addActionListener(e -> {
            String u = fRoleUser.getText().trim();
            if (u.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập tên đăng nhập.",
                    "Thiếu thông tin", JOptionPane.WARNING_MESSAGE); return;
            }
            String newRole = rolesAll[cbNewRole.getSelectedIndex()];
            client.conn.send(Protocol.build(Protocol.ASSIGN_ROLE, u, newRole));
            fRoleUser.setText("");
        });

        JLabel noteRole = new JLabel("<html><i>Phân quyền áp dụng ngay lập tức.<br>User đang online sẽ được cập nhật.</i></html>");
        noteRole.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        noteRole.setForeground(Config.TEXT_MUTED);

        cardRole.add(adminLabel("Tên đăng nhập"));
        cardRole.add(fRoleUser);
        cardRole.add(Box.createVerticalStrut(8));
        cardRole.add(adminLabel("Chức vụ mới"));
        cardRole.add(cbNewRole);
        cardRole.add(Box.createVerticalStrut(16));
        cardRole.add(btnAssign);
        cardRole.add(Box.createVerticalStrut(12));
        cardRole.add(noteRole);
        cardRole.add(Box.createVerticalGlue());

        // ── Card 3: Thêm thành viên nhóm ──
        JPanel cardMember = adminCard("👥  Thêm thành viên nhóm");
        JTextField fGroup  = adminField("Tên nhóm");
        JTextField fMember = adminField("Tên đăng nhập");
        JButton btnAdd = adminActionBtn("Thêm vào nhóm", new Color(0, 150, 136));
        btnAdd.addActionListener(e -> {
            String g = fGroup.getText().trim();
            String u = fMember.getText().trim();
            if (g.isEmpty() || u.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ tên nhóm và tên đăng nhập.",
                    "Thiếu thông tin", JOptionPane.WARNING_MESSAGE); return;
            }
            client.conn.send(Protocol.build(Protocol.ADD_MEMBER, g, u));
            fMember.setText("");
        });

        JLabel noteMember = new JLabel("<html><i>Điền tên nhóm chính xác như trong<br>danh sách cuộc trò chuyện.</i></html>");
        noteMember.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        noteMember.setForeground(Config.TEXT_MUTED);

        cardMember.add(adminLabel("Tên nhóm"));
        cardMember.add(fGroup);
        cardMember.add(Box.createVerticalStrut(8));
        cardMember.add(adminLabel("Thành viên cần thêm"));
        cardMember.add(fMember);
        cardMember.add(Box.createVerticalStrut(16));
        cardMember.add(btnAdd);
        cardMember.add(Box.createVerticalStrut(12));
        cardMember.add(noteMember);
        cardMember.add(Box.createVerticalGlue());

        // Cards are added to allCards panel below

        // ── Card 4: Doi mat khau nguoi dung ──
        JPanel cardPass = adminCard("  Doi mat khau nguoi dung");
        JTextField fPassUser = adminField("Ten dang nhap can doi");
        JButton btnAdminPass = adminActionBtn("Doi mat khau", new Color(183, 28, 28));
        btnAdminPass.addActionListener(e -> {
            String u = fPassUser.getText().trim();
            if (u.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Vui long nhap ten dang nhap.",
                    "Thieu thong tin", JOptionPane.WARNING_MESSAGE);
                return;
            }
            openAdminChangePassword(u);
            fPassUser.setText("");
        });

        JLabel notePass = new JLabel("<html><i>Admin co toan quyen doi mat khau<br>nguoi dung - khong can mat khau cu.</i></html>");
        notePass.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        notePass.setForeground(Config.TEXT_MUTED);

        cardPass.add(adminLabel("Ten dang nhap"));
        cardPass.add(fPassUser);
        cardPass.add(Box.createVerticalStrut(16));
        cardPass.add(btnAdminPass);
        cardPass.add(Box.createVerticalStrut(12));
        cardPass.add(notePass);
        cardPass.add(Box.createVerticalGlue());

        // Boc cards vao ScrollPane de giao dien khong bi ep
        JPanel allCards = new JPanel(new GridLayout(1, 4, 16, 0));
        allCards.setOpaque(false);
        allCards.add(cardCreate);
        allCards.add(cardRole);
        allCards.add(cardMember);
        allCards.add(cardPass);
        root.add(allCards, BorderLayout.CENTER);

        // ── Footer ghi chú ──
        JLabel footer = new JLabel(
            "<html><div style='color:#888'>🔑  Chỉ Admin mới thấy trang này. " +
            "Mọi thao tác đều được ghi log trên server.</div></html>");
        footer.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        footer.setBorder(new EmptyBorder(16, 0, 0, 0));
        root.add(footer, BorderLayout.SOUTH);
        return root;
    }

    // ── Admin panel helpers ───────────────────────────────────────────────

    private JPanel adminCard(String heading) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Config.DIVIDER, 1, true),
            new EmptyBorder(20, 20, 20, 20)));

        JLabel h = new JLabel(heading);
        h.setFont(new Font("Segoe UI", Font.BOLD, 13));
        h.setForeground(Config.TEXT_PRIMARY);
        h.setAlignmentX(Component.LEFT_ALIGNMENT);
        h.setBorder(new EmptyBorder(0, 0, 14, 0));
        card.add(h);
        return card;
    }

    private JLabel adminLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(Config.TEXT_SECONDARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(new EmptyBorder(0, 0, 4, 0));
        return lbl;
    }

    private JTextField adminField(String placeholder) {
        JTextField tf = new JTextField();
        styleAdminField(tf, placeholder);
        return tf;
    }

    private void styleAdminField(JComponent tf, String placeholder) {
        tf.setFont(Config.FONT_NORMAL);
        tf.setBackground(new Color(248, 250, 252));
        tf.setForeground(Config.TEXT_PRIMARY);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Config.INPUT_BORDER, 1, true),
            new EmptyBorder(7, 10, 7, 10)));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        tf.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (tf instanceof JTextField jtf) jtf.putClientProperty("JTextField.placeholderText", placeholder);
    }

    private JButton adminActionBtn(String text, Color bg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? bg.darker() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                String t = getText();
                g2.drawString(t, (getWidth()-fm.stringWidth(t))/2,
                    (getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(Integer.MAX_VALUE, 36));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void pickFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Chọn file để gửi");
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        queueAttachment(fc.getSelectedFile());
    }

    private void pickImage() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Chọn ảnh để gửi");
        File imageDir = preferredImageDirectory();
        if (imageDir != null && imageDir.exists()) {
            fc.setCurrentDirectory(imageDir);
        }
        fc.setFileFilter(new FileNameExtensionFilter(
            "Image Files", "png", "jpg", "jpeg", "gif", "bmp", "webp"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        queueAttachment(fc.getSelectedFile());
    }

    private File preferredImageDirectory() {
        File home = new File(System.getProperty("user.home", "."));
        File[] candidates = {
            new File(home, "Pictures"),
            new File(home, "OneDrive\\Pictures"),
            FileSystemView.getFileSystemView().getDefaultDirectory()
        };
        for (File candidate : candidates) {
            if (candidate != null && candidate.exists() && candidate.isDirectory()) {
                return candidate;
            }
        }
        return home.exists() ? home : null;
    }

    private void queueAttachment(File file) {
        if (file == null) return;
        if (currentKey == null || "Lobby".equals(currentKey) || currentKey.startsWith("#")) {
            JOptionPane.showMessageDialog(this,
                "Chỉ gửi file/ảnh trong chat riêng tư.",
                "Không thể gửi", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (file.length() > Config.MAX_FILE_SIZE) {
            JOptionPane.showMessageDialog(this,
                "File quá lớn! Tối đa: " + formatSize(Config.MAX_FILE_SIZE),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        pendingAttachment = file;
        showAttachmentPreview(file);
    }

    private void showAttachmentPreview(File file) {
        attachmentPreviewWrapper.removeAll();
        attachmentPreviewWrapper.add(attachmentClearButton, BorderLayout.EAST);

        if (isImageFile(file)) {
            try {
                BufferedImage image = javax.imageio.ImageIO.read(file);
                if (image != null) {
                    Image scaled = image.getScaledInstance(72, 72, Image.SCALE_SMOOTH);
                    JLabel thumb = new JLabel(new ImageIcon(scaled));
                    thumb.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(210, 220, 232), 1, true),
                        new EmptyBorder(2, 2, 2, 2)));

                    JPanel info = new JPanel();
                    info.setOpaque(false);
                    info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
                    JLabel title = new JLabel(file.getName());
                    title.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    title.setForeground(Config.TEXT_PRIMARY);
                    JLabel meta = new JLabel("Ảnh chờ gửi • " + formatSize(file.length()));
                    meta.setFont(Config.FONT_SMALL);
                    meta.setForeground(Config.TEXT_MUTED);
                    info.add(title);
                    info.add(Box.createVerticalStrut(4));
                    info.add(meta);

                    JPanel left = new JPanel(new BorderLayout(10, 0));
                    left.setOpaque(false);
                    left.add(thumb, BorderLayout.WEST);
                    left.add(info, BorderLayout.CENTER);
                    attachmentPreviewWrapper.add(left, BorderLayout.CENTER);
                }
            } catch (Exception ignored) {}
        }

        if (attachmentPreviewWrapper.getComponentCount() == 1) {
            attachmentPreviewLabel.setText("📎 " + file.getName() + " • " + formatSize(file.length()));
            attachmentPreviewWrapper.add(attachmentPreviewLabel, BorderLayout.CENTER);
        }

        attachmentPreviewWrapper.revalidate();
        attachmentPreviewWrapper.repaint();
        attachmentPreviewWrapper.setVisible(true);
    }

    private void clearAttachmentPreview() {
        pendingAttachment = null;
        attachmentPreviewWrapper.setVisible(false);
        attachmentPreviewWrapper.removeAll();
        attachmentPreviewWrapper.add(attachmentPreviewLabel, BorderLayout.CENTER);
        attachmentPreviewWrapper.add(attachmentClearButton, BorderLayout.EAST);
        attachmentPreviewLabel.setText(" ");
    }

    private boolean isImageFile(File file) {
        String lower = file.getName().toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
            || lower.endsWith(".gif") || lower.endsWith(".bmp") || lower.endsWith(".webp");
    }

    private void showQuoteBar(String sender, String text) {
        quoteLabel.setText("↩ " + sender + ": " + truncate(text, 60));
        quotePanelWrapper.setVisible(true);
        ChatPanel cp = panels.get(currentKey);
        if (cp != null) cp.setQuotedText(sender + ": " + text);
    }

    private void clearQuoteBar() {
        quotePanelWrapper.setVisible(false);
        quoteLabel.setText(" ");
        ChatPanel cp = currentKey != null ? panels.get(currentKey) : null;
        if (cp != null) cp.clearQuote();
    }

    private void disconnect() {
        client.conn.disconnect();
        statusLabel.setText("Đã ngắt kết nối");
    }

    private void confirmExit() {
        int r = JOptionPane.showConfirmDialog(this, "Thoát khỏi LAN Chat?",
            "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) { disconnect(); System.exit(0); }
    }

    private String formatSize(long b) {
        if (b < 1024) return b + " B";
        if (b < 1024*1024) return String.format("%.1f KB", b/1024.0);
        return String.format("%.1f MB", b/(1024.0*1024));
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private void openAttachment(String attachmentMessage) {
        TransferMessageUtil.FileMessage fileMessage = TransferMessageUtil.parseFileMessage(attachmentMessage);
        if (fileMessage == null) return;

        File cached = client.fileMgr.resolveCachedFile(fileMessage.transferId(), fileMessage.fileName());
        if (cached == null || !cached.exists()) {
            JOptionPane.showMessageDialog(this,
                "File này không còn trong cache cục bộ.\nHãy yêu cầu gửi lại nếu cần.",
                "Không mở được file",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String lower = fileMessage.fileName().toLowerCase();
            if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".gif") || lower.endsWith(".bmp") || lower.endsWith(".webp")) {
                byte[] data = java.nio.file.Files.readAllBytes(cached.toPath());
                new ImageViewer(this, fileMessage.fileName(), data).setVisible(true);
            } else if (canOpenWithDesktop()) {
                Desktop.getDesktop().open(createOpenableCopy(cached, fileMessage.fileName()));
            } else {
                byte[] data = java.nio.file.Files.readAllBytes(cached.toPath());
                new FileViewer(this, fileMessage.fileName(), data).setVisible(true);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Không thể mở file: " + e.getMessage(),
                "Lỗi",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveAttachmentDirect(String attachmentMessage) {
        TransferMessageUtil.FileMessage fileMessage = TransferMessageUtil.parseFileMessage(attachmentMessage);
        if (fileMessage == null) return;

        File cached = client.fileMgr.resolveCachedFile(fileMessage.transferId(), fileMessage.fileName());
        if (cached == null || !cached.exists()) {
            JOptionPane.showMessageDialog(this,
                "Không tìm thấy ảnh trong cache cục bộ.",
                "Không lưu được ảnh",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Lưu ảnh");
        fc.setSelectedFile(new File(fileMessage.fileName()));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try {
            Files.copy(cached.toPath(), fc.getSelectedFile().toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            JOptionPane.showMessageDialog(this,
                "Đã lưu ảnh: " + fc.getSelectedFile().getAbsolutePath(),
                "Lưu thành công",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Không thể lưu ảnh: " + e.getMessage(),
                "Lỗi",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean canOpenWithDesktop() {
        return Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN);
    }

    private File createOpenableCopy(File cached, String originalFileName) throws java.io.IOException {
        java.nio.file.Path dir = java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), "lanchat-open");
        Files.createDirectories(dir);

        String safeName = originalFileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        java.nio.file.Path target = dir.resolve(safeName);
        if (Files.exists(target)) {
            int dot = safeName.lastIndexOf('.');
            String base = (dot > 0) ? safeName.substring(0, dot) : safeName;
            String ext = (dot > 0) ? safeName.substring(dot) : "";
            target = dir.resolve(base + "-" + System.currentTimeMillis() + ext);
        }

        Files.copy(cached.toPath(), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        target.toFile().deleteOnExit();
        return target.toFile();
    }

    private String decodeHistoryContent(String encoded) {
        try {
            return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return encoded;
        }
    }

    private String nowTs() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }

    private JButton compactIconBtn(String icon, String tooltip) {
        JButton b = new JButton(icon);
        b.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        b.setForeground(Config.TEXT_SECONDARY);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setToolTipText(tooltip);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(4, 8, 4, 8));
        return b;
    }

    private JButton inputIconBtn(String icon) {
        JButton b = new JButton(icon);
        b.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        b.setForeground(Config.TEXT_SECONDARY);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(4, 6, 4, 6));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setForeground(Config.ZALO_BLUE); }
            public void mouseExited(MouseEvent e)  { b.setForeground(Config.TEXT_SECONDARY); }
        });
        return b;
    }

    // ── Zalo-style conversation list renderer ──────────────────────────────
    private class ZaloConvRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object val, int idx, boolean sel, boolean foc) {
            String v = (String) val;
            boolean isGroup = v.startsWith("#");
            boolean isActive = v.equals(currentKey);

            JPanel row = new JPanel(new BorderLayout(12, 0));
            row.setOpaque(true);
            row.setBackground(isActive ? Config.CHAT_LIST_ACTIVE :
                              sel ? Config.CHAT_LIST_HOVER : Config.CHAT_LIST_BG);
            row.setBorder(new EmptyBorder(10, 14, 10, 14));

            // Avatar (tròn với initials)
            JPanel avatarWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            avatarWrap.setOpaque(false);
            avatarWrap.setPreferredSize(new Dimension(44, 44));
            JLabel avatar = makeConvAvatar(v, isGroup);
            avatarWrap.add(avatar);

            // Text area
            JPanel textArea = new JPanel();
            textArea.setLayout(new BoxLayout(textArea, BoxLayout.Y_AXIS));
            textArea.setOpaque(false);

            JPanel nameRow = new JPanel(new BorderLayout());
            nameRow.setOpaque(false);

            JLabel nameLabel = new JLabel(v);
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            nameLabel.setForeground(Config.TEXT_PRIMARY);
            nameRow.add(nameLabel, BorderLayout.WEST);

            // Timestamp (fake for now)
            JLabel timeLabel = new JLabel("vừa xong");
            timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            timeLabel.setForeground(Config.TEXT_TIME);
            nameRow.add(timeLabel, BorderLayout.EAST);

            JPanel subRow = new JPanel(new BorderLayout());
            subRow.setOpaque(false);

            JLabel previewLabel = new JLabel(isGroup ? "Nhóm trò chuyện" : "Nhấp để trò chuyện");
            previewLabel.setFont(Config.FONT_SMALL);
            previewLabel.setForeground(Config.TEXT_MUTED);
            subRow.add(previewLabel, BorderLayout.WEST);

            // Unread badge
            Integer n = unread.get(v);
            if (n != null && n > 0) {
                JLabel badge = new JLabel(n > 99 ? "99+" : String.valueOf(n)) {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(Config.RED_ACCENT);
                        g2.fillOval(0, 0, getWidth(), getHeight());
                        g2.dispose();
                        super.paintComponent(g);
                    }
                };
                badge.setForeground(Color.WHITE);
                badge.setFont(new Font("Segoe UI", Font.BOLD, 10));
                badge.setHorizontalAlignment(SwingConstants.CENTER);
                badge.setOpaque(false);
                int bw = Math.max(18, badge.getPreferredSize().width + 8);
                badge.setPreferredSize(new Dimension(bw, 18));
                subRow.add(badge, BorderLayout.EAST);
            }

            textArea.add(nameRow);
            textArea.add(Box.createVerticalStrut(3));
            textArea.add(subRow);

            row.add(avatarWrap, BorderLayout.WEST);
            row.add(textArea, BorderLayout.CENTER);

            // Bottom divider
            row.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, Config.DIVIDER),
                new EmptyBorder(10, 14, 10, 14)));

            return row;
        }

        private JLabel makeConvAvatar(String name, boolean isGroup) {
            String displayName = isGroup ? name.substring(1) : name;
            String initials = displayName.length() >= 2
                ? displayName.substring(0, 2).toUpperCase()
                : displayName.toUpperCase();

            Color[] palette = {
                new Color(0, 120, 212),
                new Color(52, 168, 83),
                new Color(234, 67, 53),
                new Color(251, 188, 4),
                new Color(155, 89, 182),
                new Color(26, 188, 156),
                new Color(230, 126, 34),
                new Color(22, 160, 133)
            };
            Color avatarColor = palette[Math.abs(name.hashCode()) % palette.length];

            JLabel av = new JLabel(isGroup ? "👥" : initials, SwingConstants.CENTER) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(isGroup ? new Color(0, 120, 212) : avatarColor);
                    g2.fillOval(0, 0, getWidth(), getHeight());
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            av.setForeground(Color.WHITE);
            av.setFont(isGroup
                ? new Font("Segoe UI Emoji", Font.PLAIN, 18)
                : new Font("Segoe UI", Font.BOLD, 14));
            av.setPreferredSize(new Dimension(42, 42));
            av.setOpaque(false);
            return av;
        }
    }
}
