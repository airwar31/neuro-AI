# -*- coding: utf-8 -*-
"""
NeuroAI Multiple Sclerosis Subclass Classifier & Biomarker Phenotyping Engine.
Differentiates 5 distinct clinical phenotypes:
- Healthy Controls (Control)
- Clinically Isolated Syndrome (CIS)
- Relapsing-Remitting MS (RRMS)
- Secondary Progressive MS (SPMS)
- Primary Progressive MS (PPMS)
"""

import os
import sys
import joblib
import numpy as np
import pandas as pd
from sklearn.base import BaseEstimator, ClassifierMixin
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier, VotingClassifier
from sklearn.preprocessing import StandardScaler, LabelEncoder
from sklearn.model_selection import StratifiedKFold
from sklearn.metrics import accuracy_score, f1_score, roc_auc_score, confusion_matrix, classification_report

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

MS_CLASSES = ['CIS', 'Control', 'PPMS', 'RRMS', 'SPMS']

RAW_FEATURES = [
    'age', 'gender', 'onset_presentation', 'disease_duration_months',
    'relapse_rate_arr', 'edss', 'fss_fatigue', 'sdmt_cognitive',
    't2_lesion_count', 'periventricular_lesions', 'juxtacortical_lesions',
    'infratentorial_lesions', 'spinal_cord_lesions', 'gd_enhancing_lesions',
    't1_black_holes', 'n_wbv', 'thalamus_vol_mm3', 'oligoclonal_bands',
    'serum_nfl_pg_ml', 'csf_igg_index'
]

ENGINEERED_FEATURES = [
    'dis_criteria_met',
    'dit_criteria_met',
    'acute_inflammatory_activity',
    'neurodegenerative_burden',
    'disability_accumulation_rate',
    'cognitive_fatigue_composite'
]

ALL_FEATURES = RAW_FEATURES + ENGINEERED_FEATURES


def compute_ms_single_record(data: dict) -> dict:
    """Computes composite indices for a single patient record dictionary."""
    pv = 1 if int(data.get('periventricular_lesions', 0)) >= 1 else 0
    jc = 1 if int(data.get('juxtacortical_lesions', 0)) >= 1 else 0
    it = 1 if int(data.get('infratentorial_lesions', 0)) >= 1 else 0
    sc = 1 if int(data.get('spinal_cord_lesions', 0)) >= 1 else 0

    gd = int(data.get('gd_enhancing_lesions', 0))
    ocb = int(data.get('oligoclonal_bands', 0))
    arr = float(data.get('relapse_rate_arr', 0.0))
    nfl = float(data.get('serum_nfl_pg_ml', 10.0))
    black_holes = int(data.get('t1_black_holes', 0))
    thalamus = float(data.get('thalamus_vol_mm3', 14000.0))
    wbv = float(data.get('n_wbv', 0.78))
    edss = float(data.get('edss', 0.0))
    duration_months = max(1.0, float(data.get('disease_duration_months', 0.0)))
    fss = float(data.get('fss_fatigue', 2.0))
    sdmt = float(data.get('sdmt_cognitive', 55.0))

    zones = pv + jc + it + sc
    dis_met = 1 if zones >= 2 else 0
    dit_met = 1 if (gd >= 1 or ocb == 1) else 0

    inflam = (gd * 2.5) + (arr * 2.0) + (nfl / 20.0)
    neurodeg = (black_holes * 1.2) + max(0.0, (15200.0 - thalamus) / 1000.0) + max(0.0, (0.80 - wbv) * 60.0)
    prog_rate = edss / (duration_months / 12.0 + 0.5)
    cog_fatigue = (fss / 7.0 * 50.0) + max(0.0, (60.0 - sdmt) / 60.0 * 50.0)

    return {
        "dis_criteria_met": dis_met,
        "dit_criteria_met": dit_met,
        "acute_inflammatory_activity": round(inflam, 3),
        "neurodegenerative_burden": round(neurodeg, 3),
        "disability_accumulation_rate": round(prog_rate, 3),
        "cognitive_fatigue_composite": round(cog_fatigue, 3)
    }


