# Family Expansion Features Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 完成共享待办、时刻墙增强、库存提醒与投票、菜谱餐单四组剩余功能，并把首页与移动端入口同步扩展到新模块。

**Architecture:** 后端优先复用现有 `chore_task`、`album_photo`、`inventory_item`、`shopping_item`、`reminder` 等表和服务能力；对确实没有承载体的“家庭投票”和“菜谱餐单”新增轻量表与领域包。前端继续维持单 `App.vue` 状态驱动模式，不额外引入 Router，统一采用“列表页 -> 新增页 / 编辑页 / 详情页”的交互。

**Tech Stack:** Spring Boot 2.7、JdbcTemplate、MySQL 8 初始化 SQL、Vue 3、TypeScript、Vite、Lucide。

---

### Task 1: 扩展数据库结构与首页卡片默认配置

**Files:**
- Modify: `backend/src/main/resources/db/migration/V1__init_schema.sql`
- Modify: `backend/src/main/java/com/homeofus/family/service/FamilyService.java`

**Step 1: 写出本轮新增字段与新表清单**

需要新增：
- `chore_task.task_scope`
- `chore_task.owner_member_id`
- `chore_task.note`
- `album_photo.entry_type`
- `album_photo.wish_text`
- `album_photo.reminder_enabled`
- `album_photo.reminder_days_before`
- `album_photo.next_remind_at`
- `inventory_item.item_type`
- `inventory_item.expires_on`
- `inventory_item.reminder_days_before`
- `inventory_item.note`
- `family_vote`
- `family_vote_option`
- `family_vote_choice`
- `recipe`
- `meal_plan`

**Step 2: 在 `V1__init_schema.sql` 中追加幂等 DDL**

要求：
- 继续使用 `CREATE TABLE IF NOT EXISTS`
- 对既有表字段使用 `INFORMATION_SCHEMA.COLUMNS` + 动态 `ALTER TABLE`
- 对 `album_photo.image_url` 做兼容性调整，允许为空

**Step 3: 更新首页卡片默认顺序**

`FamilyService.DEFAULT_HOME_CARD_ORDER` 需要扩展为：

```java
List.of("todo", "plants", "care", "shopping", "finance", "reminders", "members",
        "album", "pets", "inventory", "recipes", "private", "profile")
```

同时保证旧排序偏好能自动补齐新卡片，不丢原有顺序。

**Step 4: 编译后端确认 DDL 相关代码无误**

Run:

```bash
cd /Users/tch/develop/home-of-us/backend
mvn -DskipTests compile
```

Expected:
- `BUILD SUCCESS`

### Task 2: 把“记录”替换为共享待办后端能力

**Files:**
- Create: `backend/src/main/java/com/homeofus/chore/dto/UpdateChoreTaskRequest.java`
- Create: `backend/src/test/java/com/homeofus/chore/service/ChoreServiceTest.java`
- Modify: `backend/src/main/java/com/homeofus/chore/controller/ChoreController.java`
- Modify: `backend/src/main/java/com/homeofus/chore/service/ChoreService.java`
- Modify: `backend/src/main/java/com/homeofus/chore/repository/ChoreRepository.java`
- Modify: `backend/src/main/java/com/homeofus/system/dashboard/DashboardRepository.java`

**Step 1: 写失败用例**

至少覆盖：
- 创建个人任务与共享任务
- 共享任务认领
- 完成任务后状态同步
- 删除任务后列表不再返回

**Step 2: 补接口**

新增接口：
- `PATCH /chore/tasks/{id}`
- `PATCH /chore/tasks/{id}/claim`
- `PATCH /chore/tasks/{id}/complete`
- `DELETE /chore/tasks/{id}`

**Step 3: 服务实现**

规则：
- `task_scope=PERSONAL` 时 `owner_member_id=当前登录成员`
- `task_scope=SHARED` 时可不指定认领人
- 认领后更新 `assignee_id`
- 完成时写入 `completed_at`
- 如果设置了 `dueAt`，则创建 `TODO_TASK` 提醒
- 更新或删除任务时同步回收提醒

**Step 4: 后端测试**

Run:

```bash
cd /Users/tch/develop/home-of-us/backend
mvn -Dtest=ChoreServiceTest -DfailIfNoTests=false test
```

Expected:
- `BUILD SUCCESS`

### Task 3: 把“共同相册”升级为时刻墙后端能力

**Files:**
- Create: `backend/src/main/java/com/homeofus/album/dto/UpdateMemoryEntryRequest.java`
- Create: `backend/src/test/java/com/homeofus/album/service/AlbumServiceTest.java`
- Modify: `backend/src/main/java/com/homeofus/album/controller/AlbumController.java`
- Modify: `backend/src/main/java/com/homeofus/album/service/AlbumService.java`
- Modify: `backend/src/main/java/com/homeofus/album/repository/AlbumRepository.java`

