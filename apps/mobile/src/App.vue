<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch, type Component } from "vue";
import {
  api,
  clearAuthToken,
  getRememberedLogin,
  setAuthToken,
  type AnyRow,
  type RequestError,
  type TodaySummary
} from "@home-of-us/shared";
import {
  ArrowLeft,
  Bell,
  Camera,
  ChefHat,
  Check,
  ClipboardList,
  Eye,
  EyeOff,
  GripVertical,
  Home,
  ImagePlus,
  Leaf,
  Lock,
  LogOut,
  LoaderCircle,
  MessageSquare,
  Package,
  PawPrint,
  Pencil,
  Plus,
  Send,
  Stethoscope,
  Sprout,
  Trash2,
  User,
  UserPlus,
  Users,
  WalletCards
} from "lucide-vue-next";

type TabKey =
  | "today"
  | "todo"
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
  | "pets"
  | "inventory"
  | "recipes";

type ToastType = "success" | "error" | "info";
type HomeCardKey =
  | "todo"
  | "plants"
  | "care"
  | "shopping"
  | "finance"
  | "reminders"
  | "private"
  | "members"
  | "album"
  | "pets"
  | "profile"
  | "inventory"
  | "recipes";
type ReminderViewKey = "list" | "create";
type ProfileViewKey = "detail" | "edit";
type PetViewKey = "list" | "detail" | "create" | "edit" | "photos" | "photoCreate" | "medical" | "medicalCreate";
type TodoViewKey = "list" | "create" | "edit";
type TodoFilterKey = "all" | "mine" | "shared" | "done";
type MemoryViewKey = "list" | "create" | "edit";
type InventoryViewKey = "list" | "create" | "edit";
type InventorySegmentKey = "items" | "votes";
type VoteViewKey = "list" | "create";
type RecipeViewKey = "list" | "create" | "edit" | "week" | "mealPlanCreate";
type HomeCardDefinition = {
  key: HomeCardKey;
  label: string;
  value: string;
  description: string;
  tone: string;
  icon: Component;
};

const defaultHomeCardOrder: HomeCardKey[] = [
  "todo",
  "plants",
  "care",
  "shopping",
  "finance",
  "reminders",
  "members",
  "album",
  "pets",
  "inventory",
  "recipes",
  "private",
  "profile"
];

