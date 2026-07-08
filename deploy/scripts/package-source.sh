#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUT_DIR="${ROOT_DIR}/deploy/dist"
OUT_FILE="${OUT_DIR}/home-of-us-source.tar.gz"

mkdir -p "${OUT_DIR}"
rm -f "${OUT_FILE}"

cd "${ROOT_DIR}"

COPYFILE_DISABLE=1 tar --no-xattrs \
    --exclude='./node_modules' \
    --exclude='./backend/target' \
    --exclude='./apps/mobile/dist' \
    --exclude='./apps/admin/dist' \
    --exclude='./.git' \
    --exclude='./data' \
    --exclude='./deploy/dist' \
    -czf "${OUT_FILE}" .

echo "源码包已生成：${OUT_FILE}"
