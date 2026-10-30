// -*- coding: utf-8 -*-
/**
 * NeuroAI Studio Client Application.
 * Apple + MSD Minimalist Clinical Architecture.
 * Complete Bilingual System: English (EN) & Russian (RU).
 * Dual Mode:
 * 1. General Neurological Screening (6 Disorders)
 * 2. Precision Multiple Sclerosis Staging & Subtype Classifier (5 Subclasses)
 */

let CURRENT_LANG = localStorage.getItem("neuroai_lang") || "ru";
let CURRENT_MODE = "general"; // "general" or "ms"
let GENERAL_PRESETS = {};
let MS_PRESETS = {};

let LAST_GENERAL_RESULT = null;
let LAST_MS_RESULT = null;

// ==========================================================================
// Clinical Color System
// ==========================================================================
const COLOR_MAP_GENERAL = {
    "Control": "#059669",        // Clinical Emerald
    "MCI": "#d97706",            // Clinical Amber
    "Alzheimers": "#dc2626",     // Clinical Ruby
    "Parkinsons": "#7c3aed",     // Clinical Violet
    "Multiple_Sclerosis": "#0284c7", // Clinical Sky/Navy
    "ALS": "#e11d48"             // Clinical Rose
};

const COLOR_MAP_MS = {
    "Control": "#059669",
    "CIS": "#0284c7",
    "RRMS": "#ea580c",
    "SPMS": "#dc2626",
    "PPMS": "#7c3aed"
};