const activeTab = ref<TabKey>("today");
const loading = ref(false);
const isAuthenticated = ref(false);
const message = ref("");
const messageType = ref<ToastType>("success");
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
const todoTasks = ref<AnyRow[]>([]);
const reminders = ref<AnyRow[]>([]);
const plants = ref<AnyRow[]>([]);
const shoppingItems = ref<AnyRow[]>([]);
const financeRecords = ref<AnyRow[]>([]);
const financeCategories = ref<AnyRow[]>([]);
const financeOverview = ref<AnyRow>({
  totalExpense: 0,
  monthExpense: 0,
  weekExpense: 0
});
const careRecords = ref<AnyRow[]>([]);
const privateMessages = ref<AnyRow[]>([]);
const albumPhotos = ref<AnyRow[]>([]);
const inventoryItems = ref<AnyRow[]>([]);
const familyVotes = ref<AnyRow[]>([]);
const recipes = ref<AnyRow[]>([]);
const mealPlans = ref<AnyRow[]>([]);
const pets = ref<AnyRow[]>([]);
const petPhotos = ref<AnyRow[]>([]);
const petMedicalRecords = ref<AnyRow[]>([]);
const selectedPetId = ref("");
const reminderView = ref<ReminderViewKey>("list");
const profileView = ref<ProfileViewKey>("detail");
const petView = ref<PetViewKey>("list");
const todoView = ref<TodoViewKey>("list");
const todoFilter = ref<TodoFilterKey>("all");
const memoryView = ref<MemoryViewKey>("list");
const inventoryView = ref<InventoryViewKey>("list");
const inventorySegment = ref<InventorySegmentKey>("items");
const voteView = ref<VoteViewKey>("list");
const recipeView = ref<RecipeViewKey>("list");
const homeCardOrder = ref<HomeCardKey[]>([...defaultHomeCardOrder]);
const savedHomeCardOrder = ref<HomeCardKey[]>([...defaultHomeCardOrder]);
const homeCardDraggingKey = ref<HomeCardKey | "">("");
const actionKey = ref("");
const selectedTodoId = ref("");
const selectedMemoryId = ref("");
const selectedInventoryId = ref("");
const selectedRecipeId = ref("");
const mealPlanWeekStart = ref(getWeekStartValue());
const rememberLogin = ref(getRememberedLogin());
const loginDraft = ref({
  username: "",
  password: ""
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
const todoDraft = ref({
  title: "",
  taskScope: "PERSONAL",
  assigneeId: "",
  taskType: "TEMPORARY",
  cycleRule: "",
  note: "",
  dueAt: ""
});
const todoEditDraft = ref({
  id: "",
  title: "",
  taskScope: "PERSONAL",
  assigneeId: "",
  taskType: "TEMPORARY",
  cycleRule: "",
  note: "",
  dueAt: ""
});
const memberDraft = ref({
  displayName: "",
  roleCode: "家庭成员",
  username: "",
  password: "",
  avatarColor: "#7d8f68",
  avatarUrl: "",
  bio: ""
});
const memberAvatarFile = ref<File | null>(null);
const memberEditAvatarFile = ref<File | null>(null);
const memberEditPasswordVisible = ref(false);
const memberEditDraft = ref({
  id: "",
  displayName: "",
  roleCode: "家庭成员",
  username: "",
  password: "",
  avatarColor: "#7d8f68",
  avatarUrl: "",
  bio: ""
});
const profileDraft = ref({
  displayName: "",
  roleCode: "家庭成员",
  username: "",
  password: "",
  avatarColor: "#2F6B4F",
  avatarUrl: "",
  bio: ""
});
const profileAvatarFile = ref<File | null>(null);
const profilePasswordVisible = ref(false);
const profilePasswordHintVisible = ref(false);
const privateDraft = ref({
  content: "",
  visibility: "TO_PARTNER",
  receiverMemberId: ""
});
const albumDraft = ref({
  title: "",
  entryType: "PHOTO",
  imageUrl: "",
  description: "",
  wishText: "",
  reminderEnabled: false,
  reminderDaysBefore: 3,
  takenOn: ""
});
const albumEditDraft = ref({
  id: "",
  title: "",
  entryType: "PHOTO",
  imageUrl: "",
  description: "",
  wishText: "",
  reminderEnabled: false,
  reminderDaysBefore: 3,
  takenOn: ""
});
const albumFile = ref<File | null>(null);
const inventoryDraft = ref({
  name: "",
  itemType: "SUPPLY",
  category: "",
  quantity: "",
  unit: "",
  lowStockThreshold: "",
  expiresOn: "",
  reminderDaysBefore: 3,
  note: ""
});
const inventoryEditDraft = ref({
  id: "",
  name: "",
  itemType: "SUPPLY",
  category: "",
  quantity: "",
  unit: "",
  lowStockThreshold: "",
  expiresOn: "",
  reminderDaysBefore: 3,
  note: ""
});
const voteDraft = ref({
  title: "",
  voteCategory: "CUSTOM",
  optionsText: ""
});
const recipeDraft = ref({
  title: "",
  mealType: "DINNER",
  ingredientsText: "",
  stepsText: ""
});
const recipeEditDraft = ref({
  id: "",
  title: "",
  mealType: "DINNER",
  ingredientsText: "",
  stepsText: ""
});
const mealPlanDraft = ref({
  plannedOn: getTodayDateValue(),
  mealSlot: "DINNER",
  recipeId: "",
  titleSnapshot: "",
  remindAt: ""
});
const weeklyPlanDraft = ref({
  enableReminder: true,
  remindTime: "18:00"
});
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
const petEditAvatarFile = ref<File | null>(null);
const petEditDraft = ref({
  id: "",
  name: "",
  species: "CAT",
  breed: "",
  gender: "",
  birthday: "",
  avatarUrl: "",
  note: ""
});
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

const roleOptions = ["主人", "女主人", "男主人", "伴侣", "家庭成员", "家人", "宝宝", "宠物家长"];

let messageTimer: number | undefined;
let refreshTimer: number | undefined;

const pendingReminders = computed(() => reminders.value.filter((item) => text(item, "status") === "PENDING"));
const openTodoTasks = computed(() => todoTasks.value.filter((item) => text(item, "status") === "TODO"));
const completedTodoTasks = computed(() => todoTasks.value.filter((item) => text(item, "status") === "DONE"));
const myTodoTasks = computed(() =>
  openTodoTasks.value.filter((item) => text(item, "assignee_id") === String(currentUser.value.memberId ?? ""))
);
const sharedTodoTasks = computed(() => openTodoTasks.value.filter((item) => text(item, "task_scope") === "SHARED"));
const filteredTodoTasks = computed(() => {
  if (todoFilter.value === "mine") {
    return myTodoTasks.value;
  }
  if (todoFilter.value === "shared") {
    return sharedTodoTasks.value;
  }
  if (todoFilter.value === "done") {
    return completedTodoTasks.value;
  }
  return openTodoTasks.value;
});
const todoShoppingItems = computed(() => shoppingItems.value.filter((item) => text(item, "status") === "TODO"));
const doneShoppingItems = computed(() => shoppingItems.value.filter((item) => text(item, "status") === "DONE"));
const expenseRecords = computed(() => financeRecords.value.filter((item) => text(item, "direction") === "EXPENSE"));
const lowInventoryItems = computed(() => inventoryItems.value.filter((item) => text(item, "status") === "LOW"));
const foodInventoryItems = computed(() => inventoryItems.value.filter((item) => text(item, "item_type") === "FOOD"));
const supplyInventoryItems = computed(() =>
  inventoryItems.value.filter((item) => text(item, "item_type") !== "FOOD")
);
const openFamilyVotes = computed(() => familyVotes.value.filter((item) => text(item, "status") === "OPEN"));
const historyFamilyVotes = computed(() => familyVotes.value.filter((item) => text(item, "status") !== "OPEN"));
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
const currentPet = computed(() =>
  pets.value.find((pet) => text(pet, "id") === selectedPetId.value)
);
const currentMemoryEntry = computed(() =>
  albumPhotos.value.find((item) => text(item, "id") === selectedMemoryId.value)
);
const currentInventoryItem = computed(() =>
  inventoryItems.value.find((item) => text(item, "id") === selectedInventoryId.value)
);
const currentRecipe = computed(() =>
  recipes.value.find((item) => text(item, "id") === selectedRecipeId.value)
);
const canEditMemberPassword = computed(
  () => memberEditDraft.value.id === String(currentUser.value.memberId ?? "")
);
const homeCardDefinitions = computed<Record<HomeCardKey, HomeCardDefinition>>(() => ({
  todo: {
    key: "todo",
    label: "待办",
    value: `${openTodoTasks.value.length}项`,
    description: "个人任务和家庭共享任务",
    tone: "tile-ink",
    icon: ClipboardList
  },
  plants: {
    key: "plants",
    label: "花卉",
    value: `${plants.value.length}盆`,
    description: "查看档案与状态",
    tone: "tile-green",
    icon: Leaf
  },
  care: {
    key: "care",
    label: "养护",
    value: `${careRecords.value.length}条`,
    description: "处理日常养护",
    tone: "tile-green",
    icon: Sprout
  },
  shopping: {
    key: "shopping",
    label: "清单",
    value: `${todoShoppingItems.value.length}项`,
    description: "待买事项一眼可见",
    tone: "tile-coral",
    icon: ClipboardList
  },
  finance: {
    key: "finance",
    label: "记账",
    value: `￥${numberValue(financeOverview.value, "monthExpense")}`,
    description: "本月支出",
    tone: "tile-gold",
    icon: WalletCards
  },
  reminders: {
    key: "reminders",
    label: "提醒",
    value: `${pendingReminders.value.length}条`,
    description: "待处理提醒",
    tone: "tile-ink",
    icon: Bell
  },
  private: {
    key: "private",
    label: "私密",
    value: `${privateMessages.value.length}条`,
    description: "悄悄话和共享留言",
    tone: "tile-plum",
    icon: MessageSquare
  },
  members: {
    key: "members",
    label: "成员",
    value: `${familyMembers.value.length}位`,
    description: "查看与维护家庭成员",
    tone: "tile-ink",
    icon: Users
  },
  album: {
    key: "album",
    label: "时刻墙",
    value: `${albumPhotos.value.length}张`,
    description: "照片、心情和纪念日",
    tone: "tile-coral",
    icon: Camera
  },
  pets: {
    key: "pets",
    label: "宠物",
    value: `${pets.value.length}只`,
    description: "档案、照片、医疗记录",
    tone: "tile-gold",
    icon: PawPrint
  },
  inventory: {
    key: "inventory",
    label: "库存",
    value: `${lowInventoryItems.value.length}项`,
    description: "库存预警和家庭投票",
    tone: "tile-emerald",
    icon: Package
  },
  recipes: {
    key: "recipes",
    label: "菜谱",
    value: `${recipes.value.length}道`,
    description: "菜谱库和一周餐单",
    tone: "tile-gold",
    icon: ChefHat
  },
  profile: {
    key: "profile",
    label: "个人",
    value: text(currentUser.value, "displayName") || "我的资料",
    description: roleLabel(text(currentMember.value ?? {}, "role_code")),
    tone: "tile-emerald",
    icon: User
  }
}));
const homeCards = computed(() => homeCardOrder.value.map((key) => homeCardDefinitions.value[key]));
const familyTitle = computed(() => {
  const familyRow = family.value.family;
  return text((familyRow as AnyRow) ?? {}, "name") || "我们的小家";
});
const headerTitle = computed(() => {
  if (activeTab.value === "today") {
    return familyTitle.value;
  }
  if (activeTab.value === "reminders") {
    return reminderView.value === "create" ? "新建提醒" : "提醒";
  }
  if (activeTab.value === "profile") {
    return profileView.value === "edit" ? "编辑个人资料" : "个人";
  }
  if (activeTab.value === "pets") {
    return (
      {
        list: "宠物",
        detail: text(currentPet.value ?? {}, "name") || "宠物详情",
        create: "新增宠物",
        edit: "编辑宠物",
        photos: "宠物照片",
        photoCreate: "新增宠物照片",
        medical: "医疗记录",
        medicalCreate: "新增医疗记录"
      } satisfies Record<PetViewKey, string>
    )[petView.value];
  }
  if (activeTab.value === "todo") {
    return todoView.value === "create" ? "新增待办" : todoView.value === "edit" ? "编辑待办" : "家庭待办";
  }
  if (activeTab.value === "album") {
    return memoryView.value === "create" ? "新增时刻" : memoryView.value === "edit" ? "编辑时刻" : "时刻墙";
  }
  if (activeTab.value === "inventory") {
    if (inventorySegment.value === "votes") {
      return voteView.value === "create" ? "新增投票" : "家庭投票";
    }
    return inventoryView.value === "create" ? "新增物资" : inventoryView.value === "edit" ? "编辑物资" : "库存与物资";
  }
  if (activeTab.value === "recipes") {
    return (
      {
        list: "菜谱",
        create: "新增菜谱",
        edit: "编辑菜谱",
        week: "一周餐单",
        mealPlanCreate: "新增餐单"
      } satisfies Record<RecipeViewKey, string>
    )[recipeView.value];
  }
  return (
    {
      plants: "花卉",
      care: "养护",
      shopping: "清单",
      finance: "记账",
      private: "私密",
      members: "成员",
      memberAdd: "添加成员",
      memberEdit: "编辑成员",
      album: "时刻墙"
    } satisfies Partial<Record<TabKey, string>>
  )[activeTab.value] ?? "我们的小家";
});

function isHomeCardKey(value: string): value is HomeCardKey {
  return defaultHomeCardOrder.includes(value as HomeCardKey);
}

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

function formatDateTime(value: string): string {
  if (!value) {
    return "";
  }
  const normalized = value.replace("T", " ");
  return normalized.length > 16 ? normalized.slice(0, 16) : normalized;
}

function getTodayDateValue(): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = `${now.getMonth() + 1}`.padStart(2, "0");
  const day = `${now.getDate()}`.padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function getWeekStartValue(baseDate = new Date()): string {
  const date = new Date(baseDate);
  const day = date.getDay();
  const offset = day === 0 ? -6 : 1 - day;
  date.setDate(date.getDate() + offset);
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const dayOfMonth = `${date.getDate()}`.padStart(2, "0");
  return `${year}-${month}-${dayOfMonth}`;
}

function shiftWeek(weekStart: string, offset: number): string {
  const base = new Date(`${weekStart}T00:00:00`);
  base.setDate(base.getDate() + offset * 7);
  return getWeekStartValue(base);
}

function normalizeHomeCardOrder(keys: string[]): HomeCardKey[] {
  const uniqueKeys = new Set<HomeCardKey>();
  keys.forEach((key) => {
    const normalizedKey = key === "record" ? "todo" : key;
    if (isHomeCardKey(normalizedKey)) {
      uniqueKeys.add(normalizedKey);
    }
  });
  defaultHomeCardOrder.forEach((key) => {
    uniqueKeys.add(key);
  });
  return [...uniqueKeys];
}

function applyHomeCardOrder(keys: string[]) {
  const normalized = normalizeHomeCardOrder(keys);
  homeCardOrder.value = normalized;
  savedHomeCardOrder.value = [...normalized];
}

function sameHomeCardOrder(left: HomeCardKey[], right: HomeCardKey[]): boolean {
  return left.length === right.length && left.every((item, index) => item === right[index]);
}

function readHomeCardOrder(data: AnyRow): HomeCardKey[] {
  const preferences = data.preferences as AnyRow | undefined;
  const cardKeys = preferences?.homeCardOrder;
  if (!Array.isArray(cardKeys)) {
    return [...defaultHomeCardOrder];
  }
  return normalizeHomeCardOrder(cardKeys.map((item) => String(item)));
}

function moveHomeCard(dragKey: HomeCardKey, targetKey: HomeCardKey) {
  if (dragKey === targetKey) {
    return;
  }
  const nextOrder = [...homeCardOrder.value];
  const fromIndex = nextOrder.indexOf(dragKey);
  const targetIndex = nextOrder.indexOf(targetKey);
  if (fromIndex < 0 || targetIndex < 0) {
    return;
  }
  nextOrder.splice(fromIndex, 1);
  nextOrder.splice(targetIndex, 0, dragKey);
  homeCardOrder.value = nextOrder;
}

function roleLabel(code: string): string {
  if (!code) {
    return "家庭成员";
  }
  if (code === "OWNER") {
    return "主人";
  }
  if (code === "PARTNER") {
    return "伴侣";
  }
  return code;
}

function todoScopeLabel(code: string): string {
  return code === "SHARED" ? "家庭共享" : "个人任务";
}

function careTypeLabel(code: string): string {
  return (
    {
      WATER: "浇水",
      FERTILIZE: "施肥",
      PRUNE: "修剪",
      OBSERVE: "观察"
    }[code] ?? code
  );
}

function memoryEntryTypeLabel(code: string): string {
  return (
    {
      PHOTO: "照片",
      MOOD: "心情",
      ANNIVERSARY: "纪念日",
      BIRTHDAY: "生日"
    }[code] ?? code
  );
}

function inventoryItemTypeLabel(code: string): string {
  return code === "FOOD" ? "食品" : "消耗品";
}

function voteCategoryLabel(code: string): string {
  return (
    {
      EAT: "吃什么",
      PLAY: "玩什么",
      CUSTOM: "自定义"
    }[code] ?? code
  );
}

function mealTypeLabel(code: string): string {
  return (
    {
      BREAKFAST: "早餐",
      LUNCH: "午餐",
      DINNER: "晚餐",
      SNACK: "加餐"
    }[code] ?? code
  );
}

function petRecordTypeLabel(code: string): string {
  return (
    {
      DEWORMING: "驱虫",
      VACCINE: "疫苗",
      CHECKUP: "体检",
      MEDICINE: "用药",
      OTHER: "其他"
    }[code] ?? code
  );
}

function speciesLabel(code: string): string {
  return (
    {
      CAT: "猫",
      DOG: "狗",
      OTHER: "其他"
    }[code] ?? code
  );
}

function petGenderLabel(code: string): string {
  return (
    {
      MALE: "男孩",
      FEMALE: "女孩",
      UNKNOWN: "不确定"
    }[code] ?? (code || "未记录性别")
  );
}

function visibilityLabel(code: string): string {
  return (
    {
      TO_PARTNER: "悄悄话",
      PRIVATE: "只给自己",
      SHARED: "共同可见"
    }[code] ?? code
  );
}

function categoryName(code: string): string {
  const category = financeCategories.value.find((item) => text(item, "code") === code);
  return category ? text(category, "name") : code;
}

function memberName(memberId: string): string {
  const member = familyMembers.value.find((item) => text(item, "id") === memberId);
  return member ? text(member, "display_name") : "家庭成员";
}

function voteOptions(vote: AnyRow): AnyRow[] {
  const options = vote.options;
  return Array.isArray(options) ? (options as AnyRow[]) : [];
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
    roleCode: roleLabel(text(currentUser.value, "roleCode")),
    username: text(currentUser.value, "username"),
    password: "",
    avatarColor: text(currentUser.value, "avatarColor") || "#2F6B4F",
    avatarUrl: text(currentUser.value, "avatarUrl"),
    bio: text(currentUser.value, "bio")
  };
}

function syncPetEditDraft() {
  petEditDraft.value = {
    id: text(currentPet.value ?? {}, "id"),
    name: text(currentPet.value ?? {}, "name"),
    species: text(currentPet.value ?? {}, "species") || "CAT",
    breed: text(currentPet.value ?? {}, "breed"),
    gender: text(currentPet.value ?? {}, "gender"),
    birthday: text(currentPet.value ?? {}, "birthday"),
    avatarUrl: text(currentPet.value ?? {}, "avatar_url"),
    note: text(currentPet.value ?? {}, "note")
  };
}

function resetTodoDraft() {
  todoDraft.value = {
    title: "",
    taskScope: "PERSONAL",
    assigneeId: "",
    taskType: "TEMPORARY",
    cycleRule: "",
    note: "",
    dueAt: ""
  };
}

function syncTodoEditDraft(task: AnyRow) {
  todoEditDraft.value = {
    id: text(task, "id"),
    title: text(task, "title"),
    taskScope: text(task, "task_scope") || "PERSONAL",
    assigneeId: text(task, "assignee_id"),
    taskType: text(task, "task_type") || "TEMPORARY",
    cycleRule: text(task, "cycle_rule"),
    note: text(task, "note"),
    dueAt: text(task, "due_at")
  };
}

function resetMemoryDraft() {
  albumDraft.value = {
    title: "",
    entryType: "PHOTO",
    imageUrl: "",
    description: "",
    wishText: "",
    reminderEnabled: false,
    reminderDaysBefore: 3,
    takenOn: ""
  };
  albumFile.value = null;
}

function syncMemoryEditDraft(entry: AnyRow) {
  albumEditDraft.value = {
    id: text(entry, "id"),
    title: text(entry, "title"),
    entryType: text(entry, "entry_type") || "PHOTO",
    imageUrl: text(entry, "image_url"),
    description: text(entry, "description"),
    wishText: text(entry, "wish_text"),
    reminderEnabled: text(entry, "reminder_enabled") === "true" || text(entry, "reminder_enabled") === "1",
    reminderDaysBefore: numberValue(entry, "reminder_days_before") || 3,
    takenOn: text(entry, "taken_on")
  };
}

function resetInventoryDraft() {
  inventoryDraft.value = {
    name: "",
    itemType: "SUPPLY",
    category: "",
    quantity: "",
    unit: "",
    lowStockThreshold: "",
    expiresOn: "",
    reminderDaysBefore: 3,
    note: ""
  };
}

function syncInventoryEditDraft(item: AnyRow) {
  inventoryEditDraft.value = {
    id: text(item, "id"),
    name: text(item, "name"),
    itemType: text(item, "item_type") || "SUPPLY",
    category: text(item, "category"),
    quantity: text(item, "quantity"),
    unit: text(item, "unit"),
    lowStockThreshold: text(item, "low_stock_threshold"),
    expiresOn: text(item, "expires_on"),
    reminderDaysBefore: numberValue(item, "reminder_days_before") || 3,
    note: text(item, "note")
  };
}

function resetVoteDraft() {
  voteDraft.value = {
    title: "",
    voteCategory: "CUSTOM",
    optionsText: ""
  };
}

function resetRecipeDraft() {
  recipeDraft.value = {
    title: "",
    mealType: "DINNER",
    ingredientsText: "",
    stepsText: ""
  };
}

function syncRecipeEditDraft(recipe: AnyRow) {
  recipeEditDraft.value = {
    id: text(recipe, "id"),
    title: text(recipe, "title"),
    mealType: text(recipe, "meal_type") || "DINNER",
    ingredientsText: text(recipe, "ingredients_text"),
    stepsText: text(recipe, "steps_text")
  };
}

function resetMealPlanDraft() {
  mealPlanDraft.value = {
    plannedOn: mealPlanWeekStart.value,
    mealSlot: "DINNER",
    recipeId: "",
    titleSnapshot: "",
    remindAt: ""
  };
}

function resetPetDraft() {
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
}

function resetPetEditDraft() {
  petEditDraft.value = {
    id: "",
    name: "",
    species: "CAT",
    breed: "",
    gender: "",
    birthday: "",
    avatarUrl: "",
    note: ""
  };
  petEditAvatarFile.value = null;
}

function openTab(tab: TabKey) {
  activeTab.value = tab;
  if (tab === "todo") {
    todoView.value = "list";
  }
  if (tab === "reminders") {
    reminderView.value = "list";
  }
  if (tab === "profile") {
    profileView.value = "detail";
    profilePasswordHintVisible.value = false;
  }
  if (tab === "album") {
    memoryView.value = "list";
  }
  if (tab === "inventory") {
    inventorySegment.value = "items";
    inventoryView.value = "list";
    voteView.value = "list";
  }
  if (tab === "recipes") {
    recipeView.value = "list";
  }
  if (tab === "pets") {
    petView.value = "list";
  }
}

function goHome() {
  activeTab.value = "today";
}

function startCreateTodo() {
  resetTodoDraft();
  todoView.value = "create";
}

function startEditTodo(task: AnyRow) {
  selectedTodoId.value = text(task, "id");
  syncTodoEditDraft(task);
  todoView.value = "edit";
}

function startCreateReminder() {
  reminderView.value = "create";
}

function startEditProfile() {
  syncProfileDraft();
  profilePasswordVisible.value = false;
  profileView.value = "edit";
}

function openPetDetail(petId: string) {
  selectedPetId.value = petId;
  petView.value = "detail";
}

function startCreateMemory() {
  resetMemoryDraft();
  memoryView.value = "create";
}

function startEditMemory(entry: AnyRow) {
  selectedMemoryId.value = text(entry, "id");
  syncMemoryEditDraft(entry);
  albumFile.value = null;
  memoryView.value = "edit";
}

function startCreateInventoryItem() {
  resetInventoryDraft();
  inventoryView.value = "create";
}

function startEditInventoryItem(item: AnyRow) {
  selectedInventoryId.value = text(item, "id");
  syncInventoryEditDraft(item);
  inventoryView.value = "edit";
}

function startCreateVote() {
  resetVoteDraft();
  voteView.value = "create";
}

function applyVoteTemplate(category: "EAT" | "PLAY") {
  if (category === "EAT") {
    voteDraft.value.title = "今晚吃什么";
    voteDraft.value.voteCategory = "EAT";
    voteDraft.value.optionsText = "火锅\n串串\n中餐\n烧烤\n炒菜";
    return;
  }
  voteDraft.value.title = "周末玩什么";
  voteDraft.value.voteCategory = "PLAY";
  voteDraft.value.optionsText = "喝酒\n看电影\n散步\n桌游\n宅家休息";
}

function startCreateRecipe() {
  resetRecipeDraft();
  recipeView.value = "create";
}

function startEditRecipe(recipe: AnyRow) {
  selectedRecipeId.value = text(recipe, "id");
  syncRecipeEditDraft(recipe);
  recipeView.value = "edit";
}

function openMealPlanWeek() {
  recipeView.value = "week";
}

function startCreateMealPlan() {
  resetMealPlanDraft();
  recipeView.value = "mealPlanCreate";
}

function changeMealPlanWeek(offset: number) {
  mealPlanWeekStart.value = shiftWeek(mealPlanWeekStart.value, offset);
}

function startCreatePet() {
  resetPetDraft();
  petView.value = "create";
}

function startEditPet() {
  syncPetEditDraft();
  petView.value = "edit";
}

function openPetPhotos() {
  petView.value = "photos";
}

function openPetPhotoCreate() {
  petPhotoDraft.value = {
    imageUrl: "",
    description: "",
    takenOn: ""
  };
  petPhotoFile.value = null;
  petView.value = "photoCreate";
}

function openPetMedical() {
  petView.value = "medical";
}

function openPetMedicalCreate() {
  petMedicalDraft.value = {
    recordType: "DEWORMING",
    recordDate: "",
    hospital: "",
    medicine: "",
    description: "",
    nextDueAt: ""
  };
  petView.value = "medicalCreate";
}

async function persistHomeCardOrder() {
  if (sameHomeCardOrder(homeCardOrder.value, savedHomeCardOrder.value)) {
    return;
  }
  try {
    await api.updateHomeCardOrder({
      cardKeys: homeCardOrder.value
    });
    savedHomeCardOrder.value = [...homeCardOrder.value];
  } catch (error) {
    homeCardOrder.value = [...savedHomeCardOrder.value];
    handleRequestError(error, "首页卡片顺序保存失败");
  }
}

function beginHomeCardDrag(cardKey: HomeCardKey, event: PointerEvent) {
  if (typeof window === "undefined" || typeof document === "undefined") {
    return;
  }
  homeCardDraggingKey.value = cardKey;
  const handlePointerMove = (moveEvent: PointerEvent) => {
    const element = document.elementFromPoint(moveEvent.clientX, moveEvent.clientY);
    const targetKey = element?.closest("[data-home-card-key]")?.getAttribute("data-home-card-key") || "";
    if (isHomeCardKey(targetKey)) {
      moveHomeCard(cardKey, targetKey);
    }
  };
  const handlePointerEnd = () => {
    window.removeEventListener("pointermove", handlePointerMove);
    window.removeEventListener("pointerup", handlePointerEnd);
    window.removeEventListener("pointercancel", handlePointerEnd);
    homeCardDraggingKey.value = "";
    void persistHomeCardOrder();
  };
  window.addEventListener("pointermove", handlePointerMove);
  window.addEventListener("pointerup", handlePointerEnd);
  window.addEventListener("pointercancel", handlePointerEnd);
  event.preventDefault();
}

function selectedFile(event: Event): File | null {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  return file ?? null;
}

function hasStoredToken(): boolean {
  if (typeof window === "undefined") {
    return false;
  }
  return Boolean(window.sessionStorage.getItem("homeOfUsToken") || window.localStorage.getItem("homeOfUsToken"));
}

function clearMessage() {
  if (messageTimer) {
    window.clearTimeout(messageTimer);
    messageTimer = undefined;
  }
  message.value = "";
}

function showMessage(textValue: string, type: ToastType = "success", timeout = 2600) {
  clearMessage();
  message.value = textValue;
  messageType.value = type;
  if (timeout > 0) {
    messageTimer = window.setTimeout(() => {
      message.value = "";
    }, timeout);
  }
}

function resetDataState() {
  currentUser.value = {};
  family.value = {};
  today.value = {
    pendingReminders: 0,
    plantCount: 0,
    shoppingTodoCount: 0,
    choreTodoCount: 0,
    inventoryLowCount: 0,
    monthExpense: 0
  };
  records.value = [];
  todoTasks.value = [];
  reminders.value = [];
  plants.value = [];
  shoppingItems.value = [];
  financeRecords.value = [];
  financeOverview.value = {
    totalExpense: 0,
    monthExpense: 0,
    weekExpense: 0
  };
  careRecords.value = [];
  privateMessages.value = [];
  albumPhotos.value = [];
  inventoryItems.value = [];
  familyVotes.value = [];
  recipes.value = [];
  mealPlans.value = [];
  pets.value = [];
  petPhotos.value = [];
  petMedicalRecords.value = [];
  selectedTodoId.value = "";
  selectedMemoryId.value = "";
  selectedInventoryId.value = "";
  selectedRecipeId.value = "";
  selectedPetId.value = "";
  todoView.value = "list";
  todoFilter.value = "all";
  reminderView.value = "list";
  profileView.value = "detail";
  memoryView.value = "list";
  inventoryView.value = "list";
  inventorySegment.value = "items";
  voteView.value = "list";
  recipeView.value = "list";
  petView.value = "list";
  profilePasswordVisible.value = false;
  profilePasswordHintVisible.value = false;
  memberEditPasswordVisible.value = false;
  mealPlanWeekStart.value = getWeekStartValue();
  homeCardOrder.value = [...defaultHomeCardOrder];
  savedHomeCardOrder.value = [...defaultHomeCardOrder];
  activeTab.value = "today";
}

function logout(messageText = "") {
  clearAuthToken();
  isAuthenticated.value = false;
  actionKey.value = "";
  loading.value = false;
  resetDataState();
  if (messageText) {
    showMessage(messageText, "info");
    return;
  }
  clearMessage();
}

function isAuthError(error: unknown): boolean {
  const requestError = error as RequestError;
  return requestError?.status === 401 || requestError?.code === "AUTH_REQUIRED";
}

function handleRequestError(error: unknown, fallback: string): string {
  const requestError = error as RequestError;
  const safeMessage = requestError?.message || fallback;
  if (isAuthError(error)) {
    logout("登录状态已失效，请重新登录");
    return safeMessage;
  }
  showMessage(safeMessage, "error", 3200);
  return safeMessage;
}

function isSubmitting(key: string): boolean {
  return actionKey.value === key;
}

async function executeAction<T>(key: string, fallback: string, action: () => Promise<T>): Promise<T | undefined> {
  if (actionKey.value) {
    return undefined;
  }
  actionKey.value = key;
  try {
    return await action();
  } catch (error) {
    handleRequestError(error, fallback);
    return undefined;
  } finally {
    if (actionKey.value === key) {
      actionKey.value = "";
    }
  }
}

function confirmDelete(label: string): boolean {
  if (typeof window === "undefined") {
    return true;
  }
  return window.confirm(`确认删除${label}吗？`);
}

async function uploadSelectedImage(
  file: File | null,
  linkedType: string,
  linkedId?: number,
  fileLabel = "图片"
): Promise<string> {
  if (!file) {
    return "";
  }
  showMessage(`正在上传${fileLabel}`, "info", 0);
  const uploaded = await api.uploadImage(file, linkedType, linkedId);
  return text(uploaded, "url");
}

async function bootstrap() {
  if (!hasStoredToken()) {
    return;
  }
  isAuthenticated.value = true;
  try {
    await loadAll();
  } catch (error) {
    handleRequestError(error, "登录状态已失效，请重新登录");
  }
}

async function submitLogin() {
  if (!loginDraft.value.username.trim() || !loginDraft.value.password.trim()) {
    showMessage("请先填写登录名和密码", "error");
    return;
  }
  await executeAction("login", "登录失败，请稍后重试", async () => {
    loading.value = true;
    try {
      const result = await api.login(loginDraft.value);
      setAuthToken(text(result, "token"), rememberLogin.value);
      isAuthenticated.value = true;
      await loadAll();
      showMessage("登录成功");
    } finally {
      loading.value = false;
    }
  });
}

async function loadAll(options: { silent?: boolean } = {}) {
  const silent = options.silent ?? false;
  if (!silent) {
    loading.value = true;
  }
  try {
    const [
      meData,
      todayData,
      familyData,
      choreData,
      reminderData,
      plantData,
      shoppingData,
      financeData,
      financeOverviewData,
      categoryData,
      privateData,
      albumData,
      inventoryData,
      voteData,
      recipeData,
      mealPlanData,
      petData
    ] = await Promise.all([
      api.me(),
      api.today(),
      api.family(),
      api.choreTasks(),
      api.reminders(),
      api.plants(),
      api.shoppingItems(),
      api.financeRecords(),
      api.financeOverview(),
      api.financeCategories(),
      api.privateMessages(),
      api.albumPhotos(),
      api.inventoryItems(),
      api.familyVotes(),
      api.recipes(),
      api.mealPlans(mealPlanWeekStart.value),
      api.pets()
    ]);
    currentUser.value = meData;
    today.value = todayData;
    family.value = familyData;
    applyHomeCardOrder(readHomeCardOrder(familyData));
    todoTasks.value = choreData;
    reminders.value = reminderData;
    plants.value = plantData;
    shoppingItems.value = shoppingData;
    financeRecords.value = financeData;
    financeOverview.value = financeOverviewData;
    financeCategories.value = categoryData;
    privateMessages.value = privateData;
    albumPhotos.value = albumData;
    inventoryItems.value = inventoryData;
    familyVotes.value = voteData;
    recipes.value = recipeData;
    mealPlans.value = mealPlanData;
    pets.value = petData;
    if (profileView.value !== "edit") {
      syncProfileDraft();
    }
    syncPurchaseDrafts(shoppingData);

    if (petData.length === 0) {
      selectedPetId.value = "";
      if (petView.value !== "create") {
        petView.value = "list";
      }
    } else if (!petData.some((pet) => text(pet, "id") === selectedPetId.value)) {
      selectedPetId.value = text(petData[0], "id");
    }

    if (plantData.length === 0) {
      careDraft.value.plantId = "";
    } else if (!plantData.some((plant) => text(plant, "id") === careDraft.value.plantId)) {
      careDraft.value.plantId = text(plantData[0], "id");
    }

    if (!privateDraft.value.receiverMemberId && otherMembers.value.length > 0) {
      privateDraft.value.receiverMemberId = text(otherMembers.value[0], "id");
    } else if (
      privateDraft.value.receiverMemberId &&
      !otherMembers.value.some((member) => text(member, "id") === privateDraft.value.receiverMemberId)
    ) {
      privateDraft.value.receiverMemberId = text(otherMembers.value[0] ?? {}, "id");
    }

    await Promise.all([loadPetDetails(true), loadCareRecords(true)]);
  } finally {
    if (!silent) {
      loading.value = false;
    }
  }
}

async function loadCareRecords(silent = false) {
  const plantId = Number(careDraft.value.plantId);
  if (!Number.isFinite(plantId) || plantId <= 0) {
    careRecords.value = [];
    return;
  }
  try {
    careRecords.value = await api.careRecords(plantId);
  } catch (error) {
    if (silent) {
      throw error;
    }
    handleRequestError(error, "养护记录加载失败");
  }
}

async function loadMealPlans(silent = false) {
  try {
    mealPlans.value = await api.mealPlans(mealPlanWeekStart.value);
  } catch (error) {
    if (silent) {
      throw error;
    }
    handleRequestError(error, "餐单加载失败");
  }
}

async function refreshSilently() {
  if (!isAuthenticated.value) {
    return;
  }
  try {
    await loadAll({ silent: true });
  } catch (error) {
    if (isAuthError(error)) {
      handleRequestError(error, "登录状态已失效，请重新登录");
    }
  }
}

function stopAutoRefresh() {
  if (refreshTimer) {
    window.clearInterval(refreshTimer);
    refreshTimer = undefined;
  }
}

function startAutoRefresh() {
  if (typeof window === "undefined") {
    return;
  }
  stopAutoRefresh();
  refreshTimer = window.setInterval(() => {
    if (document.hidden) {
      return;
    }
    void refreshSilently();
  }, 10000);
}

function handleWindowFocus() {
  void refreshSilently();
}

function handleVisibilityChange() {
  if (!document.hidden) {
    void refreshSilently();
  }
}

function buildTodoPayload(source: {
  title: string;
  taskScope: string;
  assigneeId: string;
  taskType: string;
  cycleRule: string;
  note: string;
  dueAt: string;
}) {
  return {
    title: source.title,
    taskScope: source.taskScope,
    assigneeId: source.taskScope === "SHARED" && source.assigneeId ? Number(source.assigneeId) : undefined,
    taskType: source.taskType,
    cycleRule: source.cycleRule,
    note: source.note,
    dueAt: source.dueAt || undefined
  };
}

async function submitTodoCreate() {
  if (!todoDraft.value.title.trim()) {
    showMessage("请先填写待办标题", "error");
    return;
  }
  await executeAction("todo-create", "保存待办失败", async () => {
    await api.createTodo(buildTodoPayload(todoDraft.value));
    resetTodoDraft();
    todoView.value = "list";
    await loadAll();
    showMessage("待办已创建");
  });
}

async function submitTodoEdit() {
  const taskId = Number(todoEditDraft.value.id);
  if (!Number.isFinite(taskId) || taskId <= 0 || !todoEditDraft.value.title.trim()) {
    showMessage("请先完善待办信息", "error");
    return;
  }
  await executeAction("todo-update", "更新待办失败", async () => {
    await api.updateTodo(taskId, buildTodoPayload(todoEditDraft.value));
    todoView.value = "list";
    await loadAll();
    showMessage("待办已更新");
  });
}

async function claimTodoTask(id: number) {
  await executeAction(`todo-claim-${id}`, "认领待办失败", async () => {
    await api.claimTodo(id);
    await loadAll();
    showMessage("任务已认领");
  });
}

async function completeTodoTask(id: number) {
  await executeAction(`todo-complete-${id}`, "完成待办失败", async () => {
    await api.completeTodo(id);
    await loadAll();
    showMessage("任务已完成");
  });
}

async function deleteTodoTask(id: number) {
  if (!confirmDelete("这个待办")) {
    return;
  }
  await executeAction(`todo-delete-${id}`, "删除待办失败", async () => {
    await api.deleteTodo(id);
    await loadAll();
    showMessage("待办已删除");
  });
}

function buildMemoryPayload(source: {
  title: string;
  entryType: string;
  imageUrl: string;
  description: string;
  wishText: string;
  reminderEnabled: boolean;
  reminderDaysBefore: number;
  takenOn: string;
}) {
  return {
    title: source.title,
    entryType: source.entryType,
    imageUrl: source.imageUrl || undefined,
    description: source.description || undefined,
    wishText: source.wishText || undefined,
    reminderEnabled: source.reminderEnabled,
    reminderDaysBefore: source.reminderDaysBefore,
    takenOn: source.takenOn || undefined
  };
}

async function submitMemoryCreate() {
  if (!albumDraft.value.title.trim()) {
    showMessage("请先填写时刻标题", "error");
    return;
  }
  await executeAction("memory-create", "保存时刻失败", async () => {
    const imageUrl = await uploadSelectedImage(albumFile.value, "ALBUM", undefined, "时刻图片");
    const safeImageUrl = imageUrl || albumDraft.value.imageUrl;
    if (albumDraft.value.entryType === "PHOTO" && !safeImageUrl.trim()) {
      showMessage("照片类型需要上传图片", "error");
      return;
    }
    await api.createAlbumPhoto(
      buildMemoryPayload({
        ...albumDraft.value,
        imageUrl: safeImageUrl
      })
    );
    resetMemoryDraft();
    memoryView.value = "list";
    await loadAll();
    showMessage("时刻已保存");
  });
}

async function submitMemoryEdit() {
  const entryId = Number(albumEditDraft.value.id);
  if (!Number.isFinite(entryId) || entryId <= 0 || !albumEditDraft.value.title.trim()) {
    showMessage("请先完善时刻内容", "error");
    return;
  }
  await executeAction("memory-update", "更新时刻失败", async () => {
    const imageUrl = await uploadSelectedImage(albumFile.value, "ALBUM", entryId, "时刻图片");
    const safeImageUrl = imageUrl || albumEditDraft.value.imageUrl;
    if (albumEditDraft.value.entryType === "PHOTO" && !safeImageUrl.trim()) {
      showMessage("照片类型需要上传图片", "error");
      return;
    }
    await api.updateAlbumPhoto(
      entryId,
      buildMemoryPayload({
        ...albumEditDraft.value,
        imageUrl: safeImageUrl
      })
    );
    albumFile.value = null;
    memoryView.value = "list";
    await loadAll();
    showMessage("时刻已更新");
  });
}

async function submitInventoryCreate() {
  if (!inventoryDraft.value.name.trim()) {
    showMessage("请先填写物资名称", "error");
    return;
  }
  await executeAction("inventory-create", "保存物资失败", async () => {
    await api.createInventoryItem({
      ...inventoryDraft.value,
      quantity: inventoryDraft.value.quantity || undefined,
      lowStockThreshold: inventoryDraft.value.lowStockThreshold || undefined
    });
    resetInventoryDraft();
    inventoryView.value = "list";
    await loadAll();
    showMessage("物资已添加");
  });
}

async function submitInventoryEdit() {
  const itemId = Number(inventoryEditDraft.value.id);
  if (!Number.isFinite(itemId) || itemId <= 0 || !inventoryEditDraft.value.name.trim()) {
    showMessage("请先完善物资信息", "error");
    return;
  }
  await executeAction("inventory-update", "更新物资失败", async () => {
    await api.updateInventoryItem(itemId, {
      ...inventoryEditDraft.value,
      quantity: inventoryEditDraft.value.quantity || undefined,
      lowStockThreshold: inventoryEditDraft.value.lowStockThreshold || undefined
    });
    inventoryView.value = "list";
    await loadAll();
    showMessage("物资已更新");
  });
}

async function deleteInventoryItem(id: number) {
  if (!confirmDelete("这个物资")) {
    return;
  }
  await executeAction(`inventory-delete-${id}`, "删除物资失败", async () => {
    await api.deleteInventoryItem(id);
    await loadAll();
    showMessage("物资已删除");
  });
}

async function submitVoteCreate() {
  if (!voteDraft.value.title.trim()) {
    showMessage("请先填写投票标题", "error");
    return;
  }
  const options = voteDraft.value.optionsText
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean);
  await executeAction("vote-create", "创建投票失败", async () => {
    await api.createFamilyVote({
      title: voteDraft.value.title,
      voteCategory: voteDraft.value.voteCategory,
      options
    });
    resetVoteDraft();
    voteView.value = "list";
    inventorySegment.value = "votes";
    await loadAll();
    showMessage("投票已创建");
  });
}

