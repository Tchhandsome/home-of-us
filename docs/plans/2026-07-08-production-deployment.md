# Home Of Us Production Deployment Implementation Plan

**Goal:** 为 Home Of Us 增加第一版 Docker Compose 生产部署能力。

**Architecture:** 前端移动端和管理端由同一个 Nginx 镜像托管，移动端位于 `/`，管理端位于 `/admin/`。后端使用 Spring Boot 镜像运行，MySQL 使用官方镜像，上传文件挂载到宿主机 `./data/uploads`。

**Tech Stack:** Docker Compose、Nginx、Spring Boot 2.7、MySQL 8、Vue 3、Vite。

---

### Checkpoint 1: 镜像构建文件

- 新增 `backend/Dockerfile`，使用 Maven 多阶段构建 Spring Boot jar，运行阶段使用 JRE 17。
- 新增 `deploy/web/Dockerfile`，构建移动端和管理端，再复制到 Nginx 镜像。
- 新增 `deploy/maven/settings.xml`，生产构建时默认使用阿里云 Maven 镜像。

### Checkpoint 2: Nginx 与 Compose

- 新增 `deploy/nginx/default.conf`，代理 `/api/` 到后端，托管 `/` 和 `/admin/`。
- 新增 `docker-compose.prod.yml`，支持服务器源码构建。
- 新增 `docker-compose.images.yml`，支持预构建镜像上传或从镜像仓库拉取。

### Checkpoint 3: 部署文档与验证

- 更新环境变量示例，补充生产部署变量。
- 新增 `docs/deploy.md`，说明源码上传部署、镜像 tar 上传部署和常用排障命令。
- 本地执行前端构建、后端编译和 Compose 配置校验。
