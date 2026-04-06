package chat.ui.client.panels;

import chat.util.Config;
import chat.util.TransferMessageUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Panel chat kiểu Zalo.
 * Hỗ trợ:
 * - Gom cụm tin nhắn liên tiếp cùng người gửi
 * - Emoji-only bubble lớn hơn
 * - Preview ảnh inline nếu attachment là ảnh và đã có trong cache cục bộ
 */
public class ChatPanel extends JPanel {

    private final JPanel msgBox;
    private final JScrollPane scroll;
    private final String myName;
    private final Consumer<String> onAttachmentOpen;
    private final Consumer<String> onAttachmentSave;
    private final Function<TransferMessageUtil.FileMessage, File> attachmentResolver;

    private String quotedText;
    private BiConsumer<String, String> onQuoteCb;

    private String lastSender;
    private boolean lastWasSelf;
    private boolean lastWasMessage;
    private ZaloBubble lastBubble;

    // Dem so file va anh da gui/nhan trong cuoc tro chuyen nay (realtime)
    private int localFileCount  = 0;
    private int localImageCount = 0;

    public int getLocalFileCount()  { return localFileCount; }
    public int getLocalImageCount() { return localImageCount; }

    public ChatPanel(String myName,
                     Consumer<String> onAttachmentOpen,
                     Consumer<String> onAttachmentSave,
                     Function<TransferMessageUtil.FileMessage, File> attachmentResolver) {
        this.myName = myName;
        this.onAttachmentOpen = onAttachmentOpen;
        this.onAttachmentSave = onAttachmentSave;
        this.attachmentResolver = attachmentResolver;

        setLayout(new BorderLayout());
        setBackground(Config.CHAT_BG);

        msgBox = new JPanel();
        msgBox.setLayout(new BoxLayout(msgBox, BoxLayout.Y_AXIS));
        msgBox.setBackground(Config.CHAT_BG);
        msgBox.setBorder(new EmptyBorder(18, 16, 18, 16));

        scroll = new JScrollPane(msgBox,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Config.CHAT_BG);
        scroll.setBackground(Config.CHAT_BG);

        JScrollBar vsb = scroll.getVerticalScrollBar();
        vsb.setPreferredSize(new Dimension(5, 0));
        vsb.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            protected void configureScrollBarColors() {
                thumbColor = new Color(180, 190, 210);
                trackColor = Config.CHAT_BG;
            }
            protected JButton createDecreaseButton(int o) { return zeroBtn(); }
            protected JButton createIncreaseButton(int o) { return zeroBtn(); }
            private JButton zeroBtn() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });

        add(scroll, BorderLayout.CENTER);
    }

    public void addMessage(String sender, String text, String ts, String status, String quoted) {
        boolean isSelf = sender.equals(myName);
        boolean grouped = lastWasMessage && sender.equals(lastSender) && isSelf == lastWasSelf;

        // Cap nhat counter file/anh realtime
        if (text != null) {
            TransferMessageUtil.FileMessage fm = TransferMessageUtil.parseFileMessage(text);
            if (fm != null) {
                if (isImageAttachment(fm)) localImageCount++;
                else                       localFileCount++;
            }
        }

        if (msgBox.getComponentCount() == 0) {
            addDateSeparator("Hôm nay");
        }

        JPanel row = buildBubble(sender, text, ts, status, quoted, isSelf, grouped);
        ZaloBubble currentBubble = (ZaloBubble) row.getClientProperty("bubble");
        if (grouped && lastBubble != null && currentBubble != null) {
            lastBubble.setGroupedWithNext(true);
            currentBubble.setGroupedWithPrev(true);
        }
        msgBox.add(row);
        msgBox.add(Box.createVerticalStrut(grouped ? 2 : 6));

        lastSender = sender;
        lastWasSelf = isSelf;
        lastWasMessage = true;
        lastBubble = currentBubble;

        msgBox.revalidate();
        scrollToBottom();
    }

    public void addSystemMsg(String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
        row.setOpaque(false);
        row.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel label = new JLabel(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 30));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        label.setForeground(Config.TEXT_SECONDARY);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setOpaque(false);
        label.setBorder(new EmptyBorder(4, 14, 4, 14));

        row.add(label);
        msgBox.add(row);
        msgBox.add(Box.createVerticalStrut(4));

        lastWasMessage = false;
        lastBubble = null;
        msgBox.revalidate();
        scrollToBottom();
    }

    public void updateStatus(String msgId, String status) {
        Component[] comps = msgBox.getComponents();
        for (int i = comps.length - 1; i >= 0; i--) {
            if (comps[i] instanceof JPanel row) {
                JLabel tick = (JLabel) row.getClientProperty("tick");
                if (tick != null) {
                    tick.setText(statusIcon(status));
                    tick.setForeground("read".equals(status)
                        ? Config.ZALO_BLUE : new Color(104, 129, 158));
                    break;
                }
            }
        }
    }

    public void setOnQuote(BiConsumer<String, String> cb) { this.onQuoteCb = cb; }
    public void setQuotedText(String t) { this.quotedText = t; }
    public String getQuotedText() { return quotedText; }
    public void clearQuote() { quotedText = null; }

    public void setQuoteLabel(JLabel ql, Runnable cb) {}

    private void addDateSeparator(String dateText) {
        JPanel sep = new JPanel(new BorderLayout(8, 0));
        sep.setOpaque(false);
        sep.setBorder(new EmptyBorder(8, 20, 8, 20));
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);

        JSeparator line1 = new JSeparator();
        line1.setForeground(Config.DIVIDER);
        JSeparator line2 = new JSeparator();
        line2.setForeground(Config.DIVIDER);

        JLabel lbl = new JLabel(dateText);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(Config.TEXT_MUTED);
        lbl.setBorder(new EmptyBorder(0, 10, 0, 10));

        sep.add(line1, BorderLayout.WEST);
        sep.add(lbl, BorderLayout.CENTER);
        sep.add(line2, BorderLayout.EAST);
        msgBox.add(sep);
        lastWasMessage = false;
        lastBubble = null;
    }

    private JPanel buildBubble(String sender, String text, String ts,
                               String status, String quoted, boolean isSelf, boolean grouped) {
        boolean emojiOnly = isEmojiOnly(text);

        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (!isSelf) {
            if (grouped) {
                row.add(Box.createHorizontalStrut(48));
            } else {
                JLabel avatar = makeAvatar(sender);
                JPanel avWrap = new JPanel(new BorderLayout());
                avWrap.setOpaque(false);
                avWrap.setPreferredSize(new Dimension(38, 38));
                avWrap.setMaximumSize(new Dimension(38, 400));
                avWrap.add(avatar, BorderLayout.SOUTH);
                row.add(avWrap);
            }
            row.add(Box.createHorizontalStrut(10));
        } else {
            row.add(Box.createHorizontalGlue());
        }

        JPanel bubbleCol = new JPanel();
        bubbleCol.setLayout(new BoxLayout(bubbleCol, BoxLayout.Y_AXIS));
        bubbleCol.setOpaque(false);
        int maxW = Math.min(440, (int) (Config.CLIENT_W * 0.56));

        if (!isSelf && !grouped) {
            JLabel nameLabel = new JLabel(sender);
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
            nameLabel.setForeground(accentForName(sender));
            nameLabel.setBorder(new EmptyBorder(0, 4, 4, 0));
            nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            bubbleCol.add(nameLabel);
        }

        ZaloBubble bubble = new ZaloBubble(isSelf, emojiOnly);
        bubble.setLayout(new BorderLayout());
        bubble.setBorder(emojiOnly
            ? new EmptyBorder(8, 10, 6, 10)
            : new EmptyBorder(10, 14, 8, 14));
        bubble.setAlignmentX(isSelf ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);

        JPanel contentBox = new JPanel();
        contentBox.setOpaque(false);
        contentBox.setLayout(new BoxLayout(contentBox, BoxLayout.Y_AXIS));

        if (quoted != null && !quoted.isBlank()) {
            JPanel qBlock = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(isSelf ? new Color(255, 255, 255, 65) : new Color(0, 120, 212, 20));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            qBlock.setOpaque(false);
            qBlock.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 3, 0, 0, isSelf ? new Color(120, 162, 205) : Config.ZALO_BLUE),
                new EmptyBorder(5, 9, 5, 9)));

            JLabel qText = new JLabel("<html><i>" + escHtml(quoted) + "</i></html>");
            qText.setForeground(isSelf ? new Color(72, 96, 128) : Config.ZALO_BLUE);
            qText.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            qBlock.add(qText);

            contentBox.add(qBlock);
            contentBox.add(Box.createVerticalStrut(7));
        }

        TransferMessageUtil.FileMessage fileMessage = TransferMessageUtil.parseFileMessage(text);
        if (fileMessage != null) {
            contentBox.add(isImageAttachment(fileMessage)
                ? buildImageAttachment(fileMessage, isSelf)
                : buildFileAttachment(fileMessage, isSelf));
        } else {
            JTextArea msgText = new JTextArea(text);
            msgText.setEditable(false);
            msgText.setOpaque(false);
            msgText.setForeground(isSelf ? new Color(34, 56, 84) : Config.TEXT_PRIMARY);
            msgText.setFont(messageFont(text));
            msgText.setLineWrap(!emojiOnly);
            msgText.setWrapStyleWord(true);
            msgText.setBorder(null);
            msgText.setMargin(new Insets(0, 0, 0, 0));
            msgText.setAlignmentX(emojiOnly ? Component.CENTER_ALIGNMENT : Component.LEFT_ALIGNMENT);

            Dimension textSize = measureWrappedText(msgText, text, maxW - 30);
            msgText.setPreferredSize(textSize);
            msgText.setMinimumSize(textSize);
            msgText.setMaximumSize(textSize);

            JPopupMenu popup = buildPopup(sender, text);
            msgText.setComponentPopupMenu(popup);
            bubble.setComponentPopupMenu(popup);
            contentBox.add(msgText);
        }

        JPanel footer = new JPanel(new FlowLayout(emojiOnly ? FlowLayout.CENTER : FlowLayout.LEFT, 4, 0));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(emojiOnly ? 2 : 3, 0, 0, 0));
        footer.setAlignmentX(emojiOnly ? Component.CENTER_ALIGNMENT : Component.LEFT_ALIGNMENT);

        JLabel tsLabel = new JLabel(ts);
        tsLabel.setForeground(emojiOnly
            ? new Color(113, 129, 150)
            : (isSelf ? new Color(91, 114, 142) : new Color(148, 156, 171)));
        tsLabel.setFont(new Font("Segoe UI", Font.PLAIN, emojiOnly ? 10 : 11));
        footer.add(tsLabel);

        if (isSelf) {
            JLabel tick = new JLabel(status != null ? statusIcon(status) : "✓");
            tick.setFont(new Font("Segoe UI", Font.PLAIN, emojiOnly ? 10 : 11));
            tick.setForeground(emojiOnly ? new Color(121, 142, 171) : new Color(104, 129, 158));
            footer.add(tick);
            row.putClientProperty("tick", tick);
        }

        contentBox.add(footer);
        bubble.add(contentBox, BorderLayout.CENTER);

        Dimension bubblePref = bubble.getPreferredSize();
        bubble.setPreferredSize(bubblePref);
        bubble.setMaximumSize(bubblePref);
        bubble.setMinimumSize(bubblePref);

        bubbleCol.add(bubble);
        Dimension bubbleColPref = bubbleCol.getPreferredSize();
        bubbleCol.setPreferredSize(bubbleColPref);
        bubbleCol.setMaximumSize(bubbleColPref);
        row.add(bubbleCol);

        if (!isSelf) {
            row.add(Box.createHorizontalGlue());
        } else {
            row.add(Box.createHorizontalStrut(10));
        }

        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);
        outer.setBorder(new EmptyBorder(grouped ? 1 : 4, 0, 4, 0));
        outer.add(row, BorderLayout.CENTER);
        outer.putClientProperty("bubble", bubble);
        return outer;
    }

    private JComponent buildImageAttachment(TransferMessageUtil.FileMessage fileMessage, boolean isSelf) {
        File cached = attachmentResolver != null ? attachmentResolver.apply(fileMessage) : null;
        if (cached == null || !cached.exists()) return buildFileAttachment(fileMessage, isSelf);

        try {
            BufferedImage image = ImageIO.read(cached);
            if (image == null) return buildFileAttachment(fileMessage, isSelf);

            Dimension size = fitInside(image.getWidth(), image.getHeight(), 300, 360);
            JComponent preview = roundedImagePreview(image, size, isSelf, fileMessage);

            JPanel wrap = new JPanel();
            wrap.setOpaque(false);
            wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
            wrap.add(preview);

            JLabel meta = new JLabel(fileMessage.fileName());
            meta.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            meta.setForeground(isSelf ? new Color(91, 114, 142) : Config.TEXT_MUTED);
            meta.setBorder(new EmptyBorder(5, 2, 0, 2));
            meta.setAlignmentX(isSelf ? 1f : 0f);
            wrap.add(meta);
            return wrap;
        } catch (Exception ignored) {
            return buildFileAttachment(fileMessage, isSelf);
        }
    }

    private JComponent buildFileAttachment(TransferMessageUtil.FileMessage fileMessage, boolean isSelf) {
        JPanel wrap = new JPanel(new BorderLayout(0, 6));
        wrap.setOpaque(false);

        JPanel fileCard = new JPanel();
        fileCard.setLayout(new BorderLayout(10, 0));
        fileCard.setOpaque(false);
        fileCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(
                isSelf ? new Color(187, 208, 231) : new Color(214, 222, 235), 1, true),
            new EmptyBorder(10, 12, 10, 12)));

        JLabel icon = new JLabel("📎");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        icon.setForeground(isSelf ? new Color(56, 84, 118) : Config.ZALO_BLUE);

        JPanel textCol = new JPanel();
        textCol.setOpaque(false);
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(fileMessage.fileName());
        name.setFont(new Font("Segoe UI", Font.BOLD, 12));
        name.setForeground(isSelf ? new Color(34, 56, 84) : Config.TEXT_PRIMARY);

        JLabel meta = new JLabel("Nhấn để mở • " + TransferMessageUtil.formatSize(fileMessage.size()));
        meta.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        meta.setForeground(isSelf ? new Color(91, 114, 142) : Config.TEXT_MUTED);

        textCol.add(name);
        textCol.add(Box.createVerticalStrut(2));
        textCol.add(meta);

        JPanel centerCol = new JPanel();
        centerCol.setOpaque(false);
        centerCol.setLayout(new BoxLayout(centerCol, BoxLayout.Y_AXIS));
        textCol.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerCol.add(textCol);
        centerCol.add(Box.createVerticalStrut(8));

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionRow.setOpaque(false);
        actionRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton openBtn = attachmentActionButton("M\u1edf", isSelf);
        openBtn.addActionListener(e -> openFileMessage(fileMessage));
        JButton saveBtn = attachmentActionButton("L\u01b0u", isSelf);
        saveBtn.addActionListener(e -> saveFileMessage(fileMessage));

        actionRow.add(openBtn);
        actionRow.add(saveBtn);
        centerCol.add(actionRow);

        fileCard.add(icon, BorderLayout.WEST);
        fileCard.add(centerCol, BorderLayout.CENTER);
        wrap.add(fileCard, BorderLayout.CENTER);
        return wrap;
    }

    private void openFileMessage(TransferMessageUtil.FileMessage fileMessage) {
        if (onAttachmentOpen != null) {
            onAttachmentOpen.accept(TransferMessageUtil.buildFileMessage(
                fileMessage.transferId(), fileMessage.fileName(), fileMessage.size()));
        }
    }

    private void saveFileMessage(TransferMessageUtil.FileMessage fileMessage) {
        if (onAttachmentSave != null) {
            onAttachmentSave.accept(TransferMessageUtil.buildFileMessage(
                fileMessage.transferId(), fileMessage.fileName(), fileMessage.size()));
        }
    }

    private boolean isImageAttachment(TransferMessageUtil.FileMessage fileMessage) {
        String name = fileMessage.fileName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
            || name.endsWith(".gif") || name.endsWith(".bmp") || name.endsWith(".webp");
    }

    private Dimension fitInside(int srcW, int srcH, int maxW, int maxH) {
        double scale = Math.min((double) maxW / srcW, (double) maxH / srcH);
        scale = Math.min(scale, 1.0);
        return new Dimension((int) Math.round(srcW * scale), (int) Math.round(srcH * scale));
    }

    private JComponent roundedImagePreview(BufferedImage image, Dimension size, boolean isSelf,
                                           TransferMessageUtil.FileMessage fileMessage) {
        JComponent preview = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                Shape clip = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 26, 26);
                g2.setClip(clip);
                g2.drawImage(image, 0, 0, getWidth(), getHeight(), null);
                g2.setClip(null);
                g2.setColor(isSelf ? new Color(187, 208, 231) : new Color(214, 222, 235));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 2, getHeight() - 2, 26, 26));
                g2.dispose();
            }
        };
        preview.setPreferredSize(size);
        preview.setMinimumSize(size);
        preview.setMaximumSize(size);
        preview.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        preview.setAlignmentX(isSelf ? 1f : 0f);
        preview.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) openFileMessage(fileMessage);
            }
        });
        preview.setComponentPopupMenu(buildImagePopup(fileMessage));
        return preview;
    }

    private JPopupMenu buildImagePopup(TransferMessageUtil.FileMessage fileMessage) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBackground(Color.WHITE);
        popup.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Config.DIVIDER, 1),
            new EmptyBorder(4, 0, 4, 0)));

        JMenuItem openItem = menuItem("🖼️  Mở ảnh");
        openItem.addActionListener(e -> openFileMessage(fileMessage));
        popup.add(openItem);

        JMenuItem saveItem = menuItem("💾  Lưu ảnh");
        saveItem.addActionListener(e -> {
            if (onAttachmentSave != null) {
                onAttachmentSave.accept(TransferMessageUtil.buildFileMessage(
                    fileMessage.transferId(), fileMessage.fileName(), fileMessage.size()));
            }
        });
        popup.add(saveItem);
        return popup;
    }

    private JButton attachmentActionButton(String text, boolean isSelf) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setForeground(isSelf ? new Color(34, 56, 84) : Config.ZALO_BLUE);
        button.setBackground(isSelf ? new Color(236, 244, 253) : new Color(240, 247, 255));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(isSelf ? new Color(198, 216, 238) : new Color(202, 224, 245), 1, true),
            new EmptyBorder(4, 10, 4, 10)));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JLabel makeAvatar(String name) {
        String initials = name.length() >= 2
            ? name.substring(0, 2).toUpperCase()
            : name.toUpperCase();
        Color[] palette = {
            new Color(0, 120, 212), new Color(52, 168, 83), new Color(234, 67, 53),
            new Color(155, 89, 182), new Color(26, 188, 156), new Color(230, 126, 34)
        };
        Color c = palette[Math.abs(name.hashCode()) % palette.length];

        JLabel av = new JLabel(initials, SwingConstants.CENTER) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        av.setForeground(Color.WHITE);
        av.setFont(new Font("Segoe UI", Font.BOLD, 12));
        av.setPreferredSize(new Dimension(36, 36));
        av.setOpaque(false);
        return av;
    }

    private JPopupMenu buildPopup(String sender, String text) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBackground(Color.WHITE);
        popup.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Config.DIVIDER, 1),
            new EmptyBorder(4, 0, 4, 0)));

        JMenuItem quoteItem = menuItem("↩  Trả lời");
        quoteItem.addActionListener(e -> {
            if (onQuoteCb != null) onQuoteCb.accept(sender, text);
        });

        JMenuItem copyItem = menuItem("📋  Sao chép");
        copyItem.addActionListener(e -> {
            Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new java.awt.datatransfer.StringSelection(text), null);
        });

        popup.add(quoteItem);
        popup.addSeparator();
        popup.add(copyItem);
        return popup;
    }

    private JMenuItem menuItem(String text) {
        JMenuItem m = new JMenuItem(text);
        m.setBackground(Color.WHITE);
        m.setForeground(Config.TEXT_PRIMARY);
        m.setFont(Config.FONT_NORMAL);
        m.setBorder(new EmptyBorder(6, 16, 6, 16));
        return m;
    }

    private Color accentForName(String name) {
        Color[] palette = {
            new Color(0, 120, 212), new Color(52, 168, 83), new Color(234, 67, 53),
            new Color(155, 89, 182), new Color(26, 188, 156), new Color(230, 126, 34)
        };
        return palette[Math.abs(name.hashCode()) % palette.length];
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scroll.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    private String statusIcon(String s) {
        return switch (s == null ? "" : s) {
            case "sent" -> "✓";
            case "delivered" -> "✓✓";
            case "read" -> "✓✓";
            default -> "✓";
        };
    }

    private String escHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private Font messageFont(String text) {
        return new Font("Segoe UI Emoji", Font.PLAIN, emojiFontSize(text));
    }

    private boolean isEmojiOnly(String text) {
        if (text == null) return false;
        String compact = text.replaceAll("\\s+", "");
        if (compact.isEmpty()) return false;
        return compact.codePoints().allMatch(cp ->
            Character.getType(cp) == Character.SURROGATE ||
            Character.getType(cp) == Character.OTHER_SYMBOL ||
            Character.getType(cp) == Character.NON_SPACING_MARK ||
            cp == 0xFE0F ||
            cp == 0x200D);
    }

    private Dimension measureWrappedText(JTextArea textArea, String text, int maxWidth) {
        if (isEmojiOnly(text)) {
            textArea.setSize(Short.MAX_VALUE, Short.MAX_VALUE);
            Dimension pref = textArea.getPreferredSize();
            return new Dimension(pref.width + 4, pref.height + 2);
        }

        FontMetrics fm = textArea.getFontMetrics(textArea.getFont());
        int longestWord = 0;
        for (String word : text.split("\\s+")) {
            longestWord = Math.max(longestWord, fm.stringWidth(word));
        }

        int naturalWidth = 0;
        for (String line : text.split("\\R", -1)) {
            naturalWidth = Math.max(naturalWidth, fm.stringWidth(line));
        }

        int width = Math.min(maxWidth, Math.max(44, Math.max(longestWord, naturalWidth) + 8));
        textArea.setSize(width, Short.MAX_VALUE);
        Dimension pref = textArea.getPreferredSize();
        return new Dimension(width, pref.height);
    }

    private int emojiFontSize(String text) {
        if (!isEmojiOnly(text)) return 14;
        int count = emojiVisualCount(text);
        if (count <= 1) return 54;
        if (count == 2) return 46;
        if (count == 3) return 40;
        return 34;
    }

    private int emojiVisualCount(String text) {
        if (text == null || text.isBlank()) return 0;
        int count = 0;
        for (int cp : text.codePoints().toArray()) {
            if (Character.isWhitespace(cp) || cp == 0xFE0F || cp == 0x200D) continue;
            if (Character.getType(cp) == Character.NON_SPACING_MARK) continue;
            count++;
        }
        return Math.max(1, count);
    }

    private static class ZaloBubble extends JPanel {
        private final boolean isSelf;
        private final boolean emojiOnly;
        private boolean groupedWithPrev;
        private boolean groupedWithNext;

        ZaloBubble(boolean isSelf, boolean emojiOnly) {
            this.isSelf = isSelf;
            this.emojiOnly = emojiOnly;
            setOpaque(false);
        }

        void setGroupedWithPrev(boolean groupedWithPrev) {
            this.groupedWithPrev = groupedWithPrev;
            repaint();
        }

        void setGroupedWithNext(boolean groupedWithNext) {
            this.groupedWithNext = groupedWithNext;
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int arc = emojiOnly ? 26 : 18;
            Color shadow = emojiOnly
                ? new Color(72, 108, 148, 16)
                : (isSelf ? new Color(120, 155, 196, 26) : new Color(15, 23, 42, 18));
            Color fill = emojiOnly
                ? new Color(247, 251, 255)
                : (isSelf ? new Color(222, 236, 252) : Color.WHITE);
            Color border = emojiOnly
                ? new Color(215, 228, 244)
                : (isSelf ? new Color(189, 210, 233) : new Color(222, 228, 238));

            g2.setColor(shadow);
            float shadowYOffset = emojiOnly ? 2f : 3f;
            g2.fill(new RoundRectangle2D.Float(1.5f, shadowYOffset, getWidth() - 3, getHeight() - 3, arc, arc));
            flattenJoinedCorners(g2, shadow, (int) shadowYOffset);

            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 3, getHeight() - 3, arc, arc));
            flattenJoinedCorners(g2, fill, 0);

            g2.setColor(border);
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 4, getHeight() - 4, arc, arc));

            g2.dispose();
            super.paintComponent(g);
        }

        private void flattenJoinedCorners(Graphics2D g2, Color fill, int yOffset) {
            int patch = 16;
            int w = getWidth() - 3;
            int h = getHeight() - 3;
            g2.setColor(fill);
            if (groupedWithPrev) {
                if (isSelf) g2.fillRect(w - patch, yOffset, patch, patch);
                else g2.fillRect(0, yOffset, patch, patch);
            }
            if (groupedWithNext) {
                if (isSelf) g2.fillRect(w - patch, h - patch + yOffset, patch, patch);
                else g2.fillRect(0, h - patch + yOffset, patch, patch);
            }
        }
    }
}
