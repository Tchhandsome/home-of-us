<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { api, type AnyRow, type TodaySummary } from "@home-of-us/shared";
import {
  Archive,
  Bell,
  Camera,
  ClipboardList,
  Flower2,
  Home,
  LayoutDashboard,
  LoaderCircle,
  MessageSquare,
  PawPrint,
  Plus,
  ReceiptText,
  Search,
  ShoppingCart,
  Sparkles,
  Tags,
  Users,
  WalletCards
} from "lucide-vue-next";

type SectionKey =
  | "dashboard"
  | "records"
  | "plants"
  | "reminders"
  | "shopping"
  | "chores"
  | "inventory"
  | "finance"
  | "attachments"
  | "members"
  | "privateMessages"
  | "album"
  | "pets";

const activeSection = ref<SectionKey>("dashboard");
const loading = ref(false);
const message = ref("");
const today = ref<TodaySummary>({
  pendingReminders: 0,
  plantCount: 0,
  shoppingTodoCount: 0,
  choreTodoCount: 0,
  inventoryLowCount: 0,
  monthExpense: 0
});
const family = ref<AnyRow>({});
const records = ref<AnyRow[]>([]);
const plants = ref<AnyRow[]>([]);
const reminders = ref<AnyRow[]>([]);
const shoppingItems = ref<AnyRow[]>([]);
const choreTasks = ref<AnyRow[]>([]);
const inventoryItems = ref<AnyRow[]>([]);
const financeRecords = ref<AnyRow[]>([]);
const attachments = ref<AnyRow[]>([]);
const privateMessages = ref<AnyRow[]>([]);
const albumPhotos = ref<AnyRow[]>([]);
const pets = ref<AnyRow[]>([]);
const plantDraft = ref({
  name: "",
  variety: "",
  flowerColor: "",
  location: ""
});
const recordDraft = ref("");

const sections = [
  { key: "dashboard", label: "总览", icon: LayoutDashboard },
  { key: "records", label: "快速记录", icon: Sparkles },
  { key: "plants", label: "花卉", icon: Flower2 },
  { key: "reminders", label: "提醒", icon: Bell },
  { key: "shopping", label: "购物", icon: ShoppingCart },
  { key: "chores", label: "家务", icon: ClipboardList },
  { key: "inventory", label: "库存", icon: Archive },
  { key: "finance", label: "账本", icon: WalletCards },
  { key: "attachments", label: "资料库", icon: ReceiptText },
  { key: "members", label: "成员", icon: Users },
  { key: "privateMessages", label: "私密留言", icon: MessageSquare },
  { key: "album", label: "共同相册", icon: Camera },
  { key: "pets", label: "宠物", icon: PawPrint }
] as const;

const familyName = computed(() => {
  const familyRow = family.value.family as AnyRow | undefined;
  return text(familyRow ?? {}, "name") || "我们的小家";
});
const memberRows = computed<AnyRow[]>(() => {
  const members = family.value.members;
  return Array.isArray(members) ? (members as AnyRow[]) : [];
});

function text(row: AnyRow, key: string): string {
  const value = row[key];
  if (value === undefined || value === null) {
    return "";
  }
  return String(value);
}

function sectionRows(section: SectionKey): AnyRow[] {
  const mapping: Record<SectionKey, AnyRow[]> = {
    dashboard: [],
    records: records.value,
    plants: plants.value,
    reminders: reminders.value,
    shopping: shoppingItems.value,
    chores: choreTasks.value,
    inventory: inventoryItems.value,
    finance: financeRecords.value,
    attachments: attachments.value,
    members: memberRows.value,
    privateMessages: privateMessages.value,
    album: albumPhotos.value,
    pets: pets.value
  };
  return mapping[section];
}

async function loadAll() {
  loading.value = true;
  try {
    const [
      todayData,
      familyData,
      recordData,
      plantData,
      reminderData,
      shoppingData,
      choreData,
      inventoryData,
      financeData,
      attachmentData,
      privateData,
      albumData,
      petData
    ] = await Promise.all([
      api.today(),
      api.family(),
      api.records(),
      api.plants(),
      api.reminders(),
      api.shoppingItems(),
      api.choreTasks(),
      api.inventoryItems(),
      api.financeRecords(),
      api.attachments(),
      api.privateMessages(),
      api.albumPhotos(),
      api.pets()
    ]);
    today.value = todayData;
    family.value = familyData;
    records.value = recordData;
    plants.value = plantData;
    reminders.value = reminderData;
    shoppingItems.value = shoppingData;
    choreTasks.value = choreData;
    inventoryItems.value = inventoryData;
    financeRecords.value = financeData;
    attachments.value = attachmentData;
    privateMessages.value = privateData;
    albumPhotos.value = albumData;
    pets.value = petData;
  } catch (error) {
    message.value = error instanceof Error ? error.message : "加载失败";
  } finally {
    loading.value = false;
  }
}

async function submitRecord() {
  if (!recordDraft.value.trim()) {
    return;
  }
  await api.createRecord({
    rawText: recordDraft.value,
    recordType: "GENERAL"
  });
  recordDraft.value = "";
  message.value = "记录已保存";
  await loadAll();
}

