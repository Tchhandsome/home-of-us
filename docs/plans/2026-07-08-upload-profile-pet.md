# 图片上传、用户资料编辑与宠物模块 Implementation Plan

> **For Codex:** 本计划按项目规约执行；禁止任何 Git 操作。

**Goal:** 支持真实图片上传，补齐用户新增/编辑/头像能力，并新增宠物档案、宠物照片和宠物医疗记录。

**Architecture:** 第一版文件存储使用后端本地目录 `backend/data/uploads`，通过 `/api/uploads/**` 静态访问。相册、头像、宠物照片复用统一上传接口，业务表只保存图片 URL，后续可替换为对象存储。宠物模块继续作为模块化单体中的独立包。

**Tech Stack:** Spring Boot 2.7.13、Spring JDBC、MySQL 8、Vue 3、Vite、TypeScript、multipart/form-data。

---

### Task 1: 公共图片上传

**Files:**
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/java/com/homeofus/common/web/CorsConfig.java`
- Modify: `backend/src/main/java/com/homeofus/attachment/**`

**Steps:**
1. 增加 multipart 文件大小限制。
2. 新增 `POST /attachments/images`，接收图片文件和可选关联对象。
3. 图片保存到 `backend/data/uploads/yyyyMM/uuid.ext`。
4. 返回 `url`、`fileName`、`contentType`、`size`，其中 `url` 可直接作为 `<img src>`。

### Task 2: 用户资料编辑与头像

**Files:**
- Modify: `backend/src/main/resources/db/migration/V1__init_schema.sql`
- Modify: `backend/src/main/java/com/homeofus/family/**`
- Modify: `backend/src/main/java/com/homeofus/auth/**`

**Steps:**
1. 给 `family_member` 增加 `avatar_url`、`bio`。
2. 创建成员时支持头像 URL。
3. 新增 `PATCH /families/default/members/{id}`，支持昵称、登录名、密码、头像、颜色、简介更新。
4. 当前用户信息返回成员头像、颜色和简介。

### Task 3: 相册真实上传

**Files:**
- Modify: `apps/shared/src/api.ts`
- Modify: `apps/mobile/src/App.vue`

**Steps:**
1. 移动端相册使用 `<input type="file" accept="image/*">`。
2. 先调用上传接口拿到 URL，再创建相册照片。
3. 保留图片链接输入作为兼容和调试入口。

### Task 4: 宠物模块

**Files:**
- Create: `backend/src/main/java/com/homeofus/pet/**`
- Modify: `backend/src/main/resources/db/migration/V1__init_schema.sql`
- Modify: `apps/shared/src/api.ts`
- Modify: `apps/mobile/src/App.vue`

**Steps:**
1. 新增 `pet`、`pet_photo`、`pet_medical_record` 表。
2. 宠物档案支持名称、种类、性别、生日、头像、备注。
3. 宠物照片支持上传后保存。
4. 医疗记录支持驱虫、疫苗、体检、用药等类型，包含日期、医院、备注、下次提醒时间。
5. 移动端新增宠物页面，支持创建宠物、上传照片、添加医疗记录和展示列表。

### Task 5: 验证

**Files:**
- Modify: `README.md`
- Modify: `apps/admin/src/App.vue`

**Steps:**
1. 管理端增加宠物数据入口。
2. 执行后端离线编译：`mvn -f backend/pom.xml -DskipTests -o compile`。
3. 执行前端构建：`npm run build`。
4. 临时启动后端，验证 SQL 初始化、登录、上传接口和宠物接口。
