// -*- coding: utf-8 -*-
/**
 * NeuroAI Studio Client Application.
 * Handles preset switching, form synchronization, and real-time prediction.
 */

let PRESETS_DATA = {};

const COLOR_MAP = {
    "Control": "#10b981",
    "MCI": "#eab308",
    "Alzheimers": "#ef4444",
    "Parkinsons": "#a855f7",
    "Multiple_Sclerosis": "#06b6d4",
    "ALS": "#f43f5e"
};

const LABEL_MAP = {
    "Control": "Healthy Control",
    "MCI": "Mild Cognitive Impairment",
    "Alzheimers": "Alzheimer's Disease",
    "Parkinsons": "Parkinson's Disease",
    "Multiple_Sclerosis": "Multiple Sclerosis",
    "ALS": "Amyotrophic Lateral Sclerosis"
};

document.addEventListener("DOMContentLoaded", async () => {
    await fetchPresets();
    setupEventListeners();
    triggerPrediction();
});

async function fetchPresets() {
    try {
        const res = await fetch("/api/presets");
        PRESETS_DATA = await res.json();
        renderPresetButtons();
    } catch (err) {
        console.error("Error fetching presets:", err);
    }
}

function renderPresetButtons() {
    const container = document.getElementById("presets-container");
    container.innerHTML = "";

    Object.keys(PRESETS_DATA).forEach((key, idx) => {
        const item = PRESETS_DATA[key];
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = `btn-preset ${idx === 1 ? 'active' : ''}`;
        btn.innerHTML = `<span class="preset-badge">${item.category}</span> ${item.title}`;
        btn.addEventListener("click", () => loadPreset(key, btn));
        container.appendChild(btn);
    });
}

function loadPreset(presetKey, activeBtn) {
    const data = PRESETS_DATA[presetKey];
    if (!data) return;

    document.querySelectorAll(".btn-preset").forEach(b => b.classList.remove("active"));
    if (activeBtn) activeBtn.classList.add("active");

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

    triggerPrediction();
}

function setupEventListeners() {
    const form = document.getElementById("patient-form");
    form.addEventListener("submit", (e) => {
        e.preventDefault();
        triggerPrediction();
    });

    // Real-time live updating on input change
    form.querySelectorAll("input, select").forEach(input => {
        input.addEventListener("input", debounce(triggerPrediction, 250));
    });

    document.getElementById("btn-reset").addEventListener("click", () => {
        loadPreset("control", document.querySelector(".btn-preset"));
    });
}

async function triggerPrediction() {
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
        updateUI(result);
    } catch (err) {
        console.error("Inference error:", err);
    }
}

function updateUI(res) {
    const diagEl = document.getElementById("res-diagnosis");
    const confEl = document.getElementById("res-confidence");
    const riskEl = document.getElementById("res-risk");
    const urgEl = document.getElementById("res-urgency");

    const readableName = LABEL_MAP[res.primary_diagnosis] || res.primary_diagnosis;
    diagEl.textContent = readableName;
    diagEl.style.color = COLOR_MAP[res.primary_diagnosis] || "#38bdf8";

    confEl.textContent = `${res.confidence}% Probability`;
    riskEl.textContent = res.risk_level;
    riskEl.style.color = res.risk_color;
    riskEl.style.backgroundColor = `${res.risk_color}22`;
    riskEl.style.borderColor = res.risk_color;

    urgEl.textContent = res.urgency;

    // Differential bars
    const barsContainer = document.getElementById("diff-bars");
    barsContainer.innerHTML = "";

    Object.entries(res.probabilities).forEach(([cls, prob]) => {
        const color = COLOR_MAP[cls] || "#38bdf8";
        const name = LABEL_MAP[cls] || cls;
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
    document.getElementById("idx-vbr").textContent = idx.vbr.toFixed(3);
    document.getElementById("idx-hippo").textContent = `${idx.hippo_atrophy_pct}%`;
    document.getElementById("idx-cog").textContent = `${idx.cognitive_loss_pct}%`;
    document.getElementById("idx-motor").textContent = `${idx.motor_deficit_pct}%`;
    document.getElementById("idx-demyel").textContent = `${idx.demyelination_burden_pct}%`;
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