// ==========================================================================
// Comprehensive Bilingual Dictionary (EN / RU)
// ==========================================================================
const I18N = {
    en: {
        badge_studio: "Clinical Studio",
        brand_sub: "Diagnostic Decision Support & Quantitative Neuroimaging Platform",
        status_active: "Dual Inference Active",
        github_repo: "Repository",
        tab_general_title: "General Neurological Screening",
        tab_general_desc: "AD, PD, MS, ALS, MCI & Healthy Controls",
        tab_ms_title: "Multiple Sclerosis Phenotyping",
        tab_ms_desc: "CIS, RRMS, SPMS, PPMS & McDonald 2017 Validation",
        presets_label: "Validated Clinical Cohort Presets",
        presets_label_ms: "Multiple Sclerosis Phenotype Presets",
        presets_sublabel: "Pre-populates clinical surveys, MRI volumetrics, and CSF indices",
        card_general_title: "Patient Clinical Assessment",
        card_general_desc: "Structured clinical history, cognitive evaluations, and neuroimaging parameters",
        badge_live: "Real-Time Sync",
        sec_demographics: "Demographics & Cognitive Scales",
        lbl_age: "Age (Years)",
        lbl_gender: "Biological Sex",
        opt_female: "Female",
        opt_male: "Male",
        lbl_mmse: "MMSE Score (0–30)",
        lbl_moca: "MoCA Score (0–30)",
        lbl_updrs: "MDS-UPDRS Part III (0–132)",
        lbl_edss: "EDSS Score (0–10.0)",
        lbl_alsfrs: "ALSFRS-R Scale (0–48)",
        lbl_fatigue: "Fatigue Severity (FSS 1–7)",
        sec_mri_biofluids: "Quantitative MRI & Biofluids",
        lbl_hippo: "Hippocampus Volume (mm³)",
        lbl_vent: "Ventricles Volume (mm³)",
        lbl_nwbv: "Normalized Brain Vol (nWBV)",
        lbl_t2: "T2 Demyelinating Lesions",
        lbl_swi: "SWI Swallow-Tail Sign",
        opt_swi_0: "Preserved (Nigrosome-1 Intact)",
        opt_swi_1: "Lost (Dopaminergic Depletion)",
        lbl_motor_cortex: "Motor Cortex Thinning (0–1)",
        lbl_ocb: "CSF Oligoclonal Bands",
        opt_ocb_0: "Negative (Pattern 1)",
        opt_ocb_1: "Positive (Type 2 Synthesis)",
        lbl_duration: "Symptom Duration (Months)",
        btn_compute_general: "Compute Differential Risk",
        btn_reset_baseline: "Reset to Control Baseline",
        res_card_general_title: "Diagnostic Synthesis",
        res_card_general_desc: "Ensemble Model Calibration & Stratification",
        badge_ensemble: "Ensemble ML",
        lbl_primary_prob: "Primary Diagnostic Probability",
        lbl_clinical_status: "Clinical Severity Status",
        lbl_action_plan: "Recommended Clinical Action Plan",
        lbl_differential_spectrum: "Multimodal Differential Spectrum",
        lbl_calibrated_probs: "Calibrated Probabilities",
        lbl_derived_biomarkers: "Derived Pathophysiological Biomarkers",
        lbl_automated_indices: "Automated Indices",
        idx_vbr: "Vent-to-Brain (VBR)",
        idx_hippo: "Hippocampal Atrophy",
        idx_cog: "Cognitive Deficit",
        idx_motor: "Motor Impairment",
        idx_demyel: "Demyelination Burden",
        card_ms_title: "MS Phenotype & McDonald Criteria",
        card_ms_desc: "Spatial/temporal dissemination, relapse kinetics, and fluid biomarkers",
        badge_cohort: "2,500 Cohort Model",
        sec_attack_dynamics: "Attack Dynamics & Functional Scores",
        lbl_arr: "Annualized Relapse Rate (ARR)",
        lbl_edss_ms: "EDSS Disability Score (0–10.0)",
        lbl_fss: "Fatigue Severity Scale (FSS 1–7)",
        lbl_sdmt: "Cognitive Processing (SDMT 0–80)",
        lbl_ms_duration: "Disease Duration (Months)",
        lbl_onset_type: "Onset Presentation Type",
        opt_onset_0: "Asymptomatic / Incidental Finding",
        opt_onset_1: "Monofocal (Optic Neuritis / Transverse Myelitis)",
        opt_onset_2: "Multifocal Acute Attacks",
        opt_onset_3: "Insidious Progressive Myelopathy",
        sec_mcdonald_mri: "McDonald 2017 MRI Lesion Topography & Volumetrics",
        lbl_periventricular: "Periventricular Lesions",
        lbl_juxtacortical: "Juxtacortical / Cortical Lesions",
        lbl_infratentorial: "Infratentorial (Brainstem / Cerebellar)",
        lbl_spinal_cord: "Spinal Cord Lesions",
        lbl_gd_enhancing: "Active Gd+ Enhancing Lesions (T1)",
        lbl_black_holes: "T1 Hypointense Black Holes",
        lbl_nwbv_ms: "Brain Parenchymal Fraction (nWBV)",
        lbl_thalamus: "Thalamic Volume (mm³)",
        lbl_t2_total: "Total T2 Lesions Count",
        sec_molecular_biofluids: "Molecular & Biofluid Biomarkers",
        lbl_ocb_ms: "CSF Oligoclonal Bands (OCB)",
        lbl_snfl: "Serum NfL (pg/ml, Simoa)",
        lbl_csf_igg: "CSF IgG Index (Normal < 0.70)",
        btn_compute_ms: "Classify MS Phenotypic Subclass",
        res_card_ms_title: "MS Phenotype Staging",
        res_card_ms_desc: "McDonald 2017 Consensus Criteria Validation",
        badge_precision: "99.8% CV Precision",
        lbl_predicted_subclass: "Predicted Clinical Subclass",
        lbl_mcdonald_formal: "McDonald 2017 Formal Verification",
        lbl_mcdonald_standard: "Clinical Diagnostic Standard",
        lbl_dis: "Dissemination in Space (DIS)",
        lbl_dit: "Dissemination in Time (DIT)",
        lbl_dmt_protocol: "DMT Pharmacotherapy & Clinical Protocol",
        lbl_subclass_dist: "Subclass Probability Distribution",
        lbl_kaggle_benchmark: "Kaggle Benchmark Ensemble",
        lbl_neuro_indices: "Neuroimmunological Derived Indices",
        idx_inflam: "Inflammatory Index",
        idx_neurodeg: "Neurodegeneration",
        idx_prog: "Progression Rate",
        idx_fatigue: "Cognitive-Fatigue",
        footer_title: "Multimodal Clinical Neurological Decision Support System",
        footer_meta1: "Empirically validated on 2,500 patient cohort",
        footer_meta2: "Stratified 5-Fold Cross-Validation Macro-F1: 0.9983",
        footer_meta3: "Medical research software — for clinical research & exploratory evaluation",
        analyzing: "Analyzing...",
        probability: "Probability",

        // Diagnoses Labels
        labels_general: {
            "Control": "Healthy Control",
            "MCI": "Mild Cognitive Impairment (MCI)",
            "Alzheimers": "Alzheimer's Disease",
            "Parkinsons": "Parkinson's Disease",
            "Multiple_Sclerosis": "Multiple Sclerosis",
            "ALS": "Amyotrophic Lateral Sclerosis"
        },
        labels_ms: {
            "Control": "Healthy Control (Intact)",
            "CIS": "Clinically Isolated Syndrome (CIS)",
            "RRMS": "Relapsing-Remitting MS (RRMS)",
            "SPMS": "Secondary Progressive MS (SPMS)",
            "PPMS": "Primary Progressive MS (PPMS)"
        },
        // Preset titles
        presets_general: {
            "control": { title: "Healthy Control", category: "Control" },
            "mci": { title: "Mild Cognitive Impairment", category: "Prodromal" },
            "alzheimers": { title: "Alzheimer's Disease (Early AD)", category: "Dementia" },
            "ad": { title: "Alzheimer's Disease (Early AD)", category: "Dementia" },
            "parkinsons": { title: "Parkinson's Disease (Hoehn-Yahr II)", category: "Movement" },
            "pd": { title: "Parkinson's Disease (Hoehn-Yahr II)", category: "Movement" },
            "ms": { title: "Multiple Sclerosis (Active RRMS)", category: "Demyelinating" },
            "als": { title: "Amyotrophic Lateral Sclerosis", category: "Motor Neuron" }
        },
        presets_ms: {
            "control_ms": { title: "Healthy Control", category: "Intact" },
            "cis": { title: "Clinically Isolated Syndrome", category: "First Episode" },
            "rrms": { title: "Relapsing-Remitting MS", category: "Active" },
            "spms": { title: "Secondary Progressive MS", category: "Progressive" },
            "ppms": { title: "Primary Progressive MS", category: "Primary" }
        },
        // Risk levels
        risk_levels: {
            "High Priority (Severe / Neurodegenerative)": "High Priority (Severe / Neurodegenerative)",
            "Moderate to High (Specialist Intervention Needed)": "Moderate to High (Specialist Intervention Needed)",
            "Mild Impairment (Active Monitoring & Prevention)": "Mild Impairment (Active Monitoring & Prevention)",
            "Low Risk (Within Normal Limits)": "Low Risk (Within Normal Limits)"
        },
        // Recommendations
        urgencies: {
            "Urgent multidisciplinary neurological and palliative care evaluation required.": "Urgent multidisciplinary neurological and specialized specialist care evaluation required.",
            "Confirm with lumbar puncture / SWI & initiate targeted disease-modifying therapy.": "Confirm with lumbar puncture / SWI & initiate targeted disease-modifying therapy.",
            "Conduct serial MoCA follow-up and lifestyle/cognitive interventions.": "Conduct serial MoCA follow-up and targeted cognitive/lifestyle interventions.",
            "Age-appropriate physiological status; continue regular preventative health checks.": "Age-appropriate physiological status; continue regular preventative health checks."
        },
        // DMT Protocols
        dmt: {
            "No Disease-Modifying Therapy indicated. Continue standard preventative surveillance.": "No Disease-Modifying Therapy indicated. Continue standard preventative surveillance.",
            "Early initiation of Platform or High-Efficacy DMT (Interferon-beta / Ocrelizumab / Natalizumab) based on OCB status.": "Early initiation of Platform or High-Efficacy DMT (Interferon-beta / Ocrelizumab / Natalizumab) based on OCB status.",
            "High-Efficacy DMTs (Anti-CD20: Ocrelizumab / Ofatumumab, or S1P Modulators). Monitor sNfL and 3T MRI annually.": "High-Efficacy DMTs (Anti-CD20: Ocrelizumab / Ofatumumab, or S1P Modulators). Monitor sNfL and 3T MRI annually.",
            "Evaluate Siponimod or Anti-CD20 therapy if active. Multidisciplinary neurorehabilitation and cognitive remediation.": "Evaluate Siponimod or Anti-CD20 therapy if active. Multidisciplinary neurorehabilitation and cognitive remediation.",
            "Ocrelizumab (licensed for early active PPMS). Focused spinal cord rehabilitation and motor preservation.": "Ocrelizumab (licensed for early active PPMS). Focused spinal cord rehabilitation and motor preservation."
        },
        // McDonald Status
        mcdonald: {
            dis_met: (zones) => `CRITERIA MET (${zones}/4 anatomical zones)`,
            dis_unmet: (zones) => `UNMET (${zones}/4 zones, requires ≥ 2)`,
            dit_met_both: "CRITERIA MET (Gd+ and CSF OCB Type 2)",
            dit_met_gd: "CRITERIA MET (Active Gd+ Lesion)",
            dit_met_ocb: "CRITERIA MET (CSF OCB Type 2)",
            dit_unmet: "UNMET (No Gd+ enhancing lesion & OCB negative)"
        }
    },

    ru: {
        badge_studio: "Клиническая студия",
        brand_sub: "Система поддержки принятия врачебных решений и количественной нейровизуализации",
        status_active: "Двойной инференс активен",
        github_repo: "Репозиторий",
        tab_general_title: "Общий неврологический скрининг",
        tab_general_desc: "БА, БП, РС, БАС, УКН и контрольная норма",
        tab_ms_title: "Фенотипирование рассеянного склероза",
        tab_ms_desc: "КИС, РРРС, ВПРС, ППРС и критерии Макдональда 2017",
        presets_label: "Валидированные когортные пресеты",
        presets_label_ms: "Пресеты фенотипов рассеянного склероза",
        presets_sublabel: "Автозаполнение клинических шкал, морфометрии МРТ и биомаркеров",
        card_general_title: "Клиническая оценка пациента",
        card_general_desc: "Структурированный анамнез, когнитивные шкалы и параметры нейровизуализации",
        badge_live: "Синхронизация на лету",
        sec_demographics: "Демография и когнитивные шкалы",
        lbl_age: "Возраст (лет)",
        lbl_gender: "Биологический пол",
        opt_female: "Женский",
        opt_male: "Мужской",
        lbl_mmse: "Шкала MMSE (0–30)",
        lbl_moca: "Шкала MoCA (0–30)",
        lbl_updrs: "Моторная шкала UPDRS III (0–132)",
        lbl_edss: "Балл по шкале EDSS (0–10.0)",
        lbl_alsfrs: "Шкала ALSFRS-R (0–48)",
        lbl_fatigue: "Выраженность утомляемости (FSS 1–7)",
        sec_mri_biofluids: "Количественная МРТ и биомаркеры",
        lbl_hippo: "Объём гиппокампа (мм³)",
        lbl_vent: "Объём боковых желудочков (мм³)",
        lbl_nwbv: "Фракция паренхимы (nWBV)",
        lbl_t2: "Количество очагов Т2",
        lbl_swi: "Симптом «ласточкиного хвоста» SWI",
        opt_swi_0: "Сохранён (Нигросома-1 интактна)",
        opt_swi_1: "Утрачен (Деплеция дофамина)",
        lbl_motor_cortex: "Истончение моторной коры (0–1)",
        lbl_ocb: "Олигоклональные полосы (ОКБ) ликвора",
        opt_ocb_0: "Отрицательно (Паттерн 1)",
        opt_ocb_1: "Положительно (Паттерн 2, синтез IgG)",
        lbl_duration: "Длительность симптомов (мес.)",
        btn_compute_general: "Рассчитать дифференциальный риск",
        btn_reset_baseline: "Сбросить к контрольной норме",
        res_card_general_title: "Диагностический синтез",
        res_card_general_desc: "Калибровка и стратификация ансамбля ML",
        badge_ensemble: "Ансамбль ML",
        lbl_primary_prob: "Ведущая нозологическая форма",
        lbl_clinical_status: "Клинический статус риска",
        lbl_action_plan: "Рекомендованный клинический маршрут",
        lbl_differential_spectrum: "Мультимодальный спектр вероятностей",
        lbl_calibrated_probs: "Калиброванные вероятности",
        lbl_derived_biomarkers: "Расчётные патофизиологические индексы",
        lbl_automated_indices: "Автоматические маркеры",
        idx_vbr: "Желудочковый индекс (VBR)",
        idx_hippo: "Атрофия гиппокампа",
        idx_cog: "Когнитивный дефицит",
        idx_motor: "Моторный дефицит",
        idx_demyel: "Бремя демиелинизации",
        card_ms_title: "Фенотип РС и критерии Макдональда",
        card_ms_desc: "Диссеминация в пространстве/времени, кинетика обострений и биомаркеры",
        badge_cohort: "Когорта 2 500 пациентов",
        sec_attack_dynamics: "Динамика обострений и функциональный статус",
        lbl_arr: "Частота обострений в год (ARR)",
        lbl_edss_ms: "Балл инвалидизации EDSS (0–10.0)",
        lbl_fss: "Шкала утомляемости (FSS 1–7)",
        lbl_sdmt: "Когнитивная скорость (SDMT 0–80)",
        lbl_ms_duration: "Длительность заболевания (мес.)",
        lbl_onset_type: "Тип дебюта заболевания",
        opt_onset_0: "Бессимптомный / Случайная находка",
        opt_onset_1: "Монофокальный (Оптический неврит / Миелит)",
        opt_onset_2: "Мультифокальные острые атаки",
        opt_onset_3: "Медленно прогрессирующая миелопатия",
        sec_mcdonald_mri: "Топография очагов МРТ (Макдональд 2017) и объёмы",
        lbl_periventricular: "Перивентрикулярные очаги",
        lbl_juxtacortical: "Юкстакортикальные / кортикальные",
        lbl_infratentorial: "Инфратенториальные (ствол/мозжечок)",
        lbl_spinal_cord: "Очаги в спинном мозге",
        lbl_gd_enhancing: "Активные очаги, копящие Gd+ (Т1)",
        lbl_black_holes: "Хронические «чёрные дыры» Т1",
        lbl_nwbv_ms: "Фракция паренхимы (nWBV)",
        lbl_thalamus: "Объём таламуса (мм³)",
        lbl_t2_total: "Общее количество очагов Т2",
        sec_molecular_biofluids: "Молекулярные и ликворные биомаркеры",
        lbl_ocb_ms: "CSF Олигоклональные полосы (ОКБ)",
        lbl_snfl: "Нейрофиламенты сыворотки sNfL (пг/мл)",
        lbl_csf_igg: "Индекс IgG в ликворе (норма < 0.70)",
        btn_compute_ms: "Определить фенотипический подтип РС",
        res_card_ms_title: "Стадирование фенотипа РС",
        res_card_ms_desc: "Верификация консенсусных критериев Макдональда 2017",
        badge_precision: "Точность CV 99.8%",
        lbl_predicted_subclass: "Прогнозируемый клинический подтип",
        lbl_mcdonald_formal: "Критерии Макдональда 2017",
        lbl_mcdonald_standard: "Клинический диагностический стандарт",
        lbl_dis: "Диссеминация в пространстве (DIS)",
        lbl_dit: "Диссеминация во времени (DIT)",
        lbl_dmt_protocol: "Протокол патогенетической терапии (ПИТРС)",
        lbl_subclass_dist: "Распределение вероятностей подтипов",
        lbl_kaggle_benchmark: "Ансамбль бенчмарка Kaggle",
        lbl_neuro_indices: "Нейроиммунологические расчётные индексы",
        idx_inflam: "Индекс воспаления",
        idx_neurodeg: "Нейродегенерация",
        idx_prog: "Темп прогрессирования",
        idx_fatigue: "Когнитивная утомляемость",
        footer_title: "Мультимодальная система поддержки принятия врачебных решений в неврологии",
        footer_meta1: "Эмпирически валидировано на когорте 2 500 пациентов",
        footer_meta2: "Стратифицированная 5-кратная кросс-валидация Macro-F1: 0.9983",
        footer_meta3: "Медицинское ПО для клинических и биомедицинских исследований",
        analyzing: "Анализ данных...",
        probability: "Вероятность",

        // Diagnoses Labels
        labels_general: {
            "Control": "Здоровая норма",
            "MCI": "Умеренные когнитивные нарушения (УКН)",
            "Alzheimers": "Болезнь Альцгеймера",
            "Parkinsons": "Болезнь Паркинсона",
            "Multiple_Sclerosis": "Рассеянный склероз",
            "ALS": "Боковой амиотрофический склероз (БАС)"
        },
        labels_ms: {
            "Control": "Здоровая норма (без демиелинизации)",
            "CIS": "Клинически изолированный синдром (КИС)",
            "RRMS": "Ремиттирующий рассеянный склероз (РРРС)",
            "SPMS": "Вторично-прогрессирующий РС (ВПРС)",
            "PPMS": "Первично-прогрессирующий РС (ППРС)"
        },
        // Preset titles
        presets_general: {
            "control": { title: "Здоровая норма", category: "Норма" },
            "mci": { title: "Умеренные когнитивные нарушения", category: "Продром" },
            "alzheimers": { title: "Болезнь Альцгеймера (Ранняя БА)", category: "Деменция" },
            "ad": { title: "Болезнь Альцгеймера (Ранняя БА)", category: "Деменция" },
            "parkinsons": { title: "Болезнь Паркинсона (Хён-Яр II)", category: "Двигательные" },
            "pd": { title: "Болезнь Паркинсона (Хён-Яр II)", category: "Двигательные" },
            "ms": { title: "Рассеянный склероз (Активный РРРС)", category: "Демиелинизация" },
            "als": { title: "Боковой амиотрофический склероз", category: "Мотонейроны" }
        },
        presets_ms: {
            "control_ms": { title: "Здоровая норма", category: "Интактный" },
            "cis": { title: "Клинически изолированный синдром", category: "Дебют" },
            "rrms": { title: "Ремиттирующий РС", category: "Активный" },
            "spms": { title: "Вторично-прогрессирующий РС", category: "Прогрессия" },
            "ppms": { title: "Первично-прогрессирующий РС", category: "Первичный" }
        },
        // Risk levels
        risk_levels: {
            "High Priority (Severe / Neurodegenerative)": "Высокий приоритет (Тяжелая нейродегенерация)",
            "Moderate to High (Specialist Intervention Needed)": "Умеренно-высокий риск (Требуется профильный невролог)",
            "Mild Impairment (Active Monitoring & Prevention)": "Умеренные нарушения (Активный мониторинг и превенция)",
            "Low Risk (Within Normal Limits)": "Низкий риск (Физиологическая норма)"
        },
        // Recommendations
        urgencies: {
            "Urgent multidisciplinary neurological and palliative care evaluation required.": "Требуется срочная мультидисциплинарная консультация невролога и паллиативный консилиум.",
            "Confirm with lumbar puncture / SWI & initiate targeted disease-modifying therapy.": "Подтвердить люмбальной пункцией / SWI-МРТ и начать таргетированную патогенетическую терапию.",
            "Conduct serial MoCA follow-up and lifestyle/cognitive interventions.": "Регулярный контроль MoCA, нейропсихологические и модифицирующие образ жизни интервенции.",
            "Age-appropriate physiological status; continue regular preventative health checks.": "Показатели соответствуют возрастной норме; продолжать плановые профилактические осмотры."
        },
        // DMT Protocols
        dmt: {
            "No Disease-Modifying Therapy indicated. Continue standard preventative surveillance.": "Показаний к ПИТРС нет. Продолжить стандартное динамическое диспансерное наблюдение.",
            "Early initiation of Platform or High-Efficacy DMT (Interferon-beta / Ocrelizumab / Natalizumab) based on OCB status.": "Ранний старт платформенной или высокоэффективной ПИТРС (Интерферон-бета / Окрелизумаб / Натализумаб) с учётом статуса ОКБ.",
            "High-Efficacy DMTs (Anti-CD20: Ocrelizumab / Ofatumumab, or S1P Modulators). Monitor sNfL and 3T MRI annually.": "Высокоэффективная ПИТРС (Анти-CD20: Окрелизумаб / Офатумумаб или модуляторы S1P). Ежегодный мониторинг sNfL и 3Т-МРТ.",
            "Evaluate Siponimod or Anti-CD20 therapy if active. Multidisciplinary neurorehabilitation and cognitive remediation.": "Оценить назначение сипонимода или анти-CD20 терапии при активности. Комплексная нейрореабилитация и когнитивный тренинг.",
            "Ocrelizumab (licensed for early active PPMS). Focused spinal cord rehabilitation and motor preservation.": "Окрелизумаб (зарегистрирован при раннем активном ППРС). Таргетная спинальная реабилитация и поддержание моторики."
        },
        // McDonald Status
        mcdonald: {
            dis_met: (zones) => `КРИТЕРИЙ ВЫПОЛНЕН (${zones}/4 анат. зон)`,
            dis_unmet: (zones) => `НЕ ВЫПОЛНЕН (${zones}/4 зон, требуется ≥ 2)`,
            dit_met_both: "КРИТЕРИЙ ВЫПОЛНЕН (Очаг Gd+ и ОКБ 2-го типа)",
            dit_met_gd: "КРИТЕРИЙ ВЫПОЛНЕН (Активный очаг Gd+)",
            dit_met_ocb: "КРИТЕРИЙ ВЫПОЛНЕН (ОКБ 2-го типа в ликворе)",
            dit_unmet: "НЕ ВЫПОЛНЕН (Нет Gd+ очагов и ОКБ отрицательны)"
        }
    }
};

