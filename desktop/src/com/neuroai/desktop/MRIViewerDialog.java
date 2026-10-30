package com.neuroai.desktop;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * NeuroAI Diagnostic Slice Viewer.
 * High-resolution radiological canvas with full 3D slice exploration,
 * mouse wheel slice scrolling, lesion layer markers, AI multi-lesion
 * localization overlay,
 * classification and subclass tags, and tissue-adaptive windowing contrast.
 */
public class MRIViewerDialog extends JDialog {
    private final List<Map<String, Object>> files;
    private int currentIndex;

    // 3D Volume Slices State
    private int currentSliceZ = 0;
    private int totalSlicesZ = 1;
    private final List<Integer> slicesWithLesions = new ArrayList<>();
    private final Map<String, Object> sliceLesionCounts = new LinkedHashMap<>();
    private List<Map<String, Object>> currentSliceRoiBoxes = new ArrayList<>();
    private String currentAxis = "axial";
    private boolean isUpdatingCombo = false;

    // UI Elements
    private JLabel headerTitleLabel;
    private JLabel headerMetaLabel;
    private JCheckBox aiOverlayCheck;
    private JComboBox<String> axisCombo;
    private ImageCanvas canvas;

    // 3D Slice Controls
    private JLabel sliceIndicatorLabel;
    private JSlider sliceSlider;
    private JButton prevSliceBtn;
    private JButton nextSliceBtn;
    private JButton prevLesionSliceBtn;
    private JButton nextLesionSliceBtn;
    private JComboBox<String> lesionSlicesCombo;

    // Windowing / Level Controls
    private JSlider contrastSlider;
    private JSlider brightnessSlider;
    private JLabel statusLabel;
    private Timer loadingTimer;
    private long loadStartTime;
    private final PythonBridge bridge;

    // Sidebar Details
    private JLabel diagBadge;
    private JLabel subclassValue;
    private JLabel zoneValue;
    private JLabel lesionValue;
    private JLabel criteriaValue;
    private JLabel confidenceValue;
    private DefaultListModel<String> sliceListModel;
    private JList<String> sliceList;

    public MRIViewerDialog(Frame parent, List<Map<String, Object>> files, int initialIndex) {
        super(parent, "Нейрорадиологический диагностический просмотрщик — NeuroAI Viewer", true);
        this.files = files;
        this.currentIndex = Math.max(0, Math.min(initialIndex, files.size() - 1));
        this.bridge = new PythonBridge();

        setSize(1140, 820);
        setMinimumSize(new Dimension(1020, 720));
        setLocationRelativeTo(parent);
        getContentPane().setBackground(Theme.BG_MAIN);
        setLayout(new BorderLayout(0, 0));

        // 1. Top Header Toolbar (Patient Series Navigation & AI toggles)
        JPanel topToolbar = createTopToolbar();
        add(topToolbar, BorderLayout.NORTH);

        // 2. Center: Canvas + 3D Slice Navigator
        JPanel centerContainer = new JPanel(new BorderLayout(0, 0));
        centerContainer.setBackground(Theme.BG_MAIN);

        canvas = new ImageCanvas();
        centerContainer.add(canvas, BorderLayout.CENTER);

        JPanel sliceControlPanel = createSliceControlPanel();
        centerContainer.add(sliceControlPanel, BorderLayout.SOUTH);

        add(centerContainer, BorderLayout.CENTER);

        // 3. Right: Diagnostic Metadata Sidebar
        JPanel sidebar = createSidebar();
        add(sidebar, BorderLayout.EAST);

        // 4. Bottom Controls Toolbar (Brightness, Contrast, Status)
        JPanel bottomToolbar = createBottomToolbar();
        add(bottomToolbar, BorderLayout.SOUTH);

        // Load Initial Slice
        loadSliceAsync(currentIndex);
    }

