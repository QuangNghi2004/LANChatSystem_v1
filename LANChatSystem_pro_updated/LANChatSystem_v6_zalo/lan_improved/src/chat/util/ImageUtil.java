package chat.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

/** Resize ảnh và tạo avatar. */
public class ImageUtil {

    /** Scale ảnh xuống maxDim x maxDim giữ tỉ lệ. */
    public static BufferedImage resize(BufferedImage src, int maxDim) {
        if (src == null) return null;
        int w = src.getWidth(), h = src.getHeight();
        double r = Math.min((double)maxDim/w, (double)maxDim/h);
        if (r >= 1.0) return src;
        int nw = (int)(w*r), nh = (int)(h*r);
        BufferedImage dst = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                           RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return dst;
    }

    public static byte[] toBytes(BufferedImage img, String fmt) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, fmt, out);
        return out.toByteArray();
    }

    public static BufferedImage fromBytes(byte[] data) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(data));
    }
}
