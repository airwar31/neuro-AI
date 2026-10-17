# -*- coding: utf-8 -*-
import sys
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
if hasattr(sys.stderr, 'reconfigure'):
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')

"""
High-Speed DICOM Header Metadata Extractor.
Parses non-pixel tags using pydicom without reading pixel arrays.
"""

import os
import sys
import argparse
import json

try:
    import pydicom
    HAS_PYDICOM = True
except ImportError:
    HAS_PYDICOM = False

ESSENTIAL_TAGS = [
    "PatientID", "PatientAge", "PatientSex", "Modality",
    "MagneticFieldStrength", "Manufacturer", "SeriesDescription",
    "StudyDate", "SliceThickness", "RepetitionTime", "EchoTime",
    "SpacingBetweenSlices", "PixelSpacing"
]

def parse_single_dicom(filepath):
    if not HAS_PYDICOM:
        return {"error": "pydicom is not installed. Run 'pip install pydicom'"}
    try:
        # stop_before_pixels=True avoids reading the heavy image payload into memory
        dcm = pydicom.dcmread(filepath, stop_before_pixels=True, force=True)
        meta = {"filepath": os.path.basename(filepath)}
        for tag in ESSENTIAL_TAGS:
            if hasattr(dcm, tag):
                val = getattr(dcm, tag)
                meta[tag] = str(val) if val is not None else "N/A"
            else:
                meta[tag] = "Not Specified"
        return meta
    except Exception as e:
        return {"filepath": os.path.basename(filepath), "error": str(e)}

def parse_directory(dirpath, summary=False):
    results = []
    for root, _, files in os.walk(dirpath):
        for f in files:
            if f.lower().endswith((".dcm", ".dicom", ".ima")) or "." not in f:
                path = os.path.join(root, f)
                res = parse_single_dicom(path)
                if "error" not in res:
                    results.append(res)
    
    if summary:
        modalities = set(r.get("Modality") for r in results)
        series = set(r.get("SeriesDescription") for r in results)
        return {
            "total_files": len(results),
            "modalities": list(modalities),
            "series": list(series),
            "sample_metadata": results[0] if results else {}
        }
    return results

def main():
    parser = argparse.ArgumentParser(description="Parse DICOM metadata without loading pixel data")
    parser.add_argument("--input", type=str, help="Path to single DICOM file")
    parser.add_argument("--dir", type=str, help="Path to DICOM directory")
    parser.add_argument("--summary", action="store_true", help="Output aggregated summary")
    args = parser.parse_args()

    if args.input:
        out = parse_single_dicom(args.input)
    elif args.dir:
        out = parse_directory(args.dir, summary=args.summary)
    else:
        # Mock demo output if no input provided
        out = {
            "PatientAge": "067Y",
            "PatientSex": "F",
            "Modality": "MR",
            "MagneticFieldStrength": "3.0",
            "SeriesDescription": "AX T2 FLAIR 3D",
            "SliceThickness": "1.0",
            "Manufacturer": "SIEMENS",
            "RepetitionTime": "5000",
            "EchoTime": "388"
        }

    print(json.dumps(out, indent=2, ensure_ascii=False))

if __name__ == "__main__":
    main()