async function submitVoteChoice(voteId: number, optionId: number) {
  await executeAction(`vote-submit-${voteId}-${optionId}`, "提交投票失败", async () => {
    await api.submitFamilyVote(voteId, { optionId });
    await loadAll();
    showMessage("投票已提交");
  });
}

async function decideVote(voteId: number) {
  await executeAction(`vote-decide-${voteId}`, "随机决定失败", async () => {
    await api.decideFamilyVote(voteId);
    await loadAll();
    showMessage("投票结果已确认");
  });
}

async function deleteVote(voteId: number) {
  if (!confirmDelete("这个投票")) {
    return;
  }
  await executeAction(`vote-delete-${voteId}`, "删除投票失败", async () => {
    await api.deleteFamilyVote(voteId);
    await loadAll();
    showMessage("投票已删除");
  });
}

async function submitRecipeCreate() {
  if (!recipeDraft.value.title.trim()) {
    showMessage("请先填写菜谱名称", "error");
    return;
  }
  await executeAction("recipe-create", "保存菜谱失败", async () => {
    await api.createRecipe({
      ...recipeDraft.value
    });
    resetRecipeDraft();
    recipeView.value = "list";
    await loadAll();
    showMessage("菜谱已保存");
  });
}

async function submitRecipeEdit() {
  const recipeId = Number(recipeEditDraft.value.id);
  if (!Number.isFinite(recipeId) || recipeId <= 0 || !recipeEditDraft.value.title.trim()) {
    showMessage("请先完善菜谱内容", "error");
    return;
  }
  await executeAction("recipe-update", "更新菜谱失败", async () => {
    await api.updateRecipe(recipeId, {
      title: recipeEditDraft.value.title,
      mealType: recipeEditDraft.value.mealType,
      ingredientsText: recipeEditDraft.value.ingredientsText,
      stepsText: recipeEditDraft.value.stepsText
    });
    recipeView.value = "list";
    await loadAll();
    showMessage("菜谱已更新");
  });
}

