<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { api, clearAuthToken, setAuthToken, type AnyRow, type TodaySummary } from "@home-of-us/shared";
import {
  ArrowLeft,
  Bell,
  Camera,
  Check,
  ClipboardList,
  Home,
  ImagePlus,
  Leaf,
  Lock,
  LogOut,
  LoaderCircle,
  MessageSquare,
  PawPrint,
  Pencil,
  Plus,
  Send,
  Stethoscope,
  Sprout,
  User,
  UserPlus,
  Users,
  WalletCards
} from "lucide-vue-next";

type TabKey =
  | "today"
  | "record"
  | "plants"
  | "care"
  | "shopping"
  | "finance"
  | "reminders"
  | "private"
  | "members"
  | "memberAdd"
  | "memberEdit"
  | "profile"
  | "album"
  | "pets";

const activeTab = ref<TabKey>("today");
const loading = ref(false);
const isAuthenticated = ref(false);
const message = ref("");
const currentUser = ref<AnyRow>({});
const family = ref<AnyRow>({});
const today = ref<TodaySummary>({
  pendingReminders: 0,
  plantCount: 0,
  shoppingTodoCount: 0,
  choreTodoCount: 0,
  inventoryLowCount: 0,
  monthExpense: 0
});
const records = ref<AnyRow[]>([]);
const reminders = ref<AnyRow[]>([]);
const plants = ref<AnyRow[]>([]);
const shoppingItems = ref<AnyRow[]>([]);
const financeRecords = ref<AnyRow[]>([]);
const financeCategories = ref<AnyRow[]>([]);
const careRecords = ref<AnyRow[]>([]);
const privateMessages = ref<AnyRow[]>([]);
const albumPhotos = ref<AnyRow[]>([]);
const pets = ref<AnyRow[]>([]);
const petPhotos = ref<AnyRow[]>([]);
const petMedicalRecords = ref<AnyRow[]>([]);
const selectedPetId = ref("");
const loginDraft = ref({
  username: "小谭",
  password: "123456"
});
const quickText = ref("");
const newPlant = ref({
  name: "",
  flowerColor: "",
  location: "",
  carePreference: ""
});
const careDraft = ref({
  plantId: "",
  careType: "WATER",
  detail: "",
  nextCareAt: ""
});
const shoppingDraft = ref({
  name: "",
  quantity: "",
  category: "DAILY"
});
const purchaseDrafts = ref<Record<string, { amount: string; category: string; buyerId: string }>>({});
const financeDraft = ref({
  title: "",
  amount: "",
  category: "MEAL",
  occurredOn: ""
});
const reminderDraft = ref({
  title: "",
  dueAt: "",
  description: ""
});
const memberDraft = ref({
  displayName: "",
  username: "",
  password: "",
  avatarColor: "#7d8f68",
  avatarUrl: "",
  bio: ""
});
const memberAvatarFile = ref<File | null>(null);
const memberEditAvatarFile = ref<File | null>(null);
const memberEditDraft = ref({
  id: "",
  displayName: "",
  username: "",
  password: "",
  avatarColor: "#7d8f68",
  avatarUrl: "",
  bio: ""
});
const profileDraft = ref({
  displayName: "",
  username: "",
  password: "",
  avatarColor: "#2F6B4F",
  avatarUrl: "",
  bio: ""
});
const profileAvatarFile = ref<File | null>(null);
const privateDraft = ref({
  content: "",
  visibility: "TO_PARTNER",
  receiverMemberId: ""
});
const albumDraft = ref({
  title: "",
  imageUrl: "",
  description: "",
  takenOn: ""
});
const albumFile = ref<File | null>(null);
const petDraft = ref({
  name: "",
  species: "CAT",
  breed: "",
  gender: "",
  birthday: "",
  avatarUrl: "",
  note: ""
});
const petAvatarFile = ref<File | null>(null);
const petPhotoDraft = ref({
  imageUrl: "",
  description: "",
  takenOn: ""
});
const petPhotoFile = ref<File | null>(null);
const petMedicalDraft = ref({
  recordType: "DEWORMING",
  recordDate: "",
  hospital: "",
  medicine: "",
  description: "",
  nextDueAt: ""
});

const pendingReminders = computed(() => reminders.value.filter((item) => text(item, "status") === "PENDING"));
const todoShoppingItems = computed(() => shoppingItems.value.filter((item) => text(item, "status") === "TODO"));
const doneShoppingItems = computed(() => shoppingItems.value.filter((item) => text(item, "status") === "DONE"));
const expenseRecords = computed(() => financeRecords.value.filter((item) => text(item, "direction") === "EXPENSE"));
const familyMembers = computed<AnyRow[]>(() => {
  const members = family.value.members;
  return Array.isArray(members) ? (members as AnyRow[]) : [];
});
const otherMembers = computed(() =>
  familyMembers.value.filter((member) => text(member, "id") !== String(currentUser.value.memberId ?? ""))
);
const currentMember = computed(() =>
  familyMembers.value.find((member) => text(member, "id") === String(currentUser.value.memberId ?? ""))
);
const familyTitle = computed(() => {
  const familyRow = family.value.family;
  return text((familyRow as AnyRow) ?? {}, "name") || "我们的小家";
});

function text(row: AnyRow, key: string): string {
  const value = row[key];
  if (value === undefined || value === null) {
    return "";
  }
  return String(value);
}

function numberValue(row: AnyRow, key: string): number {
  const value = Number(row[key]);
  return Number.isFinite(value) ? value : 0;
}

function categoryName(code: string): string {
  const category = financeCategories.value.find((item) => text(item, "code") === code);
  return category ? text(category, "name") : code;
}

function memberName(memberId: string): string {
  const member = familyMembers.value.find((item) => text(item, "id") === memberId);
  return member ? text(member, "display_name") : "家庭成员";
}

function syncPurchaseDrafts(items: AnyRow[]) {
  const nextDrafts = { ...purchaseDrafts.value };
  items.forEach((item) => {
    const id = text(item, "id");
    if (!nextDrafts[id]) {
      nextDrafts[id] = {
        amount: "",
        category: text(item, "category") || "DAILY",
        buyerId: String(currentUser.value.memberId ?? "")
      };
    }
  });
  purchaseDrafts.value = nextDrafts;
}

