# Mobile First Batch Pages Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 完成第一轮移动端结构改造：首页改为模块卡片总入口并支持个人排序同步；提醒、宠物、个人页改为“列表/详情/新增/编辑”分离；补上宠物编辑与成员密码权限限制。

**Architecture:** 继续沿用当前 `apps/mobile/src/App.vue` 的单文件状态驱动方案，不额外引入 Vue Router。后端在 `family` 域新增轻量成员偏好存储能力，并在 `pet` 域补齐更新接口；前端通过 `activeTab + 模块子视图状态` 实现独立页面体验，并用统一时间格式化函数去掉 `T`。

**Tech Stack:** Vue 3 + TypeScript + Vite；Spring Boot 2.7 + JdbcTemplate；MySQL 8 初始化 SQL；Lucide 图标。

---

### Task 1: 家庭偏好与成员密码权限后端地基

**Files:**
- Create: `backend/src/main/java/com/homeofus/family/dto/UpdateHomeCardOrderRequest.java`
- Create: `backend/src/test/java/com/homeofus/family/service/FamilyServiceTest.java`
- Modify: `backend/src/main/resources/db/migration/V1__init_schema.sql`
- Modify: `backend/src/main/java/com/homeofus/family/controller/FamilyController.java`
- Modify: `backend/src/main/java/com/homeofus/family/service/FamilyService.java`
- Modify: `backend/src/main/java/com/homeofus/family/repository/FamilyRepository.java`

**Step 1: 写失败用例，锁定两条规则**

```java
@Test
void shouldIgnorePasswordWhenUpdatingAnotherMember() {
    // 当前登录人为 1001，目标成员为 1002，传入 password 也不允许改掉对方密码
}

@Test
void shouldReturnHomeCardOrderInFamilyOverview() {
    // 家庭概览需要带出当前登录成员自己的 homeCardOrder
}
```

**Step 2: 运行用例，确认当前实现不满足**

Run:

```bash
cd /Users/tch/develop/home-of-us/backend
mvn -Dtest=FamilyServiceTest -DfailIfNoTests=false test
```

Expected:
- `FamilyServiceTest` 至少有 2 个失败点。
- 当前 `getDefaultFamilyOverview()` 不返回个人排序。
- 当前 `updateMember()` 会把他人密码一起带入 `authRepository.updateUserByMemberId(...)`。

**Step 3: 最小实现数据库与服务改造**

```sql
CREATE TABLE IF NOT EXISTS member_preference (
    id BIGINT PRIMARY KEY,
    family_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    preference_key VARCHAR(80) NOT NULL,
    preference_value TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    UNIQUE KEY uk_member_preference_member_key (member_id, preference_key),
    INDEX idx_member_preference_family_member (family_id, member_id)
);
```

实现要点：
- `V1__init_schema.sql` 继续采用幂等 SQL，不新建 Flyway 文件。
- `FamilyRepository` 新增当前成员偏好查询、首页卡片顺序保存方法。
- `FamilyService.getDefaultFamilyOverview()` 带出 `preferences.homeCardOrder`。
- `FamilyService.updateMember()` 中，只有 `currentUser.getMemberId().equals(id)` 时才允许使用请求中的 `password`；否则强制按空串处理，不修改对方密码。
- Java 判空严格使用 `Objects.isNull()` / `Objects.nonNull()` 和 `StringUtils`。

**Step 4: 补控制器接口**

新增接口：

```java
@PatchMapping("/default/preferences/home-card-order")
public ApiResponse<Map<String, Object>> updateHomeCardOrder(...)
```

返回结构保持轻量：

```json
{
  "updated": 1,
  "homeCardOrder": ["record", "plants", "care"]
}
```

**Step 5: 回归用例**

Run:

```bash
cd /Users/tch/develop/home-of-us/backend
mvn -Dtest=FamilyServiceTest -DfailIfNoTests=false test
```

Expected:
- `BUILD SUCCESS`
- 用例覆盖“他人密码不可改”和“概览返回排序偏好”。

### Task 2: 补齐宠物编辑后端接口

**Files:**
- Create: `backend/src/main/java/com/homeofus/pet/dto/UpdatePetRequest.java`
- Create: `backend/src/test/java/com/homeofus/pet/service/PetServiceTest.java`
- Modify: `backend/src/main/java/com/homeofus/pet/controller/PetController.java`
- Modify: `backend/src/main/java/com/homeofus/pet/service/PetService.java`
- Modify: `backend/src/main/java/com/homeofus/pet/repository/PetRepository.java`

**Step 1: 写失败用例**

