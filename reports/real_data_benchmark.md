# 🔬 NeuroAI: Real-World Clinical Benchmark Evaluation Report

This report provides an objective, empirical evaluation comparing models trained on real patient cohorts.

## 📊 Summary of Evaluated Real-World Cohorts

| Dataset | Cohort Type | Patients/Scans | Accuracy | F1-Score | Multiclass / Binary ROC-AUC |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Kaggle Alzheimer's** | Real Clinical & Cognitive | 2149 | **95.39%** | **0.9338** | **0.9516** |
| **OASIS-2 MRI** | Real Brain Morphometry | 373 | **80.14%** | **0.7798** | **0.8985** |
| *Synthetic MS (Legacy)* | Formula-Generated Sim | 2,500 | 99.88% (Synthetic) | 0.9983 | 1.0000 |

### 1. Kaggle Alzheimer's Clinical Evaluation
- **Total Patients**: 2149
- **Mean Accuracy**: 95.39%
- **F1-Score**: 0.9338
- **ROC-AUC**: 0.9516

**Top 5 Predictive Biomarkers**:
1. `FunctionalAssessment` (Importance: 0.2254)
1. `ADL` (Importance: 0.1932)
1. `MMSE` (Importance: 0.1550)
1. `MemoryComplaints` (Importance: 0.1086)
1. `BehavioralProblems` (Importance: 0.0649)

### 2. OASIS-2 Longitudinal MRI Evaluation
- **Total Scans**: 373
- **Mean Accuracy**: 80.14%
- **F1-Score**: 0.7798
- **ROC-AUC**: 0.8985

**Top 5 Predictive MRI & Cognitive Biomarkers**:
1. `MMSE` (Importance: 0.3955)
1. `nWBV` (Importance: 0.1433)
1. `eTIV` (Importance: 0.1075)
1. `ASF` (Importance: 0.1038)
1. `EDUC` (Importance: 0.0868)

---
*Report generated automatically by NeuroAI Real Data Pipeline.*
