package chat.ui.client.dialogs;

import javax.swing.*;
import java.awt.*;

/** CreatePollDialog — placeholder, sẽ mở rộng. */
public class CreatePollDialog extends JDialog {
    public CreatePollDialog(Frame parent) {
        super(parent, "CreatePollDialog", true);
        setSize(400, 300); setLocationRelativeTo(parent);
        add(new JLabel("🚧 Đang phát triển: CreatePollDialog", SwingConstants.CENTER));
    }
}
