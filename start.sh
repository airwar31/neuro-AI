#!/usr/bin/env bash
# ==============================================================================
# NeuroAI Studio - Unix Startup Script
# ==============================================================================

echo "====================================================================="
echo "  🧠 NeuroAI Studio - Neurological Disorder Classifier"
echo "====================================================================="

if [ -d ".venv" ]; then
    source .venv/bin/activate
fi

python3 web/app.py