async function deleteRecipe(id: number) {
  if (!confirmDelete("这道菜谱")) {
    return;
  }
  await executeAction(`recipe-delete-${id}`, "删除菜谱失败", async () => {
    await api.deleteRecipe(id);
    await loadAll();
    showMessage("菜谱已删除");
  });
}

async function submitMealPlanCreate() {
  if (!mealPlanDraft.value.plannedOn) {
    showMessage("请先选择计划日期", "error");
    return;
  }
  if (!mealPlanDraft.value.recipeId && !mealPlanDraft.value.titleSnapshot.trim()) {
    showMessage("请选择菜谱或填写餐单标题", "error");
    return;
  }
  await executeAction("meal-plan-create", "保存餐单失败", async () => {
    await api.createMealPlan({
      weekStart: mealPlanWeekStart.value,
      plannedOn: mealPlanDraft.value.plannedOn,
      mealSlot: mealPlanDraft.value.mealSlot,
      recipeId: mealPlanDraft.value.recipeId ? Number(mealPlanDraft.value.recipeId) : undefined,
      titleSnapshot: mealPlanDraft.value.titleSnapshot || undefined,
      remindAt: mealPlanDraft.value.remindAt || undefined
    });
    recipeView.value = "week";
    await loadAll();
    showMessage("餐单已保存");
  });
}

async function generateWeeklyMealPlans() {
  await executeAction("meal-plan-generate", "生成周餐单失败", async () => {
    await api.generateWeeklyMealPlans({
      weekStart: mealPlanWeekStart.value,
      enableReminder: weeklyPlanDraft.value.enableReminder,
      remindTime: weeklyPlanDraft.value.remindTime
    });
    await loadAll();
    recipeView.value = "week";
    showMessage("本周餐单已生成");
  });
}

async function syncMealPlanShoppingList() {
  await executeAction("meal-plan-shopping-sync", "同步购物清单失败", async () => {
    await api.syncMealPlanShoppingList(mealPlanWeekStart.value);
    await loadAll();
    showMessage("已同步到购物清单");
  });
}

async function deleteMealPlan(id: number) {
  if (!confirmDelete("这条餐单")) {
    return;
  }
  await executeAction(`meal-plan-delete-${id}`, "删除餐单失败", async () => {
    await api.deleteMealPlan(id);
    await loadAll();
    showMessage("餐单已删除");
  });
}

async function submitQuickRecord() {
  if (!quickText.value.trim()) {
    showMessage("请先填写记录内容", "error");
    return;
  }
  await executeAction("record-create", "保存记录失败", async () => {
    await api.createRecord({
      rawText: quickText.value,
      recordType: "GENERAL"
    });
    quickText.value = "";
    await loadAll();
    showMessage("记录已保存");
  });
}

async function deleteRecord(id: number) {
  if (!confirmDelete("这条记录")) {
    return;
  }
  await executeAction(`record-delete-${id}`, "删除记录失败", async () => {
    await api.deleteRecord(id);
    await loadAll();
    showMessage("记录已删除");
  });
}

async function submitPlant() {
  if (!newPlant.value.name.trim()) {
    showMessage("请先填写花卉名称", "error");
    return;
  }
  await executeAction("plant-create", "新增花卉失败", async () => {
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
    await loadAll();
    showMessage("花卉档案已创建");
  });
}

async function deletePlant(id: number) {
  if (!confirmDelete("这盆花卉和它的养护记录")) {
    return;
  }
  await executeAction(`plant-delete-${id}`, "删除花卉失败", async () => {
    await api.deletePlant(id);
    await loadAll();
    showMessage("花卉档案已删除");
  });
}

async function submitCareRecord() {
  const plantId = Number(careDraft.value.plantId || plants.value[0]?.id);
  if (!Number.isFinite(plantId)) {
    showMessage("请先选择花卉", "error");
    return;
  }
  if (!careDraft.value.detail.trim()) {
    showMessage("请先填写养护内容", "error");
    return;
  }
  await executeAction("care-create", "保存养护记录失败", async () => {
    await api.createCareRecord(plantId, {
      careType: careDraft.value.careType,
      detail: careDraft.value.detail,
      rawText: careDraft.value.detail,
      nextCareAt: careDraft.value.nextCareAt
    });
    careDraft.value.detail = "";
    careDraft.value.nextCareAt = "";
    await loadAll();
    showMessage("养护记录已保存，提醒会自动同步");
  });
}

async function deleteCareRecord(id: number) {
  const plantId = Number(careDraft.value.plantId);
  if (!Number.isFinite(plantId) || !confirmDelete("这条养护记录")) {
    return;
  }
  await executeAction(`care-delete-${id}`, "删除养护记录失败", async () => {
    await api.deleteCareRecord(plantId, id);
    await loadAll();
    showMessage("养护记录已删除");
  });
}

async function submitShoppingItem() {
  if (!shoppingDraft.value.name.trim()) {
    showMessage("请先填写购物项", "error");
    return;
  }
  await executeAction("shopping-create", "加入清单失败", async () => {
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
    await loadAll();
    showMessage("已加入购物清单");
  });
}

async function checkShoppingItem(item: AnyRow) {
  const id = text(item, "id");
  const draft = purchaseDrafts.value[id];
  const amount = Number(draft?.amount);
  if (!Number.isFinite(amount) || amount <= 0) {
    showMessage("请先填写实际金额", "error");
    return;
  }
  await executeAction(`shopping-check-${id}`, "勾选购物项失败", async () => {
    await api.checkShoppingItem(Number(id), {
      actualAmount: amount,
      category: draft.category,
      buyerId: Number(draft.buyerId || currentUser.value.memberId)
    });
    purchaseDrafts.value[id].amount = "";
    await loadAll();
    showMessage("已购买，并自动记入账本");
  });
}

async function deleteShoppingItem(id: number) {
  if (!confirmDelete("这个购物项")) {
    return;
  }
  await executeAction(`shopping-delete-${id}`, "删除购物项失败", async () => {
    await api.deleteShoppingItem(id);
    await loadAll();
    showMessage("购物项已删除");
  });
}

async function submitFinanceRecord() {
  const amount = Number(financeDraft.value.amount);
  if (!financeDraft.value.title.trim()) {
    showMessage("请先填写支出项目", "error");
    return;
  }
  if (!Number.isFinite(amount) || amount <= 0) {
    showMessage("请输入正确的金额", "error");
    return;
  }
  await executeAction("finance-create", "记录支出失败", async () => {
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
    await loadAll();
    showMessage("支出已记录");
  });
}

async function deleteFinanceRecord(id: number) {
  if (!confirmDelete("这条账本记录")) {
    return;
  }
  await executeAction(`finance-delete-${id}`, "删除账本记录失败", async () => {
    await api.deleteFinanceRecord(id);
    await loadAll();
    showMessage("账本记录已删除");
  });
}

async function submitReminder() {
  if (!reminderDraft.value.title.trim() || !reminderDraft.value.dueAt) {
    showMessage("请先填写提醒事项和时间", "error");
    return;
  }
  await executeAction("reminder-create", "保存提醒失败", async () => {
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
    reminderView.value = "list";
    await loadAll();
    showMessage("提醒已创建");
  });
}

async function completeReminder(id: number) {
  await executeAction(`reminder-complete-${id}`, "完成提醒失败", async () => {
    await api.completeReminder(id);
    await loadAll();
    showMessage("提醒已完成");
  });
}

async function deleteReminder(id: number) {
  if (!confirmDelete("这个提醒")) {
    return;
  }
  await executeAction(`reminder-delete-${id}`, "删除提醒失败", async () => {
    await api.deleteReminder(id);
    await loadAll();
    showMessage("提醒已删除");
  });
}

async function submitMember() {
  if (!memberDraft.value.displayName.trim()) {
    showMessage("请先填写成员昵称", "error");
    return;
  }
  await executeAction("member-create", "添加成员失败", async () => {
    const avatarUrl = await uploadSelectedImage(memberAvatarFile.value, "MEMBER", undefined, "成员头像");
    await api.createFamilyMember({
      displayName: memberDraft.value.displayName,
      roleCode: memberDraft.value.roleCode,
      avatarColor: memberDraft.value.avatarColor,
      avatarUrl: avatarUrl || memberDraft.value.avatarUrl,
      bio: memberDraft.value.bio,
      username: memberDraft.value.username,
      password: memberDraft.value.password
    });
    memberDraft.value = {
      displayName: "",
      roleCode: "家庭成员",
      username: "",
      password: "",
      avatarColor: "#7d8f68",
      avatarUrl: "",
      bio: ""
    };
    memberAvatarFile.value = null;
    activeTab.value = "members";
    await loadAll();
    showMessage("家庭成员已添加");
  });
}

function startEditMember(member: AnyRow) {
  memberEditDraft.value = {
    id: text(member, "id"),
    displayName: text(member, "display_name"),
    roleCode: roleLabel(text(member, "role_code")),
    username: text(member, "username"),
    password: "",
    avatarColor: text(member, "avatar_color") || "#7d8f68",
    avatarUrl: text(member, "avatar_url"),
    bio: text(member, "bio")
  };
  memberEditAvatarFile.value = null;
  memberEditPasswordVisible.value = false;
  activeTab.value = "memberEdit";
}

async function submitMemberEdit() {
  const memberId = Number(memberEditDraft.value.id);
  if (!Number.isFinite(memberId) || memberId <= 0 || !memberEditDraft.value.displayName.trim()) {
    showMessage("请先完善成员信息", "error");
    return;
  }
  await executeAction("member-update", "保存成员失败", async () => {
    const avatarUrl = await uploadSelectedImage(memberEditAvatarFile.value, "MEMBER", memberId, "成员头像");
    await api.updateFamilyMember(memberId, {
      displayName: memberEditDraft.value.displayName,
      roleCode: memberEditDraft.value.roleCode,
      username: memberEditDraft.value.username,
      password: canEditMemberPassword.value ? memberEditDraft.value.password : "",
      avatarColor: memberEditDraft.value.avatarColor,
      avatarUrl: avatarUrl || memberEditDraft.value.avatarUrl,
      bio: memberEditDraft.value.bio
    });
    memberEditAvatarFile.value = null;
    activeTab.value = "members";
    await loadAll();
    showMessage("成员信息已更新");
  });
}

async function submitProfile() {
  const memberId = Number(currentUser.value.memberId);
  if (!Number.isFinite(memberId) || memberId <= 0) {
    showMessage("当前账号信息异常，请重新登录", "error");
    return;
  }
  await executeAction("profile-update", "保存资料失败", async () => {
    const avatarUrl = await uploadSelectedImage(profileAvatarFile.value, "MEMBER", memberId, "头像");
    await api.updateFamilyMember(memberId, {
      displayName: profileDraft.value.displayName,
      roleCode: profileDraft.value.roleCode,
      username: profileDraft.value.username,
      password: profileDraft.value.password,
      avatarColor: profileDraft.value.avatarColor,
      avatarUrl: avatarUrl || profileDraft.value.avatarUrl,
      bio: profileDraft.value.bio
    });
    profileAvatarFile.value = null;
    profilePasswordVisible.value = false;
    profileView.value = "detail";
    await loadAll();
    showMessage("资料已保存");
  });
}

async function submitPrivateMessage() {
  if (!privateDraft.value.content.trim()) {
    showMessage("请先填写留言内容", "error");
    return;
  }
  await executeAction("private-create", "保存留言失败", async () => {
    await api.createPrivateMessage({
      content: privateDraft.value.content,
      visibility: privateDraft.value.visibility,
      receiverMemberId:
        privateDraft.value.visibility === "TO_PARTNER" ? Number(privateDraft.value.receiverMemberId) : undefined
    });
    privateDraft.value.content = "";
    await loadAll();
    showMessage("留言已保存");
  });
}

