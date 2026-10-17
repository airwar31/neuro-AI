# -*- coding: utf-8 -*-
"""
YAML-Driven Hyperparameter Tuner for Clinical Ensemble Models.
"""

import sys
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
if hasattr(sys.stderr, 'reconfigure'):
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')

import os
import sys
import argparse
import json
import yaml
import numpy as np

# Ensure root package resolution
curr = os.path.abspath(__file__)
REPO_ROOT = os.path.dirname(curr)
while REPO_ROOT and not os.path.exists(os.path.join(REPO_ROOT, "src")):
    parent = os.path.dirname(REPO_ROOT)
    if parent == REPO_ROOT:
        break
    REPO_ROOT = parent
if REPO_ROOT not in sys.path:
    sys.path.insert(0, REPO_ROOT)

from sklearn.model_selection import StratifiedKFold
from sklearn.metrics import f1_score, roc_auc_score, accuracy_score
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier, VotingClassifier
from sklearn.preprocessing import StandardScaler, LabelEncoder

from src.dataset import generate_synthetic_cohort
from src.features import compute_clinical_indices
from src.models import FEATURE_NAMES, CLASSES

DEFAULT_CONFIG = {
    "cohort_samples": 1000,
    "cv_folds": 3,
    "grid": {
        "rf_n_estimators": [60, 100],
        "rf_max_depth": [6, 8],
        "gb_n_estimators": [50, 75],
        "gb_learning_rate": [0.05, 0.1]
    }
}

def run_tuning(config):
    print("=" * 65)
    print("  NeuroAI Hyperparameter Tuner")
    print(f"  Samples: {config.get('cohort_samples')} | Folds: {config.get('cv_folds')}")
    print("=" * 65)

    df = generate_synthetic_cohort(n_samples=config.get("cohort_samples", 1000))
    derived_rows = [compute_clinical_indices(row) for _, row in df.iterrows()]
    df_derived = __import__("pandas").DataFrame(derived_rows)
    df_full = __import__("pandas").concat([df, df_derived], axis=1)

    X = df_full[FEATURE_NAMES].values
    y_raw = df_full['diagnosis'].values
    le = LabelEncoder()
    y = le.fit_transform(y_raw)

    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X)

    grid = config.get("grid", {})
    best_score = -1.0
    best_params = {}

    cv = StratifiedKFold(n_splits=config.get("cv_folds", 3), shuffle=True, random_state=42)

    for rf_est in grid.get("rf_n_estimators", [100]):
        for rf_depth in grid.get("rf_max_depth", [8]):
            for gb_est in grid.get("gb_n_estimators", [75]):
                for gb_lr in grid.get("gb_learning_rate", [0.08]):
                    fold_f1 = []
                    for train_idx, val_idx in cv.split(X_scaled, y):
                        X_tr, X_val = X_scaled[train_idx], X_scaled[val_idx]
                        y_tr, y_val = y[train_idx], y[val_idx]

                        rf = RandomForestClassifier(n_estimators=rf_est, max_depth=rf_depth, n_jobs=1, random_state=42)
                        gb = GradientBoostingClassifier(n_estimators=gb_est, learning_rate=gb_lr, max_depth=4, random_state=42)
                        clf = VotingClassifier(estimators=[('rf', rf), ('gb', gb)], voting='soft', n_jobs=1)
                        clf.fit(X_tr, y_tr)

                        preds = clf.predict(X_val)
                        fold_f1.append(f1_score(y_val, preds, average='macro'))

                    mean_f1 = np.mean(fold_f1)
                    print(f"  RF(n={rf_est}, d={rf_depth}) + GB(n={gb_est}, lr={gb_lr}) -> Macro-F1: {mean_f1:.4f}")

                    if mean_f1 > best_score:
                        best_score = mean_f1
                        best_params = {
                            "rf_n_estimators": rf_est,
                            "rf_max_depth": rf_depth,
                            "gb_n_estimators": gb_est,
                            "gb_learning_rate": gb_lr,
                            "best_macro_f1": round(float(best_score), 4)
                        }

    print("=" * 65)
    print(f"Optimal Hyperparameters: {best_params}")
    return best_params

def main():
    parser = argparse.ArgumentParser(description="Tune ML Hyperparameters from YAML")
    parser.add_argument("--config", type=str, help="Path to YAML configuration")
    parser.add_argument("--output", type=str, default="config/best_params.json", help="Output JSON path")
    args = parser.parse_args()

    if args.config and os.path.exists(args.config):
        with open(args.config, "r", encoding="utf-8") as f:
            cfg = yaml.safe_load(f)
    else:
        cfg = DEFAULT_CONFIG

    best = run_tuning(cfg)
    os.makedirs(os.path.dirname(args.output), exist_ok=True)
    with open(args.output, "w", encoding="utf-8") as f:
        json.dump(best, f, indent=2)
    print(f"[OK] Saved best parameters to {args.output}")

if __name__ == "__main__":
    main()