package chat.ui.client.panels;

import chat.ui.client.components.EmojiAssets;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Consumer;

public class EmojiPicker extends JWindow {
    private static final Color BORDER = new Color(223, 232, 244);
    private static final Color SHADOW = new Color(19, 38, 68, 22);
    private static final Color TITLE = new Color(28, 45, 77);
    private static final Color SUBTITLE = new Color(106, 126, 154);
    private static final Color GRID_BG = new Color(246, 250, 255);
    private static final Color SECTION_TEXT = new Color(55, 78, 112);
    private static final Color SECTION_BADGE = new Color(230, 239, 250);
    private static final Color HOVER_BG = new Color(231, 241, 255);

    public EmojiPicker(Window owner, Consumer<String> onPick) {
        super(owner);
        setBackground(new Color(0, 0, 0, 0));

        JPanel root = new JPanel(new BorderLayout(0, 12)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SHADOW);
                g2.fillRoundRect(8, 10, getWidth() - 16, getHeight() - 12, 26, 26);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 14, getHeight() - 14, 26, 26);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 15, getHeight() - 15, 26, 26);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(18, 18, 18, 18));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("C\u1ea3m x\u00fac");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(TITLE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(title);

        JPanel sections = new JPanel();
        sections.setOpaque(false);
        sections.setLayout(new BoxLayout(sections, BoxLayout.Y_AXIS));
        sections.setBorder(new EmptyBorder(8, 0, 0, 0));

        for (EmojiAssets.EmojiGroup group : EmojiAssets.pickerGroups()) {
            sections.add(createSection(group, onPick));
            sections.add(Box.createVerticalStrut(10));
        }

        JScrollPane scrollPane = new JScrollPane(sections);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));

        root.add(header, BorderLayout.NORTH);
        root.add(scrollPane, BorderLayout.CENTER);
        setContentPane(root);
        setPreferredSize(new Dimension(520, 438));
        pack();

        addWindowFocusListener(new java.awt.event.WindowFocusListener() {
            @Override
            public void windowGainedFocus(java.awt.event.WindowEvent e) {}

            @Override
            public void windowLostFocus(java.awt.event.WindowEvent e) {
                setVisible(false);
            }
        });
    }

    public void showAt(Component anchor) {
        Point point = anchor.getLocationOnScreen();
        int x = Math.max(12, point.x - 12);
        int y = Math.max(12, point.y - getPreferredSize().height - 10);
        setLocation(x, y);
        setVisible(true);
        toFront();
    }

    private JComponent createSection(EmojiAssets.EmojiGroup group, Consumer<String> onPick) {
        JPanel section = new JPanel(new BorderLayout(0, 8));
        section.setOpaque(false);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(group.title());
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(SECTION_TEXT);
        label.setOpaque(true);
        label.setBackground(SECTION_BADGE);
        label.setBorder(new EmptyBorder(6, 10, 6, 10));

        JPanel labelWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        labelWrap.setOpaque(false);
        labelWrap.add(label);

        JPanel grid = new JPanel(new GridLayout(0, 7, 10, 10));
        grid.setOpaque(false);
        grid.setBorder(new EmptyBorder(14, 14, 14, 14));
        for (EmojiAssets.EmojiEntry entry : group.entries()) {
            grid.add(new EmojiTile(entry, onPick, this));
        }

        JPanel gridWrap = new JPanel(new BorderLayout());
        gridWrap.setOpaque(true);
        gridWrap.setBackground(GRID_BG);
        gridWrap.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 236, 248), 1, true),
            new EmptyBorder(2, 2, 2, 2)));
        gridWrap.add(grid, BorderLayout.NORTH);

        section.add(labelWrap, BorderLayout.NORTH);
        section.add(gridWrap, BorderLayout.CENTER);
        return section;
    }

    private static class EmojiTile extends JButton {
        private boolean hover;

        EmojiTile(EmojiAssets.EmojiEntry entry, Consumer<String> onPick, Window owner) {
            setIcon(EmojiAssets.createIcon(entry.style(), 44));
            setPreferredSize(new Dimension(58, 58));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(entry.tooltip() + "  " + entry.value());

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });

            addActionListener(e -> {
                onPick.accept(entry.value());
                owner.setVisible(false);
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (hover) {
                g2.setColor(HOVER_BG);
                g2.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 18, 18);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
