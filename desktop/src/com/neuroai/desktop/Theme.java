package com.neuroai.desktop;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.UIManager;
import javax.swing.border.Border;

/**
 * NeuroAI Workstation Design System.
 * Advanced medical UI palette supporting high-contrast Radiology Dark (MRI
 * room)
 * and Clinical Daylight (Outpatient) modes.
 */
public final class Theme {
    private Theme() {
    }

    public static boolean isDark = true;

    public interface ThemeChangeListener {
        void onThemeChanged(boolean isDark);
    }

    private static final List<ThemeChangeListener> listeners = new ArrayList<>();

    public static void addListener(ThemeChangeListener l) {
        listeners.add(l);
    }

    public static void setDarkMode(boolean dark) {
        isDark = dark;
        updateDynamicColors();
        applyGlobalDefaults();
        for (ThemeChangeListener l : listeners) {
            l.onThemeChanged(dark);
        }
    }

    // Dynamic UI Colors
    public static Color BG_MAIN;
    public static Color BG_PANEL;
    public static Color BG_INPUT;
    public static Color BG_INPUT_HOVER;
    public static Color BORDER_SUBTLE;
    public static Color BORDER_ACTIVE;
    public static Color TEXT_PRIMARY;
    public static Color TEXT_MUTED;
    public static Color TEXT_SELECTION_BG;
    public static Color TEXT_SELECTION_FG;

    public static Color WARN_BG;
    public static Color WARN_BORDER;
    public static Color WARN_TEXT;

    public static Color PRESET_BG;
    public static Color PRESET_BORDER;

    // Static Clinical Brand Colors
    public static final Color ACCENT_BLUE = new Color(0x25, 0x63, 0xEB);
    public static final Color ACCENT_HOVER = new Color(0x1D, 0x4E, 0xD8);
    public static final Color SUCCESS_GREEN = new Color(0x10, 0xB9, 0x81);
    public static final Color COLOR_MS = new Color(0x38, 0xBD, 0xF8); // Sky Cyan / Demyelination
    public static final Color COLOR_AD = new Color(0xEF, 0x44, 0x44); // Ruby / Alzheimer's
    public static final Color COLOR_PD = new Color(0xA8, 0x55, 0xF7); // Violet / Parkinson's
    public static final Color COLOR_CTRL = new Color(0x10, 0xB9, 0x81); // Emerald / Healthy Control
    public static final Color COLOR_ALS = new Color(0xF4, 0x3F, 0x5E); // Rose / Motor Neuron
    public static final Color COLOR_MCI = new Color(0xF5, 0x9E, 0x0B); // Amber / MCI