async function markPrivateMessageRead(id: number) {
  await executeAction(`private-read-${id}`, "标记已读失败", async () => {
    await api.readPrivateMessage(id);
    await loadAll();
    showMessage("已标记为已读");
  });
}

async function submitAlbumPhoto() {
  await submitMemoryCreate();
}

async function submitPet() {
  if (!petDraft.value.name.trim()) {
    showMessage("请先填写宠物名字", "error");
    return;
  }
  await executeAction("pet-create", "添加宠物失败", async () => {
    const avatarUrl = await uploadSelectedImage(petAvatarFile.value, "PET", undefined, "宠物头像");
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
    resetPetDraft();
    petView.value = "detail";
    await loadAll();
    showMessage("宠物档案已创建");
  });
}

async function submitPetEdit() {
  const petId = Number(petEditDraft.value.id || selectedPetId.value);
  if (!Number.isFinite(petId) || petId <= 0 || !petEditDraft.value.name.trim()) {
    showMessage("请先完善宠物档案", "error");
    return;
  }
  await executeAction("pet-update", "保存宠物档案失败", async () => {
    const avatarUrl = await uploadSelectedImage(petEditAvatarFile.value, "PET", petId, "宠物头像");
    await api.updatePet(petId, {
      name: petEditDraft.value.name,
      species: petEditDraft.value.species,
      breed: petEditDraft.value.breed,
      gender: petEditDraft.value.gender,
      birthday: petEditDraft.value.birthday,
      avatarUrl: avatarUrl || petEditDraft.value.avatarUrl,
      note: petEditDraft.value.note
    });
    selectedPetId.value = String(petId);
    resetPetEditDraft();
    petView.value = "detail";
    await loadAll();
    showMessage("宠物档案已更新");
  });
}

