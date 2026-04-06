package chat.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

/** Logger đơn giản — có thể gắn callback ra UI hoặc in ra console. */
public class Logger {
    private static Consumer<String> uiCallback = null;

    public static void setCallback(Consumer<String> cb) { uiCallback = cb; }

    public static void info(String msg)  { log("INFO ", msg); }
    public static void warn(String msg)  { log("WARN ", msg); }
    public static void error(String msg) { log("ERROR", msg); }

    private static void log(String level, String msg) {
        String ts   = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String line = "[" + ts + "] [" + level + "] " + msg;
        System.out.println(line);
        if (uiCallback != null) uiCallback.accept(line);
    }
}