// ==========================================================================
// Initialization
// ==========================================================================
document.addEventListener("DOMContentLoaded", async () => {
    initLanguageSwitcher();
    applyLanguage(CURRENT_LANG);

    await fetchAllPresets();
    setupTabSwitching();
    setupGeneralForm();
    setupMSForm();
    renderActivePresetRibbon();
    triggerActivePrediction();
});

// ==========================================================================
// Language Switcher Logic
// ==========================================================================
function initLanguageSwitcher() {
    const buttons = document.querySelectorAll(".lang-btn");
    buttons.forEach(btn => {
        if (btn.dataset.lang === CURRENT_LANG) {
            btn.classList.add("active");
        } else {
            btn.classList.remove("active");
        }

        btn.addEventListener("click", () => {
            const chosenLang = btn.dataset.lang;
            if (chosenLang === CURRENT_LANG) return;

            CURRENT_LANG = chosenLang;
            localStorage.setItem("neuroai_lang", CURRENT_LANG);

            buttons.forEach(b => b.classList.remove("active"));
            btn.classList.add("active");

            applyLanguage(CURRENT_LANG);
            renderActivePresetRibbon();

            // Re-render latest predictions with new language labels
            if (LAST_GENERAL_RESULT) updateGeneralUI(LAST_GENERAL_RESULT);
            if (LAST_MS_RESULT) updateMSUI(LAST_MS_RESULT);
        });
    });
}

