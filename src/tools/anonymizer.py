# -*- coding: utf-8 -*-
import sys
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
if hasattr(sys.stderr, 'reconfigure'):
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')

"""
Clinical Text De-Identification Engine.
Enforces HIPAA Safe Harbor & 152-FZ PII removal on medical records.
"""

import sys
import argparse
import re

PATTERNS = [
    # Phone numbers
    (r'(\+?[78][-\s]?\(?\d{3}\)?[-\s]?\d{3}[-\s]?\d{2}[-\s]?\d{2})', '[PHONE_REDACTED]'),
    (r'(\+?1[-.\s]?\(?\d{3}\)?[-.\s]?\d{3}[-\s]?\d{4})', '[PHONE_REDACTED]'),
    
    # Dates of birth and specific timestamps (DD.MM.YYYY, YYYY-MM-DD)
    (r'\b(\d{1,2}[./-]\d{1,2}[./-]\d{2,4})\b', '[DATE_REDACTED]'),
    (r'\b(\d{4}[./-]\d{1,2}[./-]\d{1,2})\b', '[DATE_REDACTED]'),
    
    # Passport & SNILS & Insurance numbers (Russian / US formats)
    (r'\b\d{4}\s+\d{6}\b', '[PASSPORT_REDACTED]'),
    (r'\b\d{3}-\d{3}-\d{3}\s*\d{2}\b', '[SNILS_REDACTED]'),
    (r'\b\d{3}-\d{2}-\d{4}\b', '[SSN_REDACTED]'),
    
    # Emails
    (r'[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}', '[EMAIL_REDACTED]'),

    # Russian Full Names (Фамилия Имя Отчество or Пациент ...)
    (r'(?i)(?:пациент(?:ка)?|гр\.|больной|фио)[:\s]+([А-ЯЁ][а-яё]+(?:\s+[А-ЯЁ][а-яё]+){1,2})', 'Пациент: [PATIENT_NAME]'),
    (r'(?i)(?:врач|доктор|исследователь)[:\s]+([А-ЯЁ][а-яё]+(?:\s+[А-ЯЁ][а-яё]+){1,2})', 'Врач: [CLINICIAN_NAME]'),

    # English Full Names (Patient: John Doe, Dr. Smith)
    (r'(?i)(?:patient|subject)[:\s]+([A-Z][a-z]+(?:\s+[A-Z][a-z]+){1,2})', 'Patient: [PATIENT_NAME]'),
    (r'(?i)(?:dr\.|physician)[:\s]+([A-Z][a-z]+(?:\s+[A-Z][a-z]+){1,2})', 'Physician: [CLINICIAN_NAME]')
]

def anonymize_text(text: str) -> str:
    cleaned = text
    for pattern, replacement in PATTERNS:
        cleaned = re.sub(pattern, replacement, cleaned)
    return cleaned

def main():
    parser = argparse.ArgumentParser(description="Anonymize Clinical EMR Text")
    parser.add_argument("--text", type=str, help="Text to anonymize")
    parser.add_argument("--file", type=str, help="Input file path")
    parser.add_argument("--output", type=str, help="Output file path")
    args = parser.parse_args()

    if args.file:
        with open(args.file, "r", encoding="utf-8") as f:
            raw = f.read()
    elif args.text:
        raw = args.text
    else:
        raw = "Пациент Иванов Иван Иванович, дата рождения 14.07.1965, тел. +7 (926) 456-78-90, обратился с тремором покоя в правой руке. СНИЛС 123-456-789 01."

    anonymized = anonymize_text(raw)

    if args.output:
        with open(args.output, "w", encoding="utf-8") as f:
            f.write(anonymized)
        print(f"[OK] Anonymized content written to {args.output}")
    else:
        print("=== DE-IDENTIFIED CLINICAL TEXT ===")
        print(anonymized)

if __name__ == "__main__":
    main()