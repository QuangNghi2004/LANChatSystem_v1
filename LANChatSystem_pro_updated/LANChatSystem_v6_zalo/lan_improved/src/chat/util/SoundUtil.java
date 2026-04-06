package chat.util;

import javax.sound.sampled.*;

/** Phát âm thanh thông báo "Ting!". */
public class SoundUtil {
    public static void ting() {
        new Thread(() -> {
            try {
                AudioFormat fmt  = new AudioFormat(44100, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
                if (!AudioSystem.isLineSupported(info)) return;
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(fmt, 4096); line.start();
                int samples = 4000;
                byte[] buf  = new byte[samples * 2];
                for (int i = 0; i < samples; i++) {
                    double angle = 2.0 * Math.PI * 880 * i / 44100;
                    double env   = Math.exp(-5.0 * i / samples);
                    short  s     = (short)(Short.MAX_VALUE * 0.4 * Math.sin(angle) * env);
                    buf[2*i]   = (byte)(s & 0xFF);
                    buf[2*i+1] = (byte)((s >> 8) & 0xFF);
                }
                line.write(buf, 0, buf.length);
                line.drain(); line.close();
            } catch (Exception ignored) {}
        }, "sound").start();
    }
}
