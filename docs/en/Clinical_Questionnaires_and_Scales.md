---
title: Clinical Questionnaires and Scales
tags: [diagnostics, surveys, mmse, moca, edss, updrs, alsfrs]
---

# 📋 Clinical Questionnaires and Scales in Neurology

Standardized clinical rating scales and diagnostic questionnaires constitute the foundational clinical data layer used to quantify neurological disability, track disease progression, and serve as tabular features for machine learning classifiers.

---

## 1. Cognitive and Dementia Assessment

### MMSE (Mini-Mental State Examination)
- **Total Range**: 0–30 points.
- **Subdomains**: Orientation to time and place (10), Registration of words (3), Attention and calculation (5), Delayed recall (3), Language and praxis (9).
- **Clinical Cutoffs**:
  - `28–30`: Normal cognitive status.
  - `24–27`: Mild Cognitive Impairment (MCI).
  - `20–23`: Mild dementia.
  - `11–19`: Moderate dementia.
  - `0–10`: Severe dementia.
- **Key Application**: Core assessment tool for [[Alzheimers_Disease|Alzheimer's Disease]].

### MoCA (Montreal Cognitive Assessment)
- **Total Range**: 0–30 points (normal ≥ 26).
- Superior sensitivity over MMSE for detecting executive dysfunction, subtle frontal deficits, and prodromal MCI.

---

## 2. Movement and Motor Neuron Scales

### MDS-UPDRS (Unified Parkinson's Disease Rating Scale)
The gold standard for longitudinal monitoring in [[Parkinsons_Disease|Parkinson's Disease]].
- **Part I**: Non-motor aspects of daily living.
- **Part II**: Motor aspects of daily living.
- **Part III**: Clinician-scored Motor Examination (0–132 points): speech, facial expression, resting tremor, postural tremor, rigidity, finger tapping, leg agility, posture, gait.
- **Part IV**: Motor complications (dyskinesias, fluctuations).

### ALSFRS-R (Revised ALS Functional Rating Scale)
The primary functional measure in [[Amyotrophic_Lateral_Sclerosis|Amyotrophic Lateral Sclerosis]]:
- 12 individual functional items scored 0 (complete functional loss) to 4 (normal function). Total score range: **0–48 points**.
- Domains: Bulbar function (speech, salivation, swallowing), Fine motor (handwriting, food cutting), Gross motor (walking, stairs), Respiratory function (dyspnea, orthopnea, ventilatory support).

---

## 3. Demyelinating Disease and Disability Scales

### EDSS (Expanded Disability Status Scale)
The universal functional quantitation scale in [[Multiple_Sclerosis|Multiple Sclerosis]]:
- Integrates eight distinct functional systems (FS):
  1. Pyramidal functions.
  2. Cerebellar functions.
  3. Brainstem functions.
  4. Sensory functions.
  5. Bowel and bladder functions.
  6. Visual functions.
  7. Cerebral (mental) functions.
  8. Ambulation score.
- **Disability Milestones**:
  - `0.0–3.0`: Fully ambulatory, mild neurological signs.
  - `4.0–4.5`: Fully ambulatory without aid, severe single FS disability, walking limited to 500/300 m.
  - `6.0`: Unilateral assistance (cane) required to walk 100 m.
  - `6.5`: Bilateral assistance (crutches) required to walk 20 m.
  - `7.0+`: Restricted to wheelchair.

### FSS (Fatigue Severity Scale)
- 9 items evaluating the impact of pathological fatigue on daily living (scored 1 to 7).
- A mean score > 4.0 indicates clinically significant neurological fatigue.

---

## 🤖 Role in NeuroAI Classifiers
In [[Multimodal_AI_in_Neurology]], raw clinical questionnaire scores are standardized and mapped into latent feature vectors alongside quantitative MRI parameters.

---

## 🔗 Hub Links
- Associated Diseases: [[Alzheimers_Disease]], [[Parkinsons_Disease]], [[Amyotrophic_Lateral_Sclerosis]], [[Multiple_Sclerosis]]
- Imaging Correlates: [[MRI_Biomarkers_and_Neuroimaging]]
- Classification Engine: [[Multimodal_AI_in_Neurology]]
