package chat.ui.client.dialogs;

import chat.protocol.Protocol;
import chat.util.Config;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;

/**
 * Dialog đổi mật khẩu — thiết kế đồng bộ với LoginDialog.
 * Gradient xanh đậm header, form trắng bo góc, hiệu ứng glow trên focus.
 */
public class ChangePasswordDialog extends JDialog {

    // Màu sắc đồng bộ với LoginDialog
    private static final Color BG_TOP        = new Color(10, 20, 48);
    private static final Color BG_BOTTOM     = new Color(0, 58, 112);
    private static final Color ACCENT        = new Color(64, 158, 255);
    private static final Color ACCENT_HOVER  = new Color(85, 175, 255);
    private static final Color ACCENT_DARK   = new Color(8, 96, 205);
    private static final Color FIELD_BG      = new Color(245, 248, 255);
    private static final Color FIELD_BORDER  = new Color(210, 220, 240);
    private static final Color FIELD_FOCUS   = new Color(64, 158, 255);
    private static final Color LABEL_COLOR   = new Color(90, 105, 140);
    private static final Color TEXT_MAIN     = new Color(20, 28, 48);
    private static final Color FOOTER_COLOR  = new Color(160, 170, 195);
    private static final Color ERROR_COLOR   = new Color(235, 64, 64);
    private static final Color SUCCESS_COLOR = new Color(39, 186, 89);

    private final Consumer<String> onSend;
    private final boolean isAdminMode;
    private final String  targetUser;

    private JPasswordField oldPassField, newPassField, confirmField;
    private JLabel statusLabel;

    public ChangePasswordDialog(Frame parent, Consumer<String> onSend) {
        this(parent, onSend, false, null);
    }

    public ChangePasswordDialog(Frame parent, Consumer<String> onSend,
                                 boolean isAdmin, String targetUser) {
        super(parent,
            isAdmin ? "Admin: Đổi mật khẩu người dùng" : "Đổi mật khẩu",
            true);
        this.onSend      = onSend;
        this.isAdminMode = isAdmin;
        this.targetUser  = targetUser;

        int height = isAdmin ? 440 : 510;
        setSize(420, height);
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

        // ── Header gradient (đồng bộ LoginDialog)
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
        header.setPreferredSize(new Dimension(0, 150));
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
        btnClose.addActionListener(e -> dispose());
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
        logoArea.setBorder(new EmptyBorder(0, 0, 18, 0));

        // Icon khóa
        JComponent iconView = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(64, 158, 255, 45));
                g2.fillOval(2, 2, 56, 56);
                g2.setColor(new Color(64, 158, 255, 180));
                g2.fillOval(9, 9, 42, 42);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
                FontMetrics fm = g2.getFontMetrics();
                String t = isAdminMode ? "🛡️" : "🔑";
                g2.drawString(t, (60 - fm.stringWidth(t)) / 2 + 1,
                    (60 + fm.getAscent() - fm.getDescent()) / 2 - 1);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(60, 60); }
        };
        iconView.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel(isAdminMode ? "Đổi mật khẩu người dùng" : "Đổi mật khẩu");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String subText = isAdminMode
            ? "Đổi cho tài khoản: " + targetUser
            : "Nhập mật khẩu cũ và mật khẩu mới";
        JLabel subLabel = new JLabel(subText);
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subLabel.setForeground(new Color(160, 195, 235));
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        logoArea.add(iconView);
        logoArea.add(Box.createVerticalStrut(8));
        logoArea.add(titleLabel);
        logoArea.add(Box.createVerticalStrut(3));
        logoArea.add(subLabel);

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

        header.add(closeRow, BorderLayout.NORTH);
        header.add(logoArea, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        // ── Body form
        JPanel body = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
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
        body.setBorder(new EmptyBorder(24, 36, 12, 36));

        if (!isAdminMode) {
            body.add(fieldGroup("MẬT KHẨU CŨ", oldPassField = pf()));
            body.add(Box.createVerticalStrut(14));
        }

        body.add(fieldGroup("MẬT KHẨU MỚI", newPassField = pf()));
        body.add(Box.createVerticalStrut(14));
        body.add(fieldGroup("XÁC NHẬN MẬT KHẨU MỚI", confirmField = pf()));
        body.add(Box.createVerticalStrut(6));

        // Status label (hiện lỗi inline)
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(ERROR_COLOR);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setBorder(new EmptyBorder(4, 2, 4, 2));
        body.add(statusLabel);

        body.add(Box.createVerticalStrut(16));
        JButton btnOk = actionBtn(isAdminMode ? "Đổi mật khẩu" : "Xác nhận đổi mật khẩu");
        body.add(btnOk);
        body.add(Box.createVerticalStrut(8));
        JButton btnCancel = cancelBtn("Hủy bỏ");
        body.add(btnCancel);

        ActionListener doChange = e -> doChange();
        btnOk.addActionListener(doChange);
        confirmField.addActionListener(doChange);
        btnCancel.addActionListener(e -> dispose());

        add(body, BorderLayout.CENTER);

        // Footer
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        footerPanel.setBackground(Color.WHITE);
        JLabel footer = new JLabel("LAN Chat — Bảo mật tài khoản");
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footer.setForeground(FOOTER_COLOR);
        footerPanel.add(footer);
        add(footerPanel, BorderLayout.SOUTH);
    }

    private void doChange() {
        String newPass     = new String(newPassField.getPassword());
        String confirmPass = new String(confirmField.getPassword());

        if (newPass.isEmpty()) {
            showInlineError("Mật khẩu mới không được để trống!"); return;
        }
        if (newPass.length() < 4) {
            showInlineError("Mật khẩu mới phải có ít nhất 4 ký tự!"); return;
        }
        if (!newPass.equals(confirmPass)) {
            showInlineError("Xác nhận mật khẩu không khớp!");
            confirmField.setText("");
            confirmField.requestFocus();
            return;
        }

        if (isAdminMode) {
            onSend.accept(Protocol.build(Protocol.ADMIN_CHANGE_PASS, targetUser, newPass));
        } else {
            String oldPass = new String(oldPassField.getPassword());
            if (oldPass.isEmpty()) {
                showInlineError("Vui lòng nhập mật khẩu cũ!");
                oldPassField.requestFocus();
                return;
            }
            onSend.accept(Protocol.build(Protocol.CHANGE_PASS, oldPass, newPass));
        }
        dispose();
    }

    private void showInlineError(String msg) {
        statusLabel.setForeground(ERROR_COLOR);
        statusLabel.setText("⚠ " + msg);
        // Hiệu ứng shake nhẹ
        shake();
    }

    private void shake() {
        Point loc = getLocation();
        new Thread(() -> {
            try {
                for (int i = 0; i < 6; i++) {
                    int dx = (i % 2 == 0) ? 5 : -5;
                    SwingUtilities.invokeLater(() -> setLocation(loc.x + dx, loc.y));
                    Thread.sleep(30);
                }
                SwingUtilities.invokeLater(() -> setLocation(loc));
            } catch (InterruptedException ignored) {}
        }).start();
    }

    private JPanel fieldGroup(String labelText, JComponent field) {
        JPanel group = new JPanel(new BorderLayout(0, 5));
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

    private JPasswordField pf() {
        JPasswordField f = new JPasswordField();
        f.setBackground(FIELD_BG);
        f.setForeground(TEXT_MAIN);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setCaretColor(ACCENT);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FIELD_BORDER, 1, true),
            new EmptyBorder(10, 14, 10, 14)));
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                f.setBackground(Color.WHITE);
                f.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(FIELD_FOCUS, 1, true),
                    new EmptyBorder(10, 14, 10, 14)));
                statusLabel.setText(" ");
            }
            public void focusLost(FocusEvent e) {
                f.setBackground(FIELD_BG);
                f.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(FIELD_BORDER, 1, true),
                    new EmptyBorder(10, 14, 10, 14)));
            }
        });
        return f;
    }

    private JButton actionBtn(String text) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c1 = getModel().isRollover() ? ACCENT_HOVER : ACCENT;
                Color c2 = getModel().isRollover() ? new Color(30, 110, 215) : ACCENT_DARK;
                GradientPaint gp = new GradientPaint(0, 0, c1, 0, getHeight(), c2);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(255, 255, 255, 35));
                g2.fillRoundRect(0, 0, getWidth(), getHeight() / 2, 12, 12);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                String t = getText();
                g2.drawString(t, (getWidth() - fm.stringWidth(t)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {}
        };
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(Short.MAX_VALUE, 44));
        b.setPreferredSize(new Dimension(0, 44));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton cancelBtn(String text) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover() ? new Color(240, 244, 252) : new Color(248, 250, 255);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(FIELD_BORDER);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2.setColor(LABEL_COLOR);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                FontMetrics fm = g2.getFontMetrics();
                String t = getText();
                g2.drawString(t, (getWidth() - fm.stringWidth(t)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {}
        };
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(Short.MAX_VALUE, 38));
        b.setPreferredSize(new Dimension(0, 38));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
