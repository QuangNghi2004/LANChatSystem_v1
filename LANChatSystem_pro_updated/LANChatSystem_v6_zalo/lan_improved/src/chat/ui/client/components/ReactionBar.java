package chat.ui.client.components;

import chat.util.Config;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Consumer;

public class ReactionBar extends JPanel {
    private Consumer<String> onReactionPick = reaction -> {};

    public ReactionBar() {
        setBackground(Config.BG_PANEL);
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.LEFT, 6, 0));
        setBorder(new EmptyBorder(4, 4, 4, 4));
        rebuild();
    }

    public void setOnReactionPick(Consumer<String> onReactionPick) {
        this.onReactionPick = (onReactionPick != null) ? onReactionPick : reaction -> {};
    }

    private void rebuild() {
        removeAll();
        for (EmojiAssets.EmojiEntry entry : EmojiAssets.quickReactions()) {
            add(createButton(entry));
        }
    }

    private JButton createButton(EmojiAssets.EmojiEntry entry) {
        JButton button = new JButton();
        button.setIcon(EmojiAssets.createIcon(entry.style(), 24));
        button.setPreferredSize(new Dimension(34, 34));
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setToolTipText(entry.tooltip());
        button.addActionListener(e -> onReactionPick.accept(entry.value()));
        return button;
    }
}
