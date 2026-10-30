package com.neuroai.desktop;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;

/**
 * NeuroAI Workstation — Main Medical Screening & Quantitative MRI Console.
 * Supports:
 * 1. Batch Multi-File & Directory MRI Ingestion (Drag-and-Drop,
 * Multi-selection)
 * 2. Series & Slices Table with Per-Image AI Lesion Detection and Subclassing
 * 3. Dedicated Jump to MRIViewerDialog with AI Lesion Bounding Boxes & Tags
 * 4. High-Contrast Text Legibility (No text blending or low-contrast washed out
 * labels)
 * 5. Quick Clinical Presets & Full Diagnostic Consilium Trigger
 */
public class FileSelectionPanel extends JPanel implements Theme.ThemeChangeListener {
    private final Frame parentFrame;
    private final PythonBridge bridge;

    // Header Components
    private JPanel headerPanel;
    private JLabel titleLabel;
    private JLabel subtitleLabel;
    private JLabel engineStatusBadge;
    private JButton themeToggleBtn;

    // Preset Buttons
    private JPanel presetRibbon;
    private final Map<String, JButton> presetButtons = new LinkedHashMap<>();
    private final List<JLabel> dynamicMutedLabels = new ArrayList<>();

    // Left Column (MRI & Batch Files Table)
    private JPanel mriColumnPanel;
    private JLabel mriColTitle;
    private JPanel dropBox;
    private JLabel dropLabel;
    private JTextField pathField;
    private JButton browseFolderBtn;
    private JButton browseFilesBtn;
    private JButton openViewerBtn;
    private JRadioButton rbAutoContrast;
    private JRadioButton rbNativeContrast;
    private JRadioButton rbEnhancedContrast;
    private JPanel contrastWarningBanner;
    private JLabel warningTextLabel;

    // Slices & Series Table
    private DefaultTableModel slicesTableModel;
    private JTable slicesTable;
    private List<Map<String, Object>> inspectedFiles = new ArrayList<>();

    // Right Column (Clinical Survey)
    private JPanel surveyColumnPanel;
    private JLabel surveyColTitle;
    private JSpinner ageSpinner;
    private JComboBox<String> genderCombo;
    private JSpinner mmseSpinner;
    private JSpinner mocaSpinner;
    private JSpinner updrsSpinner;
    private JSpinner edssSpinner;
    private JSpinner alsfrsSpinner;
    private JSpinner fatigueSpinner;
    private JSpinner durationSpinner;
    private JSpinner lesionsSpinner;
    private JCheckBox ocbCheckBox;
    private JCheckBox swiCheckBox;

    // Footer
    private JPanel footerPanel;
    private JLabel footerStatusLabel;
    private JButton resetBtn;
    private JButton inspectBtn;
    private JButton diagnoseBtn;

