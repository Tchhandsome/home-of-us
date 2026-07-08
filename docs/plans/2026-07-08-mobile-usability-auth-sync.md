# 移动端可用性、登录态与同步修复 Implementation Plan

> **For Codex:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.
> **For Codex:** 本项目禁止任何 Git 操作，本计划仅用于当前工作区直接实现与验证。

**Goal:** 修复移动端当前阻塞使用的登录、交互反馈、角色编辑、宠物页布局、账本汇总、提醒联动、删除能力与双端同步问题。

**Architecture:** 以后端最小增量补齐认证兜底、删除接口、账本汇总接口和提醒来源联动；移动端继续基于单文件 `App.vue` 收敛导航、补齐按钮反馈、自动消失提示、上传状态与分区布局，避免做无关拆分。涉及当前用户的写操作统一复用 `CurrentUserProvider`，避免继续写死 `1001`。

**Tech Stack:** Spring Boot 2.7.13、Spring JDBC、MySQL 8、Vue 3、Vite、TypeScript、Lucide。

---

### Task 1: 登录态与当前用户安全修复

**Files:**
- Modify: `backend/src/main/java/com/homeofus/common/web/CurrentUserProvider.java`
- Modify: `backend/src/main/java/com/homeofus/common/exception/GlobalExceptionHandler.java`
- Modify: `apps/shared/src/api.ts`
- Modify: `apps/mobile/src/App.vue`

**Steps:**
1. 去掉未登录默认回退到 `小谭` 的行为，缺少或失效 token 时返回明确认证失败。
2. 前端 token 存储改为“默认会话级”，并支持显式“记住登录”。
3. 登录页去掉默认填充的 `小谭 / 123456`。
4. 认证失效时前端自动清理 token 并回到登录页。

### Task 2: 家庭成员资料与角色可编辑

**Files:**
- Modify: `backend/src/main/java/com/homeofus/family/service/FamilyService.java`
- Modify: `backend/src/main/java/com/homeofus/family/repository/FamilyRepository.java`
- Modify: `backend/src/main/java/com/homeofus/family/dto/CreateFamilyMemberRequest.java`
- Modify: `backend/src/main/java/com/homeofus/family/dto/UpdateFamilyMemberRequest.java`
- Modify: `apps/mobile/src/App.vue`

**Steps:**
1. 创建成员时允许传入自定义 `roleCode`，默认值从 `PARTNER` 调整为更中性的角色值。
2. 编辑本人资料和编辑成员时都支持修改角色。
3. 移动端提供常用角色建议值，并允许手工输入自定义角色名称，例如“女主人”“男主人”。
4. 保存资料后立即刷新当前用户与家庭成员信息。

### Task 3: 删除能力与提醒联动后端补齐

**Files:**
- Modify: `backend/src/main/java/com/homeofus/record/controller/QuickRecordController.java`
- Modify: `backend/src/main/java/com/homeofus/record/service/QuickRecordService.java`
- Modify: `backend/src/main/java/com/homeofus/record/repository/QuickRecordRepository.java`
- Modify: `backend/src/main/java/com/homeofus/plant/controller/PlantController.java`
- Modify: `backend/src/main/java/com/homeofus/plant/service/PlantService.java`
- Modify: `backend/src/main/java/com/homeofus/plant/repository/PlantRepository.java`
- Modify: `backend/src/main/java/com/homeofus/reminder/controller/ReminderController.java`
- Modify: `backend/src/main/java/com/homeofus/reminder/service/ReminderService.java`
- Modify: `backend/src/main/java/com/homeofus/reminder/repository/ReminderRepository.java`
- Modify: `backend/src/main/java/com/homeofus/finance/controller/FinanceController.java`
- Modify: `backend/src/main/java/com/homeofus/finance/service/FinanceService.java`
- Modify: `backend/src/main/java/com/homeofus/finance/repository/FinanceRepository.java`
- Modify: `backend/src/main/java/com/homeofus/album/controller/AlbumController.java`
- Modify: `backend/src/main/java/com/homeofus/album/service/AlbumService.java`
- Modify: `backend/src/main/java/com/homeofus/album/repository/AlbumRepository.java`
- Modify: `backend/src/main/java/com/homeofus/pet/controller/PetController.java`
- Modify: `backend/src/main/java/com/homeofus/pet/service/PetService.java`
- Modify: `backend/src/main/java/com/homeofus/pet/repository/PetRepository.java`
- Modify: `backend/src/main/java/com/homeofus/shopping/controller/ShoppingController.java`
- Modify: `backend/src/main/java/com/homeofus/shopping/service/ShoppingService.java`
- Modify: `backend/src/main/java/com/homeofus/shopping/repository/ShoppingRepository.java`

**Steps:**
1. 为快速记录、花卉、养护记录、提醒、购物项、账本记录、相册照片、宠物档案、宠物照片、宠物医疗记录补齐逻辑删除接口。
2. 删除花卉、宠物等聚合根时同时处理其下属记录，避免前端残留脏数据。
3. 养护记录新增提醒时改为基于解析后的 `nextCareAt` 判定，不再只依赖原始字符串判空。
4. 宠物医疗记录若填写 `nextDueAt`，同步创建提醒。
5. 增加账本汇总接口，返回总支出、本月支出、本周支出。

### Task 4: 移动端交互反馈、导航与页面布局修复

**Files:**
- Modify: `apps/shared/src/api.ts`
- Modify: `apps/mobile/src/App.vue`
- Modify: `apps/mobile/src/styles.css`

**Steps:**
1. 给所有主要提交按钮补齐加载态、禁用态和失败提示，避免“点了没反应”。
2. Toast 改为自动消失，并在成功/失败时显示不同文案。
3. 图片上传前后给出明确提示，按钮文案体现“上传并保存”。
4. 宠物页拆成“档案总览 / 新增档案 / 照片 / 医疗记录”分区，避免全部堆叠。
5. 底部导航收敛为高频入口，低频页面通过首页“常用入口”和页面内跳转进入。
6. 记账页顶部展示总支出、本月支出、本周支出。
7. 在主要列表项补充删除按钮。

### Task 5: 双端同步与验证

**Files:**
- Modify: `apps/mobile/src/App.vue`
- Modify: `docs/deploy.md`

**Steps:**
1. 增加轻量自动刷新策略，例如登录后定时刷新与页面重新聚焦刷新。
2. 确认“丹丹新增、小谭查看”这类跨端数据在可接受时间内自动同步。
3. 执行后端增量编译：`mvn -f backend/pom.xml -DskipTests -o compile`。
4. 执行前端构建：`npm run build:mobile` 或 `npm run build`。
5. 在部署文档补充重新发版的最短路径说明。
