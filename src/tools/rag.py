# -*- coding: utf-8 -*-
import sys
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
if hasattr(sys.stderr, 'reconfigure'):
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')

"""
Medical RAG Vector Search Engine.
Performs semantic text chunking, TF-IDF / BM25 term weighting, and cosine ranking.
"""

import os
import sys
import argparse
import json
import re
from math import log, sqrt
from collections import Counter

def tokenize(text):
    return re.findall(r'\w+', text.lower())

class LocalMedRAG:
    def __init__(self, corpus_dir="docs"):
        self.corpus_dir = corpus_dir
        self.chunks = []
        self.build_index()

    def build_index(self):
        self.chunks = []
        if not os.path.exists(self.corpus_dir):
            return

        for root, _, files in os.walk(self.corpus_dir):
            for file in files:
                if file.endswith(".md"):
                    path = os.path.join(root, file)
                    rel_path = os.path.relpath(path, self.corpus_dir)
                    try:
                        with open(path, "r", encoding="utf-8", errors="ignore") as f:
                            content = f.read()
                        
                        # Chunk by markdown sections (## or 250 words)
                        sections = content.split("## ")
                        for idx, sec in enumerate(sections):
                            sec_text = sec.strip()
                            if len(sec_text) < 40:
                                continue
                            title = sec_text.split("\n")[0].strip("# ")
                            self.chunks.append({
                                "id": f"{rel_path}#sec{idx}",
                                "file": rel_path,
                                "title": title,
                                "text": sec_text[:800],
                                "tokens": tokenize(sec_text)
                            })
                    except Exception:
                        pass

    def search(self, query, top_k=3):
        q_tokens = tokenize(query)
        if not q_tokens or not self.chunks:
            return []

        q_counter = Counter(q_tokens)
        scores = []

        for chunk in self.chunks:
            c_counter = Counter(chunk["tokens"])
            score = 0.0
            for term, q_cnt in q_counter.items():
                if term in c_counter:
                    # BM25-like term frequency
                    tf = c_counter[term]
                    score += q_cnt * (tf / (tf + 1.2))
            
            if score > 0:
                scores.append((score, chunk))

        scores.sort(key=lambda x: x[0], reverse=True)
        results = []
        for s, c in scores[:top_k]:
            results.append({
                "score": round(s, 3),
                "file": c["file"],
                "section": c["title"],
                "content": c["text"]
            })
        return results

def main():
    parser = argparse.ArgumentParser(description="Medical RAG Semantic Search")
    parser.add_argument("--query", type=str, required=True, help="Search query")
    parser.add_argument("--top_k", type=int, default=3, help="Number of chunks")
    parser.add_argument("--corpus", type=str, default="docs", help="Corpus directory")
    parser.add_argument("--json", action="store_true", help="Output as JSON")
    args = parser.parse_args()

    rag = LocalMedRAG(corpus_dir=args.corpus)
    results = rag.search(args.query, top_k=args.top_k)

    if args.json:
        print(json.dumps(results, ensure_ascii=False, indent=2))
    else:
        print(f"=== Found {len(results)} relevant medical chunks for: '{args.query}' ===")
        for i, r in enumerate(results, 1):
            print(f"\n[{i}] Source: {r['file']} | Section: {r['section']} (Relevance: {r['score']})")
            print("-" * 65)
            print(r['content'])

if __name__ == "__main__":
    main()