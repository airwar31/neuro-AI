# -*- coding: utf-8 -*-
"""
NeuroAI - Clinical Multimodal Classifier Training CLI.
Executes 5-Fold Stratified Cross-Validation across 6 Neurological Classes and serializes the pipeline.
"""

import os
import sys
import argparse
import numpy as np
import pandas as pd

REPO_DIR = os.path.dirname(os.path.abspath(__file__))
if REPO_DIR not in sys.path:
    sys.path.insert(0, REPO_DIR)

from sklearn.model_selection import StratifiedKFold
from sklearn.metrics import classification_report, f1_score, roc_auc_score, accuracy_score
from sklearn.preprocessing import LabelEncoder

from src.dataset import generate_synthetic_cohort
from src.features import compute_clinical_indices
from src.models import ClinicalEnsemblePredictor, FEATURE_NAMES, CLASSES


def main():
    parser = argparse.ArgumentParser(description="Train NeuroAI Multimodal Classifier")
    parser.add_argument("--samples", type=int, default=1800, help="Total cohort sample size")
    parser.add_argument("--folds", type=int, default=5, help="Number of cross-validation folds")
    parser.add_argument("--output", type=str, default=os.path.join(REPO_DIR, "models", "classifier.joblib"), help="Output model path")
    args = parser.parse_args()

    print("=" * 72, flush=True)
    print("  NeuroAI: Multimodal Neurological Disorder Classifier Training", flush=True)
    print(f"  Cohort Size: {args.samples} patients | CV Folds: {args.folds}", flush=True)
    print("=" * 72, flush=True)

    print("\n1. Generating multimodal patient cohort...", flush=True)
    df = generate_synthetic_cohort(n_samples=args.samples)

    print("\n2. Engineering biomedical indices...", flush=True)
    derived_rows = [compute_clinical_indices(row) for _, row in df.iterrows()]
    df_derived = pd.DataFrame(derived_rows)
    df_full = pd.concat([df, df_derived], axis=1)

    X = df_full[FEATURE_NAMES].values
    y_raw = df_full['diagnosis'].values

    le = LabelEncoder()
    y = le.fit_transform(y_raw)
    class_names = list(le.classes_)
    print(f"Detected Classes ({len(class_names)}): {class_names}", flush=True)

    cv = StratifiedKFold(n_splits=args.folds, shuffle=True, random_state=42)
    fold_f1 = []
    fold_auc = []
    fold_acc = []

    print("\n3. Executing Stratified 5-Fold Cross-Validation...", flush=True)
    for fold, (train_idx, val_idx) in enumerate(cv.split(X, y)):
        X_train, X_val = X[train_idx], X[val_idx]
        y_train, y_val = y[train_idx], y[val_idx]

        model = ClinicalEnsemblePredictor()
        model.fit(X_train, y_train, classes=class_names)

        preds = model.predict(X_val)
        probs = model.predict_proba(X_val)

        acc = accuracy_score(y_val, preds)
        score_f1 = f1_score(y_val, preds, average='macro')
        score_auc = roc_auc_score(y_val, probs, multi_class='ovr')

        fold_acc.append(acc)
        fold_f1.append(score_f1)
        fold_auc.append(score_auc)
        print(f"  Fold {fold+1}/{args.folds} -> Acc: {acc:.4f} | Macro-F1: {score_f1:.4f} | Multiclass ROC-AUC: {score_auc:.4f}", flush=True)

    print("\n" + "=" * 72, flush=True)
    print(f"  BENCHMARK SUMMARY:", flush=True)
    print(f"  - Mean Accuracy        : {np.mean(fold_acc)*100:.2f}% +/- {np.std(fold_acc)*100:.2f}%", flush=True)
    print(f"  - Mean Macro-F1 Score  : {np.mean(fold_f1):.4f} +/- {np.std(fold_f1):.4f}", flush=True)
    print(f"  - Multiclass ROC-AUC   : {np.mean(fold_auc):.4f} +/- {np.std(fold_auc):.4f}", flush=True)
    print("=" * 72, flush=True)

    print("\n4. Fitting final production ensemble on full cohort...", flush=True)
    final_model = ClinicalEnsemblePredictor()
    final_model.fit(X, y, classes=class_names)

    os.makedirs(os.path.dirname(args.output), exist_ok=True)
    final_model.save(args.output)
    print(f"Production model serialized successfully to: {args.output}", flush=True)


if __name__ == "__main__":
    main()
