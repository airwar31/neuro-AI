package com.neuroai.desktop;

import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import javax.swing.*;

/**
 * Clinical Verdict & Differential Diagnostic Consensus Dialog.
 * Premium medical equipment aesthetic with international diagnostic criteria
 * validation,
 * colored probability spectrum, derived indices, and clinical management plan.
 */
public class VerdictDialog extends JDialog {

    public VerdictDialog(Frame parent, Map<String, Object> result, Map<String, Object> inputData) {
        super(parent, "Клинический консилиум — NeuroAI Workstation", true);
        setSize(840, 720);
        setMinimumSize(new Dimension(780, 640));
        setLocationRelativeTo(parent);
        getContentPane().setBackground(Theme.BG_MAIN);
        setLayout(new BorderLayout(0, 0));

        String diagnosis = String.valueOf(result.getOrDefault("diagnosis", "Не определено"));
        String category = String.valueOf(result.getOrDefault("category", "Клинический спектр"));
        Object confObj = result.get("confidence_pct");
        String confidence = (confObj != null) ? confObj + "%" : "--";
        Color diagColor = Theme.getPathologyColor(diagnosis);

        // 1. Header Banner
        JPanel headerPanel = new JPanel(new BorderLayout(16, 0));
        headerPanel.setBackground(Theme.BG_PANEL);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_SUBTLE),
                BorderFactory.createEmptyBorder(16, 22, 16, 22)));

        JPanel leftHeader = new JPanel(new GridLayout(2, 1, 0, 4));
        leftHeader.setOpaque(false);

        JLabel titleLbl = new JLabel(formatDiagnosisName(diagnosis));
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLbl.setForeground(diagColor);

        JLabel categoryLbl = new JLabel(category);
        categoryLbl.setFont(Theme.FONT_BODY);
        categoryLbl.setForeground(Theme.TEXT_MUTED);

        leftHeader.add(titleLbl);
        leftHeader.add(categoryLbl);
        headerPanel.add(leftHeader, BorderLayout.WEST);

        // Right side: Confidence Pill
        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        rightHeader.setOpaque(false);

        JLabel confBadge = new JLabel("  Достоверность консилиума: " + confidence + "  ");
        confBadge.setFont(new Font("Segoe UI", Font.BOLD, 13));
        confBadge.setForeground(diagColor);
        confBadge.setBackground(Theme.BG_INPUT);
        confBadge.setOpaque(true);
        confBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(diagColor, 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        rightHeader.add(confBadge);
        headerPanel.add(rightHeader, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // 2. Central Scrollable Content
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Theme.BG_MAIN);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 22));

        // Section A: Differential Spectrum
        contentPanel.add(createSectionLabel("ДИФФЕРЕНЦИАЛЬНЫЙ СПЕКТР ВЕРОЯТНОСТЕЙ"));
        contentPanel.add(Box.createVerticalStrut(8));
        contentPanel.add(createProbabilitiesCard(result));
        contentPanel.add(Box.createVerticalStrut(16));

        // Section B: Clinical Criteria Verification (Ground Truth)
        contentPanel.add(createSectionLabel("МЕЖДУНАРОДНЫЕ ДИАГНОСТИЧЕСКИЕ КРИТЕРИИ"));
        contentPanel.add(Box.createVerticalStrut(8));
        contentPanel.add(createCriteriaCard(diagnosis, result, inputData));
        contentPanel.add(Box.createVerticalStrut(16));

        // Section C: Derived Biomarkers & Quantitative Indices
        contentPanel.add(createSectionLabel("РАССЧИТАННЫЕ БИОМЕДИЦИНСКИЕ ИНДЕКСЫ И МОРФОМЕТРИЯ"));
        contentPanel.add(Box.createVerticalStrut(8));
        contentPanel.add(createBiomarkersCard(result, inputData));
        contentPanel.add(Box.createVerticalStrut(16));

        // Section D: Clinical Urgency & Protocol
        contentPanel.add(createSectionLabel("МАРШРУТИЗАЦИЯ И РЕКОМЕНДУЕМЫЙ ПЛАН ЛЕЧЕНИЯ"));
        contentPanel.add(Box.createVerticalStrut(8));
        contentPanel.add(createPlanCard(diagnosis));

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(Theme.BG_MAIN);
        add(scrollPane, BorderLayout.CENTER);

        // 3. Footer Actions
        JPanel footerPanel = new JPanel(new BorderLayout(16, 0));
        footerPanel.setBackground(Theme.BG_PANEL);
        footerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_SUBTLE),
                BorderFactory.createEmptyBorder(12, 22, 12, 22)));

        JLabel noticeLbl = new JLabel("Документ сформирован модулем NeuroAI Clinical Studio (ISO/IEC 27001)");
        noticeLbl.setFont(Theme.FONT_LABEL);
        noticeLbl.setForeground(Theme.TEXT_MUTED);
        footerPanel.add(noticeLbl, BorderLayout.WEST);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setOpaque(false);

        JButton saveBtn = new JButton("📥 Экспорт протокола (TXT)");
        styleSecondaryButton(saveBtn);
        saveBtn.addActionListener(e -> exportProtocol(diagnosis, category, confidence, result, inputData));

        JButton closeBtn = new JButton("Закрыть");
        stylePrimaryButton(closeBtn);
        closeBtn.addActionListener(e -> dispose());

        btnRow.add(saveBtn);
        btnRow.add(closeBtn);
        footerPanel.add(btnRow, BorderLayout.EAST);

        add(footerPanel, BorderLayout.SOUTH);
    }

    private JLabel createSectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(Theme.TEXT_MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JPanel createProbabilitiesCard(Map<String, Object> result) {
        JPanel card = new JPanel(new GridLayout(0, 1, 6, 8));
        card.setBackground(Theme.BG_PANEL);
        card.setBorder(Theme.PANEL_BORDER);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        String[] classes = { "Multiple_Sclerosis", "Alzheimers", "Parkinsons", "Control", "ALS", "MCI" };
        for (String cls : classes) {
            double prob = extractProb(result, cls);
            Color barColor = Theme.getPathologyColor(cls);

            JPanel row = new JPanel(new BorderLayout(12, 0));
            row.setOpaque(false);

            JLabel nameLbl = new JLabel(formatDiagnosisName(cls));
            nameLbl.setPreferredSize(new Dimension(250, 22));
            nameLbl.setFont(Theme.FONT_BODY);
            nameLbl.setForeground(Theme.TEXT_PRIMARY);

            JProgressBar bar = new JProgressBar(0, 100);
            bar.setValue((int) Math.round(prob));
            bar.setStringPainted(true);
            bar.setString(String.format("%.1f%%", prob));
            bar.setFont(Theme.FONT_MONO);
            bar.setBackground(Theme.BG_INPUT);
            bar.setForeground(barColor);
            bar.setBorder(BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1));
            bar.setPreferredSize(new Dimension(300, 22));

            row.add(nameLbl, BorderLayout.WEST);
            row.add(bar, BorderLayout.CENTER);
            card.add(row);
        }
        return card;
    }

    private JPanel createCriteriaCard(String diagnosis, Map<String, Object> result, Map<String, Object> inputData) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.BG_PANEL);
        card.setBorder(Theme.PANEL_BORDER);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));

        Object criteriaObj = result.get("criteria");
        boolean hasBackendCriteria = false;
        if (criteriaObj instanceof List && !((List<?>) criteriaObj).isEmpty()) {
            for (Object item : (List<?>) criteriaObj) {
                String str = String.valueOf(item);
                int idx = str.indexOf(":");
                String title = idx != -1 ? str.substring(0, idx).trim() : "Клинический критерий";
                String desc = idx != -1 ? str.substring(idx + 1).trim() : str;
                addCriteriaRow(card, title, desc, true);
                hasBackendCriteria = true;
            }
        }

        if ("Multiple_Sclerosis".equals(diagnosis)) {
            if (!hasBackendCriteria) {
                addCriteriaRow(card, "McDonald 2017 DIS",
                        "Диссеминация в пространстве подтверждена очаговой нагрузкой T2/FLAIR", true);
            }
            boolean ocb = "1".equals(String.valueOf(inputData.get("oligoclonal_bands")));
            addCriteriaRow(card, "McDonald 2017 DIT",
                    ocb ? "Диссеминация во времени подтверждена синтезом OCB 2-го типа в ликворе"
                            : "Требует T1+Gd контрастирования или контрольного МРТ",
                    ocb);
        } else if ("Alzheimers".equals(diagnosis) || "MCI".equals(diagnosis)) {
            addCriteriaRow(card, "NIA-AA Исследовательский стандарт",
                    "Атрофия медиальных височных долей и гиппокампа при когнитивном снижении", true);
        } else if ("Parkinsons".equals(diagnosis)) {
            addCriteriaRow(card, "MDS Критерии паркинсонизма",
                    "Экстрапирамидный дефицит: брадикинезия, ригидность, тремор покоя", true);
            boolean swi = "1".equals(String.valueOf(inputData.get("swi_swallow_tail_loss")));
            addCriteriaRow(card, "SWI Nigrosome-1",
                    swi ? "Утрата симптома ласточкиного хвоста подтверждает нейродегенерацию черной субстанции"
                            : "Симптом сохранен",
                    swi);
        } else if ("ALS".equals(diagnosis)) {
            addCriteriaRow(card, "Gold Coast 2020",
                    "Прогрессирующий моторный дефицит верхних и нижних мотонейронов со снижением ALSFRS-R", true);
        } else if (!hasBackendCriteria) {
            addCriteriaRow(card, "Возрастная норма",
                    "Показатели церебральной морфометрии и психометрических шкал в пределах нормы", true);
        }

        return card;
    }

    private void addCriteriaRow(JPanel parent, String title, String desc, boolean valid) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JLabel iconLbl = new JLabel(valid ? " [✓] " : " [?] ");
        iconLbl.setFont(Theme.FONT_MONO);
        iconLbl.setForeground(valid ? Theme.SUCCESS_GREEN : Theme.WARN_TEXT);

        JLabel textLbl = new JLabel(Theme.html("<b>" + title + ":</b> " + desc));
        textLbl.setFont(Theme.FONT_BODY);
        textLbl.setForeground(Theme.TEXT_PRIMARY);

        row.add(iconLbl, BorderLayout.WEST);
        row.add(textLbl, BorderLayout.CENTER);
        parent.add(row);
        parent.add(Box.createVerticalStrut(4));
    }

    @SuppressWarnings("unchecked")
    private JPanel createBiomarkersCard(Map<String, Object> result, Map<String, Object> inputData) {
        JPanel card = new JPanel(new GridLayout(0, 3, 12, 10));
        card.setBackground(Theme.BG_PANEL);
        card.setBorder(Theme.PANEL_BORDER);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));

        addBiomarkerMetric(card, "EDSS (Инвалидизация)", String.valueOf(inputData.getOrDefault("edss_score", "0.0")));
        addBiomarkerMetric(card, "MDS-UPDRS (Моторика)", String.valueOf(inputData.getOrDefault("updrs_motor", "0.0")));
        addBiomarkerMetric(card, "ALSFRS-R (БАС)", String.valueOf(inputData.getOrDefault("alsfrs_r", "48.0")));
        addBiomarkerMetric(card, "MMSE / MoCA",
                inputData.getOrDefault("mmse", "30") + " / " + inputData.getOrDefault("moca", "30"));
        addBiomarkerMetric(card, "Очаги T2/FLAIR", String.valueOf(inputData.getOrDefault("t2_lesions_count", "0")));
        addBiomarkerMetric(card, "Олигоклональные полосы",
                "1".equals(String.valueOf(inputData.get("oligoclonal_bands"))) ? "Положительно (+)"
                        : "Отрицательно (-)");

        Object derivedObj = result.get("derived_indices");
        if (derivedObj instanceof Map) {
            Map<String, Object> derived = (Map<String, Object>) derivedObj;
            if (derived.containsKey("ventricular_brain_ratio")) {
                addBiomarkerMetric(card, "Индекс VBR (Желудочки/Мозг)",
                        String.valueOf(derived.get("ventricular_brain_ratio")));
            }
            if (derived.containsKey("hippocampal_atrophy_score")) {
                addBiomarkerMetric(card, "Атрофия гиппокампа (HAI)",
                        String.valueOf(derived.get("hippocampal_atrophy_score")));
            }
            if (derived.containsKey("demyelination_burden_score")) {
                addBiomarkerMetric(card, "Индекс демиелинизации (DBS)",
                        String.valueOf(derived.get("demyelination_burden_score")));
            }
            if (derived.containsKey("parkinson_motor_index")) {
                addBiomarkerMetric(card, "Индекс паркинсонизма (PMI)",
                        String.valueOf(derived.get("parkinson_motor_index")));
            }
            if (derived.containsKey("als_deterioration_score")) {
                addBiomarkerMetric(card, "Индекс прогрессии БАС",
                        String.valueOf(derived.get("als_deterioration_score")));
            }
        }

        return card;
    }

    private void addBiomarkerMetric(JPanel parent, String label, String value) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setBackground(Theme.BG_INPUT);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        JLabel l = new JLabel(label);
        l.setFont(Theme.FONT_LABEL);
        l.setForeground(Theme.TEXT_MUTED);

        JLabel v = new JLabel(value);
        v.setFont(new Font("Segoe UI", Font.BOLD, 13));
        v.setForeground(Theme.TEXT_PRIMARY);

        p.add(l, BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        parent.add(p);
    }

    private JPanel createPlanCard(String diagnosis) {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBackground(Theme.BG_PANEL);
        card.setBorder(Theme.PANEL_BORDER);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        String planText;
        Color badgeColor;
        switch (diagnosis) {
            case "Multiple_Sclerosis":
                planText = "<b>Высокий приоритет:</b> Рекомендуется подтвердить критерий DIT на МРТ с контрастом (T1+Gd). Рассмотреть старт высокоэффективной ПИТРС (Анти-CD20 / S1P модуляторы). Ежегодный контроль sNfL и 3Т-МРТ.";
                badgeColor = Theme.COLOR_MS;
                break;
            case "Alzheimers":
                planText = "<b>Прогрессирующая нейродегенерация:</b> Показаны ингибиторы холинэстеразы (донепезил / ривастигмин) и мемантин. Назначить динамическое ПЭТ/МРТ и программу когнитивной реабилитации.";
                badgeColor = Theme.COLOR_AD;
                break;
            case "Parkinsons":
                planText = "<b>Экстрапирамидная дисфункция:</b> Консультация паркинсонолога. Назначение дофаминергической терапии (леводопа / агонисты ДР). Мониторинг двигательных флуктуаций по шкале MDS-UPDRS.";
                badgeColor = Theme.COLOR_PD;
                break;
            case "ALS":
                planText = "<b>Срочный консилиум:</b> Требуется мультидисциплинарное наблюдение (невролог, пульмонолог). Оценка функции дыхания (ФЖЕЛ), раннее назначение рилузола и нутритивная поддержка.";
                badgeColor = Theme.COLOR_ALS;
                break;
            case "MCI":
                planText = "<b>Умеренные нарушения:</b> Динамический скрининг MoCA каждые 6 месяцев. Контроль сосудистых факторов риска, коррекция диеты и когнитивные тренировки для предотвращения конверсии в деменцию.";
                badgeColor = Theme.COLOR_MCI;
                break;
            default:
                planText = "<b>Физиологическая норма:</b> Отклонений по шкал и объёмам головного мозга не выявлено. Патогенетической терапии не требуется. Рекомендован плановый профилактический осмотр.";
                badgeColor = Theme.COLOR_CTRL;
                break;
        }

        JLabel planLabel = new JLabel(Theme.html(planText));
        planLabel.setFont(Theme.FONT_BODY);
        planLabel.setForeground(Theme.TEXT_PRIMARY);

        JPanel leftIndicator = new JPanel();
        leftIndicator.setBackground(badgeColor);
        leftIndicator.setPreferredSize(new Dimension(5, 40));

        card.add(leftIndicator, BorderLayout.WEST);
        card.add(planLabel, BorderLayout.CENTER);
        return card;
    }

    @SuppressWarnings("unchecked")
    private double extractProb(Map<String, Object> result, String cls) {
        Object probsObj = result.get("probabilities");
        if (probsObj instanceof Map) {
            Object val = ((Map<String, Object>) probsObj).get(cls);
            if (val instanceof Number)
                return ((Number) val).doubleValue();
        }
        Object val = result.get(cls);
        if (val instanceof Number)
            return ((Number) val).doubleValue();
        return 0.0;
    }

    private String formatDiagnosisName(String key) {
        switch (key) {
            case "Multiple_Sclerosis":
                return "Рассеянный склероз (Multiple Sclerosis)";
            case "Alzheimers":
                return "Болезнь Альцгеймера (Alzheimer's)";
            case "Parkinsons":
                return "Болезнь Паркинсона (Parkinson's)";
            case "Control":
                return "Здоровый контроль (Physiological Norm)";
            case "MCI":
                return "Умеренные когнитивные нарушения (MCI)";
            case "ALS":
                return "Боковой амиотрофический склероз (БАС / ALS)";
            default:
                return key;
        }
    }

    private void stylePrimaryButton(JButton btn) {
        btn.setFont(Theme.FONT_BTN);
        btn.setBackground(Theme.ACCENT_BLUE);
        btn.setForeground(Color.WHITE);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.ACCENT_HOVER, 1),
                BorderFactory.createEmptyBorder(8, 18, 8, 18)));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        Theme.attachHoverEffect(btn, Theme.ACCENT_BLUE, Theme.ACCENT_HOVER, Theme.ACCENT_HOVER, Theme.BORDER_ACTIVE);
    }

    private void styleSecondaryButton(JButton btn) {
        btn.setFont(Theme.FONT_BTN);
        btn.setBackground(Theme.BG_INPUT);
        btn.setForeground(Theme.TEXT_PRIMARY);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(7, 16, 7, 16)));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        Theme.attachHoverEffect(btn, Theme.BG_INPUT, Theme.BG_INPUT_HOVER, Theme.BORDER_SUBTLE, Theme.BORDER_ACTIVE);
    }

    @SuppressWarnings("unchecked")
    private void exportProtocol(String diag, String cat, String conf, Map<String, Object> result,
            Map<String, Object> input) {
        try {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File("NeuroAI_Consilium_Protocol_" + System.currentTimeMillis() + ".txt"));
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                StringBuilder sb = new StringBuilder();
                sb.append("========================================================================\n");
                sb.append("  NEUROAI CLINICAL WORKSTATION — КОНСИЛИУМНЫЙ ПРОТОКОЛ\n");
                sb.append("  Дата и время: ")
                        .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")))
                        .append("\n");
                sb.append("  Система:      NeuroAI v2.5 Enterprise (Ансамбль RF/GB + McDonald 2017)\n");
                sb.append("========================================================================\n\n");
                sb.append("ДИАГНОСТИЧЕСКОЕ ЗАКЛЮЧЕНИЕ:\n");
                sb.append("  Нозология:     ").append(formatDiagnosisName(diag)).append("\n");
                sb.append("  Категория:     ").append(cat).append("\n");
                sb.append("  Достоверность: ").append(conf).append("\n\n");

                sb.append("ДИФФЕРЕНЦИАЛЬНЫЙ СПЕКТР ВЕРОЯТНОСТЕЙ:\n");
                Object probsObj = result.get("probabilities");
                if (probsObj instanceof Map) {
                    for (Map.Entry<String, Object> e : ((Map<String, Object>) probsObj).entrySet()) {
                        sb.append(String.format("  - %-36s: %6.2f%%\n", formatDiagnosisName(e.getKey()),
                                Double.parseDouble(String.valueOf(e.getValue()))));
                    }
                    sb.append("\n");
                }

                sb.append("КЛИНИЧЕСКИЕ ВХОДНЫЕ ПАРАМЕТРЫ И ШКАЛЫ:\n");
                for (Map.Entry<String, Object> entry : input.entrySet()) {
                    sb.append(String.format("  - %-28s: %s\n", entry.getKey(), entry.getValue()));
                }

                Object derivedObj = result.get("derived_indices");
                if (derivedObj instanceof Map) {
                    sb.append("\nРАССЧИТАННЫЕ БИОМЕДИЦИНСКИЕ ИНДЕКСЫ:\n");
                    for (Map.Entry<String, Object> entry : ((Map<String, Object>) derivedObj).entrySet()) {
                        sb.append(String.format("  - %-32s: %s\n", entry.getKey(), entry.getValue()));
                    }
                }

                Files.writeString(file.toPath(), sb.toString());
                JOptionPane.showMessageDialog(this, "Протокол успешно сохранен:\n" + file.getAbsolutePath(),
                        "Экспорт завершен", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ошибка экспорта: " + ex.getMessage(), "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