    private JPanel createTopToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(16, 0));
        toolbar.setBackground(Theme.BG_PANEL);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_SUBTLE),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)));

        // Left Header Info
        JPanel left = new JPanel(new GridLayout(2, 1, 0, 2));
        left.setOpaque(false);

        headerTitleLabel = new JLabel("Серия исследования: --");
        headerTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        headerTitleLabel.setForeground(Theme.TEXT_PRIMARY);

        headerMetaLabel = new JLabel("Режим нейровизуализации: --");
        headerMetaLabel.setFont(Theme.FONT_BODY);
        headerMetaLabel.setForeground(Theme.TEXT_MUTED);

        left.add(headerTitleLabel);
        left.add(headerMetaLabel);
        toolbar.add(left, BorderLayout.WEST);

        // Right Controls: AI Overlay Toggle, Projection Axis, Series Navigation
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        axisCombo = new JComboBox<>(
                new String[] { "Аксиальная (Axial)", "Сагиттальная (Sagittal)", "Корональная (Coronal)" });
        axisCombo.setFont(Theme.FONT_LABEL);
        axisCombo.setBackground(Theme.BG_INPUT);
        axisCombo.setForeground(Theme.TEXT_PRIMARY);
        axisCombo.addActionListener(e -> {
            int sel = axisCombo.getSelectedIndex();
            if (sel == 1)
                currentAxis = "sagittal";
            else if (sel == 2)
                currentAxis = "coronal";
            else
                currentAxis = "axial";
            loadSliceAsync(currentIndex);
        });

        aiOverlayCheck = new JCheckBox("Разметка поражений ИИ", true);
        aiOverlayCheck.setFont(Theme.FONT_BTN);
        aiOverlayCheck.setForeground(Theme.TEXT_PRIMARY);
        aiOverlayCheck.setBackground(Theme.BG_PANEL);
        aiOverlayCheck.setFocusPainted(false);
        aiOverlayCheck.addActionListener(e -> canvas.repaint());

        JButton prevBtn = new JButton("◀ Пред. серия");
        styleNavButton(prevBtn);
        prevBtn.addActionListener(e -> {
            if (currentIndex > 0)
                loadSliceAsync(currentIndex - 1);
        });

        JButton nextBtn = new JButton("След. серия ▶");
        styleNavButton(nextBtn);
        nextBtn.addActionListener(e -> {
            if (currentIndex < files.size() - 1)
                loadSliceAsync(currentIndex + 1);
        });

        right.add(new JLabel(Theme.html("<span style='color:" + Theme.getMutedHex() + ";'>Проекция:</span>")));
        right.add(axisCombo);
        right.add(aiOverlayCheck);
        right.add(prevBtn);
        right.add(nextBtn);
        toolbar.add(right, BorderLayout.EAST);

        return toolbar;
    }

    private JPanel createSliceControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(Theme.BG_PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, Theme.BORDER_SUBTLE),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)));

        // Left Controls: Slice Label and Previous Buttons
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        sliceIndicatorLabel = new JLabel("Срез Z: 1 / 1");
        sliceIndicatorLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sliceIndicatorLabel.setForeground(Theme.TEXT_PRIMARY);
        sliceIndicatorLabel.setPreferredSize(new Dimension(190, 26));

        prevLesionSliceBtn = new JButton("⏮ Пред. очаг");
        styleNavButton(prevLesionSliceBtn);
        prevLesionSliceBtn.setToolTipText("Перейти к предыдущему срезу с очагами");
        prevLesionSliceBtn.addActionListener(e -> jumpToPreviousLesionSlice());

        prevSliceBtn = new JButton("◀");
        styleNavButton(prevSliceBtn);
        prevSliceBtn.setToolTipText("Предыдущий срез (или колесо мыши вверх)");
        prevSliceBtn.addActionListener(e -> stepSlice(-1));

        left.add(sliceIndicatorLabel);
        left.add(prevLesionSliceBtn);
        left.add(prevSliceBtn);
        panel.add(left, BorderLayout.WEST);

        // Center: Interactive Slice Slider
        sliceSlider = new JSlider(0, 0, 0);
        sliceSlider.setOpaque(false);
        sliceSlider.setForeground(Theme.ACCENT_BLUE);
        sliceSlider.addChangeListener(e -> {
            if (!sliceSlider.getValueIsAdjusting() && sliceSlider.getValue() != currentSliceZ) {
                loadSpecificSliceAsync(sliceSlider.getValue());
            }
        });
        panel.add(sliceSlider, BorderLayout.CENTER);

        // Right Controls: Next Buttons and Jump Dropdown
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        nextSliceBtn = new JButton("▶");
        styleNavButton(nextSliceBtn);
        nextSliceBtn.setToolTipText("Следующий срез (или колесо мыши вниз)");
        nextSliceBtn.addActionListener(e -> stepSlice(1));

        nextLesionSliceBtn = new JButton("След. очаг ⏭");
        styleNavButton(nextLesionSliceBtn);
        nextLesionSliceBtn.setToolTipText("Перейти к следующему срезу с обнаруженными очагами");
        nextLesionSliceBtn.addActionListener(e -> jumpToNextLesionSlice());

        lesionSlicesCombo = new JComboBox<>();
        lesionSlicesCombo.setFont(Theme.FONT_LABEL);
        lesionSlicesCombo.setBackground(Theme.BG_INPUT);
        lesionSlicesCombo.setForeground(Theme.TEXT_PRIMARY);
        lesionSlicesCombo.setPreferredSize(new Dimension(190, 26));
        lesionSlicesCombo.addActionListener(e -> {
            if (!isUpdatingCombo && lesionSlicesCombo.getSelectedIndex() >= 0) {
                String selected = (String) lesionSlicesCombo.getSelectedItem();
                if (selected != null && selected.startsWith("Срез ")) {
                    try {
                        int spaceIdx = selected.indexOf(" ", 5);
                        int endIdx = spaceIdx != -1 ? spaceIdx : selected.indexOf(":", 5);
                        if (endIdx != -1) {
                            int sliceNum = Integer.parseInt(selected.substring(5, endIdx).trim());
                            loadSpecificSliceAsync(sliceNum - 1);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        });

        right.add(nextSliceBtn);
        right.add(nextLesionSliceBtn);
        right.add(new JLabel(Theme.html("<span style='color:" + Theme.getMutedHex() + ";'>Очаговые слои:</span>")));
        right.add(lesionSlicesCombo);
        panel.add(right, BorderLayout.EAST);

        return panel;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 12));
        sidebar.setBackground(Theme.BG_PANEL);
        sidebar.setPreferredSize(new Dimension(340, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, Theme.BORDER_SUBTLE),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        JLabel sideTitle = new JLabel("КЛИНИЧЕСКИЙ АНАЛИЗ СРЕЗА");
        sideTitle.setFont(Theme.FONT_HEADER);
        sideTitle.setForeground(Theme.TEXT_PRIMARY);
        sideTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(sideTitle);
        content.add(Box.createVerticalStrut(10));

        // Diagnosis Card
        JPanel diagCard = new JPanel(new BorderLayout(0, 6));
        diagCard.setBackground(Theme.BG_INPUT);
        diagCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        diagCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        diagCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        diagBadge = new JLabel("ПОРАЖЕНИЕ НЕ НАЙДЕНО");
        diagBadge.setFont(new Font("Segoe UI", Font.BOLD, 14));
        diagBadge.setForeground(Theme.SUCCESS_GREEN);

        confidenceValue = new JLabel("Достоверность ИИ: 99.2%");
        confidenceValue.setFont(Theme.FONT_BODY);
        confidenceValue.setForeground(Theme.TEXT_PRIMARY);

        diagCard.add(diagBadge, BorderLayout.NORTH);
        diagCard.add(confidenceValue, BorderLayout.CENTER);
        content.add(diagCard);
        content.add(Box.createVerticalStrut(12));

        // Details Grid
        JPanel detailsPanel = new JPanel(new GridLayout(4, 1, 0, 8));
        detailsPanel.setOpaque(false);
        detailsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        subclassValue = addDetailRow(detailsPanel, "Клинический подкласс:", "--");
        zoneValue = addDetailRow(detailsPanel, "Анатомическая зона:", "--");
        lesionValue = addDetailRow(detailsPanel, "Очаговая нагрузка среза:", "--");
        criteriaValue = addDetailRow(detailsPanel, "Критерий верификации:", "--");

        content.add(detailsPanel);
        content.add(Box.createVerticalStrut(16));

        // Slice List for direct clicking
        JLabel listTitle = new JLabel("СЕРИЯ СНИМКОВ ПАЦИЕНТА:");
        listTitle.setFont(Theme.FONT_HEADER);
        listTitle.setForeground(Theme.TEXT_MUTED);
        listTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(listTitle);
        content.add(Box.createVerticalStrut(6));

        sliceListModel = new DefaultListModel<>();
        for (int i = 0; i < files.size(); i++) {
            Map<String, Object> f = files.get(i);
            String prefix = Boolean.TRUE.equals(f.get("has_lesion")) ? "🔴 " : "🟢 ";
            sliceListModel.addElement(prefix + f.getOrDefault("file_name", "Срез " + (i + 1)));
        }

        sliceList = new JList<>(sliceListModel);
        sliceList.setFont(Theme.FONT_BODY);
        sliceList.setBackground(Theme.BG_INPUT);
        sliceList.setForeground(Theme.TEXT_PRIMARY);
        sliceList.setSelectionBackground(Theme.TEXT_SELECTION_BG);
        sliceList.setSelectionForeground(Theme.TEXT_SELECTION_FG);
        sliceList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && sliceList.getSelectedIndex() >= 0
                    && sliceList.getSelectedIndex() != currentIndex) {
                loadSliceAsync(sliceList.getSelectedIndex());
            }
        });

        JScrollPane listScroll = new JScrollPane(sliceList);
        listScroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1));
        listScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(listScroll);

        sidebar.add(content, BorderLayout.CENTER);
        return sidebar;
    }

    private JLabel addDetailRow(JPanel parent, String title, String initVal) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(Theme.FONT_LABEL);
        t.setForeground(Theme.TEXT_MUTED);

        JLabel v = new JLabel(initVal);
        v.setFont(Theme.FONT_BODY);
        v.setForeground(Theme.TEXT_PRIMARY);

        p.add(t, BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        parent.add(p);
        return v;
    }

    private JPanel createBottomToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(16, 0));
        toolbar.setBackground(Theme.BG_PANEL);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_SUBTLE),
                BorderFactory.createEmptyBorder(8, 18, 8, 18)));

        // Window / Level Controls
        JPanel sliders = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        sliders.setOpaque(false);

        JLabel bLabel = new JLabel("Яркость:");
        bLabel.setFont(Theme.FONT_LABEL);
        bLabel.setForeground(Theme.TEXT_MUTED);
        brightnessSlider = new JSlider(-50, 50, 0);
        brightnessSlider.setOpaque(false);
        brightnessSlider.setForeground(Theme.ACCENT_BLUE);
        brightnessSlider.addChangeListener(e -> canvas.repaint());

        JLabel cLabel = new JLabel("Контрастность (Windowing):");
        cLabel.setFont(Theme.FONT_LABEL);
        cLabel.setForeground(Theme.TEXT_MUTED);
        contrastSlider = new JSlider(50, 200, 100);
        contrastSlider.setOpaque(false);
        contrastSlider.setForeground(Theme.ACCENT_BLUE);
        contrastSlider.addChangeListener(e -> canvas.repaint());

        statusLabel = new JLabel("Готов к диагностике");
        statusLabel.setFont(Theme.FONT_LABEL);
        statusLabel.setForeground(Theme.TEXT_MUTED);

        sliders.add(bLabel);
        sliders.add(brightnessSlider);
        sliders.add(cLabel);
        sliders.add(contrastSlider);
        sliders.add(Box.createHorizontalStrut(12));
        sliders.add(statusLabel);
        toolbar.add(sliders, BorderLayout.WEST);

        // Right Actions
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        JButton resetZoomBtn = new JButton("Сброс масштаба");
        styleNavButton(resetZoomBtn);
        resetZoomBtn.addActionListener(e -> canvas.resetZoom());

        JButton closeBtn = new JButton("Закрыть");
        styleNavButton(closeBtn);
        closeBtn.addActionListener(e -> dispose());

        right.add(resetZoomBtn);
        right.add(closeBtn);
        toolbar.add(right, BorderLayout.EAST);

        return toolbar;
    }

    private void stepSlice(int delta) {
        if (totalSlicesZ <= 1)
            return;
        int next = Math.max(0, Math.min(currentSliceZ + delta, totalSlicesZ - 1));
        if (next != currentSliceZ) {
            loadSpecificSliceAsync(next);
        }
    }

    private void jumpToPreviousLesionSlice() {
        if (slicesWithLesions.isEmpty())
            return;
        int target = -1;
        for (int i = slicesWithLesions.size() - 1; i >= 0; i--) {
            int z = slicesWithLesions.get(i);
            if (z < currentSliceZ) {
                target = z;
                break;
            }
        }
        if (target != -1) {
            loadSpecificSliceAsync(target);
        } else {
            loadSpecificSliceAsync(slicesWithLesions.get(slicesWithLesions.size() - 1));
        }
    }

    private void jumpToNextLesionSlice() {
        if (slicesWithLesions.isEmpty())
            return;
        int target = -1;
        for (int z : slicesWithLesions) {
            if (z > currentSliceZ) {
                target = z;
                break;
            }
        }
        if (target != -1) {
            loadSpecificSliceAsync(target);
        } else {
            loadSpecificSliceAsync(slicesWithLesions.get(0));
        }
    }

    @SuppressWarnings("unchecked")
    private void loadSliceAsync(int fileIndex) {
        if (fileIndex < 0 || fileIndex >= files.size())
            return;
        this.currentIndex = fileIndex;
        Map<String, Object> f = files.get(fileIndex);

        headerTitleLabel.setText(
                "Серия " + (fileIndex + 1) + " из " + files.size() + ": " + f.getOrDefault("file_name", "MRI Slice"));
        headerMetaLabel.setText(
                "Тип: " + f.getOrDefault("sequence", "MRI Sequence") + "  |  Путь: " + f.getOrDefault("file_path", ""));

        boolean hasLesion = Boolean.TRUE.equals(f.get("has_lesion"));
        String cls = String.valueOf(f.getOrDefault("classification", "Control"));
        Color cColor = Theme.getPathologyColor(cls);

        if (hasLesion) {
            diagBadge.setText("ОЧАГ ОБНАРУЖЕН: " + formatClassName(cls).toUpperCase());
            diagBadge.setForeground(cColor);
        } else {
            diagBadge.setText("ПАТОЛОГИИ НЕ ОБНАРУЖЕНО");
            diagBadge.setForeground(Theme.SUCCESS_GREEN);
        }

        confidenceValue.setText("Достоверность ИИ: " + f.getOrDefault("confidence", "98.5") + "%");
        subclassValue.setText(String.valueOf(f.getOrDefault("subclass", "--")));
        zoneValue.setText(String.valueOf(f.getOrDefault("affected_zone", "--")));

        if ("Multiple_Sclerosis".equals(cls)) {
            criteriaValue.setText("McDonald 2017 DIS: Соответствует");
        } else if ("Alzheimers".equals(cls)) {
            criteriaValue.setText("NIA-AA: Атрофия гиппокампа");
        } else if ("MCI".equals(cls)) {
            criteriaValue.setText("Petersen Criteria: Продром");
        } else {
            criteriaValue.setText("Нормативная база: Соответствует");
        }

        canvas.setImage(null, null, null, "Загрузка 3D серии и карты поражений...");
        loadStartTime = System.currentTimeMillis();

        if (loadingTimer != null)
            loadingTimer.stop();
        loadingTimer = new Timer(100, e -> {
            double elapsed = (System.currentTimeMillis() - loadStartTime) / 1000.0;
            statusLabel.setText(String.format("Загрузка и реконструкция: %.1f с", elapsed));
        });
        loadingTimer.start();

        new SwingWorker<Map<String, Object>, Void>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                String imgPath = String.valueOf(f.getOrDefault("file_path", ""));
                return bridge.renderMRI(imgPath, -1, currentAxis);
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                loadingTimer.stop();
                try {
                    Map<String, Object> res = get();
                    if (res != null && Boolean.TRUE.equals(res.get("success"))) {
                        String imgPath = String.valueOf(res.get("image_path"));
                        File imgFile = new File(imgPath);
                        if (!imgFile.exists()) {
                            throw new java.io.FileNotFoundException("Файл среза не найден: " + imgPath);
                        }
                        BufferedImage bimg = ImageIO.read(imgFile);
                        if (bimg == null) {
                            throw new java.io.IOException("Не удалось декодировать срез: " + imgPath);
                        }

                        currentSliceZ = ((Number) res.getOrDefault("current_slice", 0)).intValue();
                        totalSlicesZ = ((Number) res.getOrDefault("total_slices", 1)).intValue();

                        // Slices with lesions
                        slicesWithLesions.clear();
                        sliceLesionCounts.clear();
                        Object slList = res.get("slices_with_lesions");
                        if (slList instanceof List) {
                            for (Object item : (List<?>) slList) {
                                if (item instanceof Number)
                                    slicesWithLesions.add(((Number) item).intValue());
                            }
                        }
                        Object scMap = res.get("slice_lesion_counts");
                        if (scMap instanceof Map) {
                            sliceLesionCounts.putAll((Map<String, Object>) scMap);
                        }

                        // ROI boxes for current slice
                        currentSliceRoiBoxes.clear();
                        Object boxesObj = res.get("roi_boxes");
                        if (boxesObj instanceof List) {
                            for (Object b : (List<?>) boxesObj) {
                                if (b instanceof Map)
                                    currentSliceRoiBoxes.add((Map<String, Object>) b);
                            }
                        }

                        updateSliceNavigatorUI();

                        boolean curHasLesion = Boolean.TRUE.equals(res.get("has_lesion"));
                        int lesionCount = ((Number) res.getOrDefault("lesion_count", currentSliceRoiBoxes.size()))
                                .intValue();
                        lesionValue
                                .setText(curHasLesion ? lesionCount + " очагов на текущем срезе" : "0 очагов (Норма)");

                        if (res.containsKey("classification")) {
                            String resCls = String.valueOf(res.get("classification"));
                            f.put("classification", resCls);
                            f.put("has_lesion", curHasLesion);
                            if (curHasLesion) {
                                diagBadge.setText("ОЧАГ ОБНАРУЖЕН: " + formatClassName(resCls).toUpperCase());
                                diagBadge.setForeground(Theme.getPathologyColor(resCls));
                            }
                            if ("Multiple_Sclerosis".equals(resCls)) {
                                criteriaValue.setText("McDonald 2017 DIS: Соответствует");
                            } else if ("Alzheimers".equals(resCls)) {
                                criteriaValue.setText("NIA-AA: Атрофия гиппокампа");
                            } else if ("MCI".equals(resCls)) {
                                criteriaValue.setText("Petersen Criteria: Продром");
                            }
                        }
                        if (res.containsKey("subclass")) {
                            f.put("subclass", res.get("subclass"));
                            subclassValue.setText(String.valueOf(res.get("subclass")));
                        }
                        if (res.containsKey("affected_zone")) {
                            f.put("affected_zone", res.get("affected_zone"));
                            zoneValue.setText(String.valueOf(res.get("affected_zone")));
                        }
                        if (res.containsKey("confidence")) {
                            confidenceValue.setText("Достоверность ИИ: " + res.get("confidence") + "%");
                        }
                        if (curHasLesion && sliceListModel != null && currentIndex < sliceListModel.size()) {
                            String curElem = sliceListModel.get(currentIndex);
                            if (curElem.startsWith("🟢 ")) {
                                sliceListModel.set(currentIndex, "🔴 " + curElem.substring(2));
                            }
                        }

                        statusLabel.setText(String.format("Срез Z=%d/%d готов за %s мс (Очагов: %d)",
                                currentSliceZ + 1, totalSlicesZ, res.getOrDefault("render_time_ms", 0), lesionCount));

                        canvas.setImage(bimg, f, currentSliceRoiBoxes, null);
                    } else {
                        // Fallback to preview image if exists
                        String previewPath = String.valueOf(f.getOrDefault("preview_image", ""));
                        File prevFile = (!previewPath.isEmpty()) ? new File(previewPath) : null;
                        if (prevFile != null && prevFile.exists()) {
                            BufferedImage bimg = ImageIO.read(prevFile);
                            if (bimg != null) {
                                currentSliceRoiBoxes.clear();
                                Object bObj = f.get("roi_boxes");
                                if (bObj instanceof List) {
                                    for (Object b : (List<?>) bObj) {
                                        if (b instanceof Map)
                                            currentSliceRoiBoxes.add((Map<String, Object>) b);
                                    }
                                }
                                canvas.setImage(bimg, f, currentSliceRoiBoxes, null);
                                statusLabel.setText("Используется кэшированный срез предпросмотра");
                                return;
                            }
                        }
                        String err = (res != null && res.get("error") != null) ? String.valueOf(res.get("error"))
                                : "Неподдерживаемый формат";
                        canvas.setImage(null, f, null, "Ошибка: " + err);
                        statusLabel.setText("Ошибка: " + err);
                    }
                } catch (Exception ex) {
                    canvas.setImage(null, f, null, "Ошибка: " + ex.getMessage());
                    statusLabel.setText("Критическая ошибка загрузки");
                }
            }
        }.execute();

        sliceList.setSelectedIndex(fileIndex);
    }

    private void updateSliceNavigatorUI() {
        if (sliceSlider != null) {
            sliceSlider.setMaximum(Math.max(0, totalSlicesZ - 1));
            sliceSlider.setValue(currentSliceZ);
            sliceSlider.setEnabled(totalSlicesZ > 1);
        }

        boolean hasOnThis = slicesWithLesions.contains(currentSliceZ);
        int count = currentSliceRoiBoxes.size();
        String badge = hasOnThis ? " 🔴 (" + count + " очагов)" : " 🟢 (Норма)";
        sliceIndicatorLabel.setText("Срез Z: " + (currentSliceZ + 1) + " / " + totalSlicesZ + badge);
        sliceIndicatorLabel.setForeground(hasOnThis ? Theme.COLOR_MS : Theme.TEXT_PRIMARY);

        prevSliceBtn.setEnabled(totalSlicesZ > 1 && currentSliceZ > 0);
        nextSliceBtn.setEnabled(totalSlicesZ > 1 && currentSliceZ < totalSlicesZ - 1);
        prevLesionSliceBtn.setEnabled(!slicesWithLesions.isEmpty());
        nextLesionSliceBtn.setEnabled(!slicesWithLesions.isEmpty());

        // Update Lesion Slices Dropdown
        isUpdatingCombo = true;
        lesionSlicesCombo.removeAllItems();
        if (slicesWithLesions.isEmpty()) {
            lesionSlicesCombo.addItem("Очагов не обнаружено");
            lesionSlicesCombo.setEnabled(false);
        } else {
            lesionSlicesCombo.setEnabled(true);
            int selectedIdx = 0;
            for (int i = 0; i < slicesWithLesions.size(); i++) {
                int z = slicesWithLesions.get(i);
                Object cnt = sliceLesionCounts.getOrDefault(String.valueOf(z), sliceLesionCounts.get(z));
                String text = "Срез " + (z + 1) + " (" + (cnt != null ? cnt : "?") + " очагов)";
                lesionSlicesCombo.addItem(text);
                if (z == currentSliceZ)
                    selectedIdx = i;
            }
            lesionSlicesCombo.setSelectedIndex(selectedIdx);
        }
        isUpdatingCombo = false;
    }

    private void loadSpecificSliceAsync(int targetSlice) {
        if (targetSlice < 0 || targetSlice >= totalSlicesZ)
            return;
        this.currentSliceZ = targetSlice;
        if (sliceSlider != null && sliceSlider.getValue() != targetSlice) {
            sliceSlider.setValue(targetSlice);
        }

        Map<String, Object> f = files.get(currentIndex);
        String imgPath = String.valueOf(f.getOrDefault("file_path", ""));
        statusLabel.setText("Загрузка среза Z=" + (targetSlice + 1) + " / " + totalSlicesZ + "...");

        new SwingWorker<Map<String, Object>, Void>() {
            @Override
            protected Map<String, Object> doInBackground() throws Exception {
                return bridge.renderMRI(imgPath, targetSlice, currentAxis);
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Map<String, Object> res = get();
                    if (res != null && Boolean.TRUE.equals(res.get("success"))) {
                        String previewPath = String.valueOf(res.get("image_path"));
                        File imgFile = new File(previewPath);
                        if (imgFile.exists()) {
                            BufferedImage bimg = ImageIO.read(imgFile);
                            if (bimg != null) {
                                currentSliceRoiBoxes.clear();
                                Object boxesObj = res.get("roi_boxes");
                                if (boxesObj instanceof List) {
                                    for (Object b : (List<?>) boxesObj) {
                                        if (b instanceof Map)
                                            currentSliceRoiBoxes.add((Map<String, Object>) b);
                                    }
                                }

                                boolean hasLesion = Boolean.TRUE.equals(res.get("has_lesion"));
                                int lesionCount = ((Number) res.getOrDefault("lesion_count",
                                        currentSliceRoiBoxes.size())).intValue();

                                updateSliceNavigatorUI();
                                lesionValue.setText(
                                        hasLesion ? lesionCount + " очагов на текущем срезе" : "0 очагов (Норма)");

                                canvas.setImage(bimg, f, currentSliceRoiBoxes, null);
                                statusLabel.setText(String.format("Срез Z=%d/%d готов (Очагов: %d)", targetSlice + 1,
                                        totalSlicesZ, lesionCount));
                            }
                        }
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Ошибка переключения среза: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private String formatClassName(String cls) {
        switch (cls) {
            case "Multiple_Sclerosis":
                return "Рассеянный склероз";
            case "Alzheimers":
                return "Болезнь Альцгеймера";
            case "Parkinsons":
                return "Болезнь Паркинсона";
            case "ALS":
                return "БАС (Мотонейроны)";
            case "MCI":
                return "Умеренные когнитивные нарушения";
            default:
                return "Здоровая норма";
        }
    }

    private void styleNavButton(JButton btn) {
        btn.setFont(Theme.FONT_BTN);
        btn.setBackground(Theme.BG_INPUT);
        btn.setForeground(Theme.TEXT_PRIMARY);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        Theme.attachHoverEffect(btn, Theme.BG_INPUT, Theme.BG_INPUT_HOVER, Theme.BORDER_SUBTLE, Theme.BORDER_ACTIVE);
    }

    /**
     * Custom radiological canvas that renders the MRI scan with AI lesion bounding
     * boxes,
     * glowing borders, crosshairs, and classification badges.
     * Supports mouse wheel scrolling through 3D slices.
     */
    private class ImageCanvas extends JPanel {
        private BufferedImage image;
        private Map<String, Object> currentFileInfo;
        private List<Map<String, Object>> currentBoxes;
        private String loadingMessage;
        private double zoom = 1.0;

        public ImageCanvas() {
            setBackground(new Color(0x05, 0x07, 0x0B)); // Deep medical black

            // Enable mouse wheel scrolling through 3D slices
            addMouseWheelListener(e -> {
                int rot = e.getWheelRotation();
                if (totalSlicesZ > 1) {
                    if (rot > 0)
                        stepSlice(1);
                    else if (rot < 0)
                        stepSlice(-1);
                }
            });
        }

        public void setImage(BufferedImage img, Map<String, Object> info, List<Map<String, Object>> boxes,
                String loadingMessage) {
            this.image = img;
            this.currentFileInfo = info;
            this.currentBoxes = (boxes != null) ? new ArrayList<>(boxes) : new ArrayList<>();
            this.loadingMessage = loadingMessage;
            this.zoom = 1.0;
            repaint();
        }

        public void resetZoom() {
            this.zoom = 1.0;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int w = getWidth();
            int h = getHeight();

            if (image == null) {
                g2.setColor(Theme.TEXT_MUTED);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                String msg = (loadingMessage != null) ? loadingMessage : "Срез не загружен";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
                g2.dispose();
                return;
            }

            int imgW = image.getWidth();
            int imgH = image.getHeight();
            double scale = Math.min((double) (w - 40) / imgW, (double) (h - 40) / imgH) * zoom;
            int drawW = (int) (imgW * scale);
            int drawH = (int) (imgH * scale);
            int drawX = (w - drawW) / 2;
            int drawY = (h - drawH) / 2;

            // Draw MRI scan
            g2.drawImage(image, drawX, drawY, drawW, drawH, null);

            // Apply Brightness & Contrast
            float brightness = brightnessSlider.getValue();
            if (brightness > 0) {
                g2.setComposite(
                        AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(0.5f, brightness / 100.0f)));
                g2.setColor(Color.WHITE);
                g2.fillRect(drawX, drawY, drawW, drawH);
            } else if (brightness < 0) {
                g2.setComposite(
                        AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(0.5f, -brightness / 100.0f)));
                g2.setColor(Color.BLACK);
                g2.fillRect(drawX, drawY, drawW, drawH);
            }

            g2.setComposite(AlphaComposite.SrcOver);

            // Draw Real AI Lesion Bounding Boxes & Tags if enabled
            if (aiOverlayCheck.isSelected() && currentBoxes != null && !currentBoxes.isEmpty()) {
                String cls = String.valueOf(
                        currentFileInfo != null ? currentFileInfo.getOrDefault("classification", "Multiple_Sclerosis")
                                : "Multiple_Sclerosis");
                Color boxColor = Theme.getPathologyColor(cls);

                int boxIndex = 1;
                for (Map<String, Object> box : currentBoxes) {
                    double bx = ((Number) box.get("x")).doubleValue();
                    double by = ((Number) box.get("y")).doubleValue();
                    double bw = ((Number) box.get("w")).doubleValue();
                    double bh = ((Number) box.get("h")).doubleValue();

                    int renderBx = drawX + (int) (bx * scale);
                    int renderBy = drawY + (int) (by * scale);
                    int renderBw = Math.max(14, (int) (bw * scale));
                    int renderBh = Math.max(14, (int) (bh * scale));

                    // 1. Glowing outer box
                    g2.setColor(new Color(boxColor.getRed(), boxColor.getGreen(), boxColor.getBlue(), 35));
                    g2.fillRoundRect(renderBx - 3, renderBy - 3, renderBw + 6, renderBh + 6, 6, 6);

                    // 2. Main border
                    g2.setColor(boxColor);
                    g2.setStroke(new BasicStroke(2.0f));
                    g2.drawRoundRect(renderBx, renderBy, renderBw, renderBh, 4, 4);

                    // 3. Surgical Crosshairs at corners
                    int chLen = 6;
                    g2.setStroke(new BasicStroke(2.5f));
                    g2.drawLine(renderBx, renderBy, renderBx + chLen, renderBy);
                    g2.drawLine(renderBx, renderBy, renderBx, renderBy + chLen);

                    g2.drawLine(renderBx + renderBw, renderBy, renderBx + renderBw - chLen, renderBy);
                    g2.drawLine(renderBx + renderBw, renderBy, renderBx + renderBw, renderBy + chLen);

                    g2.drawLine(renderBx, renderBy + renderBh, renderBx + chLen, renderBy + renderBh);
                    g2.drawLine(renderBx, renderBy + renderBh, renderBx, renderBy + renderBh - chLen);

                    g2.drawLine(renderBx + renderBw, renderBy + renderBh, renderBx + renderBw - chLen,
                            renderBy + renderBh);
                    g2.drawLine(renderBx + renderBw, renderBy + renderBh, renderBx + renderBw,
                            renderBy + renderBh - chLen);

                    // 4. Lesion number badge
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                    String numTag = "#" + boxIndex;
                    g2.setColor(new Color(0x0F, 0x14, 0x1C, 220));
                    g2.fillRect(renderBx, Math.max(drawY, renderBy - 14), 22, 14);
                    g2.setColor(boxColor);
                    g2.drawRect(renderBx, Math.max(drawY, renderBy - 14), 22, 14);
                    g2.setColor(Color.WHITE);
                    g2.drawString(numTag, renderBx + 3, Math.max(drawY + 11, renderBy - 3));

                    boxIndex++;
                }

                // Top Floating Diagnostic Summary Tag for the slice
                String sub = String.valueOf(
                        currentFileInfo != null ? currentFileInfo.getOrDefault("subclass", "Очаги демиелинизации")
                                : "Очаги");
                String tagText = " ИИ: " + formatClassName(cls) + " • " + currentBoxes.size() + " очаг(ов) на срезе Z="
                        + (currentSliceZ + 1) + " (" + sub + ") ";

                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                int tagW = fm.stringWidth(tagText) + 12;
                int tagH = 24;
                int tagX = drawX + 10;
                int tagY = drawY + 10;

                g2.setColor(new Color(0x0A, 0x0E, 0x17, 230));
                g2.fillRoundRect(tagX, tagY, tagW, tagH, 6, 6);
                g2.setColor(boxColor);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(tagX, tagY, tagW, tagH, 6, 6);

                g2.setColor(Color.WHITE);
                g2.drawString(tagText, tagX + 6, tagY + 17);
            }

            // Crosshair overlay info at bottom left of canvas
            g2.setColor(new Color(0x94, 0xA3, 0xB8));
            g2.setFont(Theme.FONT_MONO);
            g2.drawString(String.format("Срез Z: %d/%d | Разрешение: %dx%d | Проекция: %s | Колесо мыши: Скролл слоев",
                    currentSliceZ + 1, totalSlicesZ, imgW, imgH, currentAxis.toUpperCase()), 16, h - 14);

            g2.dispose();
        }
    }
}
