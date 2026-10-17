---
name: med_rag_vector_search
description: Search medical literature, clinical guidelines, and research papers using local semantic vector search. Returns concise chunks to prevent wasting LLM context tokens.
---

# 🔍 Medical RAG Vector Search Skill

This skill allows the agent to semantically search local medical documentation, clinical guidelines (McDonald criteria, NIA-AA, MDS), and research abstracts without loading massive full-text papers into the context window.

## Usage

Run the search script:
```bash
python scripts/search.py --query "McDonald 2017 criteria dissemination in space" --top_k 3
```

## Options
- `--query`: Medical search query string.
- `--top_k`: Number of top relevant chunks to retrieve (default: 3).
- `--corpus`: Path to documentation directory (default: `docs/`).
- `--json`: Output findings in JSON format.
