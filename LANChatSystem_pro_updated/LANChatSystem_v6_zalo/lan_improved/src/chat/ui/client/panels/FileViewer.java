package chat.ui.client.panels;

import chat.util.Config;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;

/**
 * Cửa sổ xem file nhận được.
 *
 * Hỗ trợ xem trực tiếp:
 *   - Ảnh  : png, jpg, jpeg, gif, bmp, webp, tiff, ico  → zoom in/out, fit-to-window
 *   - Text  : txt, log, md, csv, json, xml, html, java, py, sql, ...
 *   - PDF   : render từng trang bằng Apache PDFBox (nếu có pdfbox-app-*.jar)
 *             Nếu không có PDFBox → fallback "Lưu file"
 *   - Khác  : hiển thị icon + nút Lưu file
 *
 * Tất cả loại file đều có nút "💾 Lưu file".
 */
public class FileViewer extends JDialog {

    private final String fileName;
    private final byte[] data;

    // Image viewer state
    private BufferedImage originalImage;
    private double        zoomFactor = 1.0;
    private JLabel        imageLabel;

    public FileViewer(Frame parent, String fileName, byte[] data) {
        super(parent, "📎 " + fileName, false);
        this.fileName = fileName;
        this.data     = data;
        setSize(820, 600);
        setLocationRelativeTo(parent);
        setMinimumSize(new Dimension(480, 360));
        getContentPane().setBackground(Config.BG_DARK);
        buildUI();
    }

    // ════════════════════════════════════════════════════════════════════════
    private void buildUI() {
        setLayout(new BorderLayout(0, 0));
        add(buildToolbar(), BorderLayout.NORTH);

        String ext = getExtension(fileName).toLowerCase();
        JComponent content;

        if (isImage(ext))       content = buildImageViewer();
        else if (isPDF(ext))    content = buildPDFViewer();
        else if (isText(ext))   content = buildTextViewer();
        else                    content = buildGenericViewer();

        add(content, BorderLayout.CENTER);
    }

