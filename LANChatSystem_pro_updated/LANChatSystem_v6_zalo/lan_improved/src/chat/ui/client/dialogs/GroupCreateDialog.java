package chat.ui.client.dialogs;

import javax.swing.*;
import java.awt.*;

/** GroupCreateDialog — placeholder, sẽ mở rộng. */
public class GroupCreateDialog extends JDialog {
    public GroupCreateDialog(Frame parent) {
        super(parent, "GroupCreateDialog", true);
        setSize(400, 300); setLocationRelativeTo(parent);
        add(new JLabel("🚧 Đang phát triển: GroupCreateDialog", SwingConstants.CENTER));
    }
}
