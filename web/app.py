# -*- coding: utf-8 -*-
"""
NeuroAI Studio - Multimodal Clinical Diagnostic Dashboard.
Dual-Engine Architecture:
1. Multimodal General Neurological Screening (AD, PD, MS, ALS, MCI, Control)
2. Precision Multiple Sclerosis Staging & Subtype Classifier (Kaggle Benchmark: Control, CIS, RRMS, SPMS, PPMS)
"""

import os
import sys
import math
import numpy as np
import pandas as pd
from flask import Flask, render_template, request, jsonify

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if REPO_ROOT not in sys.path:
    sys.path.insert(0, REPO_ROOT)

from src.features import compute_clinical_indices
from src.models import ClinicalEnsemblePredictor, FEATURE_NAMES, CLASSES
from src.ms_subclasses import MSSubclassClassifier, compute_ms_single_record, RAW_FEATURES, MS_CLASSES

app = Flask(__name__)

# 1. Load General Neurological Model
GENERAL_MODEL_PATH = os.path.join(REPO_ROOT, "models", "classifier.joblib")
TRAINED_GENERAL_MODEL = None
try:
    if os.path.exists(GENERAL_MODEL_PATH):
        TRAINED_GENERAL_MODEL = ClinicalEnsemblePredictor.load(GENERAL_MODEL_PATH)
        print(f"[INFO] Loaded general neurological ML ensemble from {GENERAL_MODEL_PATH}")
except Exception as err:
    print(f"[WARN] General model load notice: {err}")

# 2. Load Multiple Sclerosis Subclass Classifier
MS_MODEL_PATH = os.path.join(REPO_ROOT, "models", "ms_subclass_classifier.joblib")
TRAINED_MS_MODEL = None
try:
    if os.path.exists(MS_MODEL_PATH):
        TRAINED_MS_MODEL = MSSubclassClassifier.load(MS_MODEL_PATH)
        print(f"[INFO] Loaded MS Subclass ensemble from {MS_MODEL_PATH}")
except Exception as err:
    print(f"[WARN] MS model load notice: {err}")