async function submitPetPhoto() {
  const petId = Number(selectedPetId.value);
  if (!Number.isFinite(petId) || petId <= 0) {
    showMessage("请先选择宠物", "error");
    return;
  }
  await executeAction("pet-photo-create", "保存宠物照片失败", async () => {
    const imageUrl = await uploadSelectedImage(petPhotoFile.value, "PET", petId, "宠物照片");
    const safeImageUrl = imageUrl || petPhotoDraft.value.imageUrl;
    if (!safeImageUrl.trim()) {
      showMessage("请选择宠物照片", "error");
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
    petView.value = "photos";
    await loadPetDetails();
    showMessage("宠物照片已保存");
  });
}

async function submitPetMedicalRecord() {
  const petId = Number(selectedPetId.value);
  if (!Number.isFinite(petId) || petId <= 0) {
    showMessage("请先选择宠物", "error");
    return;
  }
  if (!petMedicalDraft.value.description.trim()) {
    showMessage("请先填写医疗记录内容", "error");
    return;
  }
  await executeAction("pet-medical-create", "保存医疗记录失败", async () => {
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
    petView.value = "medical";
    await loadPetDetails();
    showMessage("宠物医疗记录已保存");
  });
}

async function deleteAlbumPhoto(id: number) {
  if (!confirmDelete("这张照片")) {
    return;
  }
  await executeAction(`album-delete-${id}`, "删除照片失败", async () => {
    await api.deleteAlbumPhoto(id);
    await loadAll();
    showMessage("照片已删除");
  });
}

async function loadPetDetails(silent = false) {
  const petId = Number(selectedPetId.value);
  if (!Number.isFinite(petId) || petId <= 0) {
    petPhotos.value = [];
    petMedicalRecords.value = [];
    return;
  }
  try {
    const [photoData, medicalData] = await Promise.all([api.petPhotos(petId), api.petMedicalRecords(petId)]);
    petPhotos.value = photoData;
    petMedicalRecords.value = medicalData;
  } catch (error) {
    if (silent) {
      throw error;
    }
    handleRequestError(error, "宠物资料加载失败");
  }
}

async function deletePet(id: number) {
  if (!confirmDelete("这只宠物和它的照片、医疗记录")) {
    return;
  }
  await executeAction(`pet-delete-${id}`, "删除宠物失败", async () => {
    await api.deletePet(id);
    if (selectedPetId.value === String(id)) {
      selectedPetId.value = "";
    }
    petView.value = "list";
    await loadAll();
    showMessage("宠物档案已删除");
  });
}

async function deletePetPhoto(id: number) {
  const petId = Number(selectedPetId.value);
  if (!Number.isFinite(petId) || !confirmDelete("这张宠物照片")) {
    return;
  }
  await executeAction(`pet-photo-delete-${id}`, "删除宠物照片失败", async () => {
    await api.deletePetPhoto(petId, id);
    await loadPetDetails();
    showMessage("宠物照片已删除");
  });
}

async function deletePetMedicalRecord(id: number) {
  const petId = Number(selectedPetId.value);
  if (!Number.isFinite(petId) || !confirmDelete("这条医疗记录")) {
    return;
  }
  await executeAction(`pet-medical-delete-${id}`, "删除医疗记录失败", async () => {
    await api.deletePetMedicalRecord(petId, id);
    await loadPetDetails();
    showMessage("医疗记录已删除");
  });
}

onMounted(bootstrap);
onMounted(() => {
  if (typeof window === "undefined") {
    return;
  }
  window.addEventListener("focus", handleWindowFocus);
  document.addEventListener("visibilitychange", handleVisibilityChange);
});

onUnmounted(() => {
  stopAutoRefresh();
  if (typeof window === "undefined") {
    return;
  }
  clearMessage();
  window.removeEventListener("focus", handleWindowFocus);
  document.removeEventListener("visibilitychange", handleVisibilityChange);
});

watch(
  () => isAuthenticated.value,
  (value) => {
    if (value) {
      startAutoRefresh();
      return;
    }
    stopAutoRefresh();
  }
);

watch(
  () => careDraft.value.plantId,
  () => {
    void loadCareRecords();
  }
);

watch(
  () => selectedPetId.value,
  () => {
    void loadPetDetails();
  }
);

watch(
  () => mealPlanWeekStart.value,
  () => {
    if (isAuthenticated.value) {
      void loadMealPlans();
    }
  }
);
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
      <label class="remember-row">
        <input v-model="rememberLogin" type="checkbox" />
        <span>记住登录</span>
      </label>
      <button class="primary-button" :disabled="isSubmitting('login')" type="button" @click="submitLogin">
        <LoaderCircle v-if="isSubmitting('login')" class="spin" :size="18" />
        <Lock v-else :size="18" />
        <span>登录</span>
      </button>
      <p v-if="message" :class="['toast', `toast-${messageType}`]">{{ message }}</p>
    </section>
  </main>

  <main v-else class="mobile-shell">
    <header class="topbar">
      <div class="topbar-main">
        <button
          v-if="activeTab !== 'today'"
          class="icon-button"
          type="button"
          aria-label="返回首页"
          title="返回首页"
          @click="goHome()"
        >
          <ArrowLeft :size="19" />
        </button>
        <div>
          <p class="eyebrow">{{ activeTab === "today" ? "Home Of Us" : "返回首页" }}</p>
          <h1>{{ headerTitle }}</h1>
        </div>
      </div>
      <div class="top-actions">
        <button v-if="activeTab === 'today'" class="user-chip" type="button" @click="openTab('profile')">
          <User :size="16" />
          <span>{{ text(currentUser, "displayName") }}</span>
        </button>
        <button class="icon-button" type="button" aria-label="刷新" title="刷新" @click="loadAll()">
          <LoaderCircle :class="{ spin: loading }" :size="20" />
        </button>
        <button class="icon-button" type="button" aria-label="退出登录" title="退出登录" @click="logout()">
          <LogOut :size="19" />
        </button>
      </div>
    </header>

    <p v-if="message" :class="['toast', `toast-${messageType}`]">{{ message }}</p>

    <section v-if="activeTab === 'today'" class="view">
      <article class="list-card module-home-card">
        <div class="section-title">
          <h2>全部模块</h2>
          <span>{{ homeCards.length }}</span>
        </div>
        <div class="module-card-grid">
          <article
            v-for="entry in homeCards"
            :key="entry.key"
            :data-home-card-key="entry.key"
            :class="['home-module-card', entry.tone, { dragging: homeCardDraggingKey === entry.key }]"
          >
            <button class="home-module-open" type="button" @click="openTab(entry.key)">
              <component :is="entry.icon" :size="20" />
              <span>{{ entry.label }}</span>
              <strong>{{ entry.value }}</strong>
              <small>{{ entry.description }}</small>
            </button>
            <button
              class="home-module-grip"
              type="button"
              aria-label="拖动排序"
              title="拖动排序"
              @pointerdown="beginHomeCardDrag(entry.key, $event)"
            >
              <GripVertical :size="16" />
            </button>
          </article>
        </div>
      </article>
    </section>

    <section v-if="activeTab === 'todo'" class="view">
      <article v-if="todoView === 'list'" class="list-card">
        <div class="section-title">
          <h2>家庭待办</h2>
          <button class="icon-button" type="button" aria-label="新增待办" title="新增待办" @click="startCreateTodo()">
            <Plus :size="18" />
          </button>
        </div>
        <div class="segmented-control">
          <button :class="['segment-button', { active: todoFilter === 'all' }]" type="button" @click="todoFilter = 'all'">
            全部
          </button>
          <button :class="['segment-button', { active: todoFilter === 'mine' }]" type="button" @click="todoFilter = 'mine'">
            我的
          </button>
          <button
            :class="['segment-button', { active: todoFilter === 'shared' }]"
            type="button"
            @click="todoFilter = 'shared'"
          >
            共享
          </button>
          <button :class="['segment-button', { active: todoFilter === 'done' }]" type="button" @click="todoFilter = 'done'">
            已完成
          </button>
        </div>
        <p v-if="filteredTodoTasks.length === 0" class="empty">这里还没有待办</p>
        <article v-for="task in filteredTodoTasks" :key="text(task, 'id')" class="feed-item feed-item-actions">
          <div class="feed-main">
            <span>
              {{ todoScopeLabel(text(task, "task_scope")) }}
              <template v-if="text(task, 'due_at')"> · {{ formatDateTime(text(task, "due_at")) }}</template>
            </span>
            <p>{{ text(task, "title") }}</p>
            <small v-if="text(task, 'note')">{{ text(task, "note") }}</small>
            <small v-if="text(task, 'assignee_name')">认领人：{{ text(task, "assignee_name") }}</small>
          </div>
          <div class="row-actions">
            <button
              v-if="text(task, 'status') === 'TODO' && text(task, 'task_scope') === 'SHARED' && !text(task, 'assignee_id')"
              class="icon-button"
              type="button"
              aria-label="认领任务"
              title="认领任务"
              @click="claimTodoTask(numberValue(task, 'id'))"
            >
              <User :size="16" />
            </button>
            <button
              v-if="text(task, 'status') === 'TODO'"
              class="icon-button"
              type="button"
              aria-label="完成任务"
              title="完成任务"
              @click="completeTodoTask(numberValue(task, 'id'))"
            >
              <Check :size="16" />
            </button>
            <button class="icon-button" type="button" aria-label="编辑待办" title="编辑待办" @click="startEditTodo(task)">
              <Pencil :size="16" />
            </button>
            <button
              class="icon-button danger-icon-button"
              type="button"
              aria-label="删除待办"
              title="删除待办"
              @click="deleteTodoTask(numberValue(task, 'id'))"
            >
              <Trash2 :size="16" />
            </button>
          </div>
        </article>
      </article>

      <article v-else-if="todoView === 'create'" class="form-card">
        <div class="section-title">
          <button class="icon-button" type="button" aria-label="返回待办列表" title="返回待办列表" @click="todoView = 'list'">
            <ArrowLeft :size="18" />
          </button>
          <h2>新增待办</h2>
          <ClipboardList :size="18" />
        </div>
        <input v-model="todoDraft.title" placeholder="待办标题" />
        <div class="inline-fields">
          <select v-model="todoDraft.taskScope">
            <option value="PERSONAL">个人任务</option>
            <option value="SHARED">家庭共享任务</option>
          </select>
          <input v-model="todoDraft.dueAt" type="datetime-local" placeholder="截止时间" />
        </div>
        <div v-if="todoDraft.taskScope === 'SHARED'" class="inline-fields">
          <select v-model="todoDraft.assigneeId">
            <option value="">暂不认领</option>
            <option v-for="member in familyMembers" :key="text(member, 'id')" :value="text(member, 'id')">
              {{ text(member, "display_name") }}
            </option>
          </select>
          <select v-model="todoDraft.taskType">
            <option value="TEMPORARY">临时任务</option>
            <option value="ROUTINE">周期任务</option>
          </select>
        </div>
        <textarea v-model="todoDraft.note" rows="4" placeholder="补充说明" />
        <button class="secondary-button" :disabled="isSubmitting('todo-create')" type="button" @click="submitTodoCreate()">
          <LoaderCircle v-if="isSubmitting('todo-create')" class="spin" :size="17" />
          <Check v-else :size="17" />
          <span>保存待办</span>
        </button>
      </article>

      <article v-else class="form-card">
        <div class="section-title">
          <button class="icon-button" type="button" aria-label="返回待办列表" title="返回待办列表" @click="todoView = 'list'">
            <ArrowLeft :size="18" />
          </button>
          <h2>编辑待办</h2>
          <ClipboardList :size="18" />
        </div>
        <input v-model="todoEditDraft.title" placeholder="待办标题" />
        <div class="inline-fields">
          <select v-model="todoEditDraft.taskScope">
            <option value="PERSONAL">个人任务</option>
            <option value="SHARED">家庭共享任务</option>
          </select>
          <input v-model="todoEditDraft.dueAt" type="datetime-local" placeholder="截止时间" />
        </div>
        <div v-if="todoEditDraft.taskScope === 'SHARED'" class="inline-fields">
          <select v-model="todoEditDraft.assigneeId">
            <option value="">暂不认领</option>
            <option v-for="member in familyMembers" :key="text(member, 'id')" :value="text(member, 'id')">
              {{ text(member, "display_name") }}
            </option>
          </select>
          <select v-model="todoEditDraft.taskType">
            <option value="TEMPORARY">临时任务</option>
            <option value="ROUTINE">周期任务</option>
          </select>
        </div>
        <textarea v-model="todoEditDraft.note" rows="4" placeholder="补充说明" />
        <button class="secondary-button" :disabled="isSubmitting('todo-update')" type="button" @click="submitTodoEdit()">
          <LoaderCircle v-if="isSubmitting('todo-update')" class="spin" :size="17" />
          <Check v-else :size="17" />
          <span>更新待办</span>
        </button>
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
        <button class="secondary-button" :disabled="isSubmitting('plant-create')" type="button" @click="submitPlant">
          <LoaderCircle v-if="isSubmitting('plant-create')" class="spin" :size="17" />
          <Plus v-else :size="17" />
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
        <button class="icon-button danger-icon-button" type="button" @click="deletePlant(numberValue(plant, 'id'))">
          <Trash2 :size="16" />
        </button>
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
        <button class="secondary-button" :disabled="isSubmitting('care-create')" type="button" @click="submitCareRecord">
          <LoaderCircle v-if="isSubmitting('care-create')" class="spin" :size="17" />
          <Plus v-else :size="17" />
          <span>保存养护</span>
        </button>
      </article>

      <article class="list-card">
        <div class="section-title">
          <h2>养护历史</h2>
          <span>{{ careRecords.length }}</span>
        </div>
        <p v-if="careRecords.length === 0" class="empty">选择花卉后查看养护历史</p>
        <div v-for="record in careRecords" :key="text(record, 'id')" class="feed-item feed-item-actions">
          <span>{{ careTypeLabel(text(record, "care_type")) }} · {{ text(record, "care_date") }}</span>
          <p>{{ text(record, "detail") || text(record, "raw_text") }}</p>
          <button class="text-button danger-button" type="button" @click="deleteCareRecord(numberValue(record, 'id'))">
            <Trash2 :size="15" />
            <span>删除</span>
          </button>
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
        <button class="secondary-button" :disabled="isSubmitting('shopping-create')" type="button" @click="submitShoppingItem">
          <LoaderCircle v-if="isSubmitting('shopping-create')" class="spin" :size="17" />
          <Plus v-else :size="17" />
          <span>加入清单</span>
        </button>
      </article>
      <article v-for="item in todoShoppingItems" :key="text(item, 'id')" class="shopping-card">
        <div class="shopping-head">
          <strong>{{ text(item, "name") }}</strong>
          <p>
            {{ text(item, "quantity") || "待确认数量" }} ·
            {{ categoryName(text(item, "category") || "OTHER") }}
          </p>
          <button class="text-button danger-button" type="button" @click="deleteShoppingItem(numberValue(item, 'id'))">
            <Trash2 :size="15" />
            <span>删除</span>
          </button>
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
          <button
            class="secondary-button compact-button"
            :disabled="isSubmitting(`shopping-check-${text(item, 'id')}`)"
            type="button"
            @click="checkShoppingItem(item)"
          >
            <LoaderCircle v-if="isSubmitting(`shopping-check-${text(item, 'id')}`)" class="spin" :size="17" />
            <Check v-else :size="17" />
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
        <div v-for="item in doneShoppingItems.slice(0, 8)" :key="text(item, 'id')" class="feed-item feed-item-actions">
          <span>{{ categoryName(text(item, "category") || "OTHER") }} · {{ formatDateTime(text(item, "purchased_at")) }}</span>
          <p>
            {{ text(item, "name") }}：{{ text(item, "actual_amount") || "0" }}
            <template v-if="text(item, 'buyer_id')"> · {{ memberName(text(item, "buyer_id")) }}</template>
          </p>
          <button class="text-button danger-button" type="button" @click="deleteShoppingItem(numberValue(item, 'id'))">
            <Trash2 :size="15" />
            <span>删除</span>
          </button>
        </div>
      </article>
    </section>

    <section v-if="activeTab === 'reminders'" class="view">
      <article v-if="reminderView === 'list'" class="list-card">
        <div class="section-title">
          <h2>提醒列表</h2>
          <button class="icon-button" type="button" aria-label="新增提醒" title="新增提醒" @click="startCreateReminder()">
            <Plus :size="18" />
          </button>
        </div>
        <p v-if="pendingReminders.length === 0" class="empty">暂时没有待处理提醒</p>
        <article v-for="reminder in pendingReminders" :key="text(reminder, 'id')" class="reminder-row">
          <div>
            <strong>{{ text(reminder, "title") }}</strong>
            <p>{{ formatDateTime(text(reminder, "due_at")) }}</p>
          </div>
          <div class="row-actions">
            <button
              class="icon-button"
              type="button"
              aria-label="完成提醒"
              title="完成提醒"
              @click="completeReminder(numberValue(reminder, 'id'))"
            >
              <Check :size="18" />
            </button>
            <button
              class="icon-button danger-icon-button"
              type="button"
              aria-label="删除提醒"
              title="删除提醒"
              @click="deleteReminder(numberValue(reminder, 'id'))"
            >
              <Trash2 :size="17" />
            </button>
          </div>
        </article>
      </article>

      <article v-else class="form-card">
        <div class="section-title">
          <button class="icon-button" type="button" aria-label="返回提醒列表" title="返回提醒列表" @click="reminderView = 'list'">
            <ArrowLeft :size="18" />
          </button>
          <h2>新建提醒</h2>
          <Bell :size="18" />
        </div>
        <input v-model="reminderDraft.title" placeholder="提醒事项" />
        <input v-model="reminderDraft.dueAt" type="datetime-local" />
        <textarea v-model="reminderDraft.description" rows="3" placeholder="备注" />
        <button class="secondary-button" :disabled="isSubmitting('reminder-create')" type="button" @click="submitReminder">
          <LoaderCircle v-if="isSubmitting('reminder-create')" class="spin" :size="17" />
          <Plus v-else :size="17" />
          <span>保存提醒</span>
        </button>
      </article>
    </section>

    <section v-if="activeTab === 'finance'" class="view">
      <div class="summary-strip">
        <article class="mini-metric">
          <span>总支出</span>
          <strong>{{ numberValue(financeOverview, "totalExpense") }}</strong>
        </article>
        <article class="mini-metric">
          <span>本月支出</span>
          <strong>{{ numberValue(financeOverview, "monthExpense") }}</strong>
        </article>
        <article class="mini-metric">
          <span>本周支出</span>
          <strong>{{ numberValue(financeOverview, "weekExpense") }}</strong>
        </article>
      </div>
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
        <button class="secondary-button" :disabled="isSubmitting('finance-create')" type="button" @click="submitFinanceRecord">
          <LoaderCircle v-if="isSubmitting('finance-create')" class="spin" :size="17" />
          <Plus v-else :size="17" />
          <span>保存支出</span>
        </button>
      </article>

      <article class="list-card">
        <div class="section-title">
          <h2>支出记录</h2>
          <span>{{ expenseRecords.length }}</span>
        </div>
        <p v-if="expenseRecords.length === 0" class="empty">还没有支出记录</p>
        <div v-for="record in expenseRecords" :key="text(record, 'id')" class="feed-item feed-item-actions">
          <span>{{ categoryName(text(record, "category") || "OTHER") }} · {{ text(record, "occurred_on") }}</span>
          <p>{{ text(record, "title") }}：{{ text(record, "amount") }}</p>
          <button class="text-button danger-button" type="button" @click="deleteFinanceRecord(numberValue(record, 'id'))">
            <Trash2 :size="15" />
            <span>删除</span>
          </button>
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
        <button class="secondary-button" :disabled="isSubmitting('private-create')" type="button" @click="submitPrivateMessage">
          <LoaderCircle v-if="isSubmitting('private-create')" class="spin" :size="17" />
          <Send v-else :size="17" />
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
            {{ visibilityLabel(text(item, "visibility")) }} ·
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
      <article v-if="memoryView === 'list'" class="list-card">
        <div class="section-title">
          <h2>时刻墙</h2>
          <button class="icon-button" type="button" aria-label="新增时刻" title="新增时刻" @click="startCreateMemory()">
            <Plus :size="18" />
          </button>
        </div>
        <p v-if="albumPhotos.length === 0" class="empty">还没有时刻记录</p>
        <article v-for="entry in albumPhotos" :key="text(entry, 'id')" class="memory-item">
          <img v-if="text(entry, 'image_url')" :src="text(entry, 'image_url')" :alt="text(entry, 'title')" />
          <div class="memory-copy">
            <span>
              {{ memoryEntryTypeLabel(text(entry, "entry_type")) }} ·
              {{ formatDateTime(text(entry, "taken_on") || text(entry, "created_at")) }}
            </span>
            <strong>{{ text(entry, "title") }}</strong>
            <p v-if="text(entry, 'description')">{{ text(entry, "description") }}</p>
            <p v-if="text(entry, 'wish_text')" class="muted">愿望：{{ text(entry, "wish_text") }}</p>
            <p v-if="text(entry, 'next_remind_at')" class="muted">下次提醒：{{ formatDateTime(text(entry, "next_remind_at")) }}</p>
          </div>
          <div class="row-actions">
            <button class="icon-button" type="button" aria-label="编辑时刻" title="编辑时刻" @click="startEditMemory(entry)">
              <Pencil :size="16" />
            </button>
            <button
              class="icon-button danger-icon-button"
              type="button"
              aria-label="删除时刻"
              title="删除时刻"
              @click="deleteAlbumPhoto(numberValue(entry, 'id'))"
            >
              <Trash2 :size="16" />
            </button>
          </div>
        </article>
      </article>

      <article v-else-if="memoryView === 'create'" class="form-card album-form">
        <div class="section-title">
          <button class="icon-button" type="button" aria-label="返回时刻墙" title="返回时刻墙" @click="memoryView = 'list'">
            <ArrowLeft :size="18" />
          </button>
          <h2>新增时刻</h2>
          <Camera :size="18" />
        </div>
        <select v-model="albumDraft.entryType">
          <option value="PHOTO">照片</option>
          <option value="MOOD">心情</option>
          <option value="ANNIVERSARY">纪念日</option>
          <option value="BIRTHDAY">生日</option>
        </select>
        <label v-if="albumDraft.entryType === 'PHOTO'" class="file-pick">
          <ImagePlus :size="18" />
          <span>{{ albumFile ? albumFile.name : "选择图片" }}</span>
          <input type="file" accept="image/*" @change="albumFile = selectedFile($event)" />
        </label>
        <input v-model="albumDraft.title" placeholder="标题" />
        <input v-model="albumDraft.imageUrl" placeholder="图片链接（可选）" />
        <textarea v-model="albumDraft.description" rows="3" placeholder="文字记录" />
        <input v-model="albumDraft.takenOn" type="date" />
        <textarea
          v-if="['ANNIVERSARY', 'BIRTHDAY'].includes(albumDraft.entryType)"
          v-model="albumDraft.wishText"
          rows="2"
          placeholder="愿望清单"
        />
        <label v-if="['ANNIVERSARY', 'BIRTHDAY'].includes(albumDraft.entryType)" class="check-row">
          <input v-model="albumDraft.reminderEnabled" type="checkbox" />
          <span>开启周年提醒</span>
        </label>
        <input v-if="['ANNIVERSARY', 'BIRTHDAY'].includes(albumDraft.entryType)" v-model="albumDraft.reminderDaysBefore" inputmode="numeric" placeholder="提前几天提醒" />
        <button class="secondary-button" :disabled="isSubmitting('memory-create')" type="button" @click="submitMemoryCreate()">
          <LoaderCircle v-if="isSubmitting('memory-create')" class="spin" :size="17" />
          <ImagePlus v-else :size="17" />
          <span>保存时刻</span>
        </button>
      </article>

      <article v-else class="form-card album-form">
        <div class="section-title">
          <button class="icon-button" type="button" aria-label="返回时刻墙" title="返回时刻墙" @click="memoryView = 'list'">
            <ArrowLeft :size="18" />
          </button>
          <h2>编辑时刻</h2>
          <Camera :size="18" />
        </div>
        <select v-model="albumEditDraft.entryType">
          <option value="PHOTO">照片</option>
          <option value="MOOD">心情</option>
          <option value="ANNIVERSARY">纪念日</option>
          <option value="BIRTHDAY">生日</option>
        </select>
        <label v-if="albumEditDraft.entryType === 'PHOTO'" class="file-pick">
          <ImagePlus :size="18" />
          <span>{{ albumFile ? albumFile.name : "更换图片" }}</span>
          <input type="file" accept="image/*" @change="albumFile = selectedFile($event)" />
        </label>
        <input v-model="albumEditDraft.title" placeholder="标题" />
        <input v-model="albumEditDraft.imageUrl" placeholder="图片链接（可选）" />
        <textarea v-model="albumEditDraft.description" rows="3" placeholder="文字记录" />
        <input v-model="albumEditDraft.takenOn" type="date" />
        <textarea
          v-if="['ANNIVERSARY', 'BIRTHDAY'].includes(albumEditDraft.entryType)"
          v-model="albumEditDraft.wishText"
          rows="2"
          placeholder="愿望清单"
        />
        <label v-if="['ANNIVERSARY', 'BIRTHDAY'].includes(albumEditDraft.entryType)" class="check-row">
          <input v-model="albumEditDraft.reminderEnabled" type="checkbox" />
          <span>开启周年提醒</span>
        </label>
        <input
          v-if="['ANNIVERSARY', 'BIRTHDAY'].includes(albumEditDraft.entryType)"
          v-model="albumEditDraft.reminderDaysBefore"
          inputmode="numeric"
          placeholder="提前几天提醒"
        />
        <button class="secondary-button" :disabled="isSubmitting('memory-update')" type="button" @click="submitMemoryEdit()">
          <LoaderCircle v-if="isSubmitting('memory-update')" class="spin" :size="17" />
          <ImagePlus v-else :size="17" />
          <span>更新时刻</span>
        </button>
      </article>
    </section>

    <section v-if="activeTab === 'inventory'" class="view">
      <div class="segmented-control">
        <button
          :class="['segment-button', { active: inventorySegment === 'items' }]"
          type="button"
          @click="inventorySegment = 'items'"
        >
          物资
        </button>
        <button
          :class="['segment-button', { active: inventorySegment === 'votes' }]"
          type="button"
          @click="inventorySegment = 'votes'"
        >
          投票
        </button>
      </div>

      <template v-if="inventorySegment === 'items'">
        <article v-if="inventoryView === 'list'" class="list-card">
          <div class="section-title">
            <h2>库存物资</h2>
            <button class="icon-button" type="button" aria-label="新增物资" title="新增物资" @click="startCreateInventoryItem()">
              <Plus :size="18" />
            </button>
          </div>
          <p v-if="inventoryItems.length === 0" class="empty">还没有物资记录</p>
          <article v-for="item in inventoryItems" :key="text(item, 'id')" class="feed-item feed-item-actions">
            <div class="feed-main">
              <span>
                {{ inventoryItemTypeLabel(text(item, "item_type")) }} · {{ text(item, "status") === "LOW" ? "库存偏低" : "库存正常" }}
              </span>
              <p>{{ text(item, "name") }}</p>
              <small>
                {{ text(item, "quantity") || "0" }}{{ text(item, "unit") }}
                <template v-if="text(item, 'expires_on')"> · {{ text(item, "expires_on") }} 到期</template>
              </small>
              <small v-if="text(item, 'note')">{{ text(item, "note") }}</small>
            </div>
            <div class="row-actions">
              <button class="icon-button" type="button" aria-label="编辑物资" title="编辑物资" @click="startEditInventoryItem(item)">
                <Pencil :size="16" />
              </button>
              <button
                class="icon-button danger-icon-button"
                type="button"
                aria-label="删除物资"
                title="删除物资"
                @click="deleteInventoryItem(numberValue(item, 'id'))"
              >
                <Trash2 :size="16" />
              </button>
            </div>
          </article>
        </article>

        <article v-else-if="inventoryView === 'create'" class="form-card">
          <div class="section-title">
            <button class="icon-button" type="button" aria-label="返回物资列表" title="返回物资列表" @click="inventoryView = 'list'">
              <ArrowLeft :size="18" />
            </button>
            <h2>新增物资</h2>
            <Package :size="18" />
          </div>
          <input v-model="inventoryDraft.name" placeholder="名称" />
          <div class="inline-fields">
            <select v-model="inventoryDraft.itemType">
              <option value="SUPPLY">消耗品</option>
              <option value="FOOD">食品</option>
            </select>
            <input v-model="inventoryDraft.category" placeholder="分类" />
          </div>
          <div class="inline-fields">
            <input v-model="inventoryDraft.quantity" placeholder="数量" />
            <input v-model="inventoryDraft.unit" placeholder="单位" />
          </div>
          <div class="inline-fields">
            <input v-model="inventoryDraft.lowStockThreshold" placeholder="低库存阈值" />
            <input v-model="inventoryDraft.reminderDaysBefore" inputmode="numeric" placeholder="过期提前提醒天数" />
          </div>
          <input v-model="inventoryDraft.expiresOn" type="date" />
          <textarea v-model="inventoryDraft.note" rows="3" placeholder="备注" />
          <button class="secondary-button" :disabled="isSubmitting('inventory-create')" type="button" @click="submitInventoryCreate()">
            <LoaderCircle v-if="isSubmitting('inventory-create')" class="spin" :size="17" />
            <Check v-else :size="17" />
            <span>保存物资</span>
          </button>
        </article>

        <article v-else class="form-card">
          <div class="section-title">
            <button class="icon-button" type="button" aria-label="返回物资列表" title="返回物资列表" @click="inventoryView = 'list'">
              <ArrowLeft :size="18" />
            </button>
            <h2>编辑物资</h2>
            <Package :size="18" />
          </div>
          <input v-model="inventoryEditDraft.name" placeholder="名称" />
          <div class="inline-fields">
            <select v-model="inventoryEditDraft.itemType">
              <option value="SUPPLY">消耗品</option>
              <option value="FOOD">食品</option>
            </select>
            <input v-model="inventoryEditDraft.category" placeholder="分类" />
          </div>
          <div class="inline-fields">
            <input v-model="inventoryEditDraft.quantity" placeholder="数量" />
            <input v-model="inventoryEditDraft.unit" placeholder="单位" />
          </div>
          <div class="inline-fields">
            <input v-model="inventoryEditDraft.lowStockThreshold" placeholder="低库存阈值" />
            <input v-model="inventoryEditDraft.reminderDaysBefore" inputmode="numeric" placeholder="过期提前提醒天数" />
          </div>
          <input v-model="inventoryEditDraft.expiresOn" type="date" />
          <textarea v-model="inventoryEditDraft.note" rows="3" placeholder="备注" />
          <button class="secondary-button" :disabled="isSubmitting('inventory-update')" type="button" @click="submitInventoryEdit()">
            <LoaderCircle v-if="isSubmitting('inventory-update')" class="spin" :size="17" />
            <Check v-else :size="17" />
            <span>更新物资</span>
          </button>
        </article>
      </template>

      <template v-else>
        <article v-if="voteView === 'list'" class="list-card">
          <div class="section-title">
            <h2>家庭投票</h2>
            <button class="icon-button" type="button" aria-label="新增投票" title="新增投票" @click="startCreateVote()">
              <Plus :size="18" />
            </button>
          </div>
          <p v-if="familyVotes.length === 0" class="empty">还没有投票</p>
          <article v-for="vote in openFamilyVotes" :key="text(vote, 'id')" class="vote-card">
            <div class="section-title">
              <div>
                <h3>{{ text(vote, "title") }}</h3>
                <span>{{ voteCategoryLabel(text(vote, "vote_category")) }}</span>
              </div>
              <div class="row-actions">
                <button class="icon-button" type="button" aria-label="随机决定" title="随机决定" @click="decideVote(numberValue(vote, 'id'))">
                  <Check :size="16" />
                </button>
                <button
                  class="icon-button danger-icon-button"
                  type="button"
                  aria-label="删除投票"
                  title="删除投票"
                  @click="deleteVote(numberValue(vote, 'id'))"
                >
                  <Trash2 :size="16" />
                </button>
              </div>
            </div>
            <div class="option-chip-group">
              <button v-for="option in voteOptions(vote)" :key="text(option, 'id')" class="option-chip" type="button" @click="submitVoteChoice(numberValue(vote, 'id'), numberValue(option, 'id'))">
                <span>{{ text(option, "option_text") }}</span>
                <strong>{{ numberValue(option, "voteCount") }}</strong>
              </button>
            </div>
          </article>
          <div v-if="historyFamilyVotes.length > 0" class="subsection">
            <div class="section-title">
              <h2>历史记录</h2>
              <span>{{ historyFamilyVotes.length }}</span>
            </div>
            <div v-for="vote in historyFamilyVotes" :key="`history-${text(vote, 'id')}`" class="feed-item">
              <span>{{ voteCategoryLabel(text(vote, "vote_category")) }}</span>
              <p>{{ text(vote, "title") }}</p>
              <small>结果：{{ text(vote, "decidedOptionText") || "待确认" }}</small>
            </div>
          </div>
        </article>

        <article v-else class="form-card">
          <div class="section-title">
            <button class="icon-button" type="button" aria-label="返回投票列表" title="返回投票列表" @click="voteView = 'list'">
              <ArrowLeft :size="18" />
            </button>
            <h2>新增投票</h2>
            <Users :size="18" />
          </div>
          <div class="section-actions-grid">
            <button class="secondary-button compact-button" type="button" @click="applyVoteTemplate('EAT')">
              <span>吃什么模板</span>
            </button>
            <button class="secondary-button compact-button" type="button" @click="applyVoteTemplate('PLAY')">
              <span>玩什么模板</span>
            </button>
          </div>
          <input v-model="voteDraft.title" placeholder="投票标题" />
          <select v-model="voteDraft.voteCategory">
            <option value="CUSTOM">自定义</option>
            <option value="EAT">吃什么</option>
            <option value="PLAY">玩什么</option>
          </select>
          <textarea v-model="voteDraft.optionsText" rows="6" placeholder="每行一个选项" />
          <button class="secondary-button" :disabled="isSubmitting('vote-create')" type="button" @click="submitVoteCreate()">
            <LoaderCircle v-if="isSubmitting('vote-create')" class="spin" :size="17" />
            <Plus v-else :size="17" />
            <span>创建投票</span>
          </button>
        </article>
      </template>
    </section>

    <section v-if="activeTab === 'recipes'" class="view">
      <article v-if="recipeView === 'list'" class="list-card">
        <div class="section-title">
          <h2>菜谱库</h2>
          <div class="row-actions">
            <button class="icon-button" type="button" aria-label="查看周餐单" title="查看周餐单" @click="openMealPlanWeek()">
              <ChefHat :size="18" />
            </button>
            <button class="icon-button" type="button" aria-label="新增菜谱" title="新增菜谱" @click="startCreateRecipe()">
              <Plus :size="18" />
            </button>
          </div>
        </div>
        <p v-if="recipes.length === 0" class="empty">还没有菜谱</p>
        <article v-for="recipe in recipes" :key="text(recipe, 'id')" class="feed-item feed-item-actions">
          <div class="feed-main">
            <span>{{ mealTypeLabel(text(recipe, "meal_type")) }}</span>
            <p>{{ text(recipe, "title") }}</p>
            <small>{{ text(recipe, "ingredients_text") || "还没有填写原料" }}</small>
          </div>
          <div class="row-actions">
            <button class="icon-button" type="button" aria-label="编辑菜谱" title="编辑菜谱" @click="startEditRecipe(recipe)">
              <Pencil :size="16" />
            </button>
            <button
              class="icon-button danger-icon-button"
              type="button"
              aria-label="删除菜谱"
              title="删除菜谱"
              @click="deleteRecipe(numberValue(recipe, 'id'))"
            >
              <Trash2 :size="16" />
            </button>
          </div>
        </article>
      </article>

      <article v-else-if="recipeView === 'create'" class="form-card">
        <div class="section-title">
          <button class="icon-button" type="button" aria-label="返回菜谱列表" title="返回菜谱列表" @click="recipeView = 'list'">
            <ArrowLeft :size="18" />
          </button>
          <h2>新增菜谱</h2>
          <ChefHat :size="18" />
        </div>
        <input v-model="recipeDraft.title" placeholder="菜谱名称" />
        <select v-model="recipeDraft.mealType">
          <option value="BREAKFAST">早餐</option>
          <option value="LUNCH">午餐</option>
          <option value="DINNER">晚餐</option>
          <option value="SNACK">加餐</option>
        </select>
        <textarea v-model="recipeDraft.ingredientsText" rows="5" placeholder="原料，每行一项" />
        <textarea v-model="recipeDraft.stepsText" rows="5" placeholder="做法步骤" />
        <button class="secondary-button" :disabled="isSubmitting('recipe-create')" type="button" @click="submitRecipeCreate()">
          <LoaderCircle v-if="isSubmitting('recipe-create')" class="spin" :size="17" />
          <Check v-else :size="17" />
          <span>保存菜谱</span>
        </button>
      </article>

      <article v-else-if="recipeView === 'edit'" class="form-card">
        <div class="section-title">
          <button class="icon-button" type="button" aria-label="返回菜谱列表" title="返回菜谱列表" @click="recipeView = 'list'">
            <ArrowLeft :size="18" />
          </button>
          <h2>编辑菜谱</h2>
          <ChefHat :size="18" />
        </div>
        <input v-model="recipeEditDraft.title" placeholder="菜谱名称" />
        <select v-model="recipeEditDraft.mealType">
          <option value="BREAKFAST">早餐</option>
          <option value="LUNCH">午餐</option>
          <option value="DINNER">晚餐</option>
          <option value="SNACK">加餐</option>
        </select>
        <textarea v-model="recipeEditDraft.ingredientsText" rows="5" placeholder="原料，每行一项" />
        <textarea v-model="recipeEditDraft.stepsText" rows="5" placeholder="做法步骤" />
        <button class="secondary-button" :disabled="isSubmitting('recipe-update')" type="button" @click="submitRecipeEdit()">
          <LoaderCircle v-if="isSubmitting('recipe-update')" class="spin" :size="17" />
          <Check v-else :size="17" />
          <span>更新菜谱</span>
        </button>
      </article>

      <article v-else-if="recipeView === 'week'" class="list-card">
        <div class="section-title">
          <button class="icon-button" type="button" aria-label="返回菜谱列表" title="返回菜谱列表" @click="recipeView = 'list'">
            <ArrowLeft :size="18" />
          </button>
          <h2>{{ mealPlanWeekStart }}</h2>
          <button class="icon-button" type="button" aria-label="新增餐单" title="新增餐单" @click="startCreateMealPlan()">
            <Plus :size="18" />
          </button>
        </div>
        <div class="section-actions-grid">
          <button class="secondary-button compact-button" type="button" @click="changeMealPlanWeek(-1)">
            <span>上一周</span>
          </button>
          <button class="secondary-button compact-button" type="button" @click="changeMealPlanWeek(1)">
            <span>下一周</span>
          </button>
        </div>
        <label class="check-row">
          <input v-model="weeklyPlanDraft.enableReminder" type="checkbox" />
          <span>生成时同步创建晚饭提醒</span>
        </label>
        <input v-model="weeklyPlanDraft.remindTime" type="time" />
        <div class="section-actions-grid">
          <button class="secondary-button compact-button" :disabled="isSubmitting('meal-plan-generate')" type="button" @click="generateWeeklyMealPlans()">
            <LoaderCircle v-if="isSubmitting('meal-plan-generate')" class="spin" :size="16" />
            <span>一键生成本周餐单</span>
          </button>
          <button class="secondary-button compact-button" :disabled="isSubmitting('meal-plan-shopping-sync')" type="button" @click="syncMealPlanShoppingList()">
            <LoaderCircle v-if="isSubmitting('meal-plan-shopping-sync')" class="spin" :size="16" />
            <span>同步购物清单</span>
          </button>
        </div>
        <p v-if="mealPlans.length === 0" class="empty">这一周还没有餐单</p>
        <article v-for="plan in mealPlans" :key="text(plan, 'id')" class="feed-item feed-item-actions">
          <div class="feed-main">
            <span>{{ text(plan, "planned_on") }} · {{ mealTypeLabel(text(plan, "meal_slot")) }}</span>
            <p>{{ text(plan, "title_snapshot") }}</p>
            <small v-if="text(plan, 'remind_at')">提醒：{{ formatDateTime(text(plan, "remind_at")) }}</small>
          </div>
          <button
            class="icon-button danger-icon-button"
            type="button"
            aria-label="删除餐单"
            title="删除餐单"
            @click="deleteMealPlan(numberValue(plan, 'id'))"
          >
            <Trash2 :size="16" />
          </button>
        </article>
      </article>

      <article v-else class="form-card">
        <div class="section-title">
          <button class="icon-button" type="button" aria-label="返回一周餐单" title="返回一周餐单" @click="recipeView = 'week'">
            <ArrowLeft :size="18" />
          </button>
          <h2>新增餐单</h2>
          <ChefHat :size="18" />
        </div>
        <input v-model="mealPlanDraft.plannedOn" type="date" />
        <select v-model="mealPlanDraft.mealSlot">
          <option value="BREAKFAST">早餐</option>
          <option value="LUNCH">午餐</option>
          <option value="DINNER">晚餐</option>
          <option value="SNACK">加餐</option>
        </select>
        <select v-model="mealPlanDraft.recipeId">
          <option value="">不关联菜谱，手动填写标题</option>
          <option v-for="recipe in recipes" :key="text(recipe, 'id')" :value="text(recipe, 'id')">
            {{ text(recipe, "title") }}
          </option>
        </select>
        <input v-model="mealPlanDraft.titleSnapshot" placeholder="餐单标题（可选）" />
        <input v-model="mealPlanDraft.remindAt" type="datetime-local" />
        <button class="secondary-button" :disabled="isSubmitting('meal-plan-create')" type="button" @click="submitMealPlanCreate()">
          <LoaderCircle v-if="isSubmitting('meal-plan-create')" class="spin" :size="17" />
          <Plus v-else :size="17" />
          <span>保存餐单</span>
        </button>
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
      <article v-if="profileView === 'detail'" class="list-card">
        <div class="section-title">
          <h2>个人信息</h2>
          <button class="icon-button" type="button" aria-label="编辑资料" title="编辑资料" @click="startEditProfile()">
            <Pencil :size="17" />
          </button>
        </div>
        <div class="profile-row"><span>昵称</span><strong>{{ text(currentUser, "displayName") }}</strong></div>
        <div class="profile-row"><span>角色</span><strong>{{ roleLabel(text(currentMember ?? {}, "role_code")) }}</strong></div>
        <div class="profile-row"><span>登录名</span><strong>{{ text(currentUser, "username") }}</strong></div>
        <div class="profile-row profile-row-password">
          <span>密码</span>
          <div class="password-preview">
            <strong>{{ profilePasswordHintVisible ? "原密码不支持查看" : "******" }}</strong>
            <button
              class="icon-button"
              type="button"
              aria-label="切换密码提示"
              title="切换密码提示"
              @click="profilePasswordHintVisible = !profilePasswordHintVisible"
            >
              <Eye v-if="!profilePasswordHintVisible" :size="16" />
              <EyeOff v-else :size="16" />
            </button>
          </div>
        </div>
        <div class="profile-row"><span>家庭</span><strong>{{ familyTitle }}</strong></div>
        <div class="profile-row"><span>简介</span><strong>{{ text(currentMember ?? {}, "bio") || "还没有填写" }}</strong></div>
      </article>

      <article v-else class="form-card">
        <div class="section-title">
          <button class="icon-button" type="button" aria-label="返回个人信息" title="返回个人信息" @click="profileView = 'detail'">
            <ArrowLeft :size="18" />
          </button>
          <h2>编辑资料</h2>
          <User :size="18" />
        </div>
        <label class="file-pick">
          <ImagePlus :size="18" />
          <span>{{ profileAvatarFile ? profileAvatarFile.name : "更换头像" }}</span>
          <input type="file" accept="image/*" @change="profileAvatarFile = selectedFile($event)" />
        </label>
        <input v-model="profileDraft.displayName" placeholder="昵称" />
        <input v-model="profileDraft.roleCode" list="role-options" placeholder="角色，如女主人" />
        <input v-model="profileDraft.username" placeholder="登录名" />
        <div class="password-field">
          <input
            v-model="profileDraft.password"
            :type="profilePasswordVisible ? 'text' : 'password'"
            placeholder="新密码（可不填）"
          />
          <button
            class="icon-button"
            type="button"
            aria-label="切换密码显示"
            title="切换密码显示"
            @click="profilePasswordVisible = !profilePasswordVisible"
          >
            <Eye v-if="!profilePasswordVisible" :size="16" />
            <EyeOff v-else :size="16" />
          </button>
        </div>
        <textarea v-model="profileDraft.bio" rows="3" placeholder="个人简介" />
        <input v-model="profileDraft.avatarColor" type="color" />
        <button class="secondary-button" :disabled="isSubmitting('profile-update')" type="button" @click="submitProfile">
          <LoaderCircle v-if="isSubmitting('profile-update')" class="spin" :size="17" />
          <Check v-else :size="17" />
          <span>{{ profileAvatarFile ? "上传并保存资料" : "保存资料" }}</span>
        </button>
      </article>
      <button class="primary-button" type="button" @click="logout()">
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
          <p>{{ roleLabel(text(member, "role_code")) }} · {{ text(member, "username") || "未开通登录" }}</p>
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
        <input v-model="memberDraft.roleCode" list="role-options" placeholder="角色，如女主人" />
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
        <button class="secondary-button" :disabled="isSubmitting('member-create')" type="button" @click="submitMember">
          <LoaderCircle v-if="isSubmitting('member-create')" class="spin" :size="17" />
          <Plus v-else :size="17" />
          <span>{{ memberAvatarFile ? "上传并添加成员" : "添加成员" }}</span>
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
        <input v-model="memberEditDraft.roleCode" list="role-options" placeholder="角色，如女主人" />
        <div :class="canEditMemberPassword ? 'inline-fields' : 'single-field'">
          <input v-model="memberEditDraft.username" placeholder="登录名" />
          <div v-if="canEditMemberPassword" class="password-field">
            <input
              v-model="memberEditDraft.password"
              :type="memberEditPasswordVisible ? 'text' : 'password'"
              placeholder="新密码（可不填）"
            />
            <button
              class="icon-button"
              type="button"
              aria-label="切换密码显示"
              title="切换密码显示"
              @click="memberEditPasswordVisible = !memberEditPasswordVisible"
            >
              <Eye v-if="!memberEditPasswordVisible" :size="16" />
              <EyeOff v-else :size="16" />
            </button>
          </div>
        </div>
        <textarea v-model="memberEditDraft.bio" rows="3" placeholder="成员备注" />
        <input v-model="memberEditDraft.avatarColor" type="color" />
        <button class="secondary-button" :disabled="isSubmitting('member-update')" type="button" @click="submitMemberEdit">
          <LoaderCircle v-if="isSubmitting('member-update')" class="spin" :size="17" />
          <Check v-else :size="17" />
          <span>{{ memberEditAvatarFile ? "上传并保存成员" : "保存成员" }}</span>
        </button>
      </article>
    </section>

    <section v-if="activeTab === 'pets'" class="view">
      <article v-if="petView === 'list'" class="list-card pet-focus">
        <div class="section-title">
          <h2>宠物列表</h2>
          <button class="icon-button" type="button" aria-label="新增宠物" title="新增宠物" @click="startCreatePet()">
            <Plus :size="18" />
          </button>
        </div>
        <p v-if="pets.length === 0" class="empty">还没有宠物档案，点右上角加号创建第一只宠物。</p>
        <div v-else class="pet-stack">
          <article
            v-for="pet in pets"
            :key="text(pet, 'id')"
            class="pet-card"
            :class="{ active: text(pet, 'id') === selectedPetId }"
            @click="openPetDetail(text(pet, 'id'))"
          >
            <img v-if="text(pet, 'avatar_url')" :src="text(pet, 'avatar_url')" :alt="text(pet, 'name')" />
            <span v-else class="pet-placeholder"><PawPrint :size="22" /></span>
            <div>
              <strong>{{ text(pet, "name") }}</strong>
              <p>{{ speciesLabel(text(pet, "species")) }} · {{ text(pet, "breed") || "未记录品种" }}</p>
            </div>
          </article>
        </div>
      </article>

      <article v-else-if="petView === 'create'" class="form-card pet-form">
        <div class="section-title">
          <button class="icon-button" type="button" aria-label="返回宠物列表" title="返回宠物列表" @click="petView = 'list'">
            <ArrowLeft :size="18" />
          </button>
          <h2>新增宠物档案</h2>
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
        <button class="secondary-button" :disabled="isSubmitting('pet-create')" type="button" @click="submitPet">
          <LoaderCircle v-if="isSubmitting('pet-create')" class="spin" :size="17" />
          <Plus v-else :size="17" />
          <span>{{ petAvatarFile ? "上传并创建档案" : "创建宠物档案" }}</span>
        </button>
      </article>

      <article v-else-if="petView === 'detail' && currentPet" class="list-card pet-focus">
        <div class="section-title">
          <h2>{{ text(currentPet, "name") }} 的档案</h2>
          <div class="row-actions">
            <button class="icon-button" type="button" aria-label="编辑宠物" title="编辑宠物" @click="startEditPet()">
              <Pencil :size="17" />
            </button>
            <button
              class="icon-button danger-icon-button"
              :disabled="isSubmitting(`pet-delete-${numberValue(currentPet, 'id')}`)"
              type="button"
              aria-label="删除宠物"
              title="删除宠物"
              @click="deletePet(numberValue(currentPet, 'id'))"
            >
              <Trash2 :size="17" />
            </button>
          </div>
        </div>
        <div class="pet-overview">
          <img
            v-if="text(currentPet, 'avatar_url')"
            :src="text(currentPet, 'avatar_url')"
            :alt="text(currentPet, 'name')"
          />
          <span v-else class="pet-placeholder pet-hero">
            <PawPrint :size="28" />
          </span>
          <div>
            <strong>{{ text(currentPet, "name") }}</strong>
            <p>{{ speciesLabel(text(currentPet, "species")) }} · {{ text(currentPet, "breed") || "未记录品种" }}</p>
            <p>{{ petGenderLabel(text(currentPet, "gender")) }} · {{ text(currentPet, "birthday") || "未记录生日" }}</p>
            <p>{{ text(currentPet, "note") || "还没有补充备注" }}</p>
          </div>
        </div>
        <div class="summary-strip">
          <article class="mini-metric">
            <span>照片</span>
            <strong>{{ petPhotos.length }}</strong>
          </article>
          <article class="mini-metric">
            <span>医疗记录</span>
            <strong>{{ petMedicalRecords.length }}</strong>
          </article>
          <article class="mini-metric">
            <span>待提醒</span>
            <strong>{{ petMedicalRecords.filter((item) => text(item, "next_due_at")).length }}</strong>
          </article>
        </div>
        <div class="section-actions-grid">
          <button class="secondary-button" type="button" @click="openPetPhotos()">
            <Camera :size="17" />
            <span>照片列表</span>
          </button>
          <button class="secondary-button" type="button" @click="openPetMedical()">
            <Stethoscope :size="17" />
            <span>医疗记录</span>
          </button>
        </div>
      </article>

      <article v-else-if="petView === 'edit'" class="form-card pet-form">
        <div class="section-title">
          <button class="icon-button" type="button" aria-label="返回宠物详情" title="返回宠物详情" @click="petView = 'detail'">
            <ArrowLeft :size="18" />
          </button>
          <h2>编辑宠物档案</h2>
          <Pencil :size="18" />
        </div>
        <label class="file-pick">
          <ImagePlus :size="18" />
          <span>{{ petEditAvatarFile ? petEditAvatarFile.name : "更换宠物头像" }}</span>
          <input type="file" accept="image/*" @change="petEditAvatarFile = selectedFile($event)" />
        </label>
        <input v-model="petEditDraft.name" placeholder="名字" />
        <div class="inline-fields">
          <select v-model="petEditDraft.species">
            <option value="CAT">猫</option>
            <option value="DOG">狗</option>
            <option value="OTHER">其他</option>
          </select>
          <input v-model="petEditDraft.breed" placeholder="品种" />
        </div>
        <div class="inline-fields">
          <select v-model="petEditDraft.gender">
            <option value="">性别</option>
            <option value="MALE">男孩</option>
            <option value="FEMALE">女孩</option>
            <option value="UNKNOWN">不确定</option>
          </select>
          <input v-model="petEditDraft.birthday" type="date" />
        </div>
        <textarea v-model="petEditDraft.note" rows="3" placeholder="性格、饮食偏好、注意事项" />
        <button class="secondary-button" :disabled="isSubmitting('pet-update')" type="button" @click="submitPetEdit">
          <LoaderCircle v-if="isSubmitting('pet-update')" class="spin" :size="17" />
          <Check v-else :size="17" />
          <span>{{ petEditAvatarFile ? "上传并保存档案" : "保存档案" }}</span>
        </button>
      </article>

      <article v-else-if="petView === 'photos'" class="album-grid-card">
        <div class="section-title">
          <button class="icon-button" type="button" aria-label="返回宠物详情" title="返回宠物详情" @click="petView = 'detail'">
            <ArrowLeft :size="18" />
          </button>
          <h2>{{ text(currentPet ?? {}, "name") }} 的照片</h2>
          <button class="icon-button" type="button" aria-label="新增照片" title="新增照片" @click="openPetPhotoCreate()">
            <Plus :size="18" />
          </button>
        </div>
        <p v-if="petPhotos.length === 0" class="empty">还没有宠物照片</p>
        <div class="album-grid">
          <article v-for="photo in petPhotos" :key="text(photo, 'id')" class="photo-card">
            <img :src="text(photo, 'image_url')" :alt="text(photo, 'description') || '宠物照片'" />
            <div>
              <strong>{{ formatDateTime(text(photo, "taken_on") || text(photo, "created_at")) }}</strong>
              <p>{{ text(photo, "description") || "未填写备注" }}</p>
              <button
                class="text-button danger-button"
                :disabled="isSubmitting(`pet-photo-delete-${numberValue(photo, 'id')}`)"
                type="button"
                @click="deletePetPhoto(numberValue(photo, 'id'))"
              >
                <Trash2 :size="15" />
                <span>删除</span>
              </button>
            </div>
          </article>
        </div>
      </article>

      <article v-else-if="petView === 'photoCreate'" class="form-card">
        <div class="section-title">
          <button class="icon-button" type="button" aria-label="返回照片列表" title="返回照片列表" @click="petView = 'photos'">
            <ArrowLeft :size="18" />
          </button>
          <h2>新增宠物照片</h2>
          <ImagePlus :size="18" />
        </div>
        <label class="file-pick">
          <ImagePlus :size="18" />
          <span>{{ petPhotoFile ? petPhotoFile.name : "选择照片" }}</span>
          <input type="file" accept="image/*" @change="petPhotoFile = selectedFile($event)" />
        </label>
        <textarea v-model="petPhotoDraft.description" rows="2" placeholder="照片备注" />
        <input v-model="petPhotoDraft.takenOn" type="date" />
        <button class="secondary-button" :disabled="isSubmitting('pet-photo-create')" type="button" @click="submitPetPhoto">
          <LoaderCircle v-if="isSubmitting('pet-photo-create')" class="spin" :size="17" />
          <ImagePlus v-else :size="17" />
          <span>{{ petPhotoFile ? "上传并保存照片" : "保存照片" }}</span>
        </button>
      </article>

      <article v-else-if="petView === 'medical'" class="list-card">
        <div class="section-title">
          <button class="icon-button" type="button" aria-label="返回宠物详情" title="返回宠物详情" @click="petView = 'detail'">
            <ArrowLeft :size="18" />
          </button>
          <h2>{{ text(currentPet ?? {}, "name") }} 的医疗记录</h2>
          <button class="icon-button" type="button" aria-label="新增医疗记录" title="新增医疗记录" @click="openPetMedicalCreate()">
            <Plus :size="18" />
          </button>
        </div>
        <p v-if="petMedicalRecords.length === 0" class="empty">还没有医疗记录</p>
        <div v-for="record in petMedicalRecords" :key="text(record, 'id')" class="feed-item feed-item-actions">
          <span>{{ petRecordTypeLabel(text(record, "record_type")) }} · {{ text(record, "record_date") || "未记录日期" }}</span>
          <p>{{ text(record, "description") }}</p>
          <span v-if="text(record, 'next_due_at')">下次提醒：{{ formatDateTime(text(record, "next_due_at")) }}</span>
          <button
            class="text-button danger-button"
            :disabled="isSubmitting(`pet-medical-delete-${numberValue(record, 'id')}`)"
            type="button"
            @click="deletePetMedicalRecord(numberValue(record, 'id'))"
          >
            <Trash2 :size="15" />
            <span>删除</span>
          </button>
        </div>
      </article>

      <article v-else class="form-card">
        <div class="section-title">
          <button class="icon-button" type="button" aria-label="返回医疗记录" title="返回医疗记录" @click="petView = 'medical'">
            <ArrowLeft :size="18" />
          </button>
          <h2>新增医疗记录</h2>
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
        <button
          class="secondary-button"
          :disabled="isSubmitting('pet-medical-create')"
          type="button"
          @click="submitPetMedicalRecord"
        >
          <LoaderCircle v-if="isSubmitting('pet-medical-create')" class="spin" :size="17" />
          <Plus v-else :size="17" />
          <span>保存医疗记录</span>
        </button>
      </article>
    </section>
    <datalist id="role-options">
      <option v-for="item in roleOptions" :key="item" :value="item" />
    </datalist>
  </main>
</template>