function syncProfileDraft() {
  profileDraft.value = {
    displayName: text(currentUser.value, "displayName"),
    username: text(currentUser.value, "username"),
    password: "",
    avatarColor: text(currentUser.value, "avatarColor") || "#2F6B4F",
    avatarUrl: text(currentUser.value, "avatarUrl"),
    bio: text(currentUser.value, "bio")
  };
}

function selectedFile(event: Event): File | null {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  return file ?? null;
}

async function uploadSelectedImage(file: File | null, linkedType: string, linkedId?: number): Promise<string> {
  if (!file) {
    return "";
  }
  const uploaded = await api.uploadImage(file, linkedType, linkedId);
  return text(uploaded, "url");
}

async function bootstrap() {
  const token = typeof localStorage === "undefined" ? "" : localStorage.getItem("homeOfUsToken");
  if (!token) {
    return;
  }
  isAuthenticated.value = true;
  try {
    currentUser.value = await api.me();
    syncProfileDraft();
    await loadAll();
  } catch (error) {
    clearAuthToken();
    isAuthenticated.value = false;
    message.value = error instanceof Error ? error.message : "登录已失效";
  }
}

async function submitLogin() {
  if (!loginDraft.value.username.trim() || !loginDraft.value.password.trim()) {
    return;
  }
  loading.value = true;
  try {
    const result = await api.login(loginDraft.value);
    setAuthToken(text(result, "token"));
    currentUser.value = (result.user as AnyRow) ?? {};
    isAuthenticated.value = true;
    message.value = "登录成功";
    await loadAll();
  } catch (error) {
    message.value = error instanceof Error ? error.message : "登录失败";
  } finally {
    loading.value = false;
  }
}

function logout() {
  clearAuthToken();
  isAuthenticated.value = false;
  currentUser.value = {};
  family.value = {};
  message.value = "";
}

async function loadAll() {
  loading.value = true;
  try {
    const [
      meData,
      todayData,
      familyData,
      recordData,
      reminderData,
      plantData,
      shoppingData,
      financeData,
      categoryData,
      privateData,
      albumData,
      petData
    ] = await Promise.all([
      api.me(),
      api.today(),
      api.family(),
      api.records(),
      api.reminders(),
      api.plants(),
      api.shoppingItems(),
      api.financeRecords(),
      api.financeCategories(),
      api.privateMessages(),
      api.albumPhotos(),
      api.pets()
    ]);
    currentUser.value = meData;
    today.value = todayData;
    family.value = familyData;
    records.value = recordData;
    reminders.value = reminderData;
    plants.value = plantData;
    shoppingItems.value = shoppingData;
    financeRecords.value = financeData;
    financeCategories.value = categoryData;
    privateMessages.value = privateData;
    albumPhotos.value = albumData;
    pets.value = petData;
    syncProfileDraft();
    syncPurchaseDrafts(shoppingData);
    if (!selectedPetId.value && petData.length > 0) {
      selectedPetId.value = text(petData[0], "id");
    }
    if (!careDraft.value.plantId && plantData.length > 0) {
      careDraft.value.plantId = text(plantData[0], "id");
    }
    if (!privateDraft.value.receiverMemberId && otherMembers.value.length > 0) {
      privateDraft.value.receiverMemberId = text(otherMembers.value[0], "id");
    }
    await loadPetDetails();
    await loadCareRecords();
  } catch (error) {
    message.value = error instanceof Error ? error.message : "加载失败";
  } finally {
    loading.value = false;
  }
}

async function loadCareRecords() {
  const plantId = Number(careDraft.value.plantId);
  if (!Number.isFinite(plantId) || plantId <= 0) {
    careRecords.value = [];
    return;
  }
  careRecords.value = await api.careRecords(plantId);
}

async function submitQuickRecord() {
  if (!quickText.value.trim()) {
    return;
  }
  await api.createRecord({
    rawText: quickText.value,
    recordType: "GENERAL"
  });
  quickText.value = "";
  message.value = "已记录";
  await loadAll();
}

async function submitPlant() {
  if (!newPlant.value.name.trim()) {
    return;
  }
  await api.createPlant({
    name: newPlant.value.name,
    flowerColor: newPlant.value.flowerColor,
    location: newPlant.value.location,
    carePreference: newPlant.value.carePreference
  });
  newPlant.value = {
    name: "",
    flowerColor: "",
    location: "",
    carePreference: ""
  };
  message.value = "花卉档案已创建";
  await loadAll();
}

async function submitCareRecord() {
  const plantId = Number(careDraft.value.plantId || plants.value[0]?.id);
  if (!Number.isFinite(plantId) || !careDraft.value.detail.trim()) {
    return;
  }
  await api.createCareRecord(plantId, {
    careType: careDraft.value.careType,
    detail: careDraft.value.detail,
    rawText: careDraft.value.detail,
    nextCareAt: careDraft.value.nextCareAt
  });
  careDraft.value.detail = "";
  careDraft.value.nextCareAt = "";
  message.value = "养护记录已保存";
  await loadAll();
}

async function submitShoppingItem() {
  if (!shoppingDraft.value.name.trim()) {
    return;
  }
  await api.createShoppingItem({
    name: shoppingDraft.value.name,
    quantity: shoppingDraft.value.quantity,
    category: shoppingDraft.value.category
  });
  shoppingDraft.value = {
    name: "",
    quantity: "",
    category: "DAILY"
  };
  message.value = "已加入购物清单";
  await loadAll();
}

async function submitFinanceRecord() {
  const amount = Number(financeDraft.value.amount);
  if (!financeDraft.value.title.trim() || !Number.isFinite(amount) || amount <= 0) {
    return;
  }
  await api.createFinanceRecord({
    title: financeDraft.value.title,
    amount,
    direction: "EXPENSE",
    category: financeDraft.value.category,
    occurredOn: financeDraft.value.occurredOn
  });
  financeDraft.value = {
    title: "",
    amount: "",
    category: "MEAL",
    occurredOn: ""
  };
  message.value = "支出已记录";
  await loadAll();
}

async function submitReminder() {
  if (!reminderDraft.value.title.trim() || !reminderDraft.value.dueAt) {
    return;
  }
  await api.createReminder({
    title: reminderDraft.value.title,
    description: reminderDraft.value.description,
    sourceType: "MANUAL",
    dueAt: reminderDraft.value.dueAt
  });
  reminderDraft.value = {
    title: "",
    dueAt: "",
    description: ""
  };
  message.value = "提醒已创建";
  await loadAll();
}

