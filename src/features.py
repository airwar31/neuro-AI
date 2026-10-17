# -*- coding: utf-8 -*-
"""
Biomedical and Radiomic Feature Engineering Module.
Derives composite clinical risk indicators from survey responses and quantitative MRI data.
"""

import math
import numpy as np


def compute_clinical_indices(row: dict) -> dict:
    """
    Computes derived biomedical indices:
    1. Ventricular-Brain Ratio (VBR)
    2. Hippocampal Atrophy Score (HAI)
    3. Cognitive Impairment Composite (CIC)
    4. Parkinsonian Motor Severity Index (PMSI)
    5. ALS Functional Deterioration Score (AFDS)
    6. Demyelination Burden Score (DBS)
    """
    age = float(row.get('age', 60.0))
    mmse = float(row.get('mmse', 28.0))
    moca = float(row.get('moca', 27.0))
    updrs = float(row.get('updrs_motor', 0.0))
    edss = float(row.get('edss_score', 0.0))
    alsfrs = float(row.get('alsfrs_r', 48.0))
    fatigue = float(row.get('fatigue_score', 2.0))
    hippo = float(row.get('hippocampus_vol', 4000.0))
    vent = float(row.get('ventricles_vol', 25000.0))
    lesions = int(row.get('t2_lesions_count', 0))
    swi_loss = int(row.get('swi_swallow_tail_loss', 0))
    motor_atrophy = float(row.get('motor_cortex_atrophy', 0.0))
    ocb = int(row.get('oligoclonal_bands', 0))

    # 1. Ventricular-to-Brain Ratio (central atrophy & hydrocephalus ex-vacuo)
    vbr = vent / (hippo * 10.0 + 1e-5)

    # 2. Hippocampal Atrophy Score relative to physiological age expectation
    expected_hippo = max(2400.0, 4800.0 - (age * 16.0))
    hippo_atrophy = max(0.0, (expected_hippo - hippo) / expected_hippo)

    # 3. Cognitive Impairment Composite (fuses MMSE and MoCA point drops)
    mmse_loss = max(0.0, (30.0 - mmse) / 30.0)
    moca_loss = max(0.0, (30.0 - moca) / 30.0)
    cog_composite = (mmse_loss * 0.45) + (moca_loss * 0.55)

    # 4. Parkinsonian Motor Severity Index
    updrs_norm = min(1.0, updrs / 80.0)
    pmsi = (updrs_norm * 0.6) + (swi_loss * 0.4)

    # 5. ALS Functional Deterioration Score
    als_loss = max(0.0, (48.0 - alsfrs) / 48.0)
    afds = (als_loss * 0.6) + (motor_atrophy * 0.4)

    # 6. Demyelination Burden Score (Multiple Sclerosis)
    lesion_component = min(1.0, lesions / 30.0)
    edss_component = min(1.0, edss / 8.0)
    dbs = (lesion_component * 0.45) + (edss_component * 0.35) + (ocb * 0.20)

    return {
        "ventricular_brain_ratio": round(vbr, 3),
        "hippocampal_atrophy_score": round(hippo_atrophy, 3),
        "cognitive_composite_loss": round(cog_composite, 3),
        "parkinson_motor_index": round(pmsi, 3),
        "als_deterioration_score": round(afds, 3),
        "demyelination_burden_score": round(dbs, 3)
    }
