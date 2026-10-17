# -*- coding: utf-8 -*-
"""
Medical Dataset Token Counter & Context Window Optimizer.
"""

import sys
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
if hasattr(sys.stderr, 'reconfigure'):
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')

import sys
import os
import argparse
import json

try:
    import tiktoken
    HAS_TIKTOKEN = True
except ImportError:
    HAS_TIKTOKEN = False

def count_tokens(text: str, encoding_name: str = "cl100k_base") -> int:
    if HAS_TIKTOKEN:
        try:
            enc = tiktoken.get_encoding(encoding_name)
            return len(enc.encode(text))
        except Exception:
            pass
    # Approximate fallback: ~4 characters per token for English/code, ~2 for Cyrillic
    return max(1, len(text) // 3)

def analyze_dataset_tokens(file_path: str, max_context: int = 32000):
    if not os.path.exists(file_path):
        return {"error": f"File not found: {file_path}"}

    with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
        lines = f.readlines()

    total_tokens = 0
    row_tokens = []

    for line in lines:
        t_cnt = count_tokens(line)
        row_tokens.append(t_cnt)
        total_tokens += t_cnt

    avg_tokens = total_tokens / max(1, len(lines))
    max_batch_size = max(1, int(max_context / max(1, avg_tokens)))

    return {
        "file": os.path.basename(file_path),
        "total_lines": len(lines),
        "total_tokens": total_tokens,
        "avg_tokens_per_line": round(avg_tokens, 1),
        "max_recommended_batch_size": max_batch_size,
        "fits_in_context_32k": total_tokens <= 32000,
        "fits_in_context_128k": total_tokens <= 128000
    }

def main():
    parser = argparse.ArgumentParser(description="Estimate and count tokens for medical data")
    parser.add_argument("--file", type=str, help="Dataset file path (.csv, .json, .txt)")
    parser.add_argument("--text", type=str, help="Direct text string")
    parser.add_argument("--max_context", type=int, default=32000, help="Target context window")
    args = parser.parse_args()

    if args.file:
        res = analyze_dataset_tokens(args.file, args.max_context)
    elif args.text:
        cnt = count_tokens(args.text)
        res = {
            "text_length_chars": len(args.text),
            "token_count": cnt,
            "estimated_pct_of_32k": round((cnt / 32000) * 100, 2)
        }
    else:
        sample = "Patient: 71yo Male. MMSE: 25/30, MoCA: 22/30. MRI Hippocampal Vol: 3350 mm3. VBR: 0.107. Diagnosis: Mild Cognitive Impairment."
        cnt = count_tokens(sample)
        res = {
            "sample": sample,
            "token_count": cnt
        }

    print(json.dumps(res, indent=2, ensure_ascii=False))

if __name__ == "__main__":
    main()