async function submitPlant() {
  if (!plantDraft.value.name.trim()) {
    return;
  }
  await api.createPlant({
    name: plantDraft.value.name,
    variety: plantDraft.value.variety,
    flowerColor: plantDraft.value.flowerColor,
    location: plantDraft.value.location
  });
  plantDraft.value = {
    name: "",
    variety: "",
    flowerColor: "",
    location: ""
  };
  message.value = "花卉档案已创建";
  await loadAll();
}

onMounted(loadAll);
</script>

<template>
  <main class="admin-shell">
    <aside class="sidebar">
      <div class="brand">
        <Home :size="24" />
        <div>
          <strong>Home Of Us</strong>
          <span>{{ familyName }}</span>
        </div>
      </div>
      <nav>
        <button
          v-for="section in sections"
          :key="section.key"
          type="button"
          :class="{ active: activeSection === section.key }"
          :title="section.label"
          @click="activeSection = section.key"
        >
          <component :is="section.icon" :size="18" />
          <span>{{ section.label }}</span>
        </button>
      </nav>
    </aside>

    <section class="workspace">
      <header class="workspace-header">
        <div>
          <p class="eyebrow">Control Desk</p>
          <h1>{{ sections.find((item) => item.key === activeSection)?.label }}</h1>
        </div>
        <div class="toolbar">
          <label class="search">
            <Search :size="17" />
            <input placeholder="搜索模块数据" />
          </label>
          <button class="icon-button" type="button" aria-label="刷新" title="刷新" @click="loadAll">
            <LoaderCircle :class="{ spin: loading }" :size="18" />
          </button>
        </div>
      </header>

      <p v-if="message" class="notice">{{ message }}</p>

      <section v-if="activeSection === 'dashboard'" class="dashboard-view">
        <div class="metrics">
          <article class="metric moss">
            <Bell :size="19" />
            <span>待提醒</span>
            <strong>{{ today.pendingReminders }}</strong>
          </article>
          <article class="metric clay">
            <Flower2 :size="19" />
            <span>花卉档案</span>
            <strong>{{ today.plantCount }}</strong>
          </article>
          <article class="metric ink">
            <ShoppingCart :size="19" />
            <span>待买事项</span>
            <strong>{{ today.shoppingTodoCount }}</strong>
          </article>
          <article class="metric brass">
            <WalletCards :size="19" />
            <span>本月支出</span>
            <strong>{{ today.monthExpense }}</strong>
          </article>
        </div>

        <div class="panel-grid">
          <article class="panel">
            <div class="panel-title">
              <h2>快速记录</h2>
              <Sparkles :size="18" />
            </div>
            <textarea v-model="recordDraft" rows="5" placeholder="记录一条生活片段" />
            <button class="primary-button" type="button" @click="submitRecord">
              <Plus :size="17" />
              <span>新增记录</span>
            </button>
          </article>

          <article class="panel">
            <div class="panel-title">
              <h2>新增花卉</h2>
              <Flower2 :size="18" />
            </div>
            <div class="form-grid">
              <input v-model="plantDraft.name" placeholder="名称" />
              <input v-model="plantDraft.variety" placeholder="品种" />
              <input v-model="plantDraft.flowerColor" placeholder="花色" />
              <input v-model="plantDraft.location" placeholder="位置" />
            </div>
            <button class="secondary-button" type="button" @click="submitPlant">
              <Plus :size="17" />
              <span>创建档案</span>
            </button>
          </article>
        </div>

        <article class="table-panel">
          <div class="panel-title">
            <h2>最近记录</h2>
            <Tags :size="18" />
          </div>
          <table>
            <thead>
              <tr>
                <th>类型</th>
                <th>内容</th>
                <th>时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="record in records.slice(0, 6)" :key="text(record, 'id')">
                <td>{{ text(record, "record_type") }}</td>
                <td>{{ text(record, "raw_text") }}</td>
                <td>{{ text(record, "created_at") }}</td>
              </tr>
            </tbody>
          </table>
        </article>
      </section>

      <section v-else class="module-view">
        <article class="table-panel">
          <div class="panel-title">
            <h2>{{ sections.find((item) => item.key === activeSection)?.label }}数据</h2>
            <Tags :size="18" />
          </div>
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>主信息</th>
                <th>状态/类型</th>
                <th>时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in sectionRows(activeSection).slice(0, 20)" :key="text(row, 'id')">
                <td>{{ text(row, "id") }}</td>
                <td>
                  {{
                    text(row, "name") ||
                    text(row, "title") ||
                    text(row, "raw_text") ||
                    text(row, "file_name") ||
                    text(row, "display_name") ||
                    text(row, "content") ||
                    text(row, "image_url")
                  }}
                </td>
                <td>
                  {{
                    text(row, "status") ||
                    text(row, "record_type") ||
                    text(row, "care_type") ||
                    text(row, "direction") ||
                    text(row, "role_code") ||
                    text(row, "visibility")
                  }}
                </td>
                <td>
                  {{ text(row, "created_at") || text(row, "due_at") || text(row, "occurred_on") || text(row, "taken_on") }}
                </td>
              </tr>
            </tbody>
          </table>
          <p v-if="sectionRows(activeSection).length === 0" class="empty">暂无数据</p>
        </article>
      </section>
    </section>
  </main>
</template>