**Step 1: 扩展请求与列表语义**

保留现有相册能力，但接口语义升级为通用记忆条目：
- `PHOTO`
- `MOOD`
- `ANNIVERSARY`
- `BIRTHDAY`

**Step 2: 新增更新接口**

新增：
- `PATCH /album/photos/{id}`

并把创建接口支持：
- 图片可为空
- `wishText`
- `reminderEnabled`
- `reminderDaysBefore`

**Step 3: 实现周年提醒计算**

规则：
- `ANNIVERSARY` / `BIRTHDAY` 使用 `taken_on` 作为基准日期
- 自动计算下一次发生日
- 根据 `reminderDaysBefore` 生成 `MEMORY_EVENT` 提醒
- 更新/删除条目时回收旧提醒

**Step 4: 后端测试**

Run:

```bash
cd /Users/tch/develop/home-of-us/backend
mvn -Dtest=AlbumServiceTest -DfailIfNoTests=false test
```

Expected:
- `BUILD SUCCESS`

### Task 4: 完成库存提醒与家庭投票后端

**Files:**
- Create: `backend/src/main/java/com/homeofus/inventory/dto/UpdateInventoryItemRequest.java`
- Create: `backend/src/main/java/com/homeofus/vote/controller/FamilyVoteController.java`
- Create: `backend/src/main/java/com/homeofus/vote/service/FamilyVoteService.java`
- Create: `backend/src/main/java/com/homeofus/vote/repository/FamilyVoteRepository.java`
- Create: `backend/src/main/java/com/homeofus/vote/dto/CreateFamilyVoteRequest.java`
- Create: `backend/src/main/java/com/homeofus/vote/dto/SubmitFamilyVoteRequest.java`
- Create: `backend/src/test/java/com/homeofus/inventory/service/InventoryServiceTest.java`
- Create: `backend/src/test/java/com/homeofus/vote/service/FamilyVoteServiceTest.java`
- Modify: `backend/src/main/java/com/homeofus/inventory/controller/InventoryController.java`
- Modify: `backend/src/main/java/com/homeofus/inventory/service/InventoryService.java`
- Modify: `backend/src/main/java/com/homeofus/inventory/repository/InventoryRepository.java`

**Step 1: 写库存失败用例**

至少覆盖：
- 低库存时自动创建提醒
- 数量恢复后删除低库存提醒
- 设置过期时间时创建过期提醒

**Step 2: 补库存更新/删除接口**

新增：
- `PATCH /inventory/items/{id}`
- `DELETE /inventory/items/{id}`

**Step 3: 新增投票领域**

接口：
- `POST /family-votes`
- `GET /family-votes`
- `PATCH /family-votes/{id}/vote`
- `PATCH /family-votes/{id}/decide`
- `DELETE /family-votes/{id}`

规则：
- 默认模板支持“吃什么”“玩什么”
- 每个成员一票，可重复提交覆盖旧票
- `decide` 支持随机选项并写入历史结果

**Step 4: 后端测试**

Run:

```bash
cd /Users/tch/develop/home-of-us/backend
mvn -Dtest=InventoryServiceTest,FamilyVoteServiceTest -DfailIfNoTests=false test
```

Expected:
- `BUILD SUCCESS`

### Task 5: 新增菜谱与餐单后端

**Files:**
- Create: `backend/src/main/java/com/homeofus/recipe/controller/RecipeController.java`
- Create: `backend/src/main/java/com/homeofus/recipe/service/RecipeService.java`
- Create: `backend/src/main/java/com/homeofus/recipe/repository/RecipeRepository.java`
- Create: `backend/src/main/java/com/homeofus/recipe/dto/CreateRecipeRequest.java`
- Create: `backend/src/main/java/com/homeofus/recipe/dto/UpdateRecipeRequest.java`
- Create: `backend/src/main/java/com/homeofus/recipe/dto/CreateMealPlanRequest.java`
- Create: `backend/src/test/java/com/homeofus/recipe/service/RecipeServiceTest.java`

**Step 1: 定义最小可用模型**

`recipe`：
- 标题
- 分类
- 原料文本
- 做法文本
- 喜欢成员 ID 列表

`meal_plan`：
- 周起始日
- 计划日期
- 餐次
- 关联菜谱
- 提醒时间
- 是否已同步购物清单

**Step 2: 补接口**

接口：
- `POST /recipes`
- `GET /recipes`
- `PATCH /recipes/{id}`
- `DELETE /recipes/{id}`
- `POST /recipes/meal-plans`
- `GET /recipes/meal-plans`
- `POST /recipes/meal-plans/generate-weekly`
- `POST /recipes/meal-plans/{weekStart}/shopping-sync`

**Step 3: 实现自动生成规则**

