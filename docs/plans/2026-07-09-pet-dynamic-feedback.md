# Pet Dynamic Feedback And Plant Achievement Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为花卉养护增加守护成就动画，并把宠物档案升级为带年龄换算、倒计时提醒、快捷护理和体重曲线的动态页面。

**Architecture:** 花卉成就动画仅在移动端前端实现，不改后端数据模型。宠物能力新增两条轻量后端记录线：宠物护理记录和宠物体重记录；护理记录支持 FEED/DEWORMING/BATH 三类快捷操作与下一次提醒时间，体重记录用于绘制前端折线图。前端在宠物详情页聚合宠物基础档案、医疗记录、护理记录和体重记录，计算年龄文本、提醒倒计时和任务流状态。

**Tech Stack:** Vue 3 + TypeScript + Vite；Spring Boot 2.7 + JdbcTemplate；MySQL 初始化 SQL；JUnit 5 + Mockito。

---

### Task 1: 扩展宠物后端模型

**Files:**
- Create: `backend/src/main/java/com/homeofus/pet/dto/CreatePetCareRecordRequest.java`
- Create: `backend/src/main/java/com/homeofus/pet/dto/CreatePetWeightRecordRequest.java`
- Modify: `backend/src/main/java/com/homeofus/pet/controller/PetController.java`
- Modify: `backend/src/main/java/com/homeofus/pet/service/PetService.java`
- Modify: `backend/src/main/java/com/homeofus/pet/repository/PetRepository.java`
- Create: `backend/src/main/resources/db/migration/V3__pet_care_weight.sql`
- Modify: `backend/src/main/resources/application.yml`

**Step 1:** 新增 `pet_care_record`、`pet_weight_record` 两张表，并通过幂等 SQL 接入启动初始化。  
**Step 2:** 新增宠物护理记录创建/查询接口。  
**Step 3:** 新增体重记录创建/查询接口。  
**Step 4:** 宠物删除时同步回收护理提醒并逻辑删除相关记录。

### Task 2: 补足宠物后端测试

**Files:**
- Modify: `backend/src/test/java/com/homeofus/pet/service/PetServiceTest.java`

**Step 1:** 增加“创建护理记录会生成提醒”的测试。  
**Step 2:** 增加“创建体重记录”的测试。  
**Step 3:** 保留现有宠物更新测试不回退。

### Task 3: 扩展共享 API

**Files:**
- Modify: `apps/shared/src/api.ts`

**Step 1:** 增加宠物护理记录与体重记录的 payload 类型。  
**Step 2:** 增加创建/查询 API 方法。  
**Step 3:** 保持现有宠物和医疗接口不变。

### Task 4: 升级移动端宠物详情体验

**Files:**
- Modify: `apps/mobile/src/App.vue`
- Modify: `apps/mobile/src/styles.css`

**Step 1:** 宠物生日显示改为动态年龄文本。  
**Step 2:** 待提醒区域增加最近一条倒计时与颜色状态。  
**Step 3:** 在宠物详情页增加快捷操作栏：喂食、驱虫、洗澡。  
**Step 4:** 新增快捷记录弹层，支持时间戳、描述、下次提醒时间。  
**Step 5:** 增加极简体重曲线和记录入口。  
**Step 6:** 把护理任务流渲染成动态状态列表，近期待办发黄，逾期发红。

### Task 5: 增加花卉守护成就动画

**Files:**
- Modify: `apps/mobile/src/App.vue`
- Modify: `apps/mobile/src/styles.css`

**Step 1:** 在保存养护记录成功后触发一次成就动画。  
**Step 2:** 动画文案固定为“守护天数 +1”，强化即时反馈。  
**Step 3:** 不影响现有养护保存逻辑。

### Task 6: 定向验证

**Files:**
- Test: `backend/src/test/java/com/homeofus/pet/service/PetServiceTest.java`

**Step 1:** 运行后端宠物定向测试。  
**Step 2:** 运行移动端构建。  
**Step 3:** 确认没有残留开发服务。