    // ── Toolbar dùng chung ───────────────────────────────────────────────────
    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Config.BG_PANEL);
        bar.setBorder(new EmptyBorder(6, 10, 6, 10));

        // Tên file + kích thước
        JLabel nameLabel = new JLabel("📎  " + fileName + "   (" + formatSize(data.length) + ")");
        nameLabel.setForeground(Config.TEXT_PRIMARY);
        nameLabel.setFont(Config.FONT_NORMAL);
        bar.add(nameLabel, BorderLayout.WEST);

        // Nút lưu
        JButton btnSave = pill("💾 Lưu file", Config.ACCENT, Config.BG_DARK);
        btnSave.addActionListener(e -> saveFile());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(btnSave);
        bar.add(right, BorderLayout.EAST);

        return bar;
    }

    // ════════════════════════════════════════════════════════════════════════
    // ── Ảnh ─────────────────────────────────────────────────────────────────
    private JComponent buildImageViewer() {
        try {
            originalImage = ImageIO.read(new ByteArrayInputStream(data));
            if (originalImage == null) return buildGenericViewer();
        } catch (Exception e) {
            return buildGenericViewer();
        }

        // Canvas vẽ ảnh với zoom
        imageLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (originalImage == null) return;
                int nw = (int)(originalImage.getWidth()  * zoomFactor);
                int nh = (int)(originalImage.getHeight() * zoomFactor);
                int x  = Math.max(0, (getWidth()  - nw) / 2);
                int y  = Math.max(0, (getHeight() - nh) / 2);
                ((Graphics2D) g).setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.drawImage(originalImage, x, y, nw, nh, this);
            }
            @Override public Dimension getPreferredSize() {
                if (originalImage == null) return super.getPreferredSize();
                return new Dimension(
                    (int)(originalImage.getWidth()  * zoomFactor),
                    (int)(originalImage.getHeight() * zoomFactor));
            }
        };
        imageLabel.setBackground(Config.BG_DARK);
        imageLabel.setOpaque(true);

        JScrollPane sp = new JScrollPane(imageLabel);
        sp.setBorder(null);
        sp.getViewport().setBackground(Config.BG_DARK);

        // Fit to window khi mở lần đầu
        addComponentListener(new ComponentAdapter() {
            boolean done = false;
            @Override public void componentResized(ComponentEvent e) {
                if (!done) { done = true; fitImage(sp); }
            }
        });

        // Thanh zoom
        JPanel zoomBar = buildZoomBar(sp);

        // Scroll bằng chuột giữa để zoom
        sp.addMouseWheelListener(ev -> {
            if (ev.isControlDown()) {
                double delta = ev.getPreciseWheelRotation() < 0 ? 1.15 : 1.0 / 1.15;
                zoomFactor = Math.max(0.05, Math.min(zoomFactor * delta, 20.0));
                imageLabel.revalidate();
                imageLabel.repaint();
            }
        });

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Config.BG_DARK);
        wrapper.add(sp,      BorderLayout.CENTER);
        wrapper.add(zoomBar, BorderLayout.SOUTH);
        return wrapper;
    }

    private void fitImage(JScrollPane sp) {
        if (originalImage == null) return;
        Insets in = sp.getInsets();
        int w = sp.getWidth()  - in.left - in.right  - 20;
        int h = sp.getHeight() - in.top  - in.bottom - 20;
        if (w <= 0 || h <= 0) return;
        double rw = (double) w / originalImage.getWidth();
        double rh = (double) h / originalImage.getHeight();
        zoomFactor = Math.min(rw, rh);
        imageLabel.revalidate();
        imageLabel.repaint();
    }

    private JPanel buildZoomBar(JScrollPane sp) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        bar.setBackground(Config.BG_PANEL);

        JButton zoomOut = pill("🔍−", Config.BG_INPUT, Config.TEXT_PRIMARY);
        JButton zoomIn  = pill("🔍+", Config.BG_INPUT, Config.TEXT_PRIMARY);
        JButton fit     = pill("⊞ Vừa màn hình", Config.BG_INPUT, Config.TEXT_PRIMARY);
        JButton orig    = pill("1:1 Gốc", Config.BG_INPUT, Config.TEXT_PRIMARY);

        zoomOut.addActionListener(e -> { zoomFactor = Math.max(0.05, zoomFactor / 1.25); imageLabel.revalidate(); imageLabel.repaint(); });
        zoomIn .addActionListener(e -> { zoomFactor = Math.min(20.0, zoomFactor * 1.25); imageLabel.revalidate(); imageLabel.repaint(); });
        fit    .addActionListener(e -> fitImage(sp));
        orig   .addActionListener(e -> { zoomFactor = 1.0; imageLabel.revalidate(); imageLabel.repaint(); });

        bar.add(zoomOut); bar.add(zoomIn); bar.add(fit); bar.add(orig);
        return bar;
    }

    // ════════════════════════════════════════════════════════════════════════
    // ── PDF ──────────────────────────────────────────────────────────────────
    private JComponent buildPDFViewer() {
        try {
            // Dùng reflection để không bắt buộc phải có PDFBox lúc compile
            Class<?> loaderClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            Class<?> rendClass   = Class.forName("org.apache.pdfbox.rendering.PDFRenderer");
            Class<?> imgTypeClass= Class.forName("org.apache.pdfbox.rendering.ImageType");

            Object doc = loaderClass.getMethod("load", byte[].class).invoke(null, (Object) data);
            Object renderer = rendClass.getDeclaredConstructors()[0].newInstance(doc);
            int pageCount = (int) loaderClass.getMethod("getNumberOfPages").invoke(doc);

            // Render từng trang thành ảnh, xếp dọc
            JPanel pagesPanel = new JPanel();
            pagesPanel.setLayout(new BoxLayout(pagesPanel, BoxLayout.Y_AXIS));
            pagesPanel.setBackground(Config.BG_DARK);

            Object rgb = null;
            for (Object f : imgTypeClass.getEnumConstants()) {
                if (f.toString().equals("RGB")) { rgb = f; break; }
            }

            for (int i = 0; i < pageCount; i++) {
                BufferedImage pageImg = (BufferedImage) rendClass
                    .getMethod("renderImageWithDPI", int.class, float.class, imgTypeClass)
                    .invoke(renderer, i, 120f, rgb);
                JLabel lbl = new JLabel(new ImageIcon(pageImg));
                lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
                lbl.setBorder(BorderFactory.createCompoundBorder(
                    new EmptyBorder(6, 6, 6, 6),
                    BorderFactory.createLineBorder(Config.ACCENT.darker(), 1)));
                pagesPanel.add(lbl);
            }

            loaderClass.getMethod("close").invoke(doc);

            JScrollPane sp = new JScrollPane(pagesPanel);
            sp.setBorder(null);
            sp.getViewport().setBackground(Config.BG_DARK);
            sp.getVerticalScrollBar().setUnitIncrement(20);

            // Header số trang
            JLabel pageInfo = new JLabel("📄 PDF — " + pageCount + " trang", SwingConstants.CENTER);
            pageInfo.setForeground(Config.TEXT_MUTED);
            pageInfo.setFont(Config.FONT_SMALL);
            pageInfo.setBorder(new EmptyBorder(4, 0, 2, 0));
            pageInfo.setOpaque(true);
            pageInfo.setBackground(Config.BG_PANEL);

            JPanel wrap = new JPanel(new BorderLayout());
            wrap.add(pageInfo, BorderLayout.NORTH);
            wrap.add(sp, BorderLayout.CENTER);
            return wrap;

        } catch (ClassNotFoundException e) {
            // PDFBox không có → hướng dẫn lưu
            return buildFallbackViewer(
                "📄",
                "Cần thêm thư viện PDFBox để xem PDF trực tiếp.",
                "Tải pdfbox-app-*.jar tại pdfbox.apache.org\nrồi thêm vào Build Path, hoặc nhấn Lưu để mở bằng trình đọc PDF.");
        } catch (Exception e) {
            return buildFallbackViewer("⚠️", "Không thể đọc file PDF.", e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // ── Text ─────────────────────────────────────────────────────────────────
    private JComponent buildTextViewer() {
        try {
            String text = new String(data, java.nio.charset.StandardCharsets.UTF_8);
            JTextArea area = new JTextArea(text);
            area.setEditable(false);
            area.setFont(Config.FONT_MONO);
            area.setBackground(Config.BG_INPUT);
            area.setForeground(Config.TEXT_PRIMARY);
            area.setCaretColor(Config.TEXT_PRIMARY);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setBorder(new EmptyBorder(10, 14, 10, 14));

            JScrollPane sp = new JScrollPane(area);
            sp.setBorder(null);
            sp.getViewport().setBackground(Config.BG_INPUT);
            sp.getVerticalScrollBar().setUnitIncrement(16);
            return sp;
        } catch (Exception e) {
            return buildGenericViewer();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // ── Generic (không xem được) ─────────────────────────────────────────────
    private JComponent buildGenericViewer() {
        String ext = getExtension(fileName).toLowerCase();
        String icon = switch (ext) {
            case "pdf"             -> "📄";
            case "doc","docx"      -> "📝";
            case "xls","xlsx"      -> "📊";
            case "ppt","pptx"      -> "📑";
            case "zip","rar","7z"  -> "🗜️";
            case "mp4","avi","mkv","mov" -> "🎬";
            case "mp3","wav","flac","aac" -> "🎵";
            case "exe","msi"       -> "⚙️";
            default                -> "📄";
        };
        return buildFallbackViewer(icon,
            "Không thể xem trực tiếp loại file \"." + ext + "\".",
            "Nhấn Lưu file để tải về máy rồi mở bằng ứng dụng phù hợp.");
    }

    private JComponent buildFallbackViewer(String icon, String msg, String sub) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Config.BG_DARK);

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.insets = new Insets(0, 0, 12, 0);

        JLabel iconLbl = new JLabel(icon, SwingConstants.CENTER);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 72));
        iconLbl.setForeground(Config.TEXT_MUTED);
        g.gridy = 0;
        panel.add(iconLbl, g);

        JLabel msgLbl = new JLabel("<html><center>" + msg + "</center></html>", SwingConstants.CENTER);
        msgLbl.setForeground(Config.TEXT_PRIMARY);
        msgLbl.setFont(Config.FONT_NORMAL);
        g.gridy = 1; g.insets = new Insets(0, 0, 6, 0);
        panel.add(msgLbl, g);

        if (sub != null && !sub.isEmpty()) {
            JLabel subLbl = new JLabel(
                "<html><center><small>" + sub.replace("\n", "<br>") + "</small></center></html>",
                SwingConstants.CENTER);
            subLbl.setForeground(Config.TEXT_MUTED);
            subLbl.setFont(Config.FONT_SMALL);
            g.gridy = 2; g.insets = new Insets(0, 24, 16, 24);
            panel.add(subLbl, g);
        }

        JButton btnSave = pill("💾 Lưu file về máy", Config.ACCENT, Config.BG_DARK);
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSave.addActionListener(e -> saveFile());
        g.gridy = 3; g.insets = new Insets(0, 0, 0, 0);
        panel.add(btnSave, g);

        return panel;
    }

    // ── Save ─────────────────────────────────────────────────────────────────
    private void saveFile() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(fileName));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.write(fc.getSelectedFile().toPath(), data);
                JOptionPane.showMessageDialog(this,
                    "✅ Đã lưu: " + fc.getSelectedFile().getAbsolutePath(),
                    "Lưu thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                    "❌ Lỗi khi lưu: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private JButton pill(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(6, 14, 6, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static String getExtension(String name) {
        int i = name.lastIndexOf('.');
        return i >= 0 ? name.substring(i + 1) : "";
    }

    private static boolean isImage(String ext) {
        return ext.matches("png|jpg|jpeg|gif|bmp|webp|tiff|tif|ico");
    }

    private static boolean isPDF(String ext) {
        return ext.equals("pdf");
    }

    private static boolean isText(String ext) {
        return ext.matches(
            "txt|log|md|markdown|csv|tsv|json|xml|html|htm|" +
            "js|ts|jsx|tsx|java|py|c|cpp|h|cs|go|rs|php|rb|swift|kt|" +
            "yaml|yml|toml|ini|cfg|conf|properties|env|" +
            "sql|sh|bat|cmd|ps1|gradle|makefile|dockerfile");
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024)             return bytes + " B";
        if (bytes < 1024 * 1024)      return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
