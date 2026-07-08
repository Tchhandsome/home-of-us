#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUT_DIR="${ROOT_DIR}/deploy/dist"
PLATFORM="${PLATFORM:-linux/amd64}"
BACKEND_IMAGE="${BACKEND_IMAGE:-home-of-us-backend:latest}"
WEB_IMAGE="${WEB_IMAGE:-home-of-us-web:latest}"
MYSQL_IMAGE="${MYSQL_IMAGE:-mysql:8.0}"

mkdir -p "${OUT_DIR}"

cd "${ROOT_DIR}"

mvn -s deploy/maven/settings.xml -f backend/pom.xml -DskipTests package
npm run build:mobile
VITE_BASE_PATH=/admin/ npm run build:admin

docker buildx build --platform "${PLATFORM}" -f backend/Dockerfile.runtime -t "${BACKEND_IMAGE}" --load .
docker buildx build --platform "${PLATFORM}" -f deploy/web/Dockerfile.runtime -t "${WEB_IMAGE}" --load .
docker pull --platform "${PLATFORM}" "${MYSQL_IMAGE}"

docker save "${BACKEND_IMAGE}" "${WEB_IMAGE}" "${MYSQL_IMAGE}" | gzip > "${OUT_DIR}/home-of-us-images.tar.gz"
COPYFILE_DISABLE=1 tar --no-xattrs -czf "${OUT_DIR}/home-of-us-deploy.tar.gz" \
    docker-compose.images.yml \
    .env.example \
    docs/deploy.md

echo "镜像包已生成：${OUT_DIR}/home-of-us-images.tar.gz"
echo "部署配置包已生成：${OUT_DIR}/home-of-us-deploy.tar.gz"