async function completeReminder(id: number) {
  await api.completeReminder(id);
  message.value = "提醒已完成";
  await loadAll();
}

async function checkShoppingItem(item: AnyRow) {
  const id = text(item, "id");
  const draft = purchaseDrafts.value[id];
  const amount = Number(draft?.amount);
  if (!Number.isFinite(amount) || amount <= 0) {
    message.value = "请先填写实际金额";
    return;
  }
  await api.checkShoppingItem(Number(id), {
    actualAmount: amount,
    category: draft.category,
    buyerId: Number(draft.buyerId || currentUser.value.memberId)
  });
  message.value = "已购买，并自动记入账本";
  purchaseDrafts.value[id].amount = "";
  await loadAll();
}

async function submitMember() {
  if (!memberDraft.value.displayName.trim()) {
    return;
  }
  const avatarUrl = await uploadSelectedImage(memberAvatarFile.value, "MEMBER");
  await api.createFamilyMember({
    displayName: memberDraft.value.displayName,
    roleCode: "PARTNER",
    avatarColor: memberDraft.value.avatarColor,
    avatarUrl: avatarUrl || memberDraft.value.avatarUrl,
    bio: memberDraft.value.bio,
    username: memberDraft.value.username,
    password: memberDraft.value.password
  });
  memberDraft.value = {
    displayName: "",
    username: "",
    password: "",
    avatarColor: "#7d8f68",
    avatarUrl: "",
    bio: ""
  };
  memberAvatarFile.value = null;
  message.value = "家庭成员已添加";
  activeTab.value = "members";
  await loadAll();
}

function startEditMember(member: AnyRow) {
  memberEditDraft.value = {
    id: text(member, "id"),
    displayName: text(member, "display_name"),
    username: text(member, "username"),
    password: "",
    avatarColor: text(member, "avatar_color") || "#7d8f68",
    avatarUrl: text(member, "avatar_url"),
    bio: text(member, "bio")
  };
  memberEditAvatarFile.value = null;
  activeTab.value = "memberEdit";
}

async function submitMemberEdit() {
  const memberId = Number(memberEditDraft.value.id);
  if (!Number.isFinite(memberId) || memberId <= 0 || !memberEditDraft.value.displayName.trim()) {
    return;
  }
  const avatarUrl = await uploadSelectedImage(memberEditAvatarFile.value, "MEMBER", memberId);
  await api.updateFamilyMember(memberId, {
    displayName: memberEditDraft.value.displayName,
    username: memberEditDraft.value.username,
    password: memberEditDraft.value.password,
    avatarColor: memberEditDraft.value.avatarColor,
    avatarUrl: avatarUrl || memberEditDraft.value.avatarUrl,
    bio: memberEditDraft.value.bio
  });
  memberEditAvatarFile.value = null;
  message.value = "成员信息已更新";
  activeTab.value = "members";
  await loadAll();
}

async function submitProfile() {
  const memberId = Number(currentUser.value.memberId);
  if (!Number.isFinite(memberId) || memberId <= 0) {
    return;
  }
  const avatarUrl = await uploadSelectedImage(profileAvatarFile.value, "MEMBER", memberId);
  await api.updateFamilyMember(memberId, {
    displayName: profileDraft.value.displayName,
    username: profileDraft.value.username,
    password: profileDraft.value.password,
    avatarColor: profileDraft.value.avatarColor,
    avatarUrl: avatarUrl || profileDraft.value.avatarUrl,
    bio: profileDraft.value.bio
  });
  profileAvatarFile.value = null;
  message.value = "个人信息已更新";
  await loadAll();
}

async function submitPrivateMessage() {
  if (!privateDraft.value.content.trim()) {
    return;
  }
  await api.createPrivateMessage({
    content: privateDraft.value.content,
    visibility: privateDraft.value.visibility,
    receiverMemberId:
      privateDraft.value.visibility === "TO_PARTNER" ? Number(privateDraft.value.receiverMemberId) : undefined
  });
  privateDraft.value.content = "";
  message.value = "留言已保存";
  await loadAll();
}

async function markPrivateMessageRead(id: number) {
  await api.readPrivateMessage(id);
  message.value = "已标记为已读";
  await loadAll();
}

async function submitAlbumPhoto() {
  if (!albumDraft.value.title.trim()) {
    return;
  }
  const imageUrl = await uploadSelectedImage(albumFile.value, "ALBUM");
  const safeImageUrl = imageUrl || albumDraft.value.imageUrl;
  if (!safeImageUrl.trim()) {
    message.value = "请选择图片或填写图片链接";
    return;
  }
  await api.createAlbumPhoto({
    title: albumDraft.value.title,
    imageUrl: safeImageUrl,
    description: albumDraft.value.description,
    takenOn: albumDraft.value.takenOn
  });
  albumDraft.value = {
    title: "",
    imageUrl: "",
    description: "",
    takenOn: ""
  };
  albumFile.value = null;
  message.value = "照片已加入相册";
  await loadAll();
}

async function submitPet() {
  if (!petDraft.value.name.trim()) {
    return;
  }
  const avatarUrl = await uploadSelectedImage(petAvatarFile.value, "PET");
  const result = await api.createPet({
    name: petDraft.value.name,
    species: petDraft.value.species,
    breed: petDraft.value.breed,
    gender: petDraft.value.gender,
    birthday: petDraft.value.birthday,
    avatarUrl: avatarUrl || petDraft.value.avatarUrl,
    note: petDraft.value.note
  });
  selectedPetId.value = text(result, "id");
  petDraft.value = {
    name: "",
    species: "CAT",
    breed: "",
    gender: "",
    birthday: "",
    avatarUrl: "",
    note: ""
  };
  petAvatarFile.value = null;
  message.value = "宠物档案已创建";
  await loadAll();
}

async function loadPetDetails() {
  const petId = Number(selectedPetId.value);
  if (!Number.isFinite(petId) || petId <= 0) {
    petPhotos.value = [];
    petMedicalRecords.value = [];
    return;
  }
  const [photoData, medicalData] = await Promise.all([
    api.petPhotos(petId),
    api.petMedicalRecords(petId)
  ]);
  petPhotos.value = photoData;
  petMedicalRecords.value = medicalData;
}