# General Neurological Presets (6 Diseases)
GENERAL_PRESETS = {
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
        "title": "Mild Cognitive Impairment",
        "desc": "Subtle episodic memory decline (MoCA 22, MMSE 25), mild hippocampal volume loss, preserved motor scales.",
        "category": "Early Neurodegenerative",
        "age": 71, "gender": 1, "mmse": 25, "moca": 22,
        "updrs_motor": 2.0, "edss_score": 0.0, "alsfrs_r": 48.0,
        "fatigue_score": 2.8, "symptom_duration_months": 18.0,
        "n_wbv": 0.74, "hippocampus_vol": 3350, "ventricles_vol": 36000,
        "t2_lesions_count": 0, "swi_swallow_tail_loss": 0,
        "motor_cortex_atrophy": 0.08, "oligoclonal_bands": 0
    },
    "alzheimers": {
        "title": "Alzheimer's Disease",
        "desc": "Marked amnesia (MMSE 17, MoCA 14), severe bilateral hippocampal atrophy, ex-vacuo ventricular enlargement.",
        "category": "Cortical Dementia",
        "age": 78, "gender": 0, "mmse": 17, "moca": 14,
        "updrs_motor": 5.0, "edss_score": 0.0, "alsfrs_r": 44.0,
        "fatigue_score": 3.4, "symptom_duration_months": 48.0,
        "n_wbv": 0.67, "hippocampus_vol": 2400, "ventricles_vol": 58000,
        "t2_lesions_count": 1, "swi_swallow_tail_loss": 0,
        "motor_cortex_atrophy": 0.15, "oligoclonal_bands": 0
    },
    "parkinsons": {
        "title": "Parkinson's Disease",
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
        "title": "Multiple Sclerosis (General)",
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
        "title": "Amyotrophic Lateral Sclerosis",
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

# Multiple Sclerosis Subclass Presets (5 Subclasses)
MS_PRESETS = {
    "control_ms": {
        "title": "Healthy Control",
        "category": "Intact Myelin",
        "desc": "Intact white matter, zero demyelination, normal neurofilaments (8.5 pg/ml), OCB negative.",
        "age": 32, "gender": 0, "onset_presentation": 0, "disease_duration_months": 0.0,
        "relapse_rate_arr": 0.0, "edss": 0.0, "fss_fatigue": 1.5, "sdmt_cognitive": 64.0,
        "t2_lesion_count": 0, "periventricular_lesions": 0, "juxtacortical_lesions": 0,
        "infratentorial_lesions": 0, "spinal_cord_lesions": 0, "gd_enhancing_lesions": 0,
        "t1_black_holes": 0, "n_wbv": 0.81, "thalamus_vol_mm3": 14800,
        "oligoclonal_bands": 0, "serum_nfl_pg_ml": 8.5, "csf_igg_index": 0.45
    },
    "cis": {
        "title": "Clinically Isolated Syndrome (CIS)",
        "category": "First Episode",
        "desc": "First acute optic neuritis attack, 4 T2 lesions (2 periventricular), OCB positive, mild sNfL elevation.",
        "age": 28, "gender": 0, "onset_presentation": 1, "disease_duration_months": 3.0,
        "relapse_rate_arr": 0.0, "edss": 1.0, "fss_fatigue": 3.0, "sdmt_cognitive": 58.0,
        "t2_lesion_count": 4, "periventricular_lesions": 2, "juxtacortical_lesions": 1,
        "infratentorial_lesions": 0, "spinal_cord_lesions": 0, "gd_enhancing_lesions": 1,
        "t1_black_holes": 0, "n_wbv": 0.78, "thalamus_vol_mm3": 13900,
        "oligoclonal_bands": 1, "serum_nfl_pg_ml": 22.0, "csf_igg_index": 0.85
    },
    "rrms": {
        "title": "Relapsing-Remitting MS (RRMS)",
        "category": "Active Relapsing",
        "desc": "Frequent relapses (ARR 1.8), multiple Dawson fingers, 3 active Gd+ lesions, sNfL spike (68 pg/ml).",
        "age": 34, "gender": 0, "onset_presentation": 2, "disease_duration_months": 48.0,
        "relapse_rate_arr": 1.8, "edss": 2.5, "fss_fatigue": 5.2, "sdmt_cognitive": 49.0,
        "t2_lesion_count": 18, "periventricular_lesions": 9, "juxtacortical_lesions": 4,
        "infratentorial_lesions": 2, "spinal_cord_lesions": 2, "gd_enhancing_lesions": 3,
        "t1_black_holes": 2, "n_wbv": 0.74, "thalamus_vol_mm3": 12200,
        "oligoclonal_bands": 1, "serum_nfl_pg_ml": 68.0, "csf_igg_index": 1.20
    },
    "spms": {
        "title": "Secondary Progressive MS (SPMS)",
        "category": "Chronic Neurodegenerative",
        "desc": "Insidious progression after initial relapsing phase, EDSS 6.0, 11 T1 black holes, marked thalamic atrophy.",
        "age": 51, "gender": 0, "onset_presentation": 3, "disease_duration_months": 180.0,
        "relapse_rate_arr": 0.2, "edss": 6.0, "fss_fatigue": 6.8, "sdmt_cognitive": 34.0,
        "t2_lesion_count": 28, "periventricular_lesions": 14, "juxtacortical_lesions": 6,
        "infratentorial_lesions": 4, "spinal_cord_lesions": 4, "gd_enhancing_lesions": 0,
        "t1_black_holes": 11, "n_wbv": 0.68, "thalamus_vol_mm3": 9400,
        "oligoclonal_bands": 1, "serum_nfl_pg_ml": 28.0, "csf_igg_index": 1.05
    },
    "ppms": {
        "title": "Primary Progressive MS (PPMS)",
        "category": "Insidious Primary",
        "desc": "Steady motor decline from onset, spinal cord lesion burden (5 lesions), preserved cerebral volume relative to cord.",
        "age": 48, "gender": 1, "onset_presentation": 3, "disease_duration_months": 72.0,
        "relapse_rate_arr": 0.0, "edss": 5.5, "fss_fatigue": 5.9, "sdmt_cognitive": 44.0,
        "t2_lesion_count": 14, "periventricular_lesions": 5, "juxtacortical_lesions": 2,
        "infratentorial_lesions": 2, "spinal_cord_lesions": 5, "gd_enhancing_lesions": 0,
        "t1_black_holes": 4, "n_wbv": 0.71, "thalamus_vol_mm3": 10800,
        "oligoclonal_bands": 1, "serum_nfl_pg_ml": 24.0, "csf_igg_index": 0.95
    }
}


def evaluate_general_patient(data: dict) -> dict:
    """Evaluates multimodal inputs for 6 general neurological diseases."""
    derived = compute_clinical_indices(data)
    full_dict = {**data, **derived}

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

    if TRAINED_GENERAL_MODEL is not None and TRAINED_GENERAL_MODEL.is_fitted:
        x_vec = np.array([[float(full_dict.get(k, 0.0)) for k in FEATURE_NAMES]])
        probs_raw = TRAINED_GENERAL_MODEL.predict_proba(x_vec)[0]
        probs = {cls: round(float(probs_raw[i]) * 100.0, 2) for i, cls in enumerate(TRAINED_GENERAL_MODEL.classes_)}
    else:
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


def evaluate_ms_patient(data: dict) -> dict:
    """Evaluates patient for MS Phenotypic Subclasses (Control, CIS, RRMS, SPMS, PPMS)."""
    derived = compute_ms_single_record(data)

    if TRAINED_MS_MODEL is not None and TRAINED_MS_MODEL.is_fitted:
        df_single = pd.DataFrame([{k: float(data.get(k, 0.0)) for k in RAW_FEATURES}])
        probs_raw = TRAINED_MS_MODEL.predict_proba(df_single)[0]
        prob_dict = {cls: round(float(probs_raw[i]) * 100.0, 2) for i, cls in enumerate(TRAINED_MS_MODEL.classes_)}
    else:
        arr = float(data.get('relapse_rate_arr', 0.0))
        edss = float(data.get('edss', 0.0))
        bh = int(data.get('t1_black_holes', 0))
        t2 = int(data.get('t2_lesion_count', 0))
        gd = int(data.get('gd_enhancing_lesions', 0))
        ocb = int(data.get('oligoclonal_bands', 0))
        sc = int(data.get('spinal_cord_lesions', 0))

        scores = {"Control": 1.0, "CIS": 0.5, "RRMS": 0.5, "SPMS": 0.5, "PPMS": 0.5}
        if t2 == 0 and ocb == 0 and edss == 0:
            scores["Control"] += 15.0
        elif t2 <= 5 and arr == 0 and edss <= 1.5:
            scores["CIS"] += 12.0
        elif arr >= 0.8 or gd >= 1:
            scores["RRMS"] += 14.0 + (arr * 4.0) + (gd * 2.0)
        elif edss >= 4.5 and bh >= 4:
            scores["SPMS"] += 14.0 + (edss * 2.0) + (bh * 1.5)
        elif arr == 0 and sc >= 3 and edss >= 3.5:
            scores["PPMS"] += 14.0 + (sc * 3.0) + (edss * 1.5)

        tot = sum(math.exp(v) for v in scores.values())
        prob_dict = {k: round((math.exp(v) / tot) * 100.0, 2) for k, v in scores.items()}

    sorted_p = sorted(prob_dict.items(), key=lambda x: x[1], reverse=True)
    top_label, top_conf = sorted_p[0]

    if top_label == "Control":
        status_text = "Physiologically Normal / No Evidence of Demyelination"
        dmt_recommendation = "No Disease-Modifying Therapy indicated. Continue standard preventative surveillance."
        subclass_color = "#10b981"
    elif top_label == "CIS":
        status_text = "Clinically Isolated Syndrome (High Conversion Risk to CDMS)"
        dmt_recommendation = "Early initiation of Platform or High-Efficacy DMT (Interferon-beta / Ocrelizumab / Natalizumab) based on OCB status."
        subclass_color = "#38bdf8"
    elif top_label == "RRMS":
        status_text = "Relapsing-Remitting Multiple Sclerosis (Active Inflammatory Phase)"
        dmt_recommendation = "High-Efficacy DMTs (Anti-CD20: Ocrelizumab / Ofatumumab, or S1P Modulators). Monitor sNfL and 3T MRI annually."
        subclass_color = "#f97316"
    elif top_label == "SPMS":
        status_text = "Secondary Progressive Multiple Sclerosis (Neurodegenerative Dominance)"
        dmt_recommendation = "Evaluate Siponimod or Anti-CD20 therapy if active. Multidisciplinary neurorehabilitation and cognitive remediation."
        subclass_color = "#ef4444"
    else:  # PPMS
        status_text = "Primary Progressive Multiple Sclerosis (Insidious Spinal/Motor Atrophy)"
        dmt_recommendation = "Ocrelizumab (licensed for early active PPMS). Focused spinal cord rehabilitation and motor preservation."
        subclass_color = "#a855f7"

    pv = int(data.get('periventricular_lesions', 0))
    jc = int(data.get('juxtacortical_lesions', 0))
    it = int(data.get('infratentorial_lesions', 0))
    sc = int(data.get('spinal_cord_lesions', 0))
    dis_zones = (1 if pv >= 1 else 0) + (1 if jc >= 1 else 0) + (1 if it >= 1 else 0) + (1 if sc >= 1 else 0)

    gd = int(data.get('gd_enhancing_lesions', 0))
    ocb = int(data.get('oligoclonal_bands', 0))

    return {
        "primary_subclass": top_label,
        "confidence": top_conf,
        "status_text": status_text,
        "subclass_color": subclass_color,
        "dmt_recommendation": dmt_recommendation,
        "probabilities": dict(sorted_p),
        "mcdonald_criteria": {
            "dis_met": bool(derived["dis_criteria_met"]),
            "dis_zones_count": dis_zones,
            "dit_met": bool(derived["dit_criteria_met"]),
            "has_active_gd": bool(gd >= 1),
            "has_ocb_type2": bool(ocb == 1)
        },
        "indices": derived
    }


@app.route("/")
def index():
    return render_template("index.html", presets=GENERAL_PRESETS, ms_presets=MS_PRESETS)


@app.route("/api/predict", methods=["POST"])
def predict():
    payload = request.get_json() or {}
    res = evaluate_general_patient(payload)
    return jsonify(res)


@app.route("/api/presets", methods=["GET"])
def get_presets():
    return jsonify(GENERAL_PRESETS)


@app.route("/api/predict_ms_subclass", methods=["POST"])
def predict_ms_subclass():
    payload = request.get_json() or {}
    res = evaluate_ms_patient(payload)
    return jsonify(res)


@app.route("/api/ms_presets", methods=["GET"])
def get_ms_presets():
    return jsonify(MS_PRESETS)


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5050))
    print(f"Starting NeuroAI Multimodal Studio on http://127.0.0.1:{port}")
    app.run(host="0.0.0.0", port=port, debug=False)
