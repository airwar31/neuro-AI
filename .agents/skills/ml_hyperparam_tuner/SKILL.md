---
name: ml_hyperparam_tuner
description: Automated cross-validation and hyperparameter search for neurological ML models using YAML configuration files.
---

# ⚙️ ML Hyperparameter Tuner Skill

Allows the agent to modify training configurations, execute grid searches, and optimize model performance without rewriting Python training scripts from scratch.

## Usage

```bash
python scripts/tuner.py --config config/hyperparams.yaml --output models/best_params.json
```