async function submitPetPhoto() {
  const petId = Number(selectedPetId.value);
  if (!Number.isFinite(petId) || petId <= 0) {
    return;
  }
  const imageUrl = await uploadSelectedImage(petPhotoFile.value, "PET", petId);
  const safeImageUrl = imageUrl || petPhotoDraft.value.imageUrl;
  if (!safeImageUrl.trim()) {
    message.value = "请选择宠物照片";
    return;
  }
  await api.createPetPhoto(petId, {
    imageUrl: safeImageUrl,
    description: petPhotoDraft.value.description,
    takenOn: petPhotoDraft.value.takenOn
  });
  petPhotoDraft.value = {
    imageUrl: "",
    description: "",
    takenOn: ""
  };
  petPhotoFile.value = null;
  message.value = "宠物照片已保存";
  await loadPetDetails();
}

async function submitPetMedicalRecord() {
  const petId = Number(selectedPetId.value);
  if (!Number.isFinite(petId) || petId <= 0 || !petMedicalDraft.value.description.trim()) {
    return;
  }
  await api.createPetMedicalRecord(petId, {
    recordType: petMedicalDraft.value.recordType,
    recordDate: petMedicalDraft.value.recordDate,
    hospital: petMedicalDraft.value.hospital,
    medicine: petMedicalDraft.value.medicine,
    description: petMedicalDraft.value.description,
    nextDueAt: petMedicalDraft.value.nextDueAt
  });
  petMedicalDraft.value = {
    recordType: "DEWORMING",
    recordDate: "",
    hospital: "",
    medicine: "",
    description: "",
    nextDueAt: ""
  };
  message.value = "宠物医疗记录已保存";
  await loadPetDetails();
}

onMounted(bootstrap);
watch(() => careDraft.value.plantId, loadCareRecords);
watch(() => selectedPetId.value, loadPetDetails);
</script>

