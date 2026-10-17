---
name: dicom_metadata_parser
description: High-speed extraction of clinical MRI and CT metadata from DICOM files without loading raw pixel byte arrays into LLM context.
---

# 🧲 DICOM Metadata Parser Skill

Extracts critical clinical and radiomic headers (PatientAge, Modality, MagneticFieldStrength, RepetitionTime, EchoTime, SeriesDescription) directly from `.dcm` files.

## Usage

```bash
python scripts/parse_dicom.py --input path/to/scan.dcm
```

Or scan an entire patient directory:
```bash
python scripts/parse_dicom.py --dir path/to/dicom_folder/ --summary
```
