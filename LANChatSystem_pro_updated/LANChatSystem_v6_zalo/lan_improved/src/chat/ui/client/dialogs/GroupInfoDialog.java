package chat.ui.client.dialogs;

import chat.util.Config;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;

/**
 * Dialog xem thong tin cuoc tro chuyen.
 * - Chat don: thong tin doi phuong (ten, chuc vu, trang thai) + tong file/anh.
 * - Nhom chat: danh sach thanh vien + tong file/anh.
 *
 * Data duoc truyen tu ClientWindow sau khi nhan CONV_INFO_RESP tu server.
 */
public class GroupInfoDialog extends JDialog {

    private final String  target;
    private final boolean isGroup;
    private final int     fileCount;
    private final int     imageCount;
    private final String  extra; // group: "m1,m2,..." | dm: "fullname|role|status"

    public GroupInfoDialog(Frame parent, String target, boolean isGroup,
                           int fileCount, int imageCount, String extra) {
        super(parent, isGroup ? "Thong tin nhom: " + target : "Thong tin: " + target, true);
        this.target     = target;
        this.isGroup    = isGroup;
        this.fileCount  = fileCount;
        this.imageCount = imageCount;
        this.extra      = extra != null ? extra : "";
        setSize(380, isGroup ? 460 : 340);
        setLocationRelativeTo(parent);
        setResizable(false);
        build();
    }

    // Constructor cu (placeholder) - con de tuong thich
    public GroupInfoDialog(Frame parent) {
        this(parent, "?", false, 0, 0, "");
    }