    // Typography
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 17);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_BADGE = new Font("Segoe UI", Font.BOLD, 10);
    public static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 12);
    public static final Font FONT_BTN = new Font("Segoe UI", Font.BOLD, 12);

    public static Border PANEL_BORDER;
    public static Border INPUT_BORDER;

    static {
        updateDynamicColors();
        applyGlobalDefaults();
    }

    public static void updateDynamicColors() {
        if (isDark) {
            // Sleek Medical Radiology Dark (Pure white text, no blending)
            BG_MAIN = new Color(0x0A, 0x0E, 0x17);
            BG_PANEL = new Color(0x13, 0x1B, 0x2A);
            BG_INPUT = new Color(0x1E, 0x29, 0x3B);
            BG_INPUT_HOVER = new Color(0x28, 0x36, 0x4E);
            BORDER_SUBTLE = new Color(0x33, 0x41, 0x55);
            BORDER_ACTIVE = new Color(0x38, 0xBD, 0xF8);
            TEXT_PRIMARY = Color.WHITE;
            TEXT_MUTED = new Color(0x94, 0xA3, 0xB8);

            TEXT_SELECTION_BG = new Color(0x25, 0x63, 0xEB);
            TEXT_SELECTION_FG = Color.WHITE;

            WARN_BG = new Color(0x2E, 0x1E, 0x10);
            WARN_BORDER = new Color(0xB4, 0x53, 0x09);
            WARN_TEXT = new Color(0xFC, 0xD3, 0x4D);

            PRESET_BG = new Color(0x1E, 0x29, 0x3B);
            PRESET_BORDER = new Color(0x33, 0x41, 0x55);
        } else {
            // Modern Clinical Daylight (Clean slate & white)
            BG_MAIN = new Color(0xF1, 0xF5, 0xF9);
            BG_PANEL = Color.WHITE;
            BG_INPUT = new Color(0xF8, 0xFA, 0xFC);
            BG_INPUT_HOVER = new Color(0xE2, 0xE8, 0xF0);
            BORDER_SUBTLE = new Color(0xCB, 0xD5, 0xE1);
            BORDER_ACTIVE = new Color(0x25, 0x63, 0xEB);
            TEXT_PRIMARY = new Color(0x0F, 0x17, 0x2A);
            TEXT_MUTED = new Color(0x47, 0x55, 0x69);

            TEXT_SELECTION_BG = new Color(0x1D, 0x4E, 0xD8);
            TEXT_SELECTION_FG = Color.WHITE;

            WARN_BG = new Color(0xFE, 0xF3, 0xC7);
            WARN_BORDER = new Color(0xF5, 0x9E, 0x0B);
            WARN_TEXT = new Color(0x92, 0x40, 0x0E);

            PRESET_BG = new Color(0xF8, 0xFA, 0xFC);
            PRESET_BORDER = new Color(0xCB, 0xD5, 0xE1);
        }

        PANEL_BORDER = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(14, 16, 14, 16));

        INPUT_BORDER = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10));
    }

    public static void applyGlobalDefaults() {
        UIManager.put("Panel.background", BG_MAIN);
        UIManager.put("OptionPane.background", BG_PANEL);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);

        UIManager.put("TextField.selectionBackground", TEXT_SELECTION_BG);
        UIManager.put("TextField.selectionForeground", TEXT_SELECTION_FG);
        UIManager.put("TextArea.selectionBackground", TEXT_SELECTION_BG);
        UIManager.put("TextArea.selectionForeground", TEXT_SELECTION_FG);
        UIManager.put("FormattedTextField.selectionBackground", TEXT_SELECTION_BG);
        UIManager.put("FormattedTextField.selectionForeground", TEXT_SELECTION_FG);

        UIManager.put("TextField.background", BG_INPUT);
        UIManager.put("TextField.foreground", TEXT_PRIMARY);
        UIManager.put("TextField.caretColor", TEXT_PRIMARY);

        UIManager.put("Table.background", BG_INPUT);
        UIManager.put("Table.foreground", TEXT_PRIMARY);
        UIManager.put("Table.selectionBackground", TEXT_SELECTION_BG);
        UIManager.put("Table.selectionForeground", TEXT_SELECTION_FG);
        UIManager.put("TableHeader.background", BG_PANEL);
        UIManager.put("TableHeader.foreground", TEXT_PRIMARY);

        UIManager.put("List.background", BG_INPUT);
        UIManager.put("List.foreground", TEXT_PRIMARY);
        UIManager.put("List.selectionBackground", TEXT_SELECTION_BG);
        UIManager.put("List.selectionForeground", TEXT_SELECTION_FG);

        UIManager.put("ComboBox.background", BG_INPUT);
        UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
        UIManager.put("ComboBox.selectionBackground", TEXT_SELECTION_BG);
        UIManager.put("ComboBox.selectionForeground", TEXT_SELECTION_FG);

        UIManager.put("Spinner.background", BG_INPUT);
        UIManager.put("Spinner.foreground", TEXT_PRIMARY);
        UIManager.put("Slider.background", BG_PANEL);
        UIManager.put("Slider.foreground", ACCENT_BLUE);

        UIManager.put("Label.foreground", TEXT_PRIMARY);
        UIManager.put("ScrollPane.background", BG_MAIN);
    }

    public static Color getPathologyColor(String name) {
        if (name == null)
            return ACCENT_BLUE;
        switch (name) {
            case "Multiple_Sclerosis":
                return COLOR_MS;
            case "Alzheimers":
                return COLOR_AD;
            case "Parkinsons":
                return COLOR_PD;
            case "Control":
                return COLOR_CTRL;
            case "ALS":
                return COLOR_ALS;
            case "MCI":
                return COLOR_MCI;
            default:
                return ACCENT_BLUE;
        }
    }

    public static String getMutedHex() {
        return isDark ? "#94A3B8" : "#475569";
    }

    public static String getPrimaryHex() {
        return isDark ? "#FFFFFF" : "#0F172A";
    }

    /**
     * Guarantees high-contrast readable HTML across all Swing components in both
     * Dark and Light modes.
     * Automatically replaces legacy static slate-400 (#94A3B8) with dynamic
     * mode-aware contrast.
     */
    public static String html(String bodyText) {
        String color = getPrimaryHex();
        String muted = getMutedHex();
        String clean = bodyText.replace("#94A3B8", muted).replace("#94a3b8", muted);
        return "<html><body style='color: " + color + "; font-family: Segoe UI, sans-serif; margin:0; padding:0;'>"
                + clean + "</body></html>";
    }

    public static String htmlMuted(String bodyText) {
        String muted = getMutedHex();
        return "<html><body style='color: " + muted + "; font-family: Segoe UI, sans-serif; margin:0; padding:0;'>"
                + bodyText + "</body></html>";
    }

    public static void attachHoverEffect(JButton btn, Color normalBg, Color hoverBg, Color normalBorder,
            Color hoverBorder) {
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(hoverBg);
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(hoverBorder, 1),
                        BorderFactory.createEmptyBorder(6, 14, 6, 14)));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(normalBg);
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(normalBorder, 1),
                        BorderFactory.createEmptyBorder(6, 14, 6, 14)));
            }
        });
    }

    public static void stylePrimaryButton(JButton btn) {
        btn.setFont(FONT_BTN);
        btn.setBackground(ACCENT_BLUE);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_BLUE_HOVER, 1),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        attachHoverEffect(btn, ACCENT_BLUE, ACCENT_BLUE_HOVER, ACCENT_BLUE_HOVER, ACCENT_BLUE);
    }

    public static void styleSecondaryButton(JButton btn) {
        btn.setFont(FONT_BTN);
        btn.setBackground(BG_INPUT);
        btn.setForeground(TEXT_PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        attachHoverEffect(btn, BG_INPUT, BG_INPUT_HOVER, BORDER_SUBTLE, BORDER_ACTIVE);
    }
}
