package com.neuroai.desktop;

import java.awt.*;
import javax.swing.*;

/**
 * NeuroAI Diagnostic Workstation — Desktop Application Entry Point.
 * High-DPI hardware-accelerated clinical console for Windows & Linux.
 */
public class NeuroApp {

    public static void main(String[] args) {
        // Enforce anti-aliasing and modern high-DPI rendering
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("sun.java2d.dpiaware", "true");
        System.setProperty("sun.java2d.d3d", "true");

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                applyGlobalThemeDefaults();
            } catch (Exception e) {
                // Keep default cross-platform look
            }

            JFrame frame = new JFrame(
                    "NeuroAI Workstation v2.5 — Клинический комплекс дифференциальной диагностики ЦНС");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1240, 780);
            frame.setMinimumSize(new Dimension(1060, 680));
            frame.setLocationRelativeTo(null);
            frame.getContentPane().setBackground(Theme.BG_MAIN);

            PythonBridge bridge = new PythonBridge();
            FileSelectionPanel panel = new FileSelectionPanel(frame, bridge);
            frame.setContentPane(panel);

            frame.setVisible(true);
        });
    }

    private static void applyGlobalThemeDefaults() {
        UIManager.put("Panel.background", Theme.BG_MAIN);
        UIManager.put("OptionPane.background", Theme.BG_PANEL);
        UIManager.put("OptionPane.messageForeground", Theme.TEXT_PRIMARY);
        UIManager.put("Button.background", Theme.BG_INPUT);
        UIManager.put("Button.foreground", Theme.TEXT_PRIMARY);
        UIManager.put("TextField.background", Theme.BG_INPUT);
        UIManager.put("TextField.foreground", Theme.TEXT_PRIMARY);
        UIManager.put("Label.foreground", Theme.TEXT_PRIMARY);
        UIManager.put("ScrollPane.background", Theme.BG_MAIN);
    }
}
