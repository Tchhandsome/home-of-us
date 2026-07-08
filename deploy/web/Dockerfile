# syntax=docker/dockerfile:1.7

FROM node:20-alpine AS build

WORKDIR /workspace

RUN npm config set registry https://registry.npmmirror.com

COPY package.json package-lock.json ./
COPY apps/shared/package.json apps/shared/package.json
COPY apps/mobile/package.json apps/mobile/package.json
COPY apps/admin/package.json apps/admin/package.json
RUN npm ci

COPY apps apps
RUN npm run build:mobile
ENV VITE_BASE_PATH=/admin/
RUN npm run build:admin

FROM nginx:1.27-alpine

COPY deploy/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/apps/mobile/dist /usr/share/nginx/html/mobile
COPY --from=build /workspace/apps/admin/dist /usr/share/nginx/html/admin

EXPOSE 80