function applyLanguage(lang) {
    const dict = I18N[lang] || I18N.en;

    // 1. Static HTML elements with data-i18n
    document.querySelectorAll("[data-i18n]").forEach(el => {
        const key = el.getAttribute("data-i18n");
        if (dict[key]) {
            el.textContent = dict[key];
        }
    });

    // 2. Tab Presets Header text depending on current mode
    const presetsLabelEl = document.getElementById("presets-label-text");
    if (presetsLabelEl) {
        presetsLabelEl.textContent = CURRENT_MODE === "general" ? dict.presets_label : dict.presets_label_ms;
    }

    // 3. Document title
    document.title = lang === "ru" 
        ? "NeuroAI Studio — Клиническая диагностика и фенотипирование РС"
        : "NeuroAI Studio — Clinical Diagnostic Engine & MS Phenotyping";
}

// ==========================================================================
// Preset Fetching & Ribbon Rendering
// ==========================================================================
async function fetchAllPresets() {
    try {
        const [resGen, resMS] = await Promise.all([
            fetch("/api/presets"),
            fetch("/api/ms_presets")
        ]);
        GENERAL_PRESETS = await resGen.json();
        MS_PRESETS = await resMS.json();
    } catch (err) {
        console.error("Error fetching presets:", err);
    }
}

function setupTabSwitching() {
    const tabGen = document.getElementById("tab-general");
    const tabMS = document.getElementById("tab-ms");
    const viewGen = document.getElementById("view-general");
    const viewMS = document.getElementById("view-ms");
    const dict = I18N[CURRENT_LANG];

    tabGen.addEventListener("click", () => {
        CURRENT_MODE = "general";
        tabGen.classList.add("active");
        tabGen.setAttribute("aria-selected", "true");
        tabMS.classList.remove("active");
        tabMS.setAttribute("aria-selected", "false");
        viewGen.style.display = "block";
        viewMS.style.display = "none";
        document.getElementById("presets-label-text").textContent = dict.presets_label;
        renderActivePresetRibbon();
        triggerGeneralPrediction();
    });

    tabMS.addEventListener("click", () => {
        CURRENT_MODE = "ms";
        tabMS.classList.add("active");
        tabMS.setAttribute("aria-selected", "true");
        tabGen.classList.remove("active");
        tabGen.setAttribute("aria-selected", "false");
        viewGen.style.display = "none";
        viewMS.style.display = "block";
        document.getElementById("presets-label-text").textContent = dict.presets_label_ms;
        renderActivePresetRibbon();
        triggerMSPrediction();
    });
}

function renderActivePresetRibbon() {
    const container = document.getElementById("presets-container");
    container.innerHTML = "";

    const activeDict = CURRENT_MODE === "general" ? GENERAL_PRESETS : MS_PRESETS;
    const defaultKey = CURRENT_MODE === "general" ? "control" : "rrms";
    const dict = I18N[CURRENT_LANG];
    const transPresets = CURRENT_MODE === "general" ? dict.presets_general : dict.presets_ms;

    Object.keys(activeDict).forEach((key) => {
        const item = activeDict[key];
        const localized = (transPresets && transPresets[key]) ? transPresets[key] : item;
        const category = localized.category || item.category || 'Cohort';
        const title = localized.title || item.title;

        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = `btn-preset ${key === defaultKey ? 'active' : ''}`;
        btn.innerHTML = `<span class="preset-badge">${category}</span> <span>${title}</span>`;
        btn.addEventListener("click", () => {
            document.querySelectorAll(".btn-preset").forEach(b => b.classList.remove("active"));
            btn.classList.add("active");
            if (CURRENT_MODE === "general") {
                loadGeneralPreset(key);
            } else {
                loadMSPreset(key);
            }
        });
        container.appendChild(btn);
    });
}