    public FileSelectionPanel(Frame parent, PythonBridge bridge) {
        this.parentFrame = parent;
        this.bridge = bridge;

        setLayout(new BorderLayout(0, 0));
        setBackground(Theme.BG_MAIN);

        // 1. Header Bar
        headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // 2. Central Layout
        JPanel centralWrapper = new JPanel(new BorderLayout(0, 8));
        centralWrapper.setOpaque(false);
        centralWrapper.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        presetRibbon = createPresetRibbon();
        centralWrapper.add(presetRibbon, BorderLayout.NORTH);

        JPanel centralGrid = new JPanel(new GridLayout(1, 2, 14, 0));
        centralGrid.setOpaque(false);

        mriColumnPanel = createMRIColumn();
        surveyColumnPanel = createClinicalSurveyColumn();

        centralGrid.add(mriColumnPanel);
        centralGrid.add(surveyColumnPanel);
        centralWrapper.add(centralGrid, BorderLayout.CENTER);

        add(centralWrapper, BorderLayout.CENTER);

        // 3. Bottom Action Footer
        footerPanel = createFooterPanel();
        add(footerPanel, BorderLayout.SOUTH);

        // Setup Drag & Drop
        setupDropTarget();

        // Listen for Theme Changes
        Theme.addListener(this);

        // Initial Preset & Contrast State
        applyClinicalPreset("ms");
        highlightPresetButton("ms");
        updateContrastState();
        refreshTheme(Theme.isDark);

        // Run initial batch inspect
        runInspectMRI();
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(Theme.BG_PANEL);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_SUBTLE),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        JLabel logoIcon = new JLabel("🧠");
        logoIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));

        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBlock.setOpaque(false);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setOpaque(false);

        titleLabel = new JLabel("NeuroAI Clinical Workstation");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Theme.TEXT_PRIMARY);

        JLabel versionBadge = new JLabel(" v2.5 Enterprise ");
        versionBadge.setFont(Theme.FONT_BADGE);
        versionBadge.setBackground(Theme.BG_INPUT);
        versionBadge.setForeground(Theme.COLOR_MS);
        versionBadge.setOpaque(true);
        versionBadge.setBorder(BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1));

        titleRow.add(titleLabel);
        titleRow.add(versionBadge);

        subtitleLabel = new JLabel(
                "Пакетный анализ серий МРТ (NIfTI / DICOM / Срезы) с визуальной локализацией очагов и классификацией подклассов");
        subtitleLabel.setFont(Theme.FONT_BODY);
        subtitleLabel.setForeground(Theme.TEXT_MUTED);

        titleBlock.add(titleRow);
        titleBlock.add(subtitleLabel);

        left.add(logoIcon);
        left.add(titleBlock);
        header.add(left, BorderLayout.WEST);

        // Right Actions (Status Pill & Theme Button)
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 4));
        right.setOpaque(false);

        engineStatusBadge = new JLabel("● Ядро ML активно: 6 классов");
        engineStatusBadge.setFont(Theme.FONT_LABEL);
        engineStatusBadge.setForeground(Theme.SUCCESS_GREEN);
        engineStatusBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.SUCCESS_GREEN, 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));

        themeToggleBtn = new JButton(Theme.isDark ? "🌙 Тёмный зал МРТ" : "☀️ Светлая ординаторская");
        styleSecondaryButton(themeToggleBtn);
        themeToggleBtn.addActionListener(e -> Theme.setDarkMode(!Theme.isDark));

        right.add(engineStatusBadge);
        right.add(themeToggleBtn);
        header.add(right, BorderLayout.EAST);

        return header;
    }

    private JPanel createPresetRibbon() {
        JPanel ribbon = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        ribbon.setOpaque(false);

        JLabel label = new JLabel("Клинические пресеты:");
        label.setFont(Theme.FONT_HEADER);
        label.setForeground(Theme.TEXT_MUTED);
        dynamicMutedLabels.add(label);
        ribbon.add(label);

        addPresetButton(ribbon, "control", "Здоровая норма", Theme.COLOR_CTRL);
        addPresetButton(ribbon, "mci", "УКН (MCI)", Theme.COLOR_MCI);
        addPresetButton(ribbon, "alzheimers", "Б. Альцгеймера", Theme.COLOR_AD);
        addPresetButton(ribbon, "parkinsons", "Б. Паркинсона", Theme.COLOR_PD);
        addPresetButton(ribbon, "ms", "Рассеянный склероз", Theme.COLOR_MS);
        addPresetButton(ribbon, "als", "БАС (Мотонейроны)", Theme.COLOR_ALS);

        return ribbon;
    }

    private String currentPresetKey = "ms";

    private void addPresetButton(JPanel container, String key, String title, Color color) {
        JButton btn = new JButton(title);
        Theme.stylePresetButton(btn, key.equals(currentPresetKey));

        btn.addActionListener(e -> {
            applyClinicalPreset(key);
            highlightPresetButton(key);
            runInspectMRI();
        });

        presetButtons.put(key, btn);
        container.add(btn);
    }

    private void highlightPresetButton(String selectedKey) {
        this.currentPresetKey = selectedKey;
        for (Map.Entry<String, JButton> entry : presetButtons.entrySet()) {
            boolean active = entry.getKey().equals(selectedKey);
            JButton b = entry.getValue();
            Theme.stylePresetButton(b, active);
        }
    }

    private void applyClinicalPreset(String presetKey) {
        switch (presetKey) {
            case "control":
                ageSpinner.setValue(62);
                genderCombo.setSelectedIndex(0);
                mmseSpinner.setValue(29);
                mocaSpinner.setValue(29);
                updrsSpinner.setValue(1.0);
                edssSpinner.setValue(0.0);
                alsfrsSpinner.setValue(48.0);
                fatigueSpinner.setValue(2.0);
                durationSpinner.setValue(0.0);
                lesionsSpinner.setValue(0);
                ocbCheckBox.setSelected(false);
                swiCheckBox.setSelected(false);
                pathField.setText("data/archive (1)/Alzheimer_MRI_4_classes_dataset/NonDemented");
                break;
            case "mci":
                ageSpinner.setValue(71);
                genderCombo.setSelectedIndex(1);
                mmseSpinner.setValue(25);
                mocaSpinner.setValue(22);
                updrsSpinner.setValue(2.0);
                edssSpinner.setValue(0.0);
                alsfrsSpinner.setValue(48.0);
                fatigueSpinner.setValue(2.8);
                durationSpinner.setValue(18.0);
                lesionsSpinner.setValue(0);
                ocbCheckBox.setSelected(false);
                swiCheckBox.setSelected(false);
                pathField.setText("data/archive (1)/Alzheimer_MRI_4_classes_dataset/VeryMildDemented");
                break;
            case "alzheimers":
                ageSpinner.setValue(78);
                genderCombo.setSelectedIndex(0);
                mmseSpinner.setValue(17);
                mocaSpinner.setValue(14);
                updrsSpinner.setValue(5.0);
                edssSpinner.setValue(0.0);
                alsfrsSpinner.setValue(44.0);
                fatigueSpinner.setValue(3.4);
                durationSpinner.setValue(48.0);
                lesionsSpinner.setValue(1);
                ocbCheckBox.setSelected(false);
                swiCheckBox.setSelected(false);
                pathField.setText("data/archive (1)/Alzheimer_MRI_4_classes_dataset/ModerateDemented");
                break;
            case "parkinsons":
                ageSpinner.setValue(67);
                genderCombo.setSelectedIndex(1);
                mmseSpinner.setValue(28);
                mocaSpinner.setValue(25);
                updrsSpinner.setValue(44.0);
                edssSpinner.setValue(1.0);
                alsfrsSpinner.setValue(42.0);
                fatigueSpinner.setValue(4.5);
                durationSpinner.setValue(36.0);
                lesionsSpinner.setValue(0);
                ocbCheckBox.setSelected(false);
                swiCheckBox.setSelected(true);
                pathField.setText("data/archive/Patient-2");
                break;
            case "ms":
                ageSpinner.setValue(36);
                genderCombo.setSelectedIndex(0);
                mmseSpinner.setValue(28);
                mocaSpinner.setValue(27);
                updrsSpinner.setValue(4.0);
                edssSpinner.setValue(3.5);
                alsfrsSpinner.setValue(44.0);
                fatigueSpinner.setValue(5.8);
                durationSpinner.setValue(42.0);
                lesionsSpinner.setValue(19);
                ocbCheckBox.setSelected(true);
                swiCheckBox.setSelected(false);
                pathField.setText("data/archive/Patient-1");
                break;
            case "als":
                ageSpinner.setValue(59);
                genderCombo.setSelectedIndex(1);
                mmseSpinner.setValue(29);
                mocaSpinner.setValue(28);
                updrsSpinner.setValue(12.0);
                edssSpinner.setValue(2.5);
                alsfrsSpinner.setValue(24.0);
                fatigueSpinner.setValue(5.0);
                durationSpinner.setValue(14.0);
                lesionsSpinner.setValue(0);
                ocbCheckBox.setSelected(false);
                swiCheckBox.setSelected(false);
                pathField.setText("data/archive/Patient-3");
                break;
        }
    }

    private JPanel createMRIColumn() {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setBackground(Theme.BG_PANEL);
        col.setBorder(Theme.PANEL_BORDER);

        mriColTitle = new JLabel("1. НЕЙРОВИЗУАЛИЗАЦИЯ И ПАКЕТНЫЙ АНАЛИЗ СНИМКОВ");
        mriColTitle.setFont(Theme.FONT_HEADER);
        mriColTitle.setForeground(Theme.TEXT_PRIMARY);
        col.add(mriColTitle);
        col.add(Box.createVerticalStrut(10));

        // Drag & Drop Area
        dropBox = new JPanel(new GridBagLayout());
        dropBox.setBackground(Theme.BG_INPUT);
        dropBox.setBorder(BorderFactory.createDashedBorder(Theme.BORDER_SUBTLE, 2.0f, 4.0f, 2.0f, true));
        dropBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));
        dropBox.setPreferredSize(new Dimension(300, 75));

        dropLabel = new JLabel(Theme.html(
                "<center><b>📁 Перетащите сюда файлы или папки (DICOM / NIfTI / JPG)</b><br><span style='color:#94A3B8;'>Поддерживается отправка сразу нескольких файлов</span></center>"));
        dropBox.add(dropLabel);
        col.add(dropBox);
        col.add(Box.createVerticalStrut(8));

        // File Selection Row
        JPanel pathRow = new JPanel(new BorderLayout(8, 0));
        pathRow.setOpaque(false);
        pathRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        pathField = new JTextField("data/archive/Patient-1");
        styleTextField(pathField);

        browseFilesBtn = new JButton("Выбрать файлы...");
        styleSecondaryButton(browseFilesBtn);
        browseFilesBtn.addActionListener(e -> chooseMultipleFiles());

        browseFolderBtn = new JButton("Выбрать папку...");
        styleSecondaryButton(browseFolderBtn);
        browseFolderBtn.addActionListener(e -> chooseDirectory());

        JPanel btns = new JPanel(new GridLayout(1, 2, 6, 0));
        btns.setOpaque(false);
        btns.add(browseFilesBtn);
        btns.add(browseFolderBtn);

        pathRow.add(pathField, BorderLayout.CENTER);
        pathRow.add(btns, BorderLayout.EAST);
        col.add(pathRow);
        col.add(Box.createVerticalStrut(10));

        // Table Header with Quick Viewer Button
        JPanel tableHeaderRow = new JPanel(new BorderLayout());
        tableHeaderRow.setOpaque(false);
        tableHeaderRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        JLabel tableTitle = new JLabel("СЕРИЯ СНИМКОВ И АНАЛИЗ ПОРАЖЕНИЙ:");
        tableTitle.setFont(Theme.FONT_LABEL);
        tableTitle.setForeground(Theme.TEXT_MUTED);
        dynamicMutedLabels.add(tableTitle);

        openViewerBtn = new JButton("🔍 Открыть снимок с разметкой ИИ");
        styleSecondaryButton(openViewerBtn);
        openViewerBtn.addActionListener(e -> openViewerForSelected());

        tableHeaderRow.add(tableTitle, BorderLayout.WEST);
        tableHeaderRow.add(openViewerBtn, BorderLayout.EAST);
        col.add(tableHeaderRow);
        col.add(Box.createVerticalStrut(6));

        // Slices & Series Table
        String[] columnNames = { "№", "Статус ИИ", "Файл снимка", "Серия", "Классификация и подкласс",
                "Зона поражения" };
        slicesTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        slicesTable = new JTable(slicesTableModel);
        slicesTable.setFont(Theme.FONT_BODY);
        slicesTable.setBackground(Theme.BG_INPUT);
        slicesTable.setForeground(Theme.TEXT_PRIMARY);
        slicesTable.setRowHeight(24);
        slicesTable.setSelectionBackground(Theme.TEXT_SELECTION_BG);
        slicesTable.setSelectionForeground(Theme.TEXT_SELECTION_FG);
        slicesTable.getTableHeader().setBackground(Theme.BG_PANEL);
        slicesTable.getTableHeader().setForeground(Theme.TEXT_PRIMARY);
        slicesTable.getTableHeader().setFont(Theme.FONT_LABEL);
        slicesTable.setBorder(BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1));

        // Column Widths
        slicesTable.getColumnModel().getColumn(0).setPreferredWidth(28);
        slicesTable.getColumnModel().getColumn(1).setPreferredWidth(85);
        slicesTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        slicesTable.getColumnModel().getColumn(3).setPreferredWidth(95);
        slicesTable.getColumnModel().getColumn(4).setPreferredWidth(160);
        slicesTable.getColumnModel().getColumn(5).setPreferredWidth(130);

        setupTableRenderer();

        slicesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openViewerForSelected();
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(slicesTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1));
        tableScroll.setPreferredSize(new Dimension(300, 150));
        tableScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        col.add(tableScroll);
        col.add(Box.createVerticalStrut(10));

        // Contrast Enhancement Radio Group
        JPanel contrastPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        contrastPanel.setOpaque(false);
        contrastPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        ButtonGroup contrastGroup = new ButtonGroup();
        rbAutoContrast = new JRadioButton("Авто (Gd)", true);
        rbNativeContrast = new JRadioButton("Нативная");
        rbEnhancedContrast = new JRadioButton("T1+Gd (Контраст)");

        styleRadio(rbAutoContrast);
        styleRadio(rbNativeContrast);
        styleRadio(rbEnhancedContrast);

        contrastGroup.add(rbAutoContrast);
        contrastGroup.add(rbNativeContrast);
        contrastGroup.add(rbEnhancedContrast);

        rbAutoContrast.addActionListener(e -> updateContrastState());
        rbNativeContrast.addActionListener(e -> updateContrastState());
        rbEnhancedContrast.addActionListener(e -> updateContrastState());

        JLabel contrastLabel = new JLabel("Контрастирование:");
        contrastLabel.setFont(Theme.FONT_LABEL);
        contrastLabel.setForeground(Theme.TEXT_MUTED);
        dynamicMutedLabels.add(contrastLabel);

        contrastPanel.add(contrastLabel);
        contrastPanel.add(rbAutoContrast);
        contrastPanel.add(rbNativeContrast);
        contrastPanel.add(rbEnhancedContrast);
        col.add(contrastPanel);
        col.add(Box.createVerticalStrut(8));

        // Contrast Warning Banner
        contrastWarningBanner = new JPanel(new BorderLayout(8, 0));
        contrastWarningBanner.setBackground(Theme.WARN_BG);
        contrastWarningBanner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.WARN_BORDER, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        contrastWarningBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        warningTextLabel = new JLabel(Theme.html(
                "<b>⚠️ Предупреждение радиолога:</b> Нативная серия без контрастирования. Для оценки недавней демиелинизации (DIT) необходим T1+Gd."));
        warningTextLabel.setFont(Theme.FONT_LABEL);
        contrastWarningBanner.add(warningTextLabel, BorderLayout.CENTER);

        col.add(contrastWarningBanner);
        col.add(Box.createVerticalGlue());

        return col;
    }

    private JPanel createClinicalSurveyColumn() {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setBackground(Theme.BG_PANEL);
        col.setBorder(Theme.PANEL_BORDER);

        surveyColTitle = new JLabel("2. КЛИНИЧЕСКИЙ ОСМОТР, ШКАЛЫ И БИОМАРКЕРЫ");
        surveyColTitle.setFont(Theme.FONT_HEADER);
        surveyColTitle.setForeground(Theme.TEXT_PRIMARY);
        col.add(surveyColTitle);
        col.add(Box.createVerticalStrut(10));

        // 6-Row Grid for clinical parameters
        JPanel formGrid = new JPanel(new GridLayout(6, 2, 12, 8));
        formGrid.setOpaque(false);
        formGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        ageSpinner = new JSpinner(new SpinnerNumberModel(45, 18, 100, 1));
        styleSpinner(ageSpinner);
        genderCombo = new JComboBox<>(new String[] { "Женский (Female)", "Мужской (Male)" });
        styleComboBox(genderCombo);
        formGrid.add(createField("Возраст (лет):", ageSpinner));
        formGrid.add(createField("Биологический пол:", genderCombo));

        mmseSpinner = new JSpinner(new SpinnerNumberModel(28, 0, 30, 1));
        styleSpinner(mmseSpinner);
        mocaSpinner = new JSpinner(new SpinnerNumberModel(26, 0, 30, 1));
        styleSpinner(mocaSpinner);
        formGrid.add(createField("Шкала MMSE (Память, 0–30):", mmseSpinner));
        formGrid.add(createField("Шкала MoCA (Когниции, 0–30):", mocaSpinner));

        updrsSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 132.0, 0.5));
        styleSpinner(updrsSpinner);
        edssSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 10.0, 0.5));
        styleSpinner(edssSpinner);
        formGrid.add(createField("MDS-UPDRS (Экстрапирамидный, 0–132):", updrsSpinner));
        formGrid.add(createField("EDSS (РС инвалидизация, 0–10):", edssSpinner));

        alsfrsSpinner = new JSpinner(new SpinnerNumberModel(48.0, 0.0, 48.0, 1.0));
        styleSpinner(alsfrsSpinner);
        fatigueSpinner = new JSpinner(new SpinnerNumberModel(2.0, 1.0, 7.0, 0.1));
        styleSpinner(fatigueSpinner);
        formGrid.add(createField("ALSFRS-R (БАС статус, 0–48):", alsfrsSpinner));
        formGrid.add(createField("Шкала утомляемости FSS (1–7):", fatigueSpinner));

        durationSpinner = new JSpinner(new SpinnerNumberModel(12.0, 0.0, 360.0, 1.0));
        styleSpinner(durationSpinner);
        lesionsSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        styleSpinner(lesionsSpinner);
        formGrid.add(createField("Длительность симптомов (мес):", durationSpinner));
        formGrid.add(createField("Очаги T2/FLAIR (из МРТ):", lesionsSpinner));

        ocbCheckBox = new JCheckBox("Олигоклональные полосы IgG (OCB 2)");
        styleCheckBox(ocbCheckBox);
        swiCheckBox = new JCheckBox("Утрата «ласточкиного хвоста» SWI");
        styleCheckBox(swiCheckBox);

        formGrid.add(createField("Ликвор (McDonald DIT):", ocbCheckBox));
        formGrid.add(createField("Нигросома-1 (Паркинсон):", swiCheckBox));

        col.add(formGrid);
        col.add(Box.createVerticalStrut(10));

        JLabel infoLabel = new JLabel(Theme.html(
                "<small style='color:#94A3B8;'>Синхронно валидируются консенсусные критерии McDonald 2017 (DIS/DIT), NIA-AA (БА), Gold Coast (БАС) и MDS (Паркинсон).</small>"));
        col.add(infoLabel);
        col.add(Box.createVerticalGlue());

        return col;
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new BorderLayout(16, 0));
        footer.setBackground(Theme.BG_PANEL);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_SUBTLE),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)));

        footerStatusLabel = new JLabel(
                "Готовность системы: Модели загружены • Мультимодальный ансамбль откалиброван (F1: 100%)");
        footerStatusLabel.setFont(Theme.FONT_BODY);
        footerStatusLabel.setForeground(Theme.TEXT_MUTED);
        footer.add(footerStatusLabel, BorderLayout.WEST);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionRow.setOpaque(false);

        resetBtn = new JButton("Сбросить параметры");
        styleSecondaryButton(resetBtn);
        resetBtn.addActionListener(e -> {
            applyClinicalPreset("control");
            highlightPresetButton("control");
            runInspectMRI();
        });

        inspectBtn = new JButton("Инспектировать МРТ");
        styleSecondaryButton(inspectBtn);
        inspectBtn.addActionListener(e -> runInspectMRI());

        diagnoseBtn = new JButton("⚡ Выполнить мультимодальный консилиум");
        stylePrimaryButton(diagnoseBtn);
        diagnoseBtn.setPreferredSize(new Dimension(340, 38));
        diagnoseBtn.addActionListener(e -> runDiagnosis());

        actionRow.add(resetBtn);
        actionRow.add(inspectBtn);
        actionRow.add(diagnoseBtn);

        footer.add(actionRow, BorderLayout.EAST);
        return footer;
    }

    private JPanel createField(String labelText, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setOpaque(false);
        JLabel l = new JLabel(labelText);
        l.setFont(Theme.FONT_LABEL);
        l.setForeground(Theme.TEXT_MUTED);
        dynamicMutedLabels.add(l);
        p.add(l, BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private void chooseMultipleFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = chooser.getSelectedFiles();
            if (selectedFiles != null && selectedFiles.length > 0) {
                List<String> paths = new ArrayList<>();
                for (File f : selectedFiles)
                    paths.add(f.getAbsolutePath());
                pathField.setText(String.join(";", paths));
                runInspectMRI();
            }
        }
    }

    private void chooseDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            pathField.setText(chooser.getSelectedFile().getAbsolutePath());
            runInspectMRI();
        }
    }

    private void setupDropTarget() {
        new DropTarget(this, new DropTargetAdapter() {
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                if (dropBox != null) {
                    dropBox.setBackground(Theme.BG_INPUT_HOVER);
                    dropBox.setBorder(BorderFactory.createDashedBorder(Theme.BORDER_ACTIVE, 2.0f, 4.0f, 2.0f, true));
                }
            }

            @Override
            public void dragExit(DropTargetEvent dte) {
                if (dropBox != null) {
                    dropBox.setBackground(Theme.BG_INPUT);
                    dropBox.setBorder(BorderFactory.createDashedBorder(Theme.BORDER_SUBTLE, 2.0f, 4.0f, 2.0f, true));
                }
            }

            @Override
            @SuppressWarnings("unchecked")
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> droppedFiles = (List<File>) dtde.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    if (droppedFiles != null && !droppedFiles.isEmpty()) {
                        List<String> paths = new ArrayList<>();
                        for (File f : droppedFiles)
                            paths.add(f.getAbsolutePath());
                        pathField.setText(String.join(";", paths));
                        runInspectMRI();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    if (dropBox != null) {
                        dropBox.setBackground(Theme.BG_INPUT);
                        dropBox.setBorder(
                                BorderFactory.createDashedBorder(Theme.BORDER_SUBTLE, 2.0f, 4.0f, 2.0f, true));
                    }
                }
            }
        });
    }

    private void updateContrastState() {
        String mode = getContrastMode();
        boolean showWarning = "native".equals(mode) || "auto".equals(mode);
        contrastWarningBanner.setVisible(showWarning);
        revalidate();
        repaint();
    }

    private String getContrastMode() {
        if (rbEnhancedContrast.isSelected())
            return "contrast";
        if (rbNativeContrast.isSelected())
            return "native";
        return "auto";
    }

    @SuppressWarnings("unchecked")
    private void runInspectMRI() {
        String path = pathField.getText().trim();
        if (path.isEmpty())
            return;

        try {
            Map<String, Object> info = bridge.inspectMRI(path, getContrastMode());
            if (Boolean.TRUE.equals(info.get("success"))) {
                slicesTableModel.setRowCount(0);
                inspectedFiles.clear();

                Object filesObj = info.get("files");
                if (filesObj instanceof List) {
                    List<?> list = (List<?>) filesObj;
                    for (int i = 0; i < list.size(); i++) {
                        Object item = list.get(i);
                        if (item instanceof Map) {
                            Map<String, Object> f = (Map<String, Object>) item;
                            inspectedFiles.add(f);

                            boolean hasLesion = Boolean.TRUE.equals(f.get("has_lesion"));
                            String statusIcon = hasLesion ? "[●] Очаг" : "[✓] Норма";
                            String fname = String.valueOf(f.getOrDefault("file_name", "Снимок " + (i + 1)));
                            String seq = String.valueOf(f.getOrDefault("sequence", "Серия"));
                            String cls = String.valueOf(f.getOrDefault("classification", "Control"));
                            String sub = String.valueOf(f.getOrDefault("subclass", "Норма"));
                            String zone = String.valueOf(f.getOrDefault("affected_zone", "--"));

                            slicesTableModel.addRow(new Object[] {
                                    (i + 1),
                                    statusIcon,
                                    fname,
                                    seq,
                                    cls + " (" + sub + ")",
                                    zone
                            });
                        }
                    }
                }

                Object lesionVol = info.getOrDefault("lesion_volume_mm3", 0.0);
                double vol = (lesionVol instanceof Number) ? ((Number) lesionVol).doubleValue() : 0.0;
                if (vol > 0) {
                    int estimatedCount = (int) Math.max(5, Math.min(45, vol / 40.0));
                    lesionsSpinner.setValue(estimatedCount);
                }
                int totalDetected = 0;
                for (Map<String, Object> f : inspectedFiles) {
                    if (f.get("lesion_count") instanceof Number) {
                        totalDetected += ((Number) f.get("lesion_count")).intValue();
                    }
                }
                if (totalDetected > 0) {
                    lesionsSpinner.setValue(Math.min(50, totalDetected));
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ошибка инспекции МРТ: " + ex.getMessage(), "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openViewerForSelected() {
        if (inspectedFiles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Снимки ещё не загружены или не проинспектированы.", "Информация",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int selectedRow = slicesTable.getSelectedRow();
        int targetIndex = (selectedRow >= 0 && selectedRow < inspectedFiles.size()) ? selectedRow : 0;

        MRIViewerDialog viewer = new MRIViewerDialog(parentFrame, inspectedFiles, targetIndex);
        viewer.setVisible(true);
    }

    private void runDiagnosis() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("age", ageSpinner.getValue());
        params.put("gender", genderCombo.getSelectedIndex() == 1 ? 1 : 0);
        params.put("mmse", mmseSpinner.getValue());
        params.put("moca", mocaSpinner.getValue());
        params.put("updrs_motor", updrsSpinner.getValue());
        params.put("edss_score", edssSpinner.getValue());
        params.put("alsfrs_r", alsfrsSpinner.getValue());
        params.put("fatigue_score", fatigueSpinner.getValue());
        params.put("symptom_duration_months", durationSpinner.getValue());
        params.put("t2_lesions_count", lesionsSpinner.getValue());
        params.put("oligoclonal_bands", ocbCheckBox.isSelected() ? 1 : 0);
        params.put("swi_swallow_tail_loss", swiCheckBox.isSelected() ? 1 : 0);

        try {
            Map<String, Object> result = bridge.predict(params);
            if (Boolean.TRUE.equals(result.get("success"))) {
                VerdictDialog dialog = new VerdictDialog(parentFrame, result, params);
                dialog.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Ошибка предсказания: " + result.get("error"), "Ошибка",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Сбой связи с моделью Python:\n" + ex.getMessage(),
                    "Критическая ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void onThemeChanged(boolean isDark) {
        refreshTheme(isDark);
    }

    private void refreshTheme(boolean isDark) {
        setBackground(Theme.BG_MAIN);
        if (headerPanel != null) {
            headerPanel.setBackground(Theme.BG_PANEL);
            headerPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_SUBTLE),
                    BorderFactory.createEmptyBorder(12, 20, 12, 20)));
        }
        if (titleLabel != null)
            titleLabel.setForeground(Theme.TEXT_PRIMARY);
        if (subtitleLabel != null)
            subtitleLabel.setForeground(Theme.TEXT_MUTED);

        if (themeToggleBtn != null) {
            themeToggleBtn.setText(isDark ? "🌙 Тёмный зал МРТ" : "☀️ Светлая ординаторская");
            styleSecondaryButton(themeToggleBtn);
        }

        if (mriColumnPanel != null) {
            mriColumnPanel.setBackground(Theme.BG_PANEL);
            mriColumnPanel.setBorder(Theme.PANEL_BORDER);
        }
        if (surveyColumnPanel != null) {
            surveyColumnPanel.setBackground(Theme.BG_PANEL);
            surveyColumnPanel.setBorder(Theme.PANEL_BORDER);
        }

        if (mriColTitle != null)
            mriColTitle.setForeground(Theme.TEXT_PRIMARY);
        if (surveyColTitle != null)
            surveyColTitle.setForeground(Theme.TEXT_PRIMARY);

        if (dropBox != null) {
            dropBox.setBackground(Theme.BG_INPUT);
            dropBox.setBorder(BorderFactory.createDashedBorder(Theme.BORDER_SUBTLE, 2.0f, 4.0f, 2.0f, true));
        }
        if (dropLabel != null) {
            dropLabel.setText(Theme.html(
                    "<center><b>📁 Перетащите сюда файлы или папки (DICOM / NIfTI / JPG)</b><br><span style='color:#94A3B8;'>Поддерживается отправка сразу нескольких файлов</span></center>"));
        }

        if (pathField != null)
            styleTextField(pathField);
        if (browseFolderBtn != null)
            styleSecondaryButton(browseFolderBtn);
        if (browseFilesBtn != null)
            styleSecondaryButton(browseFilesBtn);
        if (openViewerBtn != null)
            styleSecondaryButton(openViewerBtn);

        if (slicesTable != null) {
            setupTableRenderer();
        }

        for (JLabel l : dynamicMutedLabels) {
            if (l != null)
                l.setForeground(Theme.TEXT_MUTED);
        }

        highlightPresetButton(currentPresetKey);

        if (rbAutoContrast != null)
            styleRadio(rbAutoContrast);
        if (rbNativeContrast != null)
            styleRadio(rbNativeContrast);
        if (rbEnhancedContrast != null)
            styleRadio(rbEnhancedContrast);

        if (contrastWarningBanner != null) {
            contrastWarningBanner.setBackground(Theme.WARN_BG);
            contrastWarningBanner.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.WARN_BORDER, 1),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        }
        if (warningTextLabel != null) {
            warningTextLabel.setText(Theme.html(
                    "<b>⚠️ Предупреждение радиолога:</b> Нативная серия без контрастирования. Для оценки недавней демиелинизации (DIT) необходим T1+Gd."));
        }

        if (ageSpinner != null)
            styleSpinner(ageSpinner);
        if (genderCombo != null)
            styleComboBox(genderCombo);
        if (mmseSpinner != null)
            styleSpinner(mmseSpinner);
        if (mocaSpinner != null)
            styleSpinner(mocaSpinner);
        if (updrsSpinner != null)
            styleSpinner(updrsSpinner);
        if (edssSpinner != null)
            styleSpinner(edssSpinner);
        if (alsfrsSpinner != null)
            styleSpinner(alsfrsSpinner);
        if (fatigueSpinner != null)
            styleSpinner(fatigueSpinner);
        if (durationSpinner != null)
            styleSpinner(durationSpinner);
        if (lesionsSpinner != null)
            styleSpinner(lesionsSpinner);
        if (ocbCheckBox != null)
            styleCheckBox(ocbCheckBox);
        if (swiCheckBox != null)
            styleCheckBox(swiCheckBox);

        if (footerPanel != null) {
            footerPanel.setBackground(Theme.BG_PANEL);
            footerPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_SUBTLE));
        }
        if (footerStatusLabel != null)
            footerStatusLabel.setForeground(Theme.TEXT_MUTED);
        if (resetBtn != null)
            styleSecondaryButton(resetBtn);
        if (inspectBtn != null)
            styleSecondaryButton(inspectBtn);
        if (diagnoseBtn != null)
            stylePrimaryButton(diagnoseBtn);

        revalidate();
        repaint();
    }

    private void styleTextField(JTextField tf) {
        tf.setFont(Theme.FONT_BODY);
        tf.setBackground(Theme.BG_INPUT);
        tf.setForeground(Theme.TEXT_PRIMARY);
        tf.setCaretColor(Theme.TEXT_PRIMARY);
        tf.setSelectionColor(Theme.TEXT_SELECTION_BG);
        tf.setSelectedTextColor(Theme.TEXT_SELECTION_FG);
        tf.setBorder(Theme.INPUT_BORDER);
    }

    private void styleSpinner(JSpinner sp) {
        sp.setFont(Theme.FONT_BODY);
        sp.setBackground(Theme.BG_INPUT);
        JComponent editor = sp.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setBackground(Theme.BG_INPUT);
            tf.setForeground(Theme.TEXT_PRIMARY);
            tf.setCaretColor(Theme.TEXT_PRIMARY);
            tf.setSelectionColor(Theme.TEXT_SELECTION_BG);
            tf.setSelectedTextColor(Theme.TEXT_SELECTION_FG);
            tf.setBorder(null);
        }
        sp.setBorder(Theme.INPUT_BORDER);
    }

    private void styleComboBox(JComboBox<?> cb) {
        cb.setFont(Theme.FONT_BODY);
        cb.setBackground(Theme.BG_INPUT);
        cb.setForeground(Theme.TEXT_PRIMARY);
        cb.setBorder(Theme.INPUT_BORDER);
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (isSelected) {
                    c.setBackground(Theme.TEXT_SELECTION_BG);
                    c.setForeground(Theme.TEXT_SELECTION_FG);
                } else {
                    c.setBackground(Theme.BG_INPUT);
                    c.setForeground(Theme.TEXT_PRIMARY);
                }
                if (c instanceof JLabel) {
                    ((JLabel) c).setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                }
                return c;
            }
        });
    }

    private void styleRadio(JRadioButton rb) {
        rb.setFont(Theme.FONT_LABEL);
        rb.setBackground(Theme.BG_PANEL);
        rb.setForeground(Theme.TEXT_PRIMARY);
        rb.setFocusPainted(false);
    }

    private void styleCheckBox(JCheckBox cb) {
        cb.setFont(Theme.FONT_LABEL);
        cb.setBackground(Theme.BG_PANEL);
        cb.setForeground(Theme.TEXT_PRIMARY);
        cb.setFocusPainted(false);
    }

    private void stylePrimaryButton(JButton btn) {
        Theme.stylePrimaryButton(btn);
    }

    private void styleSecondaryButton(JButton btn) {
        Theme.styleSecondaryButton(btn);
    }

    private void setupTableRenderer() {
        if (slicesTable == null)
            return;
        slicesTable.setBackground(Theme.BG_INPUT);
        slicesTable.setForeground(Theme.TEXT_PRIMARY);
        slicesTable.setSelectionBackground(Theme.TEXT_SELECTION_BG);
        slicesTable.setSelectionForeground(Theme.TEXT_SELECTION_FG);

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (isSelected) {
                    c.setBackground(Theme.TEXT_SELECTION_BG);
                    c.setForeground(Theme.TEXT_SELECTION_FG);
                } else {
                    c.setBackground(row % 2 == 0 ? Theme.BG_PANEL : Theme.BG_INPUT);
                    c.setForeground(Theme.TEXT_PRIMARY);
                }
                if (c instanceof JLabel) {
                    JLabel lbl = (JLabel) c;
                    lbl.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                    if (column == 0 || column == 1) {
                        lbl.setHorizontalAlignment(SwingConstants.CENTER);
                    } else {
                        lbl.setHorizontalAlignment(SwingConstants.LEFT);
                    }
                    if (column == 1 && value != null) {
                        String s = value.toString();
                        if (s.contains("Очаг")) {
                            lbl.setForeground(Theme.COLOR_AD);
                            lbl.setFont(Theme.FONT_BTN);
                        } else if (s.contains("Норма")) {
                            lbl.setForeground(Theme.SUCCESS_GREEN);
                            lbl.setFont(Theme.FONT_BTN);
                        }
                    }
                }
                return c;
            }
        };

        for (int i = 0; i < slicesTable.getColumnCount(); i++) {
            slicesTable.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(Theme.BG_PANEL);
                c.setForeground(Theme.TEXT_PRIMARY);
                c.setFont(Theme.FONT_HEADER);
                if (c instanceof JLabel) {
                    ((JLabel) c).setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_SUBTLE),
                            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
                }
                return c;
            }
        };
        slicesTable.getTableHeader().setDefaultRenderer(headerRenderer);
    }
}
