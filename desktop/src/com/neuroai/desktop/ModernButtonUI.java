package com.neuroai.desktop;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicButtonUI;

public class ModernButtonUI extends BasicButtonUI {
    private final int cornerRadius;

    public ModernButtonUI(int cornerRadius) {
        this.cornerRadius = cornerRadius;
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        AbstractButton b = (AbstractButton) c;
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setRolloverEnabled(true);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        ButtonModel model = b.getModel();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = b.getWidth();
        int h = b.getHeight();

        Color bg = b.getBackground();
        if (!b.isEnabled()) {
            bg = Theme.isDark ? new Color(0x1E, 0x24, 0x33) : new Color(0xE2, 0xE8, 0xF0);
        } else if (model.isPressed()) {
            Color pressBg = (Color) b.getClientProperty("btn.pressBg");
            bg = pressBg != null ? pressBg : (Theme.isDark ? bg.darker() : bg.darker());
        } else if (model.isRollover()) {
            Color hoverBg = (Color) b.getClientProperty("btn.hoverBg");
            if (hoverBg != null) {
                bg = hoverBg;
            } else {
                bg = Theme.isDark ? bg.brighter() : bg.darker();
            }
        }

        g2.setColor(bg);
        g2.fillRoundRect(0, 0, w, h, cornerRadius, cornerRadius);

        Color border = (Color) b.getClientProperty("btn.borderColor");
        if (border != null) {
            if (model.isRollover() && b.isEnabled()) {
                Color hoverBorder = (Color) b.getClientProperty("btn.hoverBorder");
                if (hoverBorder != null) border = hoverBorder;
            }
            g2.setColor(border);
            g2.drawRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);
        }

        g2.dispose();
        super.paint(g, c);
    }

    public static void stylePrimary(JButton btn) {
        btn.setUI(new ModernButtonUI(6));
        btn.setFont(Theme.FONT_BTN);
        btn.setBackground(Theme.ACCENT_BLUE);
        btn.setForeground(Color.WHITE);
        btn.putClientProperty("btn.hoverBg", Theme.ACCENT_HOVER);
        btn.putClientProperty("btn.pressBg", Theme.ACCENT_HOVER.darker());
        btn.putClientProperty("btn.borderColor", Theme.ACCENT_HOVER);
        btn.putClientProperty("btn.hoverBorder", Theme.BORDER_ACTIVE);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.repaint();
    }

    public static void styleSecondary(JButton btn) {
        btn.setUI(new ModernButtonUI(6));
        btn.setFont(Theme.FONT_LABEL);
        btn.setBackground(Theme.BG_INPUT);
        btn.setForeground(Theme.TEXT_PRIMARY);
        btn.putClientProperty("btn.hoverBg", Theme.BG_INPUT_HOVER);
        btn.putClientProperty("btn.pressBg", Theme.BG_INPUT.darker());
        btn.putClientProperty("btn.borderColor", Theme.BORDER_SUBTLE);
        btn.putClientProperty("btn.hoverBorder", Theme.BORDER_ACTIVE);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.repaint();
    }

    public static void stylePreset(JButton btn, boolean active) {
        btn.setUI(new ModernButtonUI(5));
        btn.setFont(Theme.FONT_LABEL);
        if (active) {
            btn.setBackground(Theme.PRESET_BG);
            btn.setForeground(Theme.isDark ? Color.WHITE : Theme.ACCENT_BLUE);
            btn.putClientProperty("btn.borderColor", Theme.BORDER_ACTIVE);
            btn.putClientProperty("btn.hoverBorder", Theme.BORDER_ACTIVE);
            btn.putClientProperty("btn.hoverBg", Theme.BG_INPUT_HOVER);
            btn.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        } else {
            btn.setBackground(Theme.BG_PANEL);
            btn.setForeground(Theme.TEXT_MUTED);
            btn.putClientProperty("btn.borderColor", Theme.BORDER_SUBTLE);
            btn.putClientProperty("btn.hoverBorder", Theme.BORDER_ACTIVE);
            btn.putClientProperty("btn.hoverBg", Theme.BG_INPUT);
            btn.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        }
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.repaint();
    }
}