// ==========================================================================
// Mode 1: General Screening
// ==========================================================================
function setupGeneralForm() {
    const form = document.getElementById("patient-form");
    form.addEventListener("submit", (e) => {
        e.preventDefault();
        triggerGeneralPrediction();
    });

    form.querySelectorAll("input, select").forEach(input => {
        input.addEventListener("input", debounce(triggerGeneralPrediction, 250));
    });

    document.getElementById("btn-reset").addEventListener("click", () => {
        loadGeneralPreset("control");
    });
}

function loadGeneralPreset(presetKey) {
    const data = GENERAL_PRESETS[presetKey];
    if (!data) return;

    const fields = [
        "age", "gender", "mmse", "moca", "updrs_motor", "edss_score", "alsfrs_r",
        "fatigue_score", "symptom_duration_months", "hippocampus_vol",
        "ventricles_vol", "n_wbv", "t2_lesions_count", "swi_swallow_tail_loss",
        "motor_cortex_atrophy", "oligoclonal_bands"
    ];

    fields.forEach(field => {
        const el = document.getElementById(field);
        if (el && data[field] !== undefined) {
            el.value = data[field];
        }
    });

    triggerGeneralPrediction();
}

async function triggerGeneralPrediction() {
    const form = document.getElementById("patient-form");
    const formData = new FormData(form);
    const payload = {};
    formData.forEach((val, key) => {
        payload[key] = parseFloat(val) || 0;
    });

    try {
        const res = await fetch("/api/predict", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
        const result = await res.json();
        LAST_GENERAL_RESULT = result;
        updateGeneralUI(result);
    } catch (err) {
        console.error("General inference error:", err);
    }
}

function updateGeneralUI(res) {
    const dict = I18N[CURRENT_LANG];
    const diagEl = document.getElementById("res-diagnosis");
    const confEl = document.getElementById("res-confidence");
    const riskEl = document.getElementById("res-risk");
    const urgEl = document.getElementById("res-urgency");

    const readableName = (dict.labels_general && dict.labels_general[res.primary_diagnosis]) 
        || res.primary_diagnosis;
    const diseaseColor = COLOR_MAP_GENERAL[res.primary_diagnosis] || "#0071e3";

    diagEl.textContent = readableName;
    diagEl.style.color = diseaseColor;

    confEl.textContent = `${res.confidence}% ${dict.probability}`;
    confEl.style.color = diseaseColor;
    confEl.style.borderColor = `${diseaseColor}33`;
    confEl.style.backgroundColor = `${diseaseColor}12`;

    const translatedRisk = (dict.risk_levels && dict.risk_levels[res.risk_level]) || res.risk_level;
    riskEl.textContent = translatedRisk;
    riskEl.style.color = res.risk_color;
    riskEl.style.backgroundColor = `${res.risk_color}12`;
    riskEl.style.borderColor = `${res.risk_color}38`;

    const translatedUrgency = (dict.urgencies && dict.urgencies[res.urgency]) || res.urgency;
    urgEl.textContent = translatedUrgency;

    const barsContainer = document.getElementById("diff-bars");
    barsContainer.innerHTML = "";

    Object.entries(res.probabilities).forEach(([cls, prob]) => {
        const color = COLOR_MAP_GENERAL[cls] || "#0071e3";
        const name = (dict.labels_general && dict.labels_general[cls]) || cls;
        const row = document.createElement("div");
        row.className = "diff-row";
        row.innerHTML = `
            <div class="diff-header">
                <span class="diff-name">${name}</span>
                <span class="diff-val">${prob.toFixed(1)}%</span>
            </div>
            <div class="diff-bar-track">
                <div class="diff-bar-fill" style="width: ${prob}%; background-color: ${color};"></div>
            </div>
        `;
        barsContainer.appendChild(row);
    });

    const idx = res.indices;
    document.getElementById("idx-vbr").textContent = idx.vbr.toFixed(3);
    document.getElementById("idx-hippo").textContent = `${idx.hippo_atrophy_pct}%`;
    document.getElementById("idx-cog").textContent = `${idx.cognitive_loss_pct}%`;
    document.getElementById("idx-motor").textContent = `${idx.motor_deficit_pct}%`;
    document.getElementById("idx-demyel").textContent = `${idx.demyelination_burden_pct}%`;
}

// ==========================================================================
// Mode 2: MS Subclass Staging
// ==========================================================================
function setupMSForm() {
    const form = document.getElementById("ms-form");
    form.addEventListener("submit", (e) => {
        e.preventDefault();
        triggerMSPrediction();
    });

    form.querySelectorAll("input, select").forEach(input => {
        input.addEventListener("input", debounce(triggerMSPrediction, 250));
    });

    document.getElementById("btn-ms-reset").addEventListener("click", () => {
        loadMSPreset("control_ms");
    });
}

function loadMSPreset(presetKey) {
    const data = MS_PRESETS[presetKey];
    if (!data) return;

    const fields = [
        "age", "gender", "onset_presentation", "disease_duration_months",
        "relapse_rate_arr", "edss", "fss_fatigue", "sdmt_cognitive",
        "t2_lesion_count", "periventricular_lesions", "juxtacortical_lesions",
        "infratentorial_lesions", "spinal_cord_lesions", "gd_enhancing_lesions",
        "t1_black_holes", "n_wbv", "thalamus_vol_mm3", "oligoclonal_bands",
        "serum_nfl_pg_ml", "csf_igg_index"
    ];

    fields.forEach(field => {
        const el = document.getElementById(`ms_${field}`);
        if (el && data[field] !== undefined) {
            el.value = data[field];
        }
    });

    triggerMSPrediction();
}

async function triggerMSPrediction() {
    const form = document.getElementById("ms-form");
    const formData = new FormData(form);
    const payload = {};
    formData.forEach((val, key) => {
        payload[key] = parseFloat(val) || 0;
    });

    try {
        const res = await fetch("/api/predict_ms_subclass", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
        const result = await res.json();
        LAST_MS_RESULT = result;
        updateMSUI(result);
    } catch (err) {
        console.error("MS inference error:", err);
    }
}

function updateMSUI(res) {
    const dict = I18N[CURRENT_LANG];
    const subEl = document.getElementById("ms-res-subclass");
    const confEl = document.getElementById("ms-res-confidence");
    const dmtEl = document.getElementById("ms-res-dmt");

    const readableName = (dict.labels_ms && dict.labels_ms[res.primary_subclass]) 
        || res.primary_subclass;
    const subColor = COLOR_MAP_MS[res.primary_subclass] || "#0071e3";

    subEl.textContent = readableName;
    subEl.style.color = subColor;

    confEl.textContent = `${res.confidence}% ${dict.probability}`;
    confEl.style.color = subColor;
    confEl.style.borderColor = `${subColor}33`;
    confEl.style.backgroundColor = `${subColor}12`;

    const translatedDmt = (dict.dmt && dict.dmt[res.dmt_recommendation]) || res.dmt_recommendation;
    dmtEl.textContent = translatedDmt;

    // McDonald 2017 Badges
    const mc = res.mcdonald_criteria;
    const disBadge = document.getElementById("mcdonald-dis-badge");
    const ditBadge = document.getElementById("mcdonald-dit-badge");

    const disStatus = document.getElementById("mcdonald-dis-status");
    const ditStatus = document.getElementById("mcdonald-dit-status");

    if (mc.dis_met) {
        disBadge.className = "mcdonald-badge met";
        disStatus.textContent = dict.mcdonald.dis_met(mc.dis_zones_count);
    } else {
        disBadge.className = "mcdonald-badge unmet";
        disStatus.textContent = dict.mcdonald.dis_unmet(mc.dis_zones_count);
    }

    if (mc.dit_met) {
        ditBadge.className = "mcdonald-badge met";
        let ditReason = dict.mcdonald.dit_met_ocb;
        if (mc.has_active_gd && mc.has_ocb_type2) {
            ditReason = dict.mcdonald.dit_met_both;
        } else if (mc.has_active_gd) {
            ditReason = dict.mcdonald.dit_met_gd;
        }
        ditStatus.textContent = ditReason;
    } else {
        ditBadge.className = "mcdonald-badge unmet";
        ditStatus.textContent = dict.mcdonald.dit_unmet;
    }

    // Subclass probability distribution bars
    const barsContainer = document.getElementById("ms-diff-bars");
    barsContainer.innerHTML = "";

    Object.entries(res.probabilities).forEach(([cls, prob]) => {
        const color = COLOR_MAP_MS[cls] || "#0071e3";
        const name = (dict.labels_ms && dict.labels_ms[cls]) || cls;
        const row = document.createElement("div");
        row.className = "diff-row";
        row.innerHTML = `
            <div class="diff-header">
                <span class="diff-name">${name}</span>
                <span class="diff-val">${prob.toFixed(1)}%</span>
            </div>
            <div class="diff-bar-track">
                <div class="diff-bar-fill" style="width: ${prob}%; background-color: ${color};"></div>
            </div>
        `;
        barsContainer.appendChild(row);
    });

    // Composite Indices
    const idx = res.indices;
    document.getElementById("ms-idx-inflam").textContent = idx.acute_inflammatory_activity.toFixed(2);
    document.getElementById("ms-idx-neurodeg").textContent = idx.neurodegenerative_burden.toFixed(2);
    document.getElementById("ms-idx-prog").textContent = idx.disability_accumulation_rate.toFixed(2);
    document.getElementById("ms-idx-fatigue").textContent = idx.cognitive_fatigue_composite.toFixed(1);
}

function triggerActivePrediction() {
    if (CURRENT_MODE === "general") {
        triggerGeneralPrediction();
    } else {
        triggerMSPrediction();
    }
}

function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}