规则：
- 一周餐单按当前可用菜谱轮转或随机
- 如果菜谱不足，可重复使用但避免连续同一道
- 餐单提醒通过 `MEAL_PLAN` 写入提醒
- 购物清单同步时按原料文本逐行拆分，去重后写入 `shopping_item`

**Step 4: 后端测试**

Run:

```bash
cd /Users/tch/develop/home-of-us/backend
mvn -Dtest=RecipeServiceTest -DfailIfNoTests=false test
```

Expected:
- `BUILD SUCCESS`

### Task 6: 扩展共享 API 合同

**Files:**
- Modify: `apps/shared/src/api.ts`

**Step 1: 新增待办、时刻墙、库存、投票、菜谱相关类型**

包括但不限于：
- `CreateTodoPayload`
- `UpdateTodoPayload`
- `CreateMemoryEntryPayload`
- `UpdateMemoryEntryPayload`
- `CreateInventoryPayload`
- `UpdateInventoryPayload`
- `CreateFamilyVotePayload`
- `SubmitFamilyVotePayload`
- `CreateRecipePayload`
- `CreateMealPlanPayload`

**Step 2: 新增 API 方法**

要求把后端新增接口全部接到共享层，避免 `App.vue` 直接写 `fetch`。

**Step 3: 类型检查**

Run:

```bash
cd /Users/tch/develop/home-of-us
npm run build --workspace apps/mobile
```

Expected:
- 若前端页面还未跟上，可能出现类型报错；记录后继续实现下一任务。

### Task 7: 重构移动端首页与待办/时刻墙/库存页

**Files:**
- Modify: `apps/mobile/src/App.vue`
- Modify: `apps/mobile/src/styles.css`

**Step 1: 首页卡片扩展**

新增或替换卡片：
- `todo`
- `inventory`
- `recipes`
- `album` 显示名改“时刻墙”

**Step 2: 待办页改造**

要求：
- 记录页彻底替换为待办页
- 默认列表
- 过滤：全部 / 我的 / 共享 / 已完成
- 支持新增、编辑、认领、完成、删除

**Step 3: 时刻墙页改造**

要求：
- 默认看时间线
- 右上角 `+` 新增
- 条目类型切换影响表单字段
- 支持编辑、删除、提醒预览、愿望清单显示

**Step 4: 库存页改造**

要求：
- 默认列表
- 分段：物资 / 投票
- 物资支持新增、编辑、删除
- 投票支持模板创建、投票、随机决定、查看历史

**Step 5: 前端构建**

Run:

```bash
cd /Users/tch/develop/home-of-us
npm run build --workspace apps/mobile
```

Expected:
- `BUILD SUCCESS`

### Task 8: 完成移动端菜谱餐单页与总回归

**Files:**
- Modify: `apps/mobile/src/App.vue`
- Modify: `apps/mobile/src/styles.css`

**Step 1: 菜谱页实现**

要求：
- 菜谱库列表页
- 新增/编辑页
- 一周餐单页
- 一键生成本周餐单
- 一键同步购物清单

**Step 2: 统一时间格式与返回体验**

要求：
- 新页面中的提醒时间、过期时间、纪念日时间统一走现有格式化函数
- 继续保持所有页面左上角返回首页

**Step 3: 后端最小测试集**

Run:

```bash
cd /Users/tch/develop/home-of-us/backend
mvn -Dtest=ChoreServiceTest,AlbumServiceTest,InventoryServiceTest,FamilyVoteServiceTest,RecipeServiceTest -DfailIfNoTests=false test
```

Expected:
- `BUILD SUCCESS`

**Step 4: 前端生产构建**

Run:

```bash
cd /Users/tch/develop/home-of-us
npm run build --workspace apps/mobile
```

Expected:
- `BUILD SUCCESS`

**Step 5: 本地手工验收**

清单：
1. 首页能看到待办、时刻墙、库存、菜谱卡片
2. 待办支持共享任务认领与完成同步
3. 时刻墙支持纯文字心情、纪念日、生日、愿望清单
4. 库存支持低库存提醒与过期提醒
5. 投票支持默认选项、成员投票、随机结果和历史记录
6. 菜谱支持新建菜谱、生成周餐单、同步购物清单

**Step 6: 停掉本轮启动的服务**

要求：
- 关闭本轮启动的 `npm run dev:mobile`
- 如果为了联调启动了后端，也一并关闭
- 结束前确认没有遗留前台会话

## Remember

- 所有输出、注释、文档保持中文。
- Java 判空必须使用 `Objects` / `StringUtils`，不要直接 `== null`。
- 不做任何 Git 操作。
- 严格复用现有提醒中心，不在各模块自行造提醒表。
- 优先做稳定可上线的规则版，不做 AI 自动生成或复杂推荐。

## Execution Handoff

计划已保存到 `docs/plans/2026-07-09-family-expansion-features.md`。当前线程直接继续执行，不另开实现线程；完成验证后关闭我本轮启动过的服务。
