package chat.ui.client.dialogs;

import javax.swing.*;
import java.awt.*;

/** UserProfileDialog — placeholder, sẽ mở rộng. */
public class UserProfileDialog extends JDialog {
    public UserProfileDialog(Frame parent) {
        super(parent, "UserProfileDialog", true);
        setSize(400, 300); setLocationRelativeTo(parent);
        add(new JLabel("🚧 Đang phát triển: UserProfileDialog", SwingConstants.CENTER));
    }
}
