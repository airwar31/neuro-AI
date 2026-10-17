// -*- coding: utf-8 -*-
/**
 * NeuroAI Studio Client Application.
 * Dual Mode Architecture:
 * 1. General Neurological Screening (6 Diseases)
 * 2. Precision Multiple Sclerosis Staging & Subtype Classifier (5 Subclasses)
 */

let CURRENT_MODE = "general"; // "general" or "ms"
let GENERAL_PRESETS = {};
let MS_PRESETS = {};

const COLOR_MAP_GENERAL = {
    "Control": "#10b981",
    "MCI": "#eab308",
    "Alzheimers": "#ef4444",
    "Parkinsons": "#a855f7",
    "Multiple_Sclerosis": "#06b6d4",
    "ALS": "#f43f5e"
};

const LABEL_MAP_GENERAL = {
    "Control": "Healthy Control",
    "MCI": "Mild Cognitive Impairment",
    "Alzheimers": "Alzheimer's Disease",
    "Parkinsons": "Parkinson's Disease",
    "Multiple_Sclerosis": "Multiple Sclerosis",
    "ALS": "Amyotrophic Lateral Sclerosis"
};

const COLOR_MAP_MS = {
    "Control": "#10b981",
    "CIS": "#38bdf8",
    "RRMS": "#f97316",
    "SPMS": "#ef4444",
    "PPMS": "#a855f7"
};

const LABEL_MAP_MS = {
    "Control": "Healthy Control (Intact)",
    "CIS": "Clinically Isolated Syndrome (CIS)",
    "RRMS": "Relapsing-Remitting MS (RRMS)",
    "SPMS": "Secondary Progressive MS (SPMS)",
    "PPMS": "Primary Progressive MS (PPMS)"
};

document.addEventListener("DOMContentLoaded", async () => {
    await fetchAllPresets();
    setupTabSwitching();
    setupGeneralForm();
    setupMSForm();
    renderActivePresetRibbon();
    triggerActivePrediction();
});

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

    tabGen.addEventListener("click", () => {
        CURRENT_MODE = "general";
        tabGen.classList.add("active");
        tabMS.classList.remove("active");
        viewGen.style.display = "block";
        viewMS.style.display = "none";
        document.getElementById("presets-label-text").textContent = "Select General Screening Archetype:";
        renderActivePresetRibbon();
        triggerGeneralPrediction();
    });

    tabMS.addEventListener("click", () => {
        CURRENT_MODE = "ms";
        tabMS.classList.add("active");
        tabGen.classList.remove("active");
        viewGen.style.display = "none";
        viewMS.style.display = "block";
        document.getElementById("presets-label-text").textContent = "Select Multiple Sclerosis Phenotype Preset:";
        renderActivePresetRibbon();
        triggerMSPrediction();
    });
}

function renderActivePresetRibbon() {
    const container = document.getElementById("presets-container");
    container.innerHTML = "";

    const activeDict = CURRENT_MODE === "general" ? GENERAL_PRESETS : MS_PRESETS;
    const defaultKey = CURRENT_MODE === "general" ? "control" : "rrms";

    Object.keys(activeDict).forEach((key) => {
        const item = activeDict[key];
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = `btn-preset ${key === defaultKey ? 'active' : ''}`;
        btn.innerHTML = `<span class="preset-badge">${item.category || 'Clinical'}</span> ${item.title}`;
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

// ----------------- GENERAL MODE -----------------
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
        updateGeneralUI(result);
    } catch (err) {
        console.error("General inference error:", err);
    }
}

function updateGeneralUI(res) {
    const diagEl = document.getElementById("res-diagnosis");
    const confEl = document.getElementById("res-confidence");
    const riskEl = document.getElementById("res-risk");
    const urgEl = document.getElementById("res-urgency");

    const readableName = LABEL_MAP_GENERAL[res.primary_diagnosis] || res.primary_diagnosis;
    diagEl.textContent = readableName;
    diagEl.style.color = COLOR_MAP_GENERAL[res.primary_diagnosis] || "#38bdf8";

    confEl.textContent = `${res.confidence}% Probability`;
    riskEl.textContent = res.risk_level;
    riskEl.style.color = res.risk_color;
    riskEl.style.backgroundColor = `${res.risk_color}22`;
    riskEl.style.borderColor = res.risk_color;

    urgEl.textContent = res.urgency;

    const barsContainer = document.getElementById("diff-bars");
    barsContainer.innerHTML = "";

    Object.entries(res.probabilities).forEach(([cls, prob]) => {
        const color = COLOR_MAP_GENERAL[cls] || "#38bdf8";
        const name = LABEL_MAP_GENERAL[cls] || cls;
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

// ----------------- MS SUBCLASS MODE -----------------
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
        updateMSUI(result);
    } catch (err) {
        console.error("MS inference error:", err);
    }
}

function updateMSUI(res) {
    const subEl = document.getElementById("ms-res-subclass");
    const confEl = document.getElementById("ms-res-confidence");
    const dmtEl = document.getElementById("ms-res-dmt");

    const readableName = LABEL_MAP_MS[res.primary_subclass] || res.primary_subclass;
    subEl.textContent = readableName;
    subEl.style.color = res.subclass_color || "#38bdf8";

    confEl.textContent = `${res.confidence}% Probability`;
    dmtEl.textContent = res.dmt_recommendation;

    // McDonald 2017 Badges
    const mc = res.mcdonald_criteria;
    const disBadge = document.getElementById("mcdonald-dis-badge");
    const ditBadge = document.getElementById("mcdonald-dit-badge");

    const disStatus = document.getElementById("mcdonald-dis-status");
    const ditStatus = document.getElementById("mcdonald-dit-status");

    if (mc.dis_met) {
        disBadge.className = "mcdonald-badge met";
        disStatus.textContent = `CRITERIA MET (${mc.dis_zones_count}/4 anatomical zones)`;
    } else {
        disBadge.className = "mcdonald-badge unmet";
        disStatus.textContent = `UNMET (${mc.dis_zones_count}/4 zones, requires >= 2)`;
    }

    if (mc.dit_met) {
        ditBadge.className = "mcdonald-badge met";
        const reason = mc.has_active_gd && mc.has_ocb_type2 ? "Gd+ and CSF OCB Type 2" : (mc.has_active_gd ? "Active Gd+ Lesion" : "CSF OCB Type 2");
        ditStatus.textContent = `CRITERIA MET (${reason})`;
    } else {
        ditBadge.className = "mcdonald-badge unmet";
        ditStatus.textContent = "UNMET (No Gd+ enhancing lesion & OCB negative)";
    }

    // Subclass probability distribution bars
    const barsContainer = document.getElementById("ms-diff-bars");
    barsContainer.innerHTML = "";

    Object.entries(res.probabilities).forEach(([cls, prob]) => {
        const color = COLOR_MAP_MS[cls] || "#38bdf8";
        const name = LABEL_MAP_MS[cls] || cls;
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

    // Indices
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
