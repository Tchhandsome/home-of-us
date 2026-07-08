# 家庭账号、购物入账与私密空间 Implementation Plan

> **For Codex:** 本计划按项目规约执行；由于项目明确禁止 Git 操作，所有 Git/worktree/commit 相关步骤均跳过。

**Goal:** 补齐分开登录、家庭成员、默认账本分类、购物完成自动入账，以及私密空间/悄悄话留言能力。

**Architecture:** 第一版继续采用模块化单体，公共家庭数据仍固定归属默认家庭。登录采用轻量会话 token，后端通过请求头识别当前用户；后续如要上 App Store 或多人家庭，再替换为 Spring Security/JWT，不影响业务表。

**Tech Stack:** Spring Boot 2.7.13、Spring JDBC、MySQL 8、Vue 3、Vite、TypeScript。

---

### Task 1: 数据模型与会话上下文

**Files:**
- Modify: `backend/src/main/resources/db/migration/V1__init_schema.sql`
- Create: `backend/src/main/java/com/homeofus/auth/**`
- Create: `backend/src/main/java/com/homeofus/common/web/CurrentUser*.java`

**Steps:**
1. 新增 `app_user` 表，关联 `family_member_id`，保存登录名、密码、昵称、会话 token。
2. 新增 `private_message` 表，支持 `PRIVATE`、`TO_PARTNER`、`SHARED` 三类可见范围。
3. 在 `shopping_item` 增加 `actual_amount`、`purchased_at`、`buyer_id`、`finance_record_id`。
4. 实现请求头 `X-Home-Token` 的当前用户解析；未登录时回退到默认用户，保证已有接口可继续跑。

### Task 2: 登录、成员与分类接口

**Files:**
- Create: `backend/src/main/java/com/homeofus/auth/controller/AuthController.java`
- Create: `backend/src/main/java/com/homeofus/auth/service/AuthService.java`
- Modify: `backend/src/main/java/com/homeofus/family/**`
- Modify: `backend/src/main/java/com/homeofus/finance/**`

**Steps:**
1. 提供 `/auth/login`、`/auth/me` 接口。
2. 提供家庭成员创建接口，默认家庭内成员可共享公共数据。
3. 提供 `/finance/categories` 默认分类接口，先返回内置分类，暂不做后台维护。
4. 新增 DTO 使用 Lombok `@Getter`、`@Setter`，对象判空统一使用 `Objects.isNull()`。

### Task 3: 购物完成自动入账

**Files:**
- Create: `backend/src/main/java/com/homeofus/shopping/dto/CompleteShoppingItemRequest.java`
- Modify: `backend/src/main/java/com/homeofus/shopping/**`
- Modify: `backend/src/main/java/com/homeofus/finance/repository/FinanceRepository.java`

**Steps:**
1. 完成购物项时接收金额、分类、购买人。
2. 金额有效且购物项未关联账本时，自动创建 `EXPENSE` 账本记录。
3. 更新购物项为 `DONE`，保存实际金额、购买时间、购买人和账本记录 ID。
4. 接口返回更新状态和账本记录 ID，前端据此提示“已购买并入账”。

### Task 4: 私密空间/悄悄话接口

**Files:**
- Create: `backend/src/main/java/com/homeofus/privatezone/**`

**Steps:**
1. 新增留言创建接口，支持私密、给对方、共享三种范围。
2. 查询接口只返回当前用户可见内容：自己的私密内容、发给自己/自己发出的悄悄话、共享内容。
3. 标记已读接口用于后续提醒扩展，当前先保留基本状态。

### Task 5: 移动端交互

**Files:**
- Modify: `apps/shared/src/api.ts`
- Modify: `apps/mobile/src/App.vue`
- Modify: `apps/mobile/src/styles.css`

**Steps:**
1. 增加登录页，登录后将 token 存在 localStorage 并自动带请求头。
2. 购物页完成操作展开金额/分类/购买人表单，提交后自动入账。
3. 记账页分类改为默认分类选择，不再强制手填。
4. 新增“成员”和“私密”入口；私密页面支持写悄悄话/私密记录。

### Task 6: 管理端与验证

**Files:**
- Modify: `apps/admin/src/App.vue`
- Modify: `apps/admin/src/styles.css`

**Steps:**
1. 管理端增加成员、私密空间的数据展示入口。
2. 执行后端增量编译：`mvn -f backend/pom.xml -DskipTests -o compile`。
3. 执行前端构建：`npm run build`。
4. 做代码审查式自检，重点检查 SQL 幂等性、重复入账、未登录兼容、手机端布局。
