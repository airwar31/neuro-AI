# -*- coding: utf-8 -*-
"""
NeuroAI Studio - Multimodal Clinical Diagnostic Dashboard.
Real-time risk assessment, differential diagnosis probabilities, and Obsidian links.
"""

import os
import sys
import math
import numpy as np
from flask import Flask, render_template, request, jsonify

# Guarantee local imports
REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if REPO_ROOT not in sys.path:
    sys.path.insert(0, REPO_ROOT)

from src.features import compute_clinical_indices
from src.models import ClinicalEnsemblePredictor, FEATURE_NAMES, CLASSES

app = Flask(__name__)

# Attempt to load trained production ensemble
MODEL_PATH = os.path.join(REPO_ROOT, "models", "classifier.joblib")
TRAINED_MODEL = None
try:
    if os.path.exists(MODEL_PATH):
        TRAINED_MODEL = ClinicalEnsemblePredictor.load(MODEL_PATH)
        print(f"[INFO] Loaded production ML ensemble from {MODEL_PATH}")
except Exception as err:
    print(f"[WARN] Falling back to expert decision rules: {err}")

# 6 Clinical Patient Archetypes
PRESETS = {
    "control": {
        "title": "Healthy Control",
        "desc": "Intact cognitive profile (MMSE 29, MoCA 29), normal motor exams, preserved hippocampal volume, no demyelination.",
        "category": "Normal Aging",
        "age": 62, "gender": 0, "mmse": 29, "moca": 29,
        "updrs_motor": 1.0, "edss_score": 0.0, "alsfrs_r": 48.0,
        "fatigue_score": 2.0, "symptom_duration_months": 0.0,
        "n_wbv": 0.79, "hippocampus_vol": 4200, "ventricles_vol": 24000,
        "t2_lesions_count": 0, "swi_swallow_tail_loss": 0,
        "motor_cortex_atrophy": 0.05, "oligoclonal_bands": 0
    },
    "mci": {
        "title": "Mild Cognitive Impairment (Prodromal AD)",
        "desc": "Subtle episodic memory decline (MoCA 22, MMSE 25), mild hippocampal volume loss, preserved motor scales.",
        "category": "Neurodegenerative (Early)",
        "age": 71, "gender": 1, "mmse": 25, "moca": 22,
        "updrs_motor": 2.0, "edss_score": 0.0, "alsfrs_r": 48.0,
        "fatigue_score": 2.8, "symptom_duration_months": 18.0,
        "n_wbv": 0.74, "hippocampus_vol": 3350, "ventricles_vol": 36000,
        "t2_lesions_count": 0, "swi_swallow_tail_loss": 0,
        "motor_cortex_atrophy": 0.08, "oligoclonal_bands": 0
    },
    "alzheimers": {
        "title": "Alzheimer's Disease (Clinical Stage)",
        "desc": "Marked amnesia (MMSE 17, MoCA 14), severe bilateral hippocampal atrophy, ex-vacuo ventricular enlargement.",
        "category": "Neurodegenerative (Dementia)",
        "age": 78, "gender": 0, "mmse": 17, "moca": 14,
        "updrs_motor": 5.0, "edss_score": 0.0, "alsfrs_r": 44.0,
        "fatigue_score": 3.4, "symptom_duration_months": 48.0,
        "n_wbv": 0.67, "hippocampus_vol": 2400, "ventricles_vol": 58000,
        "t2_lesions_count": 1, "swi_swallow_tail_loss": 0,
        "motor_cortex_atrophy": 0.15, "oligoclonal_bands": 0
    },
    "parkinsons": {
        "title": "Parkinson's Disease (Motor Stage)",
        "desc": "Resting tremor, cogwheel rigidity (MDS-UPDRS 44), loss of swallow-tail sign on SWI, preserved cognition.",
        "category": "Movement Disorder",
        "age": 67, "gender": 1, "mmse": 28, "moca": 25,
        "updrs_motor": 44.0, "edss_score": 1.0, "alsfrs_r": 42.0,
        "fatigue_score": 4.5, "symptom_duration_months": 36.0,
        "n_wbv": 0.75, "hippocampus_vol": 3900, "ventricles_vol": 29000,
        "t2_lesions_count": 0, "swi_swallow_tail_loss": 1,
        "motor_cortex_atrophy": 0.18, "oligoclonal_bands": 0
    },
    "ms": {
        "title": "Multiple Sclerosis (Active RRMS/SPMS)",
        "desc": "Multiple T2 periventricular lesions (Dawson fingers), elevated EDSS (3.5), positive CSF oligoclonal bands, severe fatigue.",
        "category": "Autoimmune Demyelinating",
        "age": 36, "gender": 0, "mmse": 28, "moca": 27,
        "updrs_motor": 4.0, "edss_score": 3.5, "alsfrs_r": 44.0,
        "fatigue_score": 5.8, "symptom_duration_months": 42.0,
        "n_wbv": 0.73, "hippocampus_vol": 3800, "ventricles_vol": 33000,
        "t2_lesions_count": 19, "swi_swallow_tail_loss": 0,
        "motor_cortex_atrophy": 0.15, "oligoclonal_bands": 1
    },
    "als": {
        "title": "Amyotrophic Lateral Sclerosis (ALS)",
        "desc": "Rapid motor collapse (ALSFRS-R 24/48), precentral gyrus cortical thinning, fasciculations, intact memory.",
        "category": "Motor Neuron Disease",
        "age": 59, "gender": 1, "mmse": 29, "moca": 28,
        "updrs_motor": 12.0, "edss_score": 2.5, "alsfrs_r": 24.0,
        "fatigue_score": 5.0, "symptom_duration_months": 14.0,
        "n_wbv": 0.75, "hippocampus_vol": 3950, "ventricles_vol": 28000,
        "t2_lesions_count": 0, "swi_swallow_tail_loss": 0,
        "motor_cortex_atrophy": 0.88, "oligoclonal_bands": 0
    }
}