<template>
  <main v-if="!isAuthenticated" class="mobile-shell login-shell">
    <section class="login-panel">
      <div class="login-mark">
        <Home :size="22" />
      </div>
      <p class="eyebrow">Home Of Us</p>
      <h1>回到我们的小家</h1>
      <input v-model="loginDraft.username" autocomplete="username" placeholder="登录名" />
      <input v-model="loginDraft.password" autocomplete="current-password" type="password" placeholder="密码" />
      <button class="primary-button" type="button" @click="submitLogin">
        <Lock :size="18" />
        <span>登录</span>
      </button>
      <p v-if="message" class="toast">{{ message }}</p>
    </section>
  </main>

  <main v-else class="mobile-shell">
    <header class="topbar">
      <div>
        <p class="eyebrow">Home Of Us</p>
        <h1>我们的小家</h1>
      </div>
      <div class="top-actions">
        <button class="user-chip" type="button" @click="activeTab = 'profile'">
          <User :size="16" />
          <span>{{ text(currentUser, "displayName") }}</span>
        </button>
        <button class="icon-button" type="button" aria-label="刷新" title="刷新" @click="loadAll">
          <LoaderCircle :class="{ spin: loading }" :size="20" />
        </button>
        <button class="icon-button" type="button" aria-label="退出登录" title="退出登录" @click="logout">
          <LogOut :size="19" />
        </button>
      </div>
    </header>

    <p v-if="message" class="toast">{{ message }}</p>

    <section v-if="activeTab === 'today'" class="view">
      <div class="summary-grid">
        <button class="metric metric-button tile-green" type="button" @click="activeTab = 'reminders'">
          <Bell :size="18" />
          <span>待提醒</span>
          <strong>{{ today.pendingReminders }}</strong>
        </button>
        <button class="metric metric-button tile-coral" type="button" @click="activeTab = 'plants'">
          <Sprout :size="18" />
          <span>花卉</span>
          <strong>{{ today.plantCount }}</strong>
        </button>
        <button class="metric metric-button tile-ink" type="button" @click="activeTab = 'shopping'">
          <ClipboardList :size="18" />
          <span>购物</span>
          <strong>{{ today.shoppingTodoCount }}</strong>
        </button>
        <button class="metric metric-button tile-gold" type="button" @click="activeTab = 'finance'">
          <WalletCards :size="18" />
          <span>本月支出</span>
          <strong>{{ today.monthExpense }}</strong>
        </button>
      </div>

      <article class="list-card module-jump-card">
        <div class="section-title">
          <h2>常用入口</h2>
          <Home :size="18" />
        </div>
        <button class="jump-row" type="button" @click="activeTab = 'record'">
          <Send :size="18" />
          <span>记录</span>
        </button>
        <button class="jump-row" type="button" @click="activeTab = 'care'">
          <Sprout :size="18" />
          <span>养护记录</span>
        </button>
        <button class="jump-row" type="button" @click="activeTab = 'private'">
          <MessageSquare :size="18" />
          <span>私密空间</span>
        </button>
        <button class="jump-row" type="button" @click="activeTab = 'album'">
          <Camera :size="18" />
          <span>共同相册</span>
        </button>
        <button class="jump-row" type="button" @click="activeTab = 'pets'">
          <PawPrint :size="18" />
          <span>宠物</span>
        </button>
      </article>
    </section>

    <section v-if="activeTab === 'record'" class="view">
      <article class="quick-card full-height">
        <div class="section-title">
          <h2>记录本</h2>
          <Plus :size="18" />
        </div>
        <textarea v-model="quickText" rows="9" placeholder="直接按微信记录习惯写，先保留原文，后面再结构化整理" />
        <button class="primary-button" type="button" @click="submitQuickRecord">
          <Send :size="18" />
          <span>保存记录</span>
        </button>
      </article>
      <article class="list-card">
        <div class="section-title">
          <h2>记录列表</h2>
          <span>{{ records.length }}</span>
        </div>
        <p v-if="records.length === 0" class="empty">还没有记录</p>
        <div v-for="record in records" :key="text(record, 'id')" class="feed-item">
          <span>{{ text(record, "record_type") }}</span>
          <p>{{ text(record, "raw_text") }}</p>
        </div>
      </article>
    </section>

    <section v-if="activeTab === 'plants'" class="view">
      <article class="form-card">
        <div class="section-title">
          <h2>新增花卉</h2>
          <Leaf :size="18" />
        </div>
        <input v-model="newPlant.name" placeholder="名称，如月季" />
        <div class="inline-fields">
          <input v-model="newPlant.flowerColor" placeholder="花色" />
          <input v-model="newPlant.location" placeholder="位置" />
        </div>
        <input v-model="newPlant.carePreference" placeholder="养护偏好" />
        <button class="secondary-button" type="button" @click="submitPlant">
          <Plus :size="17" />
          <span>新增</span>
        </button>
      </article>

      <article v-for="plant in plants" :key="text(plant, 'id')" class="plant-card">
        <span class="plant-dot"></span>
        <div>
          <h3>{{ text(plant, "name") }}</h3>
          <p>{{ text(plant, "flower_color") || "未记录花色" }} · {{ text(plant, "location") || "未记录位置" }}</p>
        </div>
        <strong>{{ text(plant, "status") }}</strong>
      </article>
    </section>

    <section v-if="activeTab === 'care'" class="view">
      <article class="form-card">
        <div class="section-title">
          <h2>添加养护记录</h2>
          <Sprout :size="18" />
        </div>
        <select v-model="careDraft.plantId">
          <option value="">选择花卉</option>
          <option v-for="plant in plants" :key="text(plant, 'id')" :value="text(plant, 'id')">
            {{ text(plant, "name") }}
          </option>
        </select>
        <div class="inline-fields">
          <select v-model="careDraft.careType">
            <option value="WATER">浇水</option>
            <option value="FERTILIZE">施肥</option>
            <option value="PRUNE">修剪</option>
            <option value="OBSERVE">观察</option>
          </select>
          <input v-model="careDraft.nextCareAt" type="datetime-local" />
        </div>
        <textarea v-model="careDraft.detail" rows="3" placeholder="月季超微已浇水，7.9用1号" />
        <button class="secondary-button" type="button" @click="submitCareRecord">
          <Plus :size="17" />
          <span>保存养护</span>
        </button>
      </article>

      <article class="list-card">
        <div class="section-title">
          <h2>养护历史</h2>
          <span>{{ careRecords.length }}</span>
        </div>
        <p v-if="careRecords.length === 0" class="empty">选择花卉后查看养护历史</p>
        <div v-for="record in careRecords" :key="text(record, 'id')" class="feed-item">
          <span>{{ text(record, "care_type") }} · {{ text(record, "care_date") }}</span>
          <p>{{ text(record, "detail") || text(record, "raw_text") }}</p>
        </div>
      </article>
    </section>

    <section v-if="activeTab === 'shopping'" class="view">
      <article class="form-card">
        <div class="section-title">
          <h2>购物清单</h2>
          <ClipboardList :size="18" />
        </div>
        <div class="inline-fields">
          <input v-model="shoppingDraft.name" placeholder="物品" />
          <input v-model="shoppingDraft.quantity" placeholder="数量" />
        </div>
        <select v-model="shoppingDraft.category">
          <option v-for="category in financeCategories" :key="text(category, 'code')" :value="text(category, 'code')">
            {{ text(category, "name") }}
          </option>
        </select>
        <button class="secondary-button" type="button" @click="submitShoppingItem">
          <Plus :size="17" />
          <span>加入清单</span>
        </button>
      </article>
      <article v-for="item in todoShoppingItems" :key="text(item, 'id')" class="shopping-card">
        <div>
          <strong>{{ text(item, "name") }}</strong>
          <p>
            {{ text(item, "quantity") || "待确认数量" }} ·
            {{ categoryName(text(item, "category") || "OTHER") }}
          </p>
        </div>
        <div v-if="purchaseDrafts[text(item, 'id')]" class="purchase-box">
          <input v-model="purchaseDrafts[text(item, 'id')].amount" inputmode="decimal" placeholder="实际金额" />
          <select v-model="purchaseDrafts[text(item, 'id')].category">
            <option v-for="category in financeCategories" :key="text(category, 'code')" :value="text(category, 'code')">
              {{ text(category, "name") }}
            </option>
          </select>
          <select v-model="purchaseDrafts[text(item, 'id')].buyerId">
            <option v-for="member in familyMembers" :key="text(member, 'id')" :value="text(member, 'id')">
              {{ text(member, "display_name") }}
            </option>
          </select>
          <button class="secondary-button compact-button" type="button" @click="checkShoppingItem(item)">
            <Check :size="17" />
            <span>已购买并入账</span>
          </button>
        </div>
      </article>
      <article class="list-card">
        <div class="section-title">
          <h2>已购买</h2>
          <span>{{ doneShoppingItems.length }}</span>
        </div>
        <p v-if="doneShoppingItems.length === 0" class="empty">还没有已购买物品</p>
        <div v-for="item in doneShoppingItems.slice(0, 8)" :key="text(item, 'id')" class="feed-item">
          <span>{{ categoryName(text(item, "category") || "OTHER") }} · {{ text(item, "purchased_at") }}</span>
          <p>
            {{ text(item, "name") }}：{{ text(item, "actual_amount") || "0" }}
            <template v-if="text(item, 'buyer_id')"> · {{ memberName(text(item, "buyer_id")) }}</template>
          </p>
        </div>
      </article>
    </section>

    <section v-if="activeTab === 'reminders'" class="view">
      <article class="form-card">
        <div class="section-title">
          <h2>新建提醒</h2>
          <Bell :size="18" />
        </div>
        <input v-model="reminderDraft.title" placeholder="提醒事项" />
        <input v-model="reminderDraft.dueAt" type="datetime-local" />
        <textarea v-model="reminderDraft.description" rows="3" placeholder="备注" />
        <button class="secondary-button" type="button" @click="submitReminder">
          <Plus :size="17" />
          <span>保存提醒</span>
        </button>
      </article>
      <article v-for="reminder in pendingReminders" :key="text(reminder, 'id')" class="reminder-row">
        <div>
          <strong>{{ text(reminder, "title") }}</strong>
          <p>{{ text(reminder, "due_at") }}</p>
        </div>
        <button class="icon-button" type="button" aria-label="完成提醒" title="完成提醒" @click="completeReminder(numberValue(reminder, 'id'))">
          <Check :size="18" />
        </button>
      </article>
      <p v-if="pendingReminders.length === 0" class="empty">暂时没有待处理提醒</p>
    </section>

    <section v-if="activeTab === 'finance'" class="view">
      <article class="form-card">
        <div class="section-title">
          <h2>记一笔支出</h2>
          <WalletCards :size="18" />
        </div>
        <input v-model="financeDraft.title" placeholder="项目，如晚饭、花盆、洗衣液" />
        <div class="inline-fields">
          <input v-model="financeDraft.amount" inputmode="decimal" placeholder="金额" />
          <select v-model="financeDraft.category">
            <option v-for="category in financeCategories" :key="text(category, 'code')" :value="text(category, 'code')">
              {{ text(category, "name") }}
            </option>
          </select>
        </div>
        <input v-model="financeDraft.occurredOn" type="date" />
        <button class="secondary-button" type="button" @click="submitFinanceRecord">
          <Plus :size="17" />
          <span>保存支出</span>
        </button>
      </article>

      <article class="list-card">
        <div class="section-title">
          <h2>支出记录</h2>
          <span>{{ expenseRecords.length }}</span>
        </div>
        <p v-if="expenseRecords.length === 0" class="empty">还没有支出记录</p>
        <div v-for="record in expenseRecords" :key="text(record, 'id')" class="feed-item">
          <span>{{ categoryName(text(record, "category") || "OTHER") }} · {{ text(record, "occurred_on") }}</span>
          <p>{{ text(record, "title") }}：{{ text(record, "amount") }}</p>
        </div>
      </article>
    </section>

    <section v-if="activeTab === 'private'" class="view">
      <article class="form-card">
        <div class="section-title">
          <h2>私密空间</h2>
          <MessageSquare :size="18" />
        </div>
        <div class="inline-fields">
          <select v-model="privateDraft.visibility">
            <option value="TO_PARTNER">悄悄话</option>
            <option value="PRIVATE">只给自己</option>
            <option value="SHARED">共同可见</option>
          </select>
          <select v-model="privateDraft.receiverMemberId" :disabled="privateDraft.visibility !== 'TO_PARTNER'">
            <option v-for="member in otherMembers" :key="text(member, 'id')" :value="text(member, 'id')">
              {{ text(member, "display_name") }}
            </option>
          </select>
        </div>
        <textarea v-model="privateDraft.content" rows="4" placeholder="写一句只属于这里的话" />
        <button class="secondary-button" type="button" @click="submitPrivateMessage">
          <Send :size="17" />
          <span>保存留言</span>
        </button>
      </article>

      <article class="list-card">
        <div class="section-title">
          <h2>留言板</h2>
          <span>{{ privateMessages.length }}</span>
        </div>
        <p v-if="privateMessages.length === 0" class="empty">这里还很安静</p>
        <div v-for="item in privateMessages" :key="text(item, 'id')" class="private-item">
          <span>
            {{ text(item, "visibility") }} ·
            {{ memberName(text(item, "sender_member_id")) }}
            <template v-if="text(item, 'receiver_member_id')"> 给 {{ memberName(text(item, "receiver_member_id")) }}</template>
          </span>
          <p>{{ text(item, "content") }}</p>
          <button
            v-if="text(item, 'receiver_member_id') === String(currentUser.memberId ?? '') && !text(item, 'read_at')"
            class="text-button"
            type="button"
            @click="markPrivateMessageRead(numberValue(item, 'id'))"
          >
            标记已读
          </button>
        </div>
      </article>
    </section>

    <section v-if="activeTab === 'album'" class="view">
      <article class="form-card album-form">
        <div class="section-title">
          <h2>共同相册</h2>
          <Camera :size="18" />
        </div>
        <label class="file-pick">
          <ImagePlus :size="18" />
          <span>{{ albumFile ? albumFile.name : "选择照片" }}</span>
          <input type="file" accept="image/*" @change="albumFile = selectedFile($event)" />
        </label>
        <input v-model="albumDraft.title" placeholder="照片标题" />
        <input v-model="albumDraft.imageUrl" placeholder="图片链接（可选）" />
        <textarea v-model="albumDraft.description" rows="3" placeholder="照片备注" />
        <input v-model="albumDraft.takenOn" type="date" />
        <button class="secondary-button" type="button" @click="submitAlbumPhoto">
          <ImagePlus :size="17" />
          <span>加入相册</span>
        </button>
      </article>

      <article class="album-grid-card">
        <div class="section-title">
          <h2>照片墙</h2>
          <span>{{ albumPhotos.length }}</span>
        </div>
        <p v-if="albumPhotos.length === 0" class="empty">还没有照片</p>
        <div class="album-grid">
          <article v-for="photo in albumPhotos" :key="text(photo, 'id')" class="photo-card">
            <img :src="text(photo, 'image_url')" :alt="text(photo, 'title')" />
            <div>
              <strong>{{ text(photo, "title") }}</strong>
              <span>{{ text(photo, "taken_on") || text(photo, "created_at") }}</span>
              <p v-if="text(photo, 'description')">{{ text(photo, "description") }}</p>
            </div>
          </article>
        </div>
      </article>
    </section>

    <section v-if="activeTab === 'profile'" class="view">
      <article class="profile-card">
        <span class="profile-avatar" :style="{ background: profileDraft.avatarUrl ? 'transparent' : profileDraft.avatarColor }">
          <img v-if="profileDraft.avatarUrl" :src="profileDraft.avatarUrl" alt="头像" />
          <template v-else>{{ text(currentUser, "displayName").slice(0, 1) }}</template>
        </span>
        <div>
          <p class="eyebrow">个人信息</p>
          <h2>{{ text(currentUser, "displayName") }}</h2>
          <p>{{ text(currentUser, "username") }} · ID {{ text(currentUser, "memberId") }}</p>
        </div>
      </article>
      <article class="form-card">
        <div class="section-title">
          <h2>编辑资料</h2>
          <User :size="18" />
        </div>
        <label class="file-pick">
          <ImagePlus :size="18" />
          <span>{{ profileAvatarFile ? profileAvatarFile.name : "更换头像" }}</span>
          <input type="file" accept="image/*" @change="profileAvatarFile = selectedFile($event)" />
        </label>
        <input v-model="profileDraft.displayName" placeholder="昵称" />
        <div class="inline-fields">
          <input v-model="profileDraft.username" placeholder="登录名" />
          <input v-model="profileDraft.password" type="password" placeholder="新密码（可不填）" />
        </div>
        <textarea v-model="profileDraft.bio" rows="3" placeholder="个人简介" />
        <input v-model="profileDraft.avatarColor" type="color" />
        <button class="secondary-button" type="button" @click="submitProfile">
          <Check :size="17" />
          <span>保存资料</span>
        </button>
      </article>
      <article class="list-card">
        <div class="section-title">
          <h2>我的家庭身份</h2>
          <Users :size="18" />
        </div>
        <div class="profile-row"><span>角色</span><strong>{{ text(currentMember ?? {}, "role_code") || "家庭成员" }}</strong></div>
        <div class="profile-row"><span>家庭</span><strong>{{ familyTitle }}</strong></div>
      </article>
      <button class="primary-button" type="button" @click="logout">
        <LogOut :size="18" />
        <span>退出登录</span>
      </button>
    </section>

    <section v-if="activeTab === 'members'" class="view">
      <article class="list-card member-toolbar">
        <div class="section-title">
          <h2>家庭成员</h2>
          <button class="icon-button" type="button" aria-label="添加成员" title="添加成员" @click="activeTab = 'memberAdd'">
            <UserPlus :size="18" />
          </button>
        </div>
        <button class="secondary-button" type="button" @click="activeTab = 'memberAdd'">
          <Plus :size="17" />
          <span>添加成员</span>
        </button>
      </article>

      <article v-for="member in familyMembers" :key="text(member, 'id')" class="member-card">
        <span class="avatar-dot" :style="{ background: text(member, 'avatar_url') ? 'transparent' : text(member, 'avatar_color') || '#7d8f68' }">
          <img v-if="text(member, 'avatar_url')" :src="text(member, 'avatar_url')" :alt="text(member, 'display_name')" />
        </span>
        <div>
          <strong>{{ text(member, "display_name") }}</strong>
          <p>{{ text(member, "role_code") }} · {{ text(member, "username") || "未开通登录" }}</p>
        </div>
        <span v-if="text(member, 'id') === String(currentUser.memberId ?? '')" class="self-badge">当前</span>
        <button class="icon-button" type="button" aria-label="编辑成员" title="编辑成员" @click="startEditMember(member)">
          <Pencil :size="17" />
        </button>
      </article>
    </section>

    <section v-if="activeTab === 'memberAdd'" class="view">
      <article class="form-card">
        <div class="section-title">
          <button class="icon-button" type="button" aria-label="返回成员" title="返回成员" @click="activeTab = 'members'">
            <ArrowLeft :size="18" />
          </button>
          <h2>添加成员</h2>
          <UserPlus :size="18" />
        </div>
        <input v-model="memberDraft.displayName" placeholder="成员昵称" />
        <label class="file-pick">
          <ImagePlus :size="18" />
          <span>{{ memberAvatarFile ? memberAvatarFile.name : "选择头像" }}</span>
          <input type="file" accept="image/*" @change="memberAvatarFile = selectedFile($event)" />
        </label>
        <div class="inline-fields">
          <input v-model="memberDraft.username" placeholder="登录名" />
          <input v-model="memberDraft.password" type="password" placeholder="登录密码" />
        </div>
        <textarea v-model="memberDraft.bio" rows="3" placeholder="成员备注" />
        <input v-model="memberDraft.avatarColor" type="color" />
        <button class="secondary-button" type="button" @click="submitMember">
          <Plus :size="17" />
          <span>添加成员</span>
        </button>
      </article>
    </section>

    <section v-if="activeTab === 'memberEdit'" class="view">
      <article class="form-card">
        <div class="section-title">
          <button class="icon-button" type="button" aria-label="返回成员" title="返回成员" @click="activeTab = 'members'">
            <ArrowLeft :size="18" />
          </button>
          <h2>编辑成员</h2>
          <Pencil :size="18" />
        </div>
        <label class="file-pick">
          <ImagePlus :size="18" />
          <span>{{ memberEditAvatarFile ? memberEditAvatarFile.name : "更换头像" }}</span>
          <input type="file" accept="image/*" @change="memberEditAvatarFile = selectedFile($event)" />
        </label>
        <input v-model="memberEditDraft.displayName" placeholder="成员昵称" />
        <div class="inline-fields">
          <input v-model="memberEditDraft.username" placeholder="登录名" />
          <input v-model="memberEditDraft.password" type="password" placeholder="新密码（可不填）" />
        </div>
        <textarea v-model="memberEditDraft.bio" rows="3" placeholder="成员备注" />
        <input v-model="memberEditDraft.avatarColor" type="color" />
        <button class="secondary-button" type="button" @click="submitMemberEdit">
          <Check :size="17" />
          <span>保存成员</span>
        </button>
      </article>
    </section>

    <section v-if="activeTab === 'pets'" class="view">
      <article class="form-card pet-form">
        <div class="section-title">
          <h2>宠物档案</h2>
          <PawPrint :size="18" />
        </div>
        <label class="file-pick">
          <ImagePlus :size="18" />
          <span>{{ petAvatarFile ? petAvatarFile.name : "选择宠物头像" }}</span>
          <input type="file" accept="image/*" @change="petAvatarFile = selectedFile($event)" />
        </label>
        <input v-model="petDraft.name" placeholder="名字" />
        <div class="inline-fields">
          <select v-model="petDraft.species">
            <option value="CAT">猫</option>
            <option value="DOG">狗</option>
            <option value="OTHER">其他</option>
          </select>
          <input v-model="petDraft.breed" placeholder="品种" />
        </div>
        <div class="inline-fields">
          <select v-model="petDraft.gender">
            <option value="">性别</option>
            <option value="MALE">男孩</option>
            <option value="FEMALE">女孩</option>
            <option value="UNKNOWN">不确定</option>
          </select>
          <input v-model="petDraft.birthday" type="date" />
        </div>
        <textarea v-model="petDraft.note" rows="3" placeholder="性格、饮食偏好、注意事项" />
        <button class="secondary-button" type="button" @click="submitPet">
          <Plus :size="17" />
          <span>添加宠物</span>
        </button>
      </article>

      <article v-if="pets.length > 0" class="list-card">
        <div class="section-title">
          <h2>选择宠物</h2>
          <span>{{ pets.length }}</span>
        </div>
        <select v-model="selectedPetId">
          <option v-for="pet in pets" :key="text(pet, 'id')" :value="text(pet, 'id')">
            {{ text(pet, "name") }}
          </option>
        </select>
      </article>

      <article v-for="pet in pets" :key="text(pet, 'id')" class="pet-card" :class="{ active: text(pet, 'id') === selectedPetId }" @click="selectedPetId = text(pet, 'id')">
        <img v-if="text(pet, 'avatar_url')" :src="text(pet, 'avatar_url')" :alt="text(pet, 'name')" />
        <span v-else class="pet-placeholder"><PawPrint :size="22" /></span>
        <div>
          <strong>{{ text(pet, "name") }}</strong>
          <p>{{ text(pet, "species") }} · {{ text(pet, "breed") || "未记录品种" }}</p>
        </div>
      </article>

      <article v-if="selectedPetId" class="form-card">
        <div class="section-title">
          <h2>宠物照片</h2>
          <Camera :size="18" />
        </div>
        <label class="file-pick">
          <ImagePlus :size="18" />
          <span>{{ petPhotoFile ? petPhotoFile.name : "选择照片" }}</span>
          <input type="file" accept="image/*" @change="petPhotoFile = selectedFile($event)" />
        </label>
        <textarea v-model="petPhotoDraft.description" rows="2" placeholder="照片备注" />
        <input v-model="petPhotoDraft.takenOn" type="date" />
        <button class="secondary-button" type="button" @click="submitPetPhoto">
          <ImagePlus :size="17" />
          <span>保存照片</span>
        </button>
      </article>

      <article v-if="selectedPetId" class="form-card">
        <div class="section-title">
          <h2>医疗记录</h2>
          <Stethoscope :size="18" />
        </div>
        <div class="inline-fields">
          <select v-model="petMedicalDraft.recordType">
            <option value="DEWORMING">驱虫</option>
            <option value="VACCINE">疫苗</option>
            <option value="CHECKUP">体检</option>
            <option value="MEDICINE">用药</option>
            <option value="OTHER">其他</option>
          </select>
          <input v-model="petMedicalDraft.recordDate" type="date" />
        </div>
        <div class="inline-fields">
          <input v-model="petMedicalDraft.hospital" placeholder="医院/机构" />
          <input v-model="petMedicalDraft.medicine" placeholder="药品/项目" />
        </div>
        <textarea v-model="petMedicalDraft.description" rows="3" placeholder="记录内容，如体内外驱虫、剂量、反应" />
        <input v-model="petMedicalDraft.nextDueAt" type="datetime-local" />
        <button class="secondary-button" type="button" @click="submitPetMedicalRecord">
          <Plus :size="17" />
          <span>保存医疗记录</span>
        </button>
      </article>

      <article v-if="selectedPetId" class="album-grid-card">
        <div class="section-title">
          <h2>照片</h2>
          <span>{{ petPhotos.length }}</span>
        </div>
        <div class="album-grid">
          <article v-for="photo in petPhotos" :key="text(photo, 'id')" class="photo-card">
            <img :src="text(photo, 'image_url')" :alt="text(photo, 'description') || '宠物照片'" />
            <div>
              <strong>{{ text(photo, "taken_on") || text(photo, "created_at") }}</strong>
              <p>{{ text(photo, "description") }}</p>
            </div>
          </article>
        </div>
        <p v-if="petPhotos.length === 0" class="empty">还没有宠物照片</p>
      </article>

      <article v-if="selectedPetId" class="list-card">
        <div class="section-title">
          <h2>医疗历史</h2>
          <span>{{ petMedicalRecords.length }}</span>
        </div>
        <p v-if="petMedicalRecords.length === 0" class="empty">还没有医疗记录</p>
        <div v-for="record in petMedicalRecords" :key="text(record, 'id')" class="feed-item">
          <span>{{ text(record, "record_type") }} · {{ text(record, "record_date") }}</span>
          <p>{{ text(record, "description") }}</p>
        </div>
      </article>
    </section>

    <nav class="bottom-nav">
      <button type="button" :class="{ active: activeTab === 'today' }" title="今日" @click="activeTab = 'today'">
        <Home :size="19" />
        <span>今日</span>
      </button>
      <button type="button" :class="{ active: activeTab === 'record' }" title="记录" @click="activeTab = 'record'">
        <Send :size="19" />
        <span>记录</span>
      </button>
      <button type="button" :class="{ active: activeTab === 'plants' }" title="花卉" @click="activeTab = 'plants'">
        <Leaf :size="19" />
        <span>花卉</span>
      </button>
      <button type="button" :class="{ active: activeTab === 'care' }" title="养护" @click="activeTab = 'care'">
        <Sprout :size="19" />
        <span>养护</span>
      </button>
      <button type="button" :class="{ active: activeTab === 'shopping' }" title="清单" @click="activeTab = 'shopping'">
        <ClipboardList :size="19" />
        <span>清单</span>
      </button>
      <button type="button" :class="{ active: activeTab === 'finance' }" title="记账" @click="activeTab = 'finance'">
        <WalletCards :size="19" />
        <span>记账</span>
      </button>
      <button type="button" :class="{ active: activeTab === 'reminders' }" title="提醒" @click="activeTab = 'reminders'">
        <Bell :size="19" />
        <span>提醒</span>
      </button>
      <button type="button" :class="{ active: activeTab === 'private' }" title="私密" @click="activeTab = 'private'">
        <MessageSquare :size="19" />
        <span>私密</span>
      </button>
      <button type="button" :class="{ active: activeTab === 'album' }" title="相册" @click="activeTab = 'album'">
        <Camera :size="19" />
        <span>相册</span>
      </button>
      <button type="button" :class="{ active: activeTab === 'pets' }" title="宠物" @click="activeTab = 'pets'">
        <PawPrint :size="19" />
        <span>宠物</span>
      </button>
      <button type="button" :class="{ active: activeTab === 'members' }" title="成员" @click="activeTab = 'members'">
        <Users :size="19" />
        <span>成员</span>
      </button>
    </nav>
  </main>
</template>