def compute_ms_indices(data):
    """Computes McDonald 2017 DIS/DIT criteria and composite indices.
    Accepts DataFrame, Series, or dict.
    """
    if isinstance(data, (dict, pd.Series)):
        return compute_ms_single_record(dict(data))

    out = data.copy()

    # 1. McDonald 2017 Dissemination in Space (DIS): >= 1 lesion in >= 2 of 4 areas
    pv = (out['periventricular_lesions'] >= 1).astype(int)
    jc = (out['juxtacortical_lesions'] >= 1).astype(int)
    it = (out['infratentorial_lesions'] >= 1).astype(int)
    sc = (out['spinal_cord_lesions'] >= 1).astype(int)
    zones = pv + jc + it + sc
    out['dis_criteria_met'] = (zones >= 2).astype(int)

    # 2. McDonald 2017 Dissemination in Time (DIT): Active Gd+ lesion OR positive OCB
    gd = out['gd_enhancing_lesions']
    ocb = out['oligoclonal_bands']
    out['dit_criteria_met'] = ((gd >= 1) | (ocb == 1)).astype(int)

    # 3. Acute Inflammatory Index
    arr = out['relapse_rate_arr']
    nfl = out['serum_nfl_pg_ml']
    out['acute_inflammatory_activity'] = (gd * 2.5) + (arr * 2.0) + (nfl / 20.0)

    # 4. Chronic Neurodegenerative & Axonal Loss Burden
    black_holes = out['t1_black_holes']
    thalamus = out['thalamus_vol_mm3']
    wbv = out['n_wbv']
    out['neurodegenerative_burden'] = (
        (black_holes * 1.2) +
        np.maximum(0.0, (15200.0 - thalamus) / 1000.0) +
        np.maximum(0.0, (0.80 - wbv) * 60.0)
    )

    # 5. Disability Progression Velocity
    duration_months = np.maximum(1.0, out['disease_duration_months'])
    out['disability_accumulation_rate'] = out['edss'] / (duration_months / 12.0 + 0.5)

    # 6. Cognitive & Central Fatigue Composite
    fss = out['fss_fatigue']
    sdmt = out['sdmt_cognitive']
    out['cognitive_fatigue_composite'] = (
        (fss / 7.0 * 50.0) +
        np.maximum(0.0, (60.0 - sdmt) / 60.0 * 50.0)
    )

    return out


class MSSubclassClassifier(BaseEstimator, ClassifierMixin):
    """Calibrated Ensemble Classifier for 5 MS Subclasses."""
    def __init__(self):
        self.scaler = StandardScaler()
        rf = RandomForestClassifier(n_estimators=140, max_depth=8, min_samples_leaf=2, random_state=42, n_jobs=1)
        gb = GradientBoostingClassifier(n_estimators=90, max_depth=4, learning_rate=0.08, random_state=42)
        self.ensemble = VotingClassifier(
            estimators=[('rf', rf), ('gb', gb)],
            voting='soft',
            n_jobs=1
        )
        self.classes_ = MS_CLASSES
        self.is_fitted = False

    def _prepare_input(self, X):
        """Standardizes dict, DataFrame, or numpy array into 26-feature matrix."""
        if isinstance(X, dict):
            X = pd.DataFrame([X])
        if isinstance(X, pd.DataFrame):
            if set(RAW_FEATURES).issubset(X.columns) and not set(ENGINEERED_FEATURES).issubset(X.columns):
                X_eng = compute_ms_indices(X)
                return X_eng[ALL_FEATURES].values
            elif set(ALL_FEATURES).issubset(X.columns):
                return X[ALL_FEATURES].values
            return X.values
        elif isinstance(X, np.ndarray):
            if X.shape[1] == len(RAW_FEATURES):
                df = pd.DataFrame(X, columns=RAW_FEATURES)
                X_eng = compute_ms_indices(df)
                return X_eng[ALL_FEATURES].values
            return X
        return np.asarray(X)

    def fit(self, X, y, classes=None):
        X_mat = self._prepare_input(X)
        X_scaled = self.scaler.fit_transform(X_mat)
        self.ensemble.fit(X_scaled, y)
        if classes is not None:
            self.classes_ = list(classes)
        self.is_fitted = True
        return self

    def predict_proba(self, X) -> np.ndarray:
        if not self.is_fitted:
            raise RuntimeError("Model is not fitted yet.")
        X_mat = self._prepare_input(X)
        X_scaled = self.scaler.transform(X_mat)
        return self.ensemble.predict_proba(X_scaled)

    def predict(self, X) -> np.ndarray:
        probs = self.predict_proba(X)
        indices = np.argmax(probs, axis=1)
        return np.array([self.classes_[i] for i in indices])

    def save(self, filepath: str):
        os.makedirs(os.path.dirname(filepath), exist_ok=True)
        joblib.dump(self, filepath)
        print(f"[OK] Saved model to: {filepath}")

    @classmethod
    def load(cls, filepath: str) -> "MSSubclassClassifier":
        model = joblib.load(filepath)
        return model
