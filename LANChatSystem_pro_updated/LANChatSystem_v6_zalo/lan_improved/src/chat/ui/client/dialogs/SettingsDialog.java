package chat.ui.client.dialogs;

import javax.swing.*;
import java.awt.*;

/** SettingsDialog — placeholder, sẽ mở rộng. */
public class SettingsDialog extends JDialog {
    public SettingsDialog(Frame parent) {
        super(parent, "SettingsDialog", true);
        setSize(400, 300); setLocationRelativeTo(parent);
        add(new JLabel("🚧 Đang phát triển: SettingsDialog", SwingConstants.CENTER));
    }
}