```java
@Test
void shouldUpdatePetProfile() {
    // 名字、品种、性别、生日、头像、备注更新后，返回 updated=1
}
```

**Step 2: 运行用例，确认当前缺接口**

Run:

```bash
cd /Users/tch/develop/home-of-us/backend
mvn -Dtest=PetServiceTest -DfailIfNoTests=false test
```

Expected:
- 当前没有 `updatePet(...)`，测试失败。

**Step 3: 最小实现**

接口形态：

```java
@PatchMapping("/{petId}")
public ApiResponse<Map<String, Object>> updatePet(@PathVariable Long petId,
        @RequestBody UpdatePetRequest request)
```

实现要点：
- `PetRepository` 新增 `updatePet(...)`。
- `PetService` 新增 `updatePet(...)`，复用现有 `parseDate(...)`。
- 必填字段只强约束 `name` 非空；可选字段允许保存最新值。
- 不改动照片/医疗记录表结构。

**Step 4: 重新运行宠物服务测试**

Run:

```bash
cd /Users/tch/develop/home-of-us/backend
mvn -Dtest=PetServiceTest -DfailIfNoTests=false test
```

Expected:
- `BUILD SUCCESS`

### Task 3: 扩展共享 API 合同

**Files:**
- Modify: `apps/shared/src/api.ts`

**Step 1: 先补类型声明**

新增类型：

```ts
export type UpdateHomeCardOrderPayload = {
  cardKeys: string[];
};

export type UpdatePetPayload = {
  name: string;
  species?: string;
  breed?: string;
  gender?: string;
  birthday?: string;
  avatarUrl?: string;
  note?: string;
};
```

**Step 2: 接上新接口**

新增 API：

```ts
updateHomeCardOrder: (payload: UpdateHomeCardOrderPayload) => ...
updatePet: (petId: number, payload: UpdatePetPayload) => ...
```

**Step 3: 编译共享前端依赖链**

Run:

```bash
cd /Users/tch/develop/home-of-us
npm run build --workspace apps/mobile
```

Expected:
- 若只改了 `api.ts` 但 `App.vue` 还没跟上，构建可能报类型未使用或调用缺失；先记录，再继续下一任务。

### Task 4: 首页改为唯一入口，并支持个人卡片排序

**Files:**
- Modify: `apps/mobile/src/App.vue`
- Modify: `apps/mobile/src/styles.css`

**Step 1: 先整理页面状态与卡片元数据**

新增前端状态：

```ts
type HomeCardKey =
  | "record"
  | "plants"
  | "care"
  | "shopping"
  | "finance"
  | "reminders"
  | "private"
  | "members"
  | "album"
  | "pets"
  | "profile";
```

实现要点：
- `activeTab === "today"` 作为唯一首页。
- 删除“常用入口”区块和底部导航。
- 首页所有模块统一渲染为卡片。
- 非首页统一显示返回按钮，返回首页。

**Step 2: 接入后端排序偏好**

行为要求：
- 初次进入按默认顺序展示。
- 若 `family.preferences.homeCardOrder` 存在，则按保存顺序渲染。
- 拖拽完成后调用 `api.updateHomeCardOrder(...)` 持久化。

**Step 3: 实现触摸/鼠标共用拖拽排序**

优先方案：
- 使用 Pointer Events 做轻量拖拽，不引第三方库。
- 卡片保持稳定尺寸，拖动时不允许文本和图标把布局撑乱。
- 排序仅影响当前登录成员，不影响别人。

**Step 4: 更新样式**

样式目标：
- 首页卡片沿用当前大卡片语义，但统一成模块入口，不再分“上面 4 个卡片”和“常用入口”两层。
- 去掉 `bottom-nav` 占位后，`mobile-shell` 底部 padding 回收。
- 新增拖拽态、按下态、返回头部态。

**Step 5: 前端构建验证**

Run:

```bash
cd /Users/tch/develop/home-of-us
npm run build --workspace apps/mobile
```

Expected:
- `vue-tsc` 通过
- `vite build` 通过

### Task 5: 提醒与个人页改成独立列表/新增/编辑流

**Files:**
- Modify: `apps/mobile/src/App.vue`
- Modify: `apps/mobile/src/styles.css`

**Step 1: 统一时间格式化函数**

新增最小工具函数：

```ts
function formatDateTime(value: string): string {
  return value.replace("T", " ").slice(0, 16);
}
```

后续把 `due_at`、`next_due_at`、`created_at`、`purchased_at` 等当前可见时间统一接到该函数上，先消掉界面里的 `T`。

**Step 2: 提醒页改为默认列表页**

