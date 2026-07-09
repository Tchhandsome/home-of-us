# Calendar Home And Period MVP Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 把首页重构为日历中心，接入待办/提醒/养护/纪念日/月经周期，并同步完成留言板、记账、共享待办与图片加载优化这批收口需求。

**Architecture:** 后端新增 `period` 领域承载月经管理 MVP，并扩展 `chore`、`privatezone`、`attachment` 领域支持多认领、留言日期/编辑与图片压缩；前端继续沿用 `App.vue` 单文件状态驱动，但首页改为日历主视图，模块入口迁移到左侧抽屉。

**Tech Stack:** Vue 3 + TypeScript + Vite；Spring Boot 2.7 + JdbcTemplate；MySQL 8 初始化 SQL；Lucide 图标。

---

### Task 1: 后端数据模型扩展

**Files:**
- Modify: `backend/src/main/resources/db/migration/V1__init_schema.sql`
- Create: `backend/src/main/java/com/homeofus/period/service/PeriodService.java`
- Create: `backend/src/main/java/com/homeofus/period/controller/PeriodController.java`
- Create: `backend/src/main/java/com/homeofus/period/repository/PeriodRepository.java`
- Create: `backend/src/main/java/com/homeofus/period/dto/SavePeriodProfileRequest.java`
- Create: `backend/src/main/java/com/homeofus/period/dto/CreatePeriodRecordRequest.java`
- Create: `backend/src/test/java/com/homeofus/period/service/PeriodServiceTest.java`

**Step 1:** 新增 `period_profile`、`period_record`、`chore_task_assignee` 三张表。  
**Step 2:** 为 `private_message` 增加 `message_date` 字段。  
**Step 3:** 用 `PeriodServiceTest` 锁定周期预测与提醒同步。

### Task 2: 待办与留言后端规则升级

**Files:**
- Modify: `backend/src/main/java/com/homeofus/chore/dto/CreateChoreTaskRequest.java`
- Modify: `backend/src/main/java/com/homeofus/chore/dto/UpdateChoreTaskRequest.java`
- Modify: `backend/src/main/java/com/homeofus/chore/service/ChoreService.java`
- Modify: `backend/src/main/java/com/homeofus/chore/repository/ChoreRepository.java`
- Modify: `backend/src/test/java/com/homeofus/chore/service/ChoreServiceTest.java`
- Modify: `backend/src/main/java/com/homeofus/privatezone/dto/CreatePrivateMessageRequest.java`
- Create: `backend/src/main/java/com/homeofus/privatezone/dto/UpdatePrivateMessageRequest.java`
- Modify: `backend/src/main/java/com/homeofus/privatezone/service/PrivateMessageService.java`
- Modify: `backend/src/main/java/com/homeofus/privatezone/repository/PrivateMessageRepository.java`
- Modify: `backend/src/main/java/com/homeofus/privatezone/controller/PrivateMessageController.java`

**Step 1:** 个人任务默认创建人认领。  
**Step 2:** 共享任务支持 0~N 个认领人。  
**Step 3:** 留言支持日期与编辑，仅发送人可修改。

### Task 3: 图片上传优化

**Files:**
- Modify: `backend/src/main/java/com/homeofus/attachment/service/AttachmentService.java`

**Step 1:** 上传时对可解析图片做压缩与缩放。  
**Step 2:** 保持原接口不变，优先兼容现有调用。  
**Step 3:** 前端配合 `lazy`、固定尺寸和异步解码。

### Task 4: 共享 API 与移动端状态重构

**Files:**
- Modify: `apps/shared/src/api.ts`
- Modify: `apps/mobile/src/App.vue`
- Modify: `apps/mobile/src/styles.css`
- Modify: `backend/src/main/java/com/homeofus/family/service/FamilyService.java`

**Step 1:** 首页改成日历页，增加左侧抽屉模块入口。  
**Step 2:** 日历聚合待办、提醒、纪念日、月经周期，并支持当天快速新增个人待办。  
**Step 3:** 记账、留言板改成“列表页 -> 新增页 / 编辑页”。  
**Step 4:** 花卉卡片排版修复，宠物/时刻墙照片启用懒加载与稳定占位。  
**Step 5:** 新增“月经管理”模块页。

### Task 5: 定向验证

**Files:**
- Test: `backend/src/test/java/com/homeofus/period/service/PeriodServiceTest.java`
- Test: `backend/src/test/java/com/homeofus/chore/service/ChoreServiceTest.java`

**Step 1:** 运行后端定向测试。  
**Step 2:** 构建移动端。  
**Step 3:** 确认不遗留本地运行服务。
