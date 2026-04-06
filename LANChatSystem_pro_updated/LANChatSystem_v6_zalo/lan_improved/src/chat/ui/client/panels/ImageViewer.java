package chat.ui.client.panels;

import chat.util.Config;
import chat.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/** Xem ảnh inline với zoom in/out. */
public class ImageViewer extends JDialog {

    private final String fileName;
    private final byte[] data;
    private BufferedImage originalImage;
    private double        zoom = 1.0;
    private JLabel        canvas;

    public ImageViewer(Frame parent, String title, byte[] data) {
        super(parent, "🖼️  " + title, false);
        this.fileName = title;
        this.data = data;
        setSize(800, 600); setLocationRelativeTo(parent);
        getContentPane().setBackground(Config.BG_DARK);
        try {
            originalImage = ImageUtil.fromBytes(data);
        } catch (IOException ignored) {}
        buildUI();
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        canvas = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (originalImage == null) return;
                int nw=(int)(originalImage.getWidth()*zoom), nh=(int)(originalImage.getHeight()*zoom);
                int x=Math.max(0,(getWidth()-nw)/2), y=Math.max(0,(getHeight()-nh)/2);
                ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.drawImage(originalImage,x,y,nw,nh,this);
            }
        };
        canvas.setBackground(Config.BG_DARK); canvas.setOpaque(true);
        JScrollPane sp = new JScrollPane(canvas);
        sp.setBorder(null); sp.getViewport().setBackground(Config.BG_DARK);
        add(sp, BorderLayout.CENTER);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER,8,4));
        bar.setBackground(Config.BG_PANEL);
        JButton save=btn("💾 Lưu ảnh"), zIn=btn("🔍+"), zOut=btn("🔍−"), fit=btn("⊞ Fit"), orig=btn("1:1");
        save.addActionListener(e -> saveImage());
        zIn .addActionListener(e->{zoom=Math.min(10,zoom*1.25);canvas.revalidate();canvas.repaint();});
        zOut.addActionListener(e->{zoom=Math.max(0.05,zoom/1.25);canvas.revalidate();canvas.repaint();});
        fit .addActionListener(e->fitImage(sp));
        orig.addActionListener(e->{zoom=1.0;canvas.revalidate();canvas.repaint();});
        bar.add(save);bar.add(zOut);bar.add(zIn);bar.add(fit);bar.add(orig);
        add(bar, BorderLayout.SOUTH);

        addComponentListener(new ComponentAdapter(){
            boolean done=false;
            @Override public void componentResized(ComponentEvent e){if(!done){done=true;fitImage(sp);}}
        });
        sp.addMouseWheelListener(ev->{
            if(ev.isControlDown()){
                double d=ev.getPreciseWheelRotation()<0?1.15:1.0/1.15;
                zoom=Math.max(0.05,Math.min(zoom*d,10));canvas.revalidate();canvas.repaint();
            }
        });
    }

    private void fitImage(JScrollPane sp) {
        if (originalImage == null) return;
        int w=sp.getWidth()-20, h=sp.getHeight()-20;
        if(w<=0||h<=0) return;
        zoom=Math.min((double)w/originalImage.getWidth(),(double)h/originalImage.getHeight());
        canvas.revalidate(); canvas.repaint();
    }

    private void saveImage() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(fileName));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            Files.write(fc.getSelectedFile().toPath(), data);
            JOptionPane.showMessageDialog(this,
                "Đã lưu ảnh: " + fc.getSelectedFile().getAbsolutePath(),
                "Lưu thành công",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                "Không thể lưu ảnh: " + e.getMessage(),
                "Lỗi",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private JButton btn(String t){JButton b=new JButton(t);b.setBackground(Config.BG_INPUT);b.setForeground(Config.TEXT_PRIMARY);b.setFont(Config.FONT_NORMAL);b.setFocusPainted(false);b.setBorder(new EmptyBorder(4,10,4,10));return b;}
}
