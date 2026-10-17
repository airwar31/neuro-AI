---
name: clinical_text_anonymizer
description: Automatically de-identifies patient records (EMR/EHR) by stripping names, dates, phone numbers, passport, and ID numbers according to HIPAA Safe Harbor and 152-FZ.
---

# 🔒 Clinical Text Anonymizer Skill

Removes personal identifiable information (PII) from clinical notes and case histories before sending them to LLM APIs, preventing privacy violations and removing noisy tokens.

## Usage

```bash
python scripts/anonymize.py --text "Пациент Иванов Иван Иванович, 12.05.1958 г.р., телефон +7-916-123-45-67..."
```

Or process a file:
```bash
python scripts/anonymize.py --file path/to/patient_note.txt --output path/to/anonymized_note.txt
```
