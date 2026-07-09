# Care Calendar Polish Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 修复养护记录“下次养护”年份错误并矫正历史数据，同时优化首页日历、模块抽屉和养护页概览体验。

**Architecture:** 后端在 `plant` 领域修正“从文本推断下次养护时间”的算法，并增加一条数据库增量脚本修复历史 `plant_care_record` / `reminder` 脏数据。前端继续沿用移动端单文件状态结构，在 `App.vue` 和 `styles.css` 内局部重构首页日历、抽屉卡片和养护页摘要布局。

**Tech Stack:** Vue 3 + TypeScript + Vite；Spring Boot 2.7 + JdbcTemplate；MySQL 初始化 SQL。

---

### Task 1: 修复养护时间推断与历史数据

**Files:**
- Modify: `backend/src/main/java/com/homeofus/plant/service/PlantService.java`
- Modify: `backend/src/test/java/com/homeofus/plant/service/PlantServiceTest.java`
- Create: `backend/src/main/resources/db/migration/V3__fix_plant_care_next_due.sql`
- Modify: `backend/src/main/resources/application.yml`

**Step 1:** 扩充单测，覆盖“文本里先出现历史日期、后出现下次日期”场景。  
**Step 2:** 把养护文本解析改为“遍历所有月日候选，优先取不早于养护日期且最接近的一项”。  
**Step 3:** 增加增量 SQL，修复历史 `plant_care_record.next_care_at` 和 `reminder.due_at/source_type=PLANT_CARE` 的异常年份数据。

### Task 2: 优化首页日历密度

**Files:**
- Modify: `apps/mobile/src/App.vue`
- Modify: `apps/mobile/src/styles.css`

**Step 1:** 收紧首页摘要卡和日历卡的纵向占用。  
**Step 2:** 缩小日历 cell 高度和内边距，确保首屏能看到“当日详情”。  
**Step 3:** 保持点击热区和可读性，不牺牲手机端单手操作。

### Task 3: 重做模块抽屉观感

**Files:**
- Modify: `apps/mobile/src/styles.css`

**Step 1:** 去掉当前抽屉列表“发虚、挤压、数值露半截”的视觉问题。  
**Step 2:** 让标题、说明、数值、拖拽按钮层级更清楚。  
**Step 3:** 保持拖拽排序能力不变。

### Task 4: 养护页增加待养护概览

**Files:**
- Modify: `apps/mobile/src/App.vue`
- Modify: `apps/mobile/src/styles.css`

**Step 1:** 基于现有提醒数据或养护记录计算“待养护”数量。  
**Step 2:** 在养护列表顶部增加摘要区，展示待养护数量和最近待处理项。  
**Step 3:** 不把新增表单和列表重新混在一起。

### Task 5: 定向验证与停服

**Files:**
- Test: `backend/src/test/java/com/homeofus/plant/service/PlantServiceTest.java`

**Step 1:** 跑后端定向单测。  
**Step 2:** 构建移动端。  
**Step 3:** 本地启动后做页面核验。  
**Step 4:** 结束前停掉本次启动的服务。
