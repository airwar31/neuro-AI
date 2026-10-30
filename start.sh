#!/usr/bin/env bash
# ==============================================================================
# NeuroAI Workstation - Clinical Diagnostic Application (Linux Startup)
# ==============================================================================

echo "====================================================================="
echo "  NeuroAI Workstation - Clinical Diagnostic Application (Win/Linux)"
echo "====================================================================="

if [ -d ".venv" ]; then
    source .venv/bin/activate
fi

if command -v java >/dev/null 2>&1; then
    JAVA_CMD="java"
elif [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
else
    echo "[ERROR] Java Runtime Environment not found. Please install OpenJDK 17+."
    exit 1
fi

echo "[INFO] Starting NeuroAI Java Medical Workstation..."
"$JAVA_CMD" -cp "desktop/build/classes:desktop/neuro-ai-app.jar" com.neuroai.desktop.NeuroApp