def evaluate_patient(data: dict) -> dict:
    """Evaluates multimodal inputs using the trained ML ensemble or calibrated rule fallback."""
    # 1. Feature Engineering
    derived = compute_clinical_indices(data)
    full_dict = {**data, **derived}

    # Extract numerical inputs for indices
    age = float(data.get('age', 60))
    mmse = float(data.get('mmse', 28))
    moca = float(data.get('moca', 27))
    updrs = float(data.get('updrs_motor', 0))
    edss = float(data.get('edss_score', 0))
    alsfrs = float(data.get('alsfrs_r', 48))
    hippo = float(data.get('hippocampus_vol', 4000))
    vent = float(data.get('ventricles_vol', 25000))
    lesions = int(data.get('t2_lesions_count', 0))
    ocb = int(data.get('oligoclonal_bands', 0))

    if TRAINED_MODEL is not None and TRAINED_MODEL.is_fitted:
        # Machine learning inference
        x_vec = np.array([[float(full_dict.get(k, 0.0)) for k in FEATURE_NAMES]])
        probs_raw = TRAINED_MODEL.predict_proba(x_vec)[0]
        probs = {cls: round(float(probs_raw[i]) * 100.0, 2) for i, cls in enumerate(TRAINED_MODEL.classes_)}
    else:
        # Clinical Rule Calibrator fallback
        scores = {"Control": 1.0, "MCI": 0.6, "Alzheimers": 0.4, "Parkinsons": 0.3, "Multiple_Sclerosis": 0.3, "ALS": 0.2}
        if lesions >= 5 or ocb == 1 or (edss >= 2.0 and age < 55):
            scores["Multiple_Sclerosis"] += (lesions * 0.45) + (ocb * 7.0) + (edss * 1.5)
        if updrs >= 20 or int(data.get('swi_swallow_tail_loss', 0)) == 1:
            scores["Parkinsons"] += (updrs * 0.35) + 9.0
        if alsfrs < 38 or float(data.get('motor_cortex_atrophy', 0)) > 0.5:
            scores["ALS"] += ((48.0 - alsfrs) * 0.45) + 10.0
        if mmse < 27 or moca < 25 or hippo < 3400:
            if mmse < 21 or hippo < 2700:
                scores["Alzheimers"] += ((30.0 - mmse) * 0.6) + ((4200.0 - hippo) / 200.0)
            else:
                scores["MCI"] += ((30.0 - moca) * 0.5) + ((4200.0 - hippo) / 280.0)
        if mmse >= 28 and moca >= 27 and updrs <= 5 and alsfrs >= 47 and lesions == 0 and hippo > 3800 and edss == 0:
            scores["Control"] += 12.0

        max_s = max(scores.values())
        exp_s = {k: math.exp(v - max_s) for k, v in scores.items()}
        tot_exp = sum(exp_s.values())
        probs = {k: round((v / tot_exp) * 100.0, 2) for k, v in exp_s.items()}

    sorted_p = sorted(probs.items(), key=lambda x: x[1], reverse=True)
    top_label, top_conf = sorted_p[0]

    # Clinical Risk Category
    if top_label in ["Alzheimers", "ALS"]:
        risk_level = "High Priority (Severe / Neurodegenerative)"
        risk_color = "#ef4444"
        urgency = "Urgent multidisciplinary neurological and palliative care evaluation required."
    elif top_label in ["Multiple_Sclerosis", "Parkinsons"]:
        risk_level = "Moderate to High (Specialist Intervention Needed)"
        risk_color = "#f97316"
        urgency = "Confirm with lumbar puncture / SWI & initiate targeted disease-modifying therapy."
    elif top_label == "MCI":
        risk_level = "Mild Impairment (Active Monitoring & Prevention)"
        risk_color = "#eab308"
        urgency = "Conduct serial MoCA follow-up and lifestyle/cognitive interventions."
    else:
        risk_level = "Low Risk (Within Normal Limits)"
        risk_color = "#10b981"
        urgency = "Age-appropriate physiological status; continue regular preventative health checks."

    # Derived Indices
    vbr = vent / (hippo * 10.0 + 1e-5)
    hippo_loss = max(0.0, (4400.0 - hippo) / 4400.0) * 100.0
    cog_loss = max(0.0, ((30.0 - mmse) * 0.5 + (30.0 - moca) * 0.5) / 30.0) * 100.0
    motor_deficit = max(min(100.0, (updrs / 80.0 * 50.0) + ((48.0 - alsfrs) / 48.0 * 50.0)), 0.0)
    demyelination_pct = min(100.0, (lesions / 30.0 * 50.0) + (ocb * 30.0) + (edss / 8.0 * 20.0))

    return {
        "primary_diagnosis": top_label,
        "confidence": top_conf,
        "risk_level": risk_level,
        "risk_color": risk_color,
        "urgency": urgency,
        "probabilities": dict(sorted_p),
        "indices": {
            "vbr": round(vbr, 3),
            "hippo_atrophy_pct": round(hippo_loss, 1),
            "cognitive_loss_pct": round(cog_loss, 1),
            "motor_deficit_pct": round(motor_deficit, 1),
            "demyelination_burden_pct": round(demyelination_pct, 1)
        }
    }


@app.route("/")
def index():
    return render_template("index.html", presets=PRESETS)


@app.route("/api/predict", methods=["POST"])
def predict():
    payload = request.get_json() or {}
    res = evaluate_patient(payload)
    return jsonify(res)


@app.route("/api/presets", methods=["GET"])
def get_presets():
    return jsonify(PRESETS)


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5060))
    print(f"Starting NeuroAI Multimodal Studio on http://127.0.0.1:{port}")
    app.run(host="0.0.0.0", port=port, debug=False)
