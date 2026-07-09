# Grouped Module Entrance And Plant Quick Care Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 把移动端模块入口改成分组式二级导航，并增强花卉页的一键养护、月度反馈和位置标签体验。

**Architecture:** 本轮以移动端前端为主，保留现有后端接口与页面路由结构，只在左侧抽屉层做“分组入口 -> 子模块”映射。花卉页复用已有养护创建接口实现“今日已养护”快捷弹层，月度养护反馈则直接基于已加载的 `allCareRecords` 和提醒数据计算，无需新增后端统计接口。

**Tech Stack:** Vue 3 + TypeScript + Vite；Lucide 图标；现有 `@home-of-us/shared` API 封装。

---

### Task 1: 重组抽屉模块入口

**Files:**
- Modify: `apps/mobile/src/App.vue`
- Modify: `apps/mobile/src/styles.css`

**Step 1:** 定义模块分组映射：花花、家庭成员、二人世界、杂项。  
**Step 2:** 保留待办、清单、记账、提醒、月经管理、个人等高频模块为单入口。  
**Step 3:** 把抽屉卡片改成“分组卡 + 子按钮”结构，并保持个人排序能力。

### Task 2: 增加花卉页顶部反馈和一键养护

**Files:**
- Modify: `apps/mobile/src/App.vue`
- Modify: `apps/mobile/src/styles.css`

**Step 1:** 在花卉页顶部增加本月照顾天数和待养护数量摘要。  
**Step 2:** 删除“花卉列表”标题，把新增花卉按钮并入更自然的位置。  
**Step 3:** 在花卉卡片上增加“今日已养护”快捷按钮。  
**Step 4:** 点击后弹出快捷表单，填写养护内容和下次养护时间后直接保存。

### Task 3: 优化花卉位置标签与卡片信息密度

**Files:**
- Modify: `apps/mobile/src/App.vue`
- Modify: `apps/mobile/src/styles.css`

**Step 1:** 为花卉新增/编辑表单补充室内、室外、阳台、书房快捷标签。  
**Step 2:** 卡片上优先展示已有标签和花色，不再大量显示“未记录位置”。  
**Step 3:** 保留自定义位置兼容能力，避免旧数据无法编辑。

### Task 4: 定向验证

**Files:**
- Test: `apps/mobile/src/App.vue`

**Step 1:** 运行移动端构建。  
**Step 2:** 核对 TypeScript 编译结果。  
**Step 3:** 确认没有残留本地开发服务。
