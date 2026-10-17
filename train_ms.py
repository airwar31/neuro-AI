# -*- coding: utf-8 -*-
"""
NeuroAI Multiple Sclerosis Subclass Training Pipeline.
Stratified 5-Fold Cross-Validation on Kaggle MS Biomarker Dataset.
Serializes calibrated ensemble to models/ms_subclass_classifier.joblib
"""

import os
import sys
import argparse
import numpy as np
import pandas as pd
from sklearn.model_selection import StratifiedKFold
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import accuracy_score, f1_score, roc_auc_score, classification_report, confusion_matrix

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

REPO_ROOT = os.path.dirname(os.path.abspath(__file__))
if REPO_ROOT not in sys.path:
    sys.path.insert(0, REPO_ROOT)

from src.ms_subclasses import (
    MSSubclassClassifier,
    compute_ms_indices,
    RAW_FEATURES,
    ENGINEERED_FEATURES,
    ALL_FEATURES,
    MS_CLASSES
)


def main():
    parser = argparse.ArgumentParser(description="Train Multiple Sclerosis Subclass Classifier on Kaggle data.")
    parser.add_argument("--data", default=os.path.join(REPO_ROOT, "data", "kaggle_ms_dataset.csv"), help="Path to Kaggle MS dataset")
    parser.add_argument("--output", default=os.path.join(REPO_ROOT, "models", "ms_subclass_classifier.joblib"), help="Path to save model")
    parser.add_argument("--folds", type=int, default=5, help="Number of cross-validation folds")
    args = parser.parse_args()

    print("=" * 72, flush=True)
    print("  NeuroAI: Multiple Sclerosis Subclass Classifier Training (Kaggle)", flush=True)
    print("  Target Classes (5): Control, CIS, RRMS, SPMS, PPMS", flush=True)
    print("=" * 72, flush=True)

    print("\n1. Loading dataset from: " + str(args.data), flush=True)
    if not os.path.exists(args.data):
        raise FileNotFoundError(f"Dataset not found at {args.data}")

    df = pd.read_csv(args.data)
    print(f"Dataset loaded: {len(df)} patients, {len(df.columns)} clinical & MRI features.", flush=True)
    print("Cohort distribution:", flush=True)
    for cls_name, cnt in df["ms_subclass"].value_counts().items():
        print(f"  - {cls_name:12s}: {cnt:4d} patients ({cnt/len(df)*100:5.1f}%)", flush=True)

    print("\n2. Engineering McDonald criteria & neurodegenerative indices...", flush=True)
    df_full = compute_ms_indices(df)

    X = df_full[ALL_FEATURES].values
    y_raw = df_full["ms_subclass"].values

    le = LabelEncoder()
    y = le.fit_transform(y_raw)
    class_names = list(le.classes_)
    print(f"Classes synchronized ({len(class_names)}): {class_names}", flush=True)
    print(f"Input feature vector dimensionality: {X.shape[1]} features.", flush=True)

    cv = StratifiedKFold(n_splits=args.folds, shuffle=True, random_state=42)
    fold_acc = []
    fold_f1 = []
    fold_auc = []

    print("\n3. Executing Stratified 5-Fold Cross-Validation on Kaggle Cohort...", flush=True)
    for fold, (train_idx, val_idx) in enumerate(cv.split(X, y)):
        X_tr, X_val = X[train_idx], X[val_idx]
        y_tr, y_val = y[train_idx], y[val_idx]

        clf = MSSubclassClassifier()
        clf.fit(X_tr, y_tr, classes=class_names)

        preds = clf.predict(X_val)
        probs = clf.predict_proba(X_val)

        preds_int = le.transform(preds)

        acc = accuracy_score(y_val, preds_int)
        score_f1 = f1_score(y_val, preds_int, average='macro')
        score_auc = roc_auc_score(y_val, probs, multi_class='ovr')

        fold_acc.append(acc)
        fold_f1.append(score_f1)
        fold_auc.append(score_auc)
        print(f"  Fold {fold+1}/{args.folds} -> Acc: {acc:.4f} | Macro-F1: {score_f1:.4f} | Multiclass ROC-AUC: {score_auc:.4f}", flush=True)

    print("\n" + "=" * 72, flush=True)
    print("  BENCHMARK SUMMARY (Multiple Sclerosis Subclasses):", flush=True)
    print(f"  - Mean Accuracy        : {np.mean(fold_acc)*100:.2f}% +/- {np.std(fold_acc)*100:.2f}%", flush=True)
    print(f"  - Mean Macro-F1 Score  : {np.mean(fold_f1):.4f} +/- {np.std(fold_f1):.4f}", flush=True)
    print(f"  - Multiclass ROC-AUC   : {np.mean(fold_auc):.4f} +/- {np.std(fold_auc):.4f}", flush=True)
    print("=" * 72, flush=True)

    print("\n4. Fitting production model on full Kaggle dataset & serializing...", flush=True)
    prod_clf = MSSubclassClassifier()
    prod_clf.fit(X, y, classes=class_names)
    prod_clf.save(args.output)
    print(f"Model saved successfully to: {args.output}", flush=True)

    # Sanity check evaluation on full dataset
    final_preds = prod_clf.predict(X)
    print("\nFull Dataset Clinical Classification Report:", flush=True)
    print(classification_report(y_raw, final_preds, target_names=class_names, digits=4), flush=True)


if __name__ == "__main__":
    main()
