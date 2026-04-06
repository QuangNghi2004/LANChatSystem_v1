package chat.ui.client.dialogs;

import chat.util.Config;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Dialog đăng nhập — thiết kế sang trọng, tối giản.
 * Nền gradient xanh đậm, form trắng bo góc mềm, hiệu ứng glow trên focus.
 */
public class LoginDialog extends JDialog {

    // Màu sắc giao diện mới
    private static final Color BG_TOP       = new Color(10, 20, 48);
    private static final Color BG_BOTTOM    = new Color(0, 58, 112);
    private static final Color ACCENT       = new Color(64, 158, 255);
    private static final Color FIELD_BG     = new Color(245, 248, 255);
    private static final Color FIELD_BORDER = new Color(210, 220, 240);
    private static final Color FIELD_FOCUS  = new Color(64, 158, 255);
    private static final Color LABEL_COLOR  = new Color(90, 105, 140);
    private static final Color TEXT_MAIN    = new Color(20, 28, 48);
    private static final Color FOOTER_COLOR = new Color(160, 170, 195);

    private JTextField     loginUserField, loginHostField;
    private JPasswordField loginPassField;

    private String  resultUser, resultPass, resultHost;
    private boolean confirmed = false;

    public LoginDialog(Frame parent) {
        super(parent, "LAN Chat — Đăng nhập", true);
        setSize(440, 530);
        setLocationRelativeTo(parent);
        setResizable(false);
        setUndecorated(true);
        build();

        addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent e) {
                setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 24, 24));
            }
        });
    }

    private void build() {
        setLayout(new BorderLayout());

        // Header gradient
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, BG_TOP, getWidth(), getHeight(), BG_BOTTOM);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Vòng trang trí
                g2.setColor(new Color(255, 255, 255, 10));
                g2.fillOval(-70, -70, 230, 230);
                g2.setColor(new Color(64, 158, 255, 18));
                g2.fillOval(getWidth() - 90, -40, 200, 200);
                g2.dispose();
            }
        };
        header.setPreferredSize(new Dimension(0, 175));
        header.setOpaque(false);

        // Nút đóng
        JButton btnClose = new JButton("✕");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnClose.setForeground(new Color(255, 255, 255, 130));
        btnClose.setContentAreaFilled(false);
        btnClose.setBorderPainted(false);
        btnClose.setFocusPainted(false);
        btnClose.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClose.setBorder(new EmptyBorder(10, 14, 10, 14));
        btnClose.addActionListener(e -> { confirmed = false; dispose(); });
        btnClose.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btnClose.setForeground(Color.WHITE); }
            public void mouseExited(MouseEvent e)  { btnClose.setForeground(new Color(255,255,255,130)); }
        });

        JPanel closeRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
        closeRow.setOpaque(false);
        closeRow.add(btnClose);

        // Logo area
        JPanel logoArea = new JPanel();
        logoArea.setLayout(new BoxLayout(logoArea, BoxLayout.Y_AXIS));
        logoArea.setOpaque(false);
        logoArea.setBorder(new EmptyBorder(4, 0, 22, 0));

        // Icon vẽ tay — vòng tròn accent + emoji
        JComponent iconView = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Hào quang
                g2.setColor(new Color(64, 158, 255, 45));
                g2.fillOval(2, 2, 56, 56);
                // Nền tròn
                g2.setColor(new Color(64, 158, 255, 180));
                g2.fillOval(9, 9, 42, 42);
                // Ký tự
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
                FontMetrics fm = g2.getFontMetrics();
                String t = "LC";
                g2.drawString(t, (60 - fm.stringWidth(t)) / 2, (60 + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(60, 60); }
        };
        iconView.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel appName = new JLabel("LAN Chat");
        appName.setFont(new Font("Segoe UI", Font.BOLD, 24));
        appName.setForeground(Color.WHITE);
        appName.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel tagline = new JLabel("Kết nối trong mạng nội bộ");
        tagline.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tagline.setForeground(new Color(160, 195, 235));
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);

        logoArea.add(iconView);
        logoArea.add(Box.createVerticalStrut(10));
        logoArea.add(appName);
        logoArea.add(Box.createVerticalStrut(4));
        logoArea.add(tagline);

        header.add(closeRow, BorderLayout.NORTH);
        header.add(logoArea, BorderLayout.CENTER);

        // Kéo cửa sổ
        Point[] origin = {null};
        header.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { origin[0] = e.getPoint(); }
        });
        header.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (origin[0] == null) return;
                Point loc = getLocation();
                setLocation(loc.x + e.getX() - origin[0].x, loc.y + e.getY() - origin[0].y);
            }
        });

        add(header, BorderLayout.NORTH);

        // Form body
        JPanel body = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                // Đổ bóng nhẹ phía trên
                GradientPaint sh = new GradientPaint(0, 0, new Color(0,0,0,20), 0, 16, new Color(0,0,0,0));
                g2.setPaint(sh);
                g2.fillRect(0, 0, getWidth(), 16);
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(28, 36, 8, 36));

        body.add(fieldGroup("SERVER IP", loginHostField = tf(Config.DEFAULT_HOST)));
        body.add(Box.createVerticalStrut(16));
        body.add(fieldGroup("TÊN ĐĂNG NHẬP", loginUserField = tf("")));
        body.add(Box.createVerticalStrut(16));
        loginPassField = pf();
        body.add(fieldGroup("MẬT KHẨU", loginPassField));
        body.add(Box.createVerticalStrut(28));

        JButton btnLogin = loginBtn();
        body.add(btnLogin);

        ActionListener doLogin = e -> {
            String u  = loginUserField.getText().trim();
            String pw = new String(loginPassField.getPassword());
            String h  = loginHostField.getText().trim();
            if (u.isEmpty() || pw.isEmpty() || h.isEmpty()) { shake(); return; }
            resultUser = u; resultPass = pw; resultHost = h;
            confirmed = true; dispose();
        };
        btnLogin.addActionListener(doLogin);
        loginPassField.addActionListener(doLogin);

        add(body, BorderLayout.CENTER);

        // Footer
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        footerPanel.setBackground(Color.WHITE);
        JLabel footer = new JLabel("LAN Chat v3 — Chỉ dùng trong mạng nội bộ");
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footer.setForeground(FOOTER_COLOR);
        footerPanel.add(footer);
        add(footerPanel, BorderLayout.SOUTH);
    }

    private JPanel fieldGroup(String labelText, JComponent field) {
        JPanel group = new JPanel(new BorderLayout(0, 6));
        group.setOpaque(false);
        group.setMaximumSize(new Dimension(Short.MAX_VALUE, 68));

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setForeground(LABEL_COLOR);
        lbl.setBorder(new EmptyBorder(0, 2, 0, 0));

        group.add(lbl,   BorderLayout.NORTH);
        group.add(field, BorderLayout.CENTER);
        return group;
    }

    private void shake() {
        Point loc = getLocation();
        new Thread(() -> {
            try {
                for (int i = 0; i < 8; i++) {
                    int dx = (i % 2 == 0) ? 7 : -7;
                    SwingUtilities.invokeLater(() -> setLocation(loc.x + dx, loc.y));
                    Thread.sleep(35);
                }
                SwingUtilities.invokeLater(() -> setLocation(loc));
            } catch (InterruptedException ignored) {}
        }).start();
    }

    public void prefillLogin(String user, String host) {
        loginUserField.setText(user);
        loginHostField.setText(host);
        loginPassField.setText("");
        loginPassField.requestFocus();
    }

    public boolean isConfirmed() { return confirmed; }
    public String  getUsername() { return resultUser; }
    public String  getPassword() { return resultPass; }
    public String  getHost()     { return resultHost; }
    public boolean isRegister()  { return false; }

    // Helpers
    private JTextField tf(String def) {
        JTextField f = new JTextField(def);
        styleInput(f); return f;
    }

    private JPasswordField pf() {
        JPasswordField f = new JPasswordField();
        styleInput(f); return f;
    }

    private void styleInput(JComponent c) {
        c.setBackground(FIELD_BG);
        c.setForeground(TEXT_MAIN);
        c.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        c.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FIELD_BORDER, 1, true),
            new EmptyBorder(10, 14, 10, 14)));
        if (c instanceof JTextField f) f.setCaretColor(ACCENT);

        c.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                c.setBackground(Color.WHITE);
                c.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(FIELD_FOCUS, 1, true),
                    new EmptyBorder(10, 14, 10, 14)));
            }
            public void focusLost(FocusEvent e) {
                c.setBackground(FIELD_BG);
                c.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(FIELD_BORDER, 1, true),
                    new EmptyBorder(10, 14, 10, 14)));
            }
        });
    }

    private JButton loginBtn() {
        JButton b = new JButton("Đăng nhập") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c1 = getModel().isRollover() ? new Color(85, 175, 255) : ACCENT;
                Color c2 = getModel().isRollover() ? new Color(30, 110, 215) : new Color(8, 96, 205);
                GradientPaint gp = new GradientPaint(0, 0, c1, 0, getHeight(), c2);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                // Highlight phía trên
                g2.setColor(new Color(255, 255, 255, 35));
                g2.fillRoundRect(0, 0, getWidth(), getHeight() / 2, 12, 12);
                // Text
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                String t = getText();
                g2.drawString(t,
                    (getWidth() - fm.stringWidth(t)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {}
        };
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(Short.MAX_VALUE, 46));
        b.setPreferredSize(new Dimension(0, 46));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