行为要求：
- 进入提醒模块先看列表。
- 右上角 `+` 进入“新增提醒页”。
- 新增成功后返回提醒列表。
- 提醒完成/删除后列表立即刷新，不在顶部长期悬挂旧提示。

**Step 3: 个人页改为详情页优先**

行为要求：
- 默认先展示个人详情，不直接进入编辑表单。
- 详情页展示昵称、角色、登录名、头像、家庭信息。
- 密码详情固定显示为掩码；由于系统只保存密码摘要，不回显历史明文密码。
- 进入编辑页后，对“本次新输入密码”提供眼睛开关。

**Step 4: 成员编辑页隐藏他人密码输入**

规则：
- 编辑别人资料时，不显示密码输入框。
- 编辑自己资料时，密码输入框可见。
- 即便前端被绕过，后端 Task 1 的限制仍然兜底。

**Step 5: 构建验证**

Run:

```bash
cd /Users/tch/develop/home-of-us
npm run build --workspace apps/mobile
```

Expected:
- `BUILD SUCCESS`

### Task 6: 宠物页改成列表 -> 详情 -> 子列表 -> 新增/编辑

**Files:**
- Modify: `apps/mobile/src/App.vue`
- Modify: `apps/mobile/src/styles.css`

**Step 1: 重做宠物模块页面状态**

建议状态：

```ts
type PetView =
  | "list"
  | "detail"
  | "create"
  | "edit"
  | "photos"
  | "photoCreate"
  | "medical"
  | "medicalCreate";
```

**Step 2: 宠物列表与详情分离**

行为要求：
- 进入宠物模块先展示宠物列表。
- 点某只宠物后进入该宠物详情页。
- 详情页只展示当前宠物，不再在中间重复渲染第二层宠物列表。
- 详情页提供“编辑档案”“照片”“医疗记录”“删除”操作。

**Step 3: 宠物编辑页独立出来**

行为要求：
- 宠物编辑页复用新增表单字段，但走 `api.updatePet(...)`。
- 保存成功后回到该宠物详情页并刷新列表。

**Step 4: 照片和医疗记录也拆成列表页/新增页**

行为要求：
- 照片页默认先看该宠物照片列表，右上角 `+` 进入新增照片页。
- 医疗页默认先看记录列表，右上角 `+` 进入新增医疗记录页。
- `next_due_at`、照片时间、记录时间统一走格式化函数。

**Step 5: 构建验证**

Run:

```bash
cd /Users/tch/develop/home-of-us
npm run build --workspace apps/mobile
```

Expected:
- `BUILD SUCCESS`

### Task 7: 最终回归验证

**Files:**
- Modify: `apps/mobile/src/App.vue`
- Modify: `apps/mobile/src/styles.css`
- Modify: `apps/shared/src/api.ts`
- Modify: `backend/src/main/java/com/homeofus/family/*`
- Modify: `backend/src/main/java/com/homeofus/pet/*`
- Modify: `backend/src/main/resources/db/migration/V1__init_schema.sql`

**Step 1: 后端最小测试集**

Run:

```bash
cd /Users/tch/develop/home-of-us/backend
mvn -Dtest=FamilyServiceTest,PetServiceTest -DfailIfNoTests=false test
```

Expected:
- `BUILD SUCCESS`

**Step 2: 前端生产构建**

Run:

```bash
cd /Users/tch/develop/home-of-us
npm run build --workspace apps/mobile
```

Expected:
- `vue-tsc` 通过
- `vite build` 通过

**Step 3: 手工回归清单**

1. 小谭编辑丹丹资料时看不到密码框，且无法改丹丹密码。
2. 首页不再有底部导航和“常用入口”，所有模块都在卡片区。
3. 卡片拖动排序后刷新仍保持当前账号自己的顺序。
4. 进入提醒先看列表，点 `+` 才进入新建页。
5. 提醒时间、宠物医疗时间不再出现 `T`。
6. 进入宠物先看列表，点宠物后只展示当前宠物详情。
7. 宠物档案可以编辑，保存后立即回显。
8. 个人页默认先看详情，编辑页才出现表单。

## Remember

- 不做与本轮无关的 `6 / 8 / 9 / 10` 增强需求。
- 不引入 Vue Router，不做跨模块大重构。
- `V1__init_schema.sql` 继续追加幂等 SQL，不拆新迁移文件。
- Java 代码禁止直接 `== null`，对象判空统一用 `Objects`，字符串判空统一用 `StringUtils`。
- 不做任何 Git 操作。

## Execution Handoff

计划已保存到 `docs/plans/2026-07-09-mobile-first-batch-pages.md`。本线程已经完成需求对齐，下一步直接按当前会话继续执行本计划，不另开实现分支。
