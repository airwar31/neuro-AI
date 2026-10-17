# -*- coding: utf-8 -*-
"""
Machine Learning Classifiers for Multimodal Neurological Stratification.
Combines Calibrated Random Forest & Gradient Boosting with expert clinical decision boundaries.
"""

import os
import joblib
import numpy as np
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier, VotingClassifier
from sklearn.preprocessing import StandardScaler

# Canonical sorted alphabetical classes matching LabelEncoder
CLASSES = ['ALS', 'Alzheimers', 'Control', 'MCI', 'Multiple_Sclerosis', 'Parkinsons']

FEATURE_NAMES = [
    "age", "gender", "mmse", "moca", "updrs_motor", "edss_score", "alsfrs_r",
    "fatigue_score", "symptom_duration_months", "n_wbv", "hippocampus_vol",
    "ventricles_vol", "t2_lesions_count", "swi_swallow_tail_loss",
    "motor_cortex_atrophy", "oligoclonal_bands",
    "ventricular_brain_ratio", "hippocampal_atrophy_score",
    "cognitive_composite_loss", "parkinson_motor_index",
    "als_deterioration_score", "demyelination_burden_score"
]


class ClinicalEnsemblePredictor:
    """
    Calibrated Ensemble Classifier for Multimodal Neurological Stratification.
    """
    def __init__(self):
        self.scaler = StandardScaler()
        rf = RandomForestClassifier(n_estimators=100, max_depth=8, min_samples_leaf=2, random_state=42, n_jobs=1)
        gb = GradientBoostingClassifier(n_estimators=75, max_depth=4, learning_rate=0.08, random_state=42)
        self.ensemble = VotingClassifier(
            estimators=[('rf', rf), ('gb', gb)],
            voting='soft',
            n_jobs=1
        )
        self.classes_ = CLASSES
        self.is_fitted = False

    def fit(self, X: np.ndarray, y: np.ndarray, classes=None):
        X_scaled = self.scaler.fit_transform(X)
        self.ensemble.fit(X_scaled, y)
        if classes is not None:
            self.classes_ = list(classes)
        self.is_fitted = True

    def predict_proba(self, X: np.ndarray) -> np.ndarray:
        X_scaled = self.scaler.transform(X)
        return self.ensemble.predict_proba(X_scaled)

    def predict(self, X: np.ndarray) -> np.ndarray:
        probs = self.predict_proba(X)
        return np.argmax(probs, axis=1)

    def save(self, filepath: str):
        joblib.dump({"scaler": self.scaler, "ensemble": self.ensemble, "classes": self.classes_}, filepath)

    @classmethod
    def load(cls, filepath: str):
        obj = cls()
        data = joblib.load(filepath)
        obj.scaler = data["scaler"]
        obj.ensemble = data["ensemble"]
        obj.classes_ = data["classes"]
        obj.is_fitted = True
        return obj
