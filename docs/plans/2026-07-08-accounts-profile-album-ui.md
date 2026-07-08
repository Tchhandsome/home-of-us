# 默认账号、个人信息、成员页与共同相册 Implementation Plan

> **For Codex:** 本计划按项目规约执行；禁止任何 Git 操作。

**Goal:** 去掉登录页“我/她”快捷入口，将默认账号调整为“小谭/123456”和“丹丹/123456”，补齐个人信息页、独立添加成员页和两人共同相册。

**Architecture:** 继续沿用模块化单体。默认成员主键 `1001/1002` 保留不变，仅迁移显示名和登录名，避免已有账本、购物和留言引用断裂。共同相册第一版记录照片链接和文字信息，不引入真实文件上传服务。

**Tech Stack:** Spring Boot 2.7.13、Spring JDBC、MySQL 8、Vue 3、Vite、TypeScript。

---

### Task 1: 默认账号迁移

**Files:**
- Modify: `backend/src/main/resources/db/migration/V1__init_schema.sql`
- Modify: `backend/src/main/java/com/homeofus/common/web/CurrentUserProvider.java`
- Modify: `README.md`

**Steps:**
1. 将默认 `family_member` 的 `1001/1002` 改为“小谭/丹丹”。
2. 将默认 `app_user` 的 `1001/1002` 改为用户名“小谭/丹丹”、密码 `123456`。
3. 将旧用户名 `me/her` 迁移或标记为删除，避免界面继续出现旧账号。
4. 更新后端默认用户兜底信息。

### Task 2: 共同相册后端

**Files:**
- Create: `backend/src/main/java/com/homeofus/album/**`
- Modify: `backend/src/main/resources/db/migration/V1__init_schema.sql`

**Steps:**
1. 新增 `album_photo` 表，字段包含标题、图片地址、说明、拍摄日期、创建人。
2. 新增相册创建和查询接口：`POST /album/photos`、`GET /album/photos`。
3. 创建相册记录时使用当前登录人作为创建者。

### Task 3: 移动端页面改造

**Files:**
- Modify: `apps/shared/src/api.ts`
- Modify: `apps/mobile/src/App.vue`
- Modify: `apps/mobile/src/styles.css`

**Steps:**
1. 登录页移除“我/她”按钮，只保留账号密码输入。
2. 新增个人信息页，顶部用户入口跳转到个人信息。
3. 成员页只展示成员列表和添加按钮，添加成员表单独立成页面。
4. 新增共同相册页，支持新增照片链接并展示照片墙。
5. 调整移动端视觉：降低大卡片压迫感，增加更轻的纸感、分层和按钮状态。

### Task 4: 管理端与验证

**Files:**
- Modify: `apps/admin/src/App.vue`

**Steps:**
1. 管理端增加共同相册数据入口。
2. 执行 `mvn -f backend/pom.xml -DskipTests -o compile`。
3. 执行 `npm run build`。
4. 临时启动后端验证 SQL 初始化、登录和相册接口。