    private void build() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // ── Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(isGroup ? Config.ZALO_BLUE : new Color(52, 168, 83));
        header.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Avatar vong tron
        JPanel avatarPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillOval(0, 0, getWidth()-1, getHeight()-1);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 24));
                FontMetrics fm = g2.getFontMetrics();
                String initials = getInitials();
                int x = (getWidth() - fm.stringWidth(initials)) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(initials, x, y);
                g2.dispose();
            }
        };
        avatarPanel.setPreferredSize(new Dimension(60, 60));
        avatarPanel.setOpaque(false);

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        titlePanel.setBorder(new EmptyBorder(0, 14, 0, 0));

        JLabel nameLabel = new JLabel(isGroup ? "#" + target : getDisplayName());
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        nameLabel.setForeground(Color.WHITE);

        JLabel typeLabel = new JLabel(isGroup ? "Nhom chat" : getRoleDisplay());
        typeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        typeLabel.setForeground(new Color(255, 255, 255, 200));

        titlePanel.add(nameLabel);
        titlePanel.add(Box.createVerticalStrut(3));
        titlePanel.add(typeLabel);
        if (!isGroup) {
            JLabel statusLabel = new JLabel(getStatusDot() + " " + getStatusText());
            statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            statusLabel.setForeground(new Color(255, 255, 255, 200));
            titlePanel.add(statusLabel);
        }

        JPanel headerContent = new JPanel(new BorderLayout());
        headerContent.setOpaque(false);
        headerContent.add(avatarPanel, BorderLayout.WEST);
        headerContent.add(titlePanel, BorderLayout.CENTER);
        header.add(headerContent);
        add(header, BorderLayout.NORTH);

        // ── Body
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(16, 20, 16, 20));

        // --- Stat cards: file + anh
        JPanel statsRow = new JPanel(new GridLayout(1, 2, 12, 0));
        statsRow.setOpaque(false);
        statsRow.setMaximumSize(new Dimension(Short.MAX_VALUE, 70));
        statsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        statsRow.add(statCard("Tong tai lieu", String.valueOf(fileCount), new Color(66, 133, 244)));
        statsRow.add(statCard("Tong hinh anh", String.valueOf(imageCount), new Color(251, 140, 0)));
        body.add(statsRow);
        body.add(Box.createVerticalStrut(16));

        // --- Phan cach
        JSeparator sep = new JSeparator();
        sep.setForeground(Config.DIVIDER);
        sep.setMaximumSize(new Dimension(Short.MAX_VALUE, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(sep);
        body.add(Box.createVerticalStrut(14));

        if (isGroup) {
            // Danh sach thanh vien nhom
            JLabel memberTitle = sectionTitle("Thanh vien nhom");
            body.add(memberTitle);
            body.add(Box.createVerticalStrut(8));

            String[] members = extra.isEmpty() ? new String[0] : extra.split(",");
            JPanel memberList = buildMemberList(members);
            body.add(memberList);
        } else {
            // Thong tin chi tiet doi phuong
            JLabel infoTitle = sectionTitle("Thong tin tai khoan");
            body.add(infoTitle);
            body.add(Box.createVerticalStrut(8));

            String[] parts = extra.split("\\|", -1);
            String fullname = parts.length > 0 ? parts[0] : target;
            String role     = parts.length > 1 ? parts[1] : "nhan_vien";
            String status   = parts.length > 2 ? parts[2] : "offline";

            body.add(infoRow("Ten dang nhap", target));
            body.add(Box.createVerticalStrut(6));
            body.add(infoRow("Ho va ten", fullname.isEmpty() ? target : fullname));
            body.add(Box.createVerticalStrut(6));
            body.add(infoRow("Chuc vu", roleToDisplay(role)));
            body.add(Box.createVerticalStrut(6));
            body.add(infoRow("Trang thai", "online".equals(status) ? "Dang hoat dong" : "Ngoai tuyen"));
        }

        add(body, BorderLayout.CENTER);

        // ── Footer button
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(new Color(248, 249, 250));
        footer.setBorder(new MatteBorder(1, 0, 0, 0, Config.DIVIDER));
        JButton btnClose = new JButton("Dong");
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClose.setForeground(Config.ZALO_BLUE);
        btnClose.setBackground(Color.WHITE);
        btnClose.setFocusPainted(false);
        btnClose.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Config.ZALO_BLUE, 1, true),
            new EmptyBorder(7, 20, 7, 20)));
        btnClose.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> dispose());
        footer.add(btnClose);
        add(footer, BorderLayout.SOUTH);
    }

    private JPanel statCard(String label, String value, Color accent) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 12));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel valLabel = new JLabel(value);
        valLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valLabel.setForeground(accent);
        valLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblLabel = new JLabel(label);
        lblLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblLabel.setForeground(Config.TEXT_MUTED);
        lblLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(valLabel);
        card.add(lblLabel);
        return card;
    }

    private JPanel buildMemberList(String[] members) {
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);
        listPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        int maxH = Math.min(members.length, 8) * 36 + 4;
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(BorderFactory.createLineBorder(Config.DIVIDER, 1, true));
        scroll.setMaximumSize(new Dimension(Short.MAX_VALUE, maxH + 16));
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(4, 0));
        scroll.setBackground(Color.WHITE);
        scroll.getViewport().setBackground(Color.WHITE);

        if (members.length == 0) {
            JLabel empty = new JLabel("  Chua co thanh vien nao.");
            empty.setFont(Config.FONT_SMALL);
            empty.setForeground(Config.TEXT_MUTED);
            listPanel.add(empty);
        } else {
            for (String m : members) {
                if (m.isBlank()) continue;
                JPanel row = new JPanel(new BorderLayout(8, 0));
                row.setBackground(Color.WHITE);
                row.setBorder(new EmptyBorder(6, 10, 6, 10));
                row.setMaximumSize(new Dimension(Short.MAX_VALUE, 36));

                // Avatar mini
                JLabel av = new JLabel(String.valueOf(m.charAt(0)).toUpperCase()) {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(Config.ZALO_BLUE);
                        g2.fillOval(0, 0, getWidth()-1, getHeight()-1);
                        g2.setColor(Color.WHITE);
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                        FontMetrics fm = g2.getFontMetrics();
                        String t = getText();
                        g2.drawString(t, (getWidth()-fm.stringWidth(t))/2,
                            (getHeight()+fm.getAscent()-fm.getDescent())/2);
                        g2.dispose();
                    }
                };
                av.setPreferredSize(new Dimension(26, 26));
                av.setOpaque(false);

                JLabel nameLabel = new JLabel(m.trim());
                nameLabel.setFont(Config.FONT_NORMAL);
                nameLabel.setForeground(Config.TEXT_PRIMARY);

                row.add(av, BorderLayout.WEST);
                row.add(nameLabel, BorderLayout.CENTER);
                listPanel.add(row);

                JSeparator sep2 = new JSeparator();
                sep2.setForeground(new Color(Config.DIVIDER.getRed(), Config.DIVIDER.getGreen(), Config.DIVIDER.getBlue(), 80));
                sep2.setMaximumSize(new Dimension(Short.MAX_VALUE, 1));
                listPanel.add(sep2);
            }
        }

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(Short.MAX_VALUE, maxH + 20));

        JLabel countLabel = new JLabel(members.length + " thanh vien");
        countLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        countLabel.setForeground(Config.TEXT_MUTED);
        countLabel.setBorder(new EmptyBorder(0, 0, 4, 0));

        wrapper.add(countLabel, BorderLayout.NORTH);
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel infoRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, 28));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label + ":");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(Config.TEXT_MUTED);
        lbl.setPreferredSize(new Dimension(110, 20));

        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        val.setForeground(Config.TEXT_PRIMARY);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        return row;
    }

    private JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(Config.TEXT_SECONDARY);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    // ── Parse helpers
    private String getDisplayName() {
        String[] parts = extra.split("\\|", -1);
        String fullname = parts.length > 0 ? parts[0] : "";
        return fullname.isEmpty() ? target : fullname;
    }

    private String getRoleDisplay() {
        String[] parts = extra.split("\\|", -1);
        String role = parts.length > 1 ? parts[1] : "nhan_vien";
        return roleToDisplay(role);
    }

    private String getStatusText() {
        String[] parts = extra.split("\\|", -1);
        String status = parts.length > 2 ? parts[2] : "offline";
        return "online".equals(status) ? "Dang hoat dong" : "Ngoai tuyen";
    }

    private String getStatusDot() {
        String[] parts = extra.split("\\|", -1);
        String status = parts.length > 2 ? parts[2] : "offline";
        return "online".equals(status) ? "O" : "o";
    }

    private String getInitials() {
        String name = isGroup ? target : getDisplayName();
        if (name == null || name.isEmpty()) return "?";
        String[] words = name.trim().split("\\s+");
        if (words.length >= 2) return String.valueOf(words[0].charAt(0)) + words[1].charAt(0);
        return String.valueOf(name.charAt(0)).toUpperCase();
    }

    private String roleToDisplay(String role) {
        if (role == null) return "Nhan vien";
        return switch (role) {
            case "admin"        -> "Quan tri vien";
            case "truong_phong" -> "Truong phong";
            case "pho_phong"    -> "Pho phong";
            case "nhan_vien"    -> "Nhan vien";
            default             -> "Nhan vien";
        };
    }
}
