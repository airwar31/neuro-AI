---
name: dataset_token_tokenizer
description: Accurately counts tokens and optimizes batch boundaries for clinical datasets using tiktoken before transmitting data into LLM context windows.
---

# 📊 Dataset Token Tokenizer Skill

Calculates exact token counts and cost estimates for clinical tabular rows, medical narratives, and prompts using standard BPE encodings (cl100k_base / o200k_base).

## Usage

```bash
python scripts/tokenize_dataset.py --file data/patients.csv --max_context 32000
```

Or calculate for an arbitrary text snippet:
```bash
python scripts/tokenize_dataset.py --text "MMSE: 25, MoCA: 22, Hippocampus: 3350 mm3..."
```
