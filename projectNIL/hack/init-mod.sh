#!/bin/bash

# Assign root directory: Use first argument if provided, otherwise default to current directory (.)
ROOT_DIR="${1:-.}"

# Define standard Java directory structure
DIRS=(
    "src/main/java"
    "src/main/resources"
    "src/test/java"
    "src/test/resources"
)

# Implementation
for dir in "${DIRS[@]}"; do
    TARGET_DIR="$ROOT_DIR/$dir"
    mkdir -p "$TARGET_DIR"
    touch "$TARGET_DIR/.gitkeep"
    echo "Initialized: $TARGET_DIR"
done

echo "Java module setup complete in: $(realpath "$ROOT_DIR")"
