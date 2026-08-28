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
  BookOpen,
  CalendarDays,
  Camera,
  ChefHat,
  Check,
  ClipboardList,
  Dices,
  Droplets,
  Eye,
  EyeOff,
  GripVertical,
  Heart,
  Home,
  ImagePlus,
  Leaf,
  LayoutGrid,
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
  Sun,
  Trees,
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
  | "period"
  | "private"
  | "members"
  | "memberAdd"
  | "memberEdit"
  | "profile"
  | "album"
  | "pets"
  | "votes"
  | "inventory"
  | "recipes";

type ToastType = "success" | "error" | "info";
type HomeViewMode = "calendar" | "cards";
type HomeCardKey =
  | "todo"
  | "plants"
  | "care"
  | "shopping"
  | "finance"
  | "reminders"
  | "period"
  | "private"
  | "members"
  | "album"
  | "pets"
  | "votes"
  | "profile"
  | "inventory"
  | "recipes";
type ReminderViewKey = "list" | "create";
type ProfileViewKey = "detail" | "edit";
type PetViewKey = "list" | "detail" | "create" | "edit" | "photos" | "photoCreate" | "medical" | "medicalCreate";
type PlantViewKey = "list" | "create" | "edit";
type CareViewKey = "list" | "create";
type TodoViewKey = "list" | "create" | "edit";
type TodoFilterKey = "all" | "mine" | "shared" | "done";
type MemoryViewKey = "list" | "create" | "edit";
type FinanceViewKey = "list" | "create";
type PrivateViewKey = "list" | "create" | "edit";
type PeriodViewKey = "list" | "profile" | "record";
type InventoryViewKey = "list" | "create" | "edit";
type VoteViewKey = "list" | "create";
type RecipeViewKey = "list" | "create" | "edit" | "week" | "mealPlanCreate";
type ShoppingViewKey = "list" | "create";
type ReminderModuleKey = "plants" | "pets" | "memory" | "todo" | "period" | "inventory" | "recipes" | "manual" | "other";
type CalendarEventType = "todo" | "reminder" | "care" | "memory" | "period";
type CalendarEvent = {
  id: string;
  date: string;
  type: CalendarEventType;
  label: string;
  title: string;
  detail: string;
  tone: string;
  tab: TabKey;
};
type HomeCardDefinition = {
  key: HomeCardKey;
  label: string;
  value: string;
  description: string;
  tone: string;
  icon: Component;
};
type ReminderModuleGroup = {
  key: ReminderModuleKey;
  label: string;
  description: string;
  icon: Component;
  reminders: AnyRow[];
};
type DrawerEntryKey = HomeCardKey | "flowers" | "familyMembers" | "coupleWorld" | "misc";
type DrawerChildItem = {
  key: HomeCardKey;
  label: string;
  value: string;
};
type DrawerEntry = {
  key: DrawerEntryKey;
  label: string;
  value: string;
  description: string;
  tone: string;
  icon: Component;
  type: "single" | "group";
  children: DrawerChildItem[];
};
type PlantLocationOption = {
  value: string;
  label: string;
  icon: Component;
};
type PlantStatusTagTone = "done" | "due" | "late";
type PlantStatusTag = {
  icon: Component;
  text: string;
  tone: PlantStatusTagTone;
};
type PlantTrendDay = {
  key: string;
  active: boolean;
  label: string;
};
type PlantCareBadge = {
  key: string;
  label: string;
  active: boolean;
};
type PlantCheckInEntry = {
  plantId: string;
  checkInDate: string;
};
type PlantAchievementContent = {
  title: string;
  subtitle: string;
};
type PetQuickActionKey = "FEED" | "DEWORMING" | "BATH";
type PetTaskTone = "calm" | "soon" | "late";
type PetTaskFlowItem = {
  key: PetQuickActionKey;
  label: string;
  description: string;
  statusText: string;
  metaText: string;
  tone: PetTaskTone;
  nextDueAt: string;
};
type PetWeightChartPoint = {
  x: number;
  y: number;
  date: string;
  shortDate: string;
  value: number;
  valueLabel: string;
};
type PetWeightChartModel = {
  hasData: boolean;
  polyline: string;
  points: PetWeightChartPoint[];
  minLabel: string;
  maxLabel: string;
  firstLabel: string;
  lastLabel: string;
};

const defaultHomeCardOrder: HomeCardKey[] = [
  "todo",
  "plants",
  "care",
  "shopping",
  "finance",
  "reminders",
  "period",
  "members",
  "album",
  "pets",
  "votes",
  "inventory",
  "recipes",
  "private",
  "profile"
];

const plantLocationOptions: PlantLocationOption[] = [
  { value: "室内", label: "室内", icon: Home },
  { value: "室外", label: "室外", icon: Trees },
  { value: "阳台", label: "阳台", icon: Sun },
  { value: "书房", label: "书房", icon: BookOpen }
];

const drawerChildLabels: Partial<Record<HomeCardKey, string>> = {
  members: "大人",
  inventory: "物资"
};

const groupedDrawerChildren: Record<"flowers" | "familyMembers" | "coupleWorld" | "misc", HomeCardKey[]> = {
  flowers: ["plants", "care"],
  familyMembers: ["members", "pets"],
  coupleWorld: ["album", "private"],
  misc: ["inventory", "recipes"]
};

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
const allCareRecords = ref<AnyRow[]>([]);
const periodSummary = ref<AnyRow>({
  profile: {
    cycle_days: 28,
    period_days: 5,
    last_period_start: "",
    reminder_enabled: true,
    reminder_time: "09:00",
    note: "",
    has_profile: false
  },
  records: [],
  prediction: {}
});
const privateMessages = ref<AnyRow[]>([]);
const albumPhotos = ref<AnyRow[]>([]);
const inventoryItems = ref<AnyRow[]>([]);
const familyVotes = ref<AnyRow[]>([]);
const recipes = ref<AnyRow[]>([]);
const mealPlans = ref<AnyRow[]>([]);
const pets = ref<AnyRow[]>([]);
const petPhotos = ref<AnyRow[]>([]);
const petMedicalRecords = ref<AnyRow[]>([]);
const petCareRecords = ref<AnyRow[]>([]);
const petWeightRecords = ref<AnyRow[]>([]);
const selectedPetId = ref("");
const reminderView = ref<ReminderViewKey>("list");
const profileView = ref<ProfileViewKey>("detail");
const petView = ref<PetViewKey>("list");
const plantView = ref<PlantViewKey>("list");
const careView = ref<CareViewKey>("list");
const todoView = ref<TodoViewKey>("list");
const todoFilter = ref<TodoFilterKey>("all");
const financeView = ref<FinanceViewKey>("list");
const privateView = ref<PrivateViewKey>("list");
const periodView = ref<PeriodViewKey>("list");
const memoryView = ref<MemoryViewKey>("list");
const inventoryView = ref<InventoryViewKey>("list");
const voteView = ref<VoteViewKey>("list");
const recipeView = ref<RecipeViewKey>("list");
const shoppingView = ref<ShoppingViewKey>("list");
const homeViewMode = ref<HomeViewMode>("cards");
const homeCardOrder = ref<HomeCardKey[]>([...defaultHomeCardOrder]);
const savedHomeCardOrder = ref<HomeCardKey[]>([...defaultHomeCardOrder]);
const savedHomeViewMode = ref<HomeViewMode>("cards");
const homeCardDraggingKey = ref<DrawerEntryKey | "">("");
const actionKey = ref("");
const selectedTodoId = ref("");
const selectedPlantId = ref("");
const plantQuickCarePlantId = ref("");
const selectedMemoryId = ref("");
const selectedCalendarDate = ref(getTodayDateValue());
const selectedInventoryId = ref("");
const selectedRecipeId = ref("");
const visibleMonth = ref(getMonthStartValue());
const mealPlanWeekStart = ref(getWeekStartValue());
const rememberLogin = ref(getRememberedLogin());
const loginDraft = ref({
  username: "",
  password: ""
});
const quickText = ref("");
const plantDraft = ref({
  name: "",
  flowerColor: "",
  location: "",
  carePreference: "",
  acquiredOn: "",
  status: "GROWING",
  coverUrl: ""
});
const plantEditDraft = ref({
  id: "",
  name: "",
  flowerColor: "",
  location: "",
  carePreference: "",
  acquiredOn: "",
  status: "GROWING",
  coverUrl: ""
});
const plantFile = ref<File | null>(null);
const plantEditFile = ref<File | null>(null);
const careDraft = ref({
  plantId: "",
  careType: "WATER",
  detail: "",
  nextCareAt: ""
});
const plantCheckInDates = ref<string[]>([]);
const plantCheckInEntries = ref<PlantCheckInEntry[]>([]);
const plantAchievementVisible = ref(false);
const plantAchievementKey = ref(0);
const plantAchievementTitle = ref("守护天数 +1");
const plantAchievementSubtitle = ref("今天也把花花照顾得很好");
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
const calendarTodoDraft = ref({
  title: "",
  time: "18:00"
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
  assigneeIds: [] as string[],
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
  assigneeIds: [] as string[],
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
  receiverMemberId: "",
  messageDate: getTodayDateValue()
});
const privateEditDraft = ref({
  id: "",
  content: "",
  visibility: "TO_PARTNER",
  receiverMemberId: "",
  messageDate: getTodayDateValue()
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
const decisionOptionsText = ref("喝酒\n吃烧烤\n看电影\n看电视剧\n看综艺\n打游戏\n喝酒看电视剧/综艺一起\n打小谭");
const decisionResult = ref("");
const decisionHistory = ref<string[]>([]);
const decisionRolling = ref(false);
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
const petQuickActionOpen = ref(false);
const petQuickActionDraft = ref({
  action: "FEED",
  recordedAt: getNowDateTimeValue(),
  description: "",
  nextDueAt: ""
});
const petWeightOpen = ref(false);
const petWeightDraft = ref({
  weightKg: "",
  recordedOn: getTodayDateValue(),
  note: ""
});
const periodProfileDraft = ref({
  cycleDays: "28",
  periodDays: "5",
  lastPeriodStart: "",
  reminderEnabled: true,
  reminderTime: "09:00",
  note: ""
});
const periodRecordDraft = ref({
  startOn: getTodayDateValue(),
  endOn: "",
  note: ""
});

const roleOptions = ["主人", "女主人", "男主人", "伴侣", "家庭成员", "家人", "宝宝", "宠物家长"];
const weekDayLabels = ["一", "二", "三", "四", "五", "六", "日"];
const reminderModuleOrder: ReminderModuleKey[] = ["plants", "pets", "memory", "todo", "period", "inventory", "recipes", "manual", "other"];
const reminderModuleDefinitions: Record<ReminderModuleKey, Omit<ReminderModuleGroup, "reminders">> = {
  plants: {
    key: "plants",
    label: "花花",
    description: "花卉养护提醒",
    icon: Leaf
  },
  pets: {
    key: "pets",
    label: "宠物",
    description: "护理与医疗提醒",
    icon: PawPrint
  },
  memory: {
    key: "memory",
    label: "时刻墙",
    description: "纪念日、生日与周年提醒",
    icon: Heart
  },
  todo: {
    key: "todo",
    label: "待办",
    description: "家庭任务与个人事项",
    icon: ClipboardList
  },
  period: {
    key: "period",
    label: "月经管理",
    description: "周期相关提醒",
    icon: Droplets
  },
  inventory: {
    key: "inventory",
    label: "库存与物资",
    description: "库存不足与临期提醒",
    icon: Package
  },
  recipes: {
    key: "recipes",
    label: "菜谱与餐单",
    description: "餐单提醒",
    icon: ChefHat
  },
  manual: {
    key: "manual",
    label: "手动提醒",
    description: "你自己新增的提醒",
    icon: Bell
  },
  other: {
    key: "other",
    label: "其他",
    description: "暂未归类的提醒",
    icon: Bell
  }
};
const petQuickActionDefinitions: Record<
  PetQuickActionKey,
  { label: string; description: string; intervalDays: number; warnDays: number; icon: Component }
> = {
  FEED: {
    label: "喂食",
    description: "记录日常进食节奏",
    intervalDays: 1,
    warnDays: 0,
    icon: Heart
  },
  DEWORMING: {
    label: "驱虫",
    description: "记录体内外驱虫安排",
    intervalDays: 90,
    warnDays: 10,
    icon: Stethoscope
  },
  BATH: {
    label: "洗澡",
    description: "记录清洁打理节奏",
    intervalDays: 30,
    warnDays: 5,
    icon: Droplets
  }
};

let messageTimer: number | undefined;
let refreshTimer: number | undefined;
let plantAchievementTimer: number | undefined;
let decisionTimer: number | undefined;

function reminderModuleKeyOfSourceType(sourceType: string): ReminderModuleKey {
  if (sourceType === "PLANT_CARE") {
    return "plants";
  }
  if (sourceType === "PET_CARE" || sourceType === "PET_MEDICAL") {
    return "pets";
  }
  if (sourceType === "MEMORY_EVENT") {
    return "memory";
  }
  if (sourceType === "TODO_TASK") {
    return "todo";
  }
  if (sourceType === "PERIOD_TRACKER") {
    return "period";
  }
  if (sourceType === "INVENTORY_LOW" || sourceType === "INVENTORY_EXPIRE") {
    return "inventory";
  }
  if (sourceType === "MEAL_PLAN") {
    return "recipes";
  }
  if (sourceType === "MANUAL" || !sourceType) {
    return "manual";
  }
  return "other";
}

const pendingReminders = computed(() => reminders.value.filter((item) => text(item, "status") === "PENDING"));
const reminderModuleGroups = computed<ReminderModuleGroup[]>(() => {
  const buckets = reminderModuleOrder.reduce(
    (result, key) => {
      result[key] = [];
      return result;
    },
    {} as Record<ReminderModuleKey, AnyRow[]>
  );
  pendingReminders.value.forEach((reminder) => {
    const moduleKey = reminderModuleKeyOfSourceType(text(reminder, "source_type"));
    buckets[moduleKey].push(reminder);
  });
  return reminderModuleOrder
    .filter((key) => buckets[key].length > 0)
    .map((key) => ({
      ...reminderModuleDefinitions[key],
      reminders: buckets[key]
    }));
});
const pendingPlantCareReminders = computed(() =>
  [...pendingReminders.value]
    .filter((item) => text(item, "source_type") === "PLANT_CARE")
    .sort((left, right) => {
      const leftTime = dateTimeFromValue(text(left, "due_at"))?.getTime() ?? Number.MAX_SAFE_INTEGER;
      const rightTime = dateTimeFromValue(text(right, "due_at"))?.getTime() ?? Number.MAX_SAFE_INTEGER;
      return leftTime - rightTime;
    })
);
const openTodoTasks = computed(() => todoTasks.value.filter((item) => text(item, "status") === "TODO"));
const completedTodoTasks = computed(() => todoTasks.value.filter((item) => text(item, "status") === "DONE"));
const periodProfile = computed<AnyRow>(() => ((periodSummary.value.profile as AnyRow) ?? {}) as AnyRow);
const periodRecords = computed<AnyRow[]>(() =>
  Array.isArray(periodSummary.value.records) ? (periodSummary.value.records as AnyRow[]) : []
);
const periodPrediction = computed<AnyRow>(() => ((periodSummary.value.prediction as AnyRow) ?? {}) as AnyRow);
const currentPlant = computed(() =>
  plants.value.find((plant) => text(plant, "id") === selectedPlantId.value)
);
const quickCarePlant = computed(() =>
  plants.value.find((plant) => text(plant, "id") === plantQuickCarePlantId.value)
);
const nextPendingPlantCareReminder = computed<AnyRow | null>(() => pendingPlantCareReminders.value[0] ?? null);
const plantCareRecordedDatesByPlantId = computed<Record<string, string[]>>(() => {
  const grouped: Record<string, Set<string>> = {};
  allCareRecords.value.forEach((record) => {
    const plantId = text(record, "plant_id");
    const careDate = text(record, "care_date");
    if (!plantId || !careDate) {
      return;
    }
    if (!grouped[plantId]) {
      grouped[plantId] = new Set<string>();
    }
    grouped[plantId].add(careDate);
  });
  return Object.fromEntries(
    Object.entries(grouped).map(([plantId, dates]) => [plantId, Array.from(dates).sort()])
  );
});
const manualPlantCheckInDatesByPlantId = computed<Record<string, string[]>>(() => {
  const grouped: Record<string, Set<string>> = {};
  plantCheckInEntries.value.forEach((entry) => {
    if (!entry.plantId || !entry.checkInDate) {
      return;
    }
    if (!grouped[entry.plantId]) {
      grouped[entry.plantId] = new Set<string>();
    }
    grouped[entry.plantId].add(entry.checkInDate);
  });
  return Object.fromEntries(
    Object.entries(grouped).map(([plantId, dates]) => [plantId, Array.from(dates).sort()])
  );
});
const effectivePlantCheckInDatesByPlantId = computed<Record<string, string[]>>(() => {
  const grouped: Record<string, Set<string>> = {};
  Object.entries(plantCareRecordedDatesByPlantId.value).forEach(([plantId, dates]) => {
    grouped[plantId] = new Set(dates);
  });
  Object.entries(manualPlantCheckInDatesByPlantId.value).forEach(([plantId, dates]) => {
    if (!grouped[plantId]) {
      grouped[plantId] = new Set<string>();
    }
    dates.forEach((date) => grouped[plantId].add(date));
  });
  return Object.fromEntries(
    Object.entries(grouped).map(([plantId, dates]) => [plantId, Array.from(dates).sort()])
  );
});
const careRecordById = computed<Record<string, AnyRow>>(() => {
  const lookup: Record<string, AnyRow> = {};
  allCareRecords.value.forEach((record) => {
    const recordId = text(record, "id");
    if (recordId) {
      lookup[recordId] = record;
    }
  });
  return lookup;
});
const nextPlantReminderByPlantId = computed<Record<string, AnyRow>>(() => {
  const lookup: Record<string, AnyRow> = {};
  pendingPlantCareReminders.value.forEach((reminder) => {
    const sourceId = text(reminder, "source_id");
    const record = careRecordById.value[sourceId];
    const plantId = text(record ?? {}, "plant_id");
    if (!plantId) {
      return;
    }
    const currentDueAt = text(reminder, "due_at");
    const existingDueAt = text(lookup[plantId] ?? {}, "due_at");
    const currentTime = dateTimeFromValue(currentDueAt)?.getTime() ?? Number.MAX_SAFE_INTEGER;
    const existingTime = dateTimeFromValue(existingDueAt)?.getTime() ?? Number.MAX_SAFE_INTEGER;
    if (!lookup[plantId] || currentTime < existingTime) {
      lookup[plantId] = reminder;
    }
  });
  return lookup;
});
const todayDuePlants = computed<AnyRow[]>(() =>
  plants.value.filter((plant) => {
    const reminder = nextPlantReminderByPlantId.value[text(plant, "id")];
    const diff = daysFromToday(text(reminder ?? {}, "due_at"));
    return diff !== null && diff <= 0;
  })
);
const todayDuePlantNames = computed(() => todayDuePlants.value.map((plant) => text(plant, "name")).filter(Boolean));
const todayDuePlantSummary = computed(() => {
  if (todayDuePlantNames.value.length === 0) {
    return "今天暂无到点花花";
  }
  if (todayDuePlantNames.value.length <= 2) {
    return `今日需操作：${todayDuePlantNames.value.join("、")}`;
  }
  return `今日需操作：${todayDuePlantNames.value.slice(0, 2).join("、")}等${todayDuePlantNames.value.length}盆`;
});
const plantCareRecordedDates = computed(() => {
  const dates = new Set<string>();
  Object.values(plantCareRecordedDatesByPlantId.value).forEach((careDates) => {
    careDates.forEach((careDate) => dates.add(careDate));
  });
  return Array.from(dates).sort();
});
const effectivePlantCheckInDates = computed(() => {
  const dates = new Set<string>(plantCheckInDates.value);
  plantCareRecordedDates.value.forEach((date) => dates.add(date));
  return Array.from(dates).sort();
});
const isPlantCheckedInToday = computed(() => effectivePlantCheckInDates.value.includes(getTodayDateValue()));
const plantCareDaysThisMonth = computed(() => {
  const monthPrefix = getTodayDateValue().slice(0, 7);
  return effectivePlantCheckInDates.value.filter((date) => date.startsWith(monthPrefix)).length;
});
const previousPlantCareDays = computed(() => {
  const currentMonthStart = dateFromValue(getMonthStartValue()) ?? new Date();
  currentMonthStart.setMonth(currentMonthStart.getMonth() - 1, 1);
  const previousPrefix = `${currentMonthStart.getFullYear()}-${`${currentMonthStart.getMonth() + 1}`.padStart(2, "0")}`;
  return effectivePlantCheckInDates.value.filter((date) => date.startsWith(previousPrefix)).length;
});
const plantCareMonthTotalDays = computed(() => {
  const todayDate = new Date();
  return new Date(todayDate.getFullYear(), todayDate.getMonth() + 1, 0).getDate();
});
const plantCareDelta = computed(() => plantCareDaysThisMonth.value - previousPlantCareDays.value);
const plantCareDeltaLabel = computed(() => {
  if (plantCareDelta.value === 0) {
    return "和上月持平";
  }
  if (plantCareDelta.value > 0) {
    return `较上月多 ${plantCareDelta.value} 天`;
  }
  return `较上月少 ${Math.abs(plantCareDelta.value)} 天`;
});
const plantTrendDays = computed<PlantTrendDay[]>(() => {
  const monthPrefix = getTodayDateValue().slice(0, 7);
  const activeDays = new Set<number>();
  effectivePlantCheckInDates.value.forEach((careDate) => {
    if (!careDate.startsWith(monthPrefix)) {
      return;
    }
    const dayNumber = Number(careDate.slice(8, 10));
    if (Number.isFinite(dayNumber) && dayNumber > 0) {
      activeDays.add(dayNumber);
    }
  });
  return Array.from({ length: plantCareMonthTotalDays.value }, (_, index) => ({
    key: `plant-trend-${index + 1}`,
    active: activeDays.has(index + 1),
    label: `${index + 1}`
  }));
});
const plantCareBadges = computed<PlantCareBadge[]>(() => [
  {
    key: "badge-7",
    label: "绿手指 · 7日坚持",
    active: plantCareDaysThisMonth.value >= 7
  },
  {
    key: "badge-15",
    label: "花花守望 · 15日坚持",
    active: plantCareDaysThisMonth.value >= 15
  },
  {
    key: "badge-full",
    label: "本月全勤",
    active: plantCareDaysThisMonth.value >= plantCareMonthTotalDays.value
  }
]);
const plantOverviewSummary = computed(() => {
  if (todayDuePlantNames.value.length > 0) {
    return todayDuePlantSummary.value;
  }
  if (nextPendingPlantCareReminder.value) {
    return `${text(nextPendingPlantCareReminder.value, "title")} · ${relativeDaysLabel(text(nextPendingPlantCareReminder.value, "due_at"))}`;
  }
  if (pendingPlantCareReminders.value.length > 0) {
    return `还有 ${pendingPlantCareReminders.value.length} 条待养护`;
  }
  if (isPlantCheckedInToday.value) {
    return "今天已经照顾过花花了";
  }
  if (plants.value.length > 0) {
    return `当前已整理 ${plants.value.length} 盆花花`;
  }
  return "开始记录第一盆花花吧";
});
const myTodoTasks = computed(() =>
  openTodoTasks.value.filter((item) => hasTaskAssignee(item, String(currentUser.value.memberId ?? "")))
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
const latestPetWeightRecord = computed<AnyRow | null>(() => petWeightRecords.value[petWeightRecords.value.length - 1] ?? null);
const petReminderCandidates = computed(() =>
  [...petCareRecords.value, ...petMedicalRecords.value]
    .filter((item) => text(item, "next_due_at"))
    .sort((left, right) => {
      const leftTime = dateTimeFromValue(text(left, "next_due_at"))?.getTime() ?? Number.MAX_SAFE_INTEGER;
      const rightTime = dateTimeFromValue(text(right, "next_due_at"))?.getTime() ?? Number.MAX_SAFE_INTEGER;
      return leftTime - rightTime;
    })
);
const petNextReminderItem = computed<AnyRow | null>(() => petReminderCandidates.value[0] ?? null);
const petReminderCount = computed(() => petReminderCandidates.value.length);
const petTaskFlowItems = computed(() => buildPetTaskFlowItems());
const petWeightChartPoints = computed(() => buildPetWeightChartPoints(petWeightRecords.value));
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
const periodCountdown = computed(() => {
  const daysUntilNext = Number(periodPrediction.value.days_until_next);
  if (Number.isFinite(daysUntilNext)) {
    return `${daysUntilNext}天`;
  }
  return `${numberValue(periodProfile.value, "cycle_days") || 28}天周期`;
});
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
    label: "养护历史",
    value: `${allCareRecords.value.length}条`,
    description: "按花卉查看历史记录",
    tone: "tile-green",
    icon: Sprout
  },
  shopping: {
    key: "shopping",
    label: "清单",
    value: `${todoShoppingItems.value.length}项`,
    description: "待买事项",
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
  period: {
    key: "period",
    label: "月经管理",
    value: periodCountdown.value,
    description: text(periodPrediction.value, "next_start") ? `下次：${text(periodPrediction.value, "next_start")}` : "记录周期与提醒",
    tone: "tile-coral",
    icon: Droplets
  },
  private: {
    key: "private",
    label: "私密",
    value: `${privateMessages.value.length}条`,
    description: "悄悄话和留言",
    tone: "tile-plum",
    icon: MessageSquare
  },
  members: {
    key: "members",
    label: "成员",
    value: `${familyMembers.value.length}位`,
    description: "角色与资料",
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
    description: petNextReminderItem.value
      ? `下次${petReminderItemLabel(petNextReminderItem.value)} ${relativeDaysLabel(text(petNextReminderItem.value, "next_due_at"))}`
      : "档案、照片和动态提醒",
    tone: "tile-gold",
    icon: PawPrint
  },
  votes: {
    key: "votes",
    label: "决策器",
    value: decisionResult.value || "随手一抽",
    description: decisionHistory.value.length > 0 ? `已经做过 ${decisionHistory.value.length} 次决定` : "把纠结交给随机数",
    tone: "tile-gold",
    icon: Dices
  },
  inventory: {
    key: "inventory",
    label: "库存",
    value: `${lowInventoryItems.value.length}项`,
    description: "消耗品、食品与库存提醒",
    tone: "tile-emerald",
    icon: Package
  },
  recipes: {
    key: "recipes",
    label: "菜谱",
    value: `${recipes.value.length}道`,
    description: "菜谱与餐单",
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
const drawerGroupDefinitions = computed<
  Record<"flowers" | "familyMembers" | "coupleWorld" | "misc", Omit<DrawerEntry, "children" | "type">>
>(() => ({
  flowers: {
    key: "flowers",
    label: "花花",
    value: `${pendingPlantCareReminders.value.length}待养护`,
    description: todayDuePlantSummary.value,
    tone: "tile-green",
    icon: Leaf
  },
  familyMembers: {
    key: "familyMembers",
    label: "家庭成员",
    value: `${familyMembers.value.length}位 · ${pets.value.length}只`,
    description: "大人和宠物",
    tone: "tile-ink",
    icon: Users
  },
  coupleWorld: {
    key: "coupleWorld",
    label: "二人世界",
    value: `${albumPhotos.value.length}条时刻 · ${privateMessages.value.length}条留言`,
    description: "时刻与留言",
    tone: "tile-coral",
    icon: Heart
  },
  misc: {
    key: "misc",
    label: "杂项",
    value: `${lowInventoryItems.value.length}项提醒 · ${recipes.value.length}道菜`,
    description: "物资和菜谱",
    tone: "tile-emerald",
    icon: Package
  }
}));
const drawerEntries = computed<DrawerEntry[]>(() => {
  const seen = new Set<DrawerEntryKey>();
  const entries: DrawerEntry[] = [];
  homeCardOrder.value.forEach((cardKey) => {
    const entryKey = drawerEntryKeyOfCard(cardKey);
    if (seen.has(entryKey)) {
      return;
    }
    seen.add(entryKey);
    if (isGroupedDrawerEntryKey(entryKey)) {
      const groupDefinition = drawerGroupDefinitions.value[entryKey];
      const children = homeCardOrder.value
        .filter((key) => groupedDrawerChildren[entryKey].includes(key))
        .map((key) => ({
          key,
          label: drawerChildLabel(key),
          value: homeCardDefinitions.value[key].value
        }));
      entries.push({
        ...groupDefinition,
        type: "group",
        children
      });
      return;
    }
    const definition = homeCardDefinitions.value[cardKey];
    entries.push({
      ...definition,
      type: "single",
      children: [
        {
          key: cardKey,
          label: drawerChildLabel(cardKey),
          value: definition.value
        }
      ]
    });
  });
  return entries;
});
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
  if (activeTab.value === "finance") {
    return financeView.value === "create" ? "新增记账" : "家庭账本";
  }
  if (activeTab.value === "private") {
    return privateView.value === "create" ? "新增留言" : privateView.value === "edit" ? "编辑留言" : "留言板";
  }
  if (activeTab.value === "period") {
    return periodView.value === "profile" ? "周期设置" : periodView.value === "record" ? "记录经期" : "月经管理";
  }
  if (activeTab.value === "plants") {
    return plantView.value === "create" ? "新增花卉" : plantView.value === "edit" ? "编辑花卉" : "花卉";
  }
  if (activeTab.value === "care") {
    return "养护历史";
  }
  if (activeTab.value === "shopping") {
    return shoppingView.value === "create" ? "新增清单" : "清单";
  }
  if (activeTab.value === "album") {
    return memoryView.value === "create" ? "新增时刻" : memoryView.value === "edit" ? "编辑时刻" : "时刻墙";
  }
  if (activeTab.value === "votes") {
    return "丹总专属决策器";
  }
  if (activeTab.value === "inventory") {
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
      care: "养护历史",
      shopping: "清单",
      finance: "记账",
      period: "月经管理",
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

function isGroupedDrawerEntryKey(value: DrawerEntryKey): value is keyof typeof groupedDrawerChildren {
  return value in groupedDrawerChildren;
}

function drawerEntryKeyOfCard(cardKey: HomeCardKey): DrawerEntryKey {
  if (groupedDrawerChildren.flowers.includes(cardKey)) {
    return "flowers";
  }
  if (groupedDrawerChildren.familyMembers.includes(cardKey)) {
    return "familyMembers";
  }
  if (groupedDrawerChildren.coupleWorld.includes(cardKey)) {
    return "coupleWorld";
  }
  if (groupedDrawerChildren.misc.includes(cardKey)) {
    return "misc";
  }
  return cardKey;
}

function drawerChildLabel(cardKey: HomeCardKey): string {
  return drawerChildLabels[cardKey] || homeCardDefinitions.value[cardKey].label;
}

function togglePlantLocation(target: typeof plantDraft.value | typeof plantEditDraft.value, location: string) {
  target.location = target.location === location ? "" : location;
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

function getNowDateTimeValue(baseDate = new Date()): string {
  const year = baseDate.getFullYear();
  const month = `${baseDate.getMonth() + 1}`.padStart(2, "0");
  const day = `${baseDate.getDate()}`.padStart(2, "0");
  const hour = `${baseDate.getHours()}`.padStart(2, "0");
  const minute = `${baseDate.getMinutes()}`.padStart(2, "0");
  return `${year}-${month}-${day}T${hour}:${minute}`;
}

function getMonthStartValue(baseDate = new Date()): string {
  const year = baseDate.getFullYear();
  const month = `${baseDate.getMonth() + 1}`.padStart(2, "0");
  return `${year}-${month}-01`;
}

function dateFromValue(value: string): Date | null {
  if (!value) {
    return null;
  }
  const parsed = new Date(`${value}T12:00:00`);
  if (Number.isNaN(parsed.getTime())) {
    return null;
  }
  return parsed;
}

function dateTimeFromValue(value: string): Date | null {
  if (!value) {
    return null;
  }
  const parsed = new Date(value.length === 10 ? `${value}T00:00:00` : value);
  if (Number.isNaN(parsed.getTime())) {
    return null;
  }
  return parsed;
}

function toDateValue(date: Date): string {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function addDays(dateValue: string, days: number): string {
  const date = dateFromValue(dateValue);
  if (!date) {
    return dateValue;
  }
  date.setDate(date.getDate() + days);
  return toDateValue(date);
}

function shiftMonth(monthStart: string, offset: number): string {
  const date = dateFromValue(monthStart) ?? new Date();
  date.setMonth(date.getMonth() + offset, 1);
  return getMonthStartValue(date);
}

function formatMonthLabel(monthStart: string): string {
  const date = dateFromValue(monthStart);
  if (!date) {
    return "";
  }
  return `${date.getFullYear()}年${date.getMonth() + 1}月`;
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

function normalizeHomeViewMode(value: string): HomeViewMode {
  return value === "calendar" ? "calendar" : "cards";
}

function applyHomeViewMode(value: string) {
  const normalized = normalizeHomeViewMode(value);
  homeViewMode.value = normalized;
  savedHomeViewMode.value = normalized;
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

function readHomeViewMode(data: AnyRow): HomeViewMode {
  const preferences = data.preferences as AnyRow | undefined;
  return normalizeHomeViewMode(String(preferences?.homeViewMode ?? ""));
}

function readPlantCheckInDates(data: AnyRow): string[] {
  const preferences = data.preferences as AnyRow | undefined;
  const checkInDates = preferences?.plantCheckInDates;
  if (!Array.isArray(checkInDates)) {
    return [];
  }
  return checkInDates
    .map((item) => String(item))
    .filter((item) => /^\d{4}-\d{2}-\d{2}$/.test(item))
    .sort();
}

function readPlantCheckInEntries(data: AnyRow): PlantCheckInEntry[] {
  const preferences = data.preferences as AnyRow | undefined;
  const entries = preferences?.plantCheckInEntries;
  if (!Array.isArray(entries)) {
    return [];
  }
  return entries
    .map((entry) => {
      const row = (entry ?? {}) as AnyRow;
      return {
        plantId: text(row, "plantId"),
        checkInDate: text(row, "checkInDate")
      };
    })
    .filter((entry) => entry.plantId && /^\d{4}-\d{2}-\d{2}$/.test(entry.checkInDate))
    .sort((left, right) =>
      left.checkInDate === right.checkInDate
        ? left.plantId.localeCompare(right.plantId)
        : left.checkInDate.localeCompare(right.checkInDate)
    );
}

function isDrawerEntryKey(value: string): value is DrawerEntryKey {
  return value === "flowers" || value === "familyMembers" || value === "coupleWorld" || value === "misc" || isHomeCardKey(value);
}

function cardKeysOfDrawerEntry(entryKey: DrawerEntryKey): HomeCardKey[] {
  if (isGroupedDrawerEntryKey(entryKey)) {
    return homeCardOrder.value.filter((cardKey) => groupedDrawerChildren[entryKey].includes(cardKey));
  }
  return [entryKey];
}

function moveDrawerEntry(dragEntryKey: DrawerEntryKey, targetEntryKey: DrawerEntryKey) {
  if (dragEntryKey === targetEntryKey) {
    return;
  }
  const dragKeys = cardKeysOfDrawerEntry(dragEntryKey);
  const targetKeys = cardKeysOfDrawerEntry(targetEntryKey);
  if (dragKeys.length === 0 || targetKeys.length === 0) {
    return;
  }
  const nextOrder = homeCardOrder.value.filter((key) => !dragKeys.includes(key));
  const targetIndex = nextOrder.indexOf(targetKeys[0]);
  if (targetIndex < 0) {
    return;
  }
  nextOrder.splice(targetIndex, 0, ...dragKeys);
  homeCardOrder.value = normalizeHomeCardOrder(nextOrder);
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

function splitIds(value: string): string[] {
  return value
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

function hasTaskAssignee(task: AnyRow, memberId: string): boolean {
  if (!memberId) {
    return false;
  }
  const assigneeIds = splitIds(text(task, "assignee_ids"));
  if (assigneeIds.includes(memberId)) {
    return true;
  }
  return text(task, "assignee_id") === memberId;
}

function todoAssigneeLabel(task: AnyRow): string {
  const assigneeNames = text(task, "assignee_names") || text(task, "assignee_name");
  if (assigneeNames) {
    return `认领人：${assigneeNames}`;
  }
  if (text(task, "task_scope") === "SHARED") {
    return "待认领";
  }
  return "我的任务";
}

function plantStatusLabel(code: string): string {
  return (
    {
      GROWING: "成长期",
      BLOOMING: "花期",
      FLOWERING: "花期",
      RESTING: "休眠期",
      DORMANT: "休眠期"
    }[code] ?? (code || "成长期")
  );
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

function plantStatusTag(plant: AnyRow): PlantStatusTag | null {
  const plantId = text(plant, "id");
  if (!plantId) {
    return null;
  }
  const todayDate = getTodayDateValue();
  const careDates = plantCareRecordedDatesByPlantId.value[plantId] ?? [];
  if (careDates.includes(todayDate)) {
    return {
      icon: Check,
      text: "今日已养护",
      tone: "done"
    };
  }
  const reminder = nextPlantReminderByPlantId.value[plantId];
  if (!reminder) {
    return null;
  }
  const diff = daysFromToday(text(reminder, "due_at"));
  if (diff === null) {
    return null;
  }
  if (diff < 0) {
    return {
      icon: Bell,
      text: `已逾期${Math.abs(diff)}天`,
      tone: "late"
    };
  }
  if (diff === 0) {
    return {
      icon: Bell,
      text: "今日需养护",
      tone: "due"
    };
  }
  const manualDates = manualPlantCheckInDatesByPlantId.value[plantId] ?? [];
  if (manualDates.includes(todayDate)) {
    return {
      icon: Check,
      text: "今日已打卡",
      tone: "done"
    };
  }
  return {
    icon: Bell,
    text: `${diff}天后需养护`,
    tone: "due"
  };
}

function hasPlantCheckInOnDate(plantId: string, dateValue = getTodayDateValue()): boolean {
  if (!plantId || !dateValue) {
    return false;
  }
  return effectivePlantCheckInDatesByPlantId.value[plantId]?.includes(dateValue) ?? false;
}

function resolvePlantAchievementContent(previousDays: number, currentDays: number): PlantAchievementContent {
  if (currentDays >= plantCareMonthTotalDays.value && previousDays < plantCareMonthTotalDays.value) {
    return {
      title: "本月全勤达成",
      subtitle: "这一个月的花花每天都被认真照顾"
    };
  }
  if (currentDays >= 15 && previousDays < 15) {
    return {
      title: "花花守望 · 15日坚持",
      subtitle: "已经把照顾植物变成很自然的日常了"
    };
  }
  if (currentDays >= 7 && previousDays < 7) {
    return {
      title: "绿手指 · 7日坚持",
      subtitle: "连续把花花放在心上，状态很棒"
    };
  }
  if (currentDays > previousDays) {
    return {
      title: "守护天数 +1",
      subtitle: "今天也把花花照顾得很好"
    };
  }
  return {
    title: "今日照顾已记录",
    subtitle: "这次操作已经稳稳记下来了"
  };
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

function memoryDateLabel(entryType: string): string {
  if (entryType === "ANNIVERSARY") {
    return "原始纪念日";
  }
  if (entryType === "BIRTHDAY") {
    return "原始生日";
  }
  return "记录日期";
}

function formatDateValue(value: string): string {
  return value || "未记录";
}

function nextOccurrenceDate(entry: AnyRow): Date | null {
  const entryType = text(entry, "entry_type");
  if (entryType !== "ANNIVERSARY" && entryType !== "BIRTHDAY") {
    return null;
  }
  const base = dateFromValue(text(entry, "taken_on"));
  if (!base) {
    return null;
  }
  const today = new Date();
  const year = today.getFullYear();
  const safeDay = (targetYear: number) =>
    Math.min(base.getDate(), new Date(targetYear, base.getMonth() + 1, 0).getDate());
  let candidate = new Date(year, base.getMonth(), safeDay(year), 12, 0, 0, 0);
  if (candidate < new Date(today.getFullYear(), today.getMonth(), today.getDate(), 0, 0, 0, 0)) {
    candidate = new Date(year + 1, base.getMonth(), safeDay(year + 1), 12, 0, 0, 0);
  }
  return candidate;
}

function daysUntil(value: Date): number {
  const today = new Date();
  const start = new Date(today.getFullYear(), today.getMonth(), today.getDate(), 0, 0, 0, 0).getTime();
  const target = new Date(value.getFullYear(), value.getMonth(), value.getDate(), 0, 0, 0, 0).getTime();
  return Math.max(0, Math.round((target - start) / 86400000));
}

function daysFromToday(value: string): number | null {
  const parsed = dateTimeFromValue(value);
  if (!parsed) {
    return null;
  }
  const today = new Date();
  const start = new Date(today.getFullYear(), today.getMonth(), today.getDate(), 0, 0, 0, 0).getTime();
  const target = new Date(parsed.getFullYear(), parsed.getMonth(), parsed.getDate(), 0, 0, 0, 0).getTime();
  return Math.round((target - start) / 86400000);
}

function relativeDaysLabel(value: string): string {
  const diff = daysFromToday(value);
  if (diff === null) {
    return "时间待补充";
  }
  if (diff === 0) {
    return "就在今天";
  }
  if (diff > 0) {
    return `还有 ${diff} 天`;
  }
  return `已逾期 ${Math.abs(diff)} 天`;
}

function relativeDaysTone(value: string, warnDays = 3): PetTaskTone {
  const diff = daysFromToday(value);
  if (diff === null) {
    return "calm";
  }
  if (diff < 0) {
    return "late";
  }
  if (diff <= warnDays) {
    return "soon";
  }
  return "calm";
}

function elapsedDaysLabel(value: string): string {
  const diff = daysFromToday(value);
  if (diff === null) {
    return "时间待补充";
  }
  if (diff === 0) {
    return "今天刚记录";
  }
  if (diff > 0) {
    return `还要 ${diff} 天`;
  }
  return `已过去 ${Math.abs(diff)} 天`;
}

function elapsedDaysTone(value: string, intervalDays: number, warnDays: number): PetTaskTone {
  const diff = daysFromToday(value);
  if (diff === null) {
    return "calm";
  }
  const daysPassed = Math.abs(Math.min(diff, 0));
  if (daysPassed > intervalDays) {
    return "late";
  }
  if (daysPassed >= Math.max(0, intervalDays - warnDays)) {
    return "soon";
  }
  return "calm";
}

function petCareTypeLabel(code: string): string {
  return petQuickActionDefinitions[code as PetQuickActionKey]?.label ?? code;
}

function petQuickActionIcon(code: string): Component {
  return petQuickActionDefinitions[code as PetQuickActionKey]?.icon ?? Heart;
}

function petReminderItemLabel(item: AnyRow): string {
  const careType = text(item, "care_type");
  if (careType) {
    return petCareTypeLabel(careType);
  }
  return petRecordTypeLabel(text(item, "record_type"));
}

function petAgeLabel(birthday: string): string {
  const birth = dateFromValue(birthday);
  if (!birth) {
    return "";
  }
  const today = new Date();
  let years = today.getFullYear() - birth.getFullYear();
  let months = today.getMonth() - birth.getMonth();
  let days = today.getDate() - birth.getDate();
  if (days < 0) {
    months -= 1;
    const lastMonthDays = new Date(today.getFullYear(), today.getMonth(), 0).getDate();
    days += lastMonthDays;
  }
  if (months < 0) {
    years -= 1;
    months += 12;
  }
  const parts: string[] = [];
  if (years > 0) {
    parts.push(`${years}岁`);
  }
  if (months > 0 || years > 0) {
    parts.push(`${months}个月`);
  }
  parts.push(`${Math.max(days, 0)}天`);
  return parts.join("");
}

function petLastEventAt(record: AnyRow): string {
  return text(record, "recorded_at") || text(record, "record_date");
}

function recentPetRecord(records: AnyRow[], dateField: string): AnyRow | null {
  return [...records]
    .sort((left, right) => {
      const leftTime = dateTimeFromValue(text(left, dateField))?.getTime() ?? 0;
      const rightTime = dateTimeFromValue(text(right, dateField))?.getTime() ?? 0;
      return rightTime - leftTime;
    })[0] ?? null;
}

function buildPetTaskFlowItems(): PetTaskFlowItem[] {
  const buildItem = (key: PetQuickActionKey): PetTaskFlowItem => {
    const definition = petQuickActionDefinitions[key];
    const careMatches = petCareRecords.value.filter((item) => text(item, "care_type") === key);
    const medicalMatches = key === "DEWORMING"
      ? petMedicalRecords.value.filter((item) => text(item, "record_type") === "DEWORMING")
      : [];
    const reminderSource = [...careMatches, ...medicalMatches]
      .filter((item) => text(item, "next_due_at"))
      .sort((left, right) => {
        const leftTime = dateTimeFromValue(text(left, "next_due_at"))?.getTime() ?? Number.MAX_SAFE_INTEGER;
        const rightTime = dateTimeFromValue(text(right, "next_due_at"))?.getTime() ?? Number.MAX_SAFE_INTEGER;
        return leftTime - rightTime;
      })[0] ?? null;
    const latestSource =
      recentPetRecord(careMatches, "recorded_at") ??
      recentPetRecord(medicalMatches, "record_date");
    const nextDueAt = text(reminderSource ?? {}, "next_due_at");
    if (nextDueAt) {
      return {
        key,
        label: definition.label,
        description: definition.description,
        statusText: `下次${definition.label}：${relativeDaysLabel(nextDueAt)}`,
        metaText: formatDateTime(nextDueAt),
        tone: relativeDaysTone(nextDueAt, definition.warnDays),
        nextDueAt
      };
    }
    const lastAt = petLastEventAt(latestSource ?? {});
    if (lastAt) {
      return {
        key,
        label: definition.label,
        description: definition.description,
        statusText: `上次${definition.label}：${elapsedDaysLabel(lastAt)}`,
        metaText: `最近记录：${formatDateTime(lastAt)}`,
        tone: elapsedDaysTone(lastAt, definition.intervalDays, definition.warnDays),
        nextDueAt: ""
      };
    }
    return {
      key,
      label: definition.label,
      description: definition.description,
      statusText: `还没有${definition.label}记录`,
      metaText: "可以先用下面的快捷操作补第一条",
      tone: "calm",
      nextDueAt: ""
    };
  };
  return (Object.keys(petQuickActionDefinitions) as PetQuickActionKey[]).map((key) => buildItem(key));
}

function buildPetWeightChartPoints(records: AnyRow[]): PetWeightChartModel {
  const sorted = [...records].sort((left, right) => {
    const leftTime = dateTimeFromValue(text(left, "recorded_on"))?.getTime() ?? 0;
    const rightTime = dateTimeFromValue(text(right, "recorded_on"))?.getTime() ?? 0;
    return leftTime - rightTime;
  });
  const points = sorted
    .map((record) => ({
      date: text(record, "recorded_on"),
      value: Number(text(record, "weight_kg"))
    }))
    .filter((item) => item.date && Number.isFinite(item.value));
  if (points.length === 0) {
    return {
      hasData: false,
      polyline: "",
      points: [],
      minLabel: "",
      maxLabel: "",
      firstLabel: "",
      lastLabel: ""
    };
  }
  const width = 280;
  const height = 132;
  const paddingX = 12;
  const paddingY = 16;
  const values = points.map((item) => item.value);
  const minValue = Math.min(...values);
  const maxValue = Math.max(...values);
  const safeRange = maxValue === minValue ? 1 : maxValue - minValue;
  const mappedPoints: PetWeightChartPoint[] = points.map((point, index) => {
    const x =
      points.length === 1
        ? width / 2
        : paddingX + (index * (width - paddingX * 2)) / Math.max(1, points.length - 1);
    const y = height - paddingY - ((point.value - minValue) / safeRange) * (height - paddingY * 2);
    return {
      x,
      y,
      date: point.date,
      shortDate: point.date.slice(5),
      value: point.value,
      valueLabel: `${point.value.toFixed(1)}kg`
    };
  });
  return {
    hasData: true,
    polyline: mappedPoints.map((point) => `${point.x},${point.y}`).join(" "),
    points: mappedPoints,
    minLabel: `${minValue.toFixed(1)}kg`,
    maxLabel: `${maxValue.toFixed(1)}kg`,
    firstLabel: mappedPoints[0]?.shortDate ?? "",
    lastLabel: mappedPoints[mappedPoints.length - 1]?.shortDate ?? ""
  };
}

function memoryCountdownLabel(entry: AnyRow): string {
  const nextOccurrence = nextOccurrenceDate(entry);
  if (!nextOccurrence) {
    return "";
  }
  const month = `${nextOccurrence.getMonth() + 1}`.padStart(2, "0");
  const day = `${nextOccurrence.getDate()}`.padStart(2, "0");
  return `下一个日子：${nextOccurrence.getFullYear()}-${month}-${day}，还有 ${daysUntil(nextOccurrence)} 天`;
}

function memoryReminderLabel(entry: AnyRow): string {
  const nextRemindAt = text(entry, "next_remind_at");
  if (!nextRemindAt) {
    return "";
  }
  const reminderDaysBefore = numberValue(entry, "reminder_days_before");
  return `提前 ${reminderDaysBefore} 天提醒：${formatDateTime(nextRemindAt)}`;
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

function periodStageLabel(dateValue: string, startOn: string): string {
  const start = dateFromValue(startOn);
  const current = dateFromValue(dateValue);
  if (!start || !current) {
    return "经期";
  }
  const diff = Math.round((current.getTime() - start.getTime()) / 86400000) + 1;
  return `经期第${Math.max(1, diff)}天`;
}

function buildOccurrenceDate(baseDateValue: string, targetYear: number): string {
  const base = dateFromValue(baseDateValue);
  if (!base) {
    return "";
  }
  const safeDay = Math.min(base.getDate(), new Date(targetYear, base.getMonth() + 1, 0).getDate());
  return toDateValue(new Date(targetYear, base.getMonth(), safeDay, 12, 0, 0, 0));
}

function isDateBetween(dateValue: string, startOn: string, endOn: string): boolean {
  return dateValue >= startOn && dateValue <= endOn;
}

function pushCalendarEvent(target: CalendarEvent[], event: CalendarEvent, dateStart: string, dateEnd: string) {
  if (event.date < dateStart || event.date > dateEnd) {
    return;
  }
  target.push(event);
}

function buildPeriodEvents(dateStart: string, dateEnd: string): CalendarEvent[] {
  const events: CalendarEvent[] = [];
  const safePeriodDays = Math.max(1, numberValue(periodProfile.value, "period_days") || 5);
  const safeCycleDays = Math.max(15, numberValue(periodProfile.value, "cycle_days") || 28);
  const actualRanges = periodRecords.value.map((record) => {
    const startOn = text(record, "start_on");
    const endOn = text(record, "end_on") || addDays(startOn, safePeriodDays - 1);
    return { startOn, endOn };
  });

  actualRanges.forEach((range, index) => {
    if (!range.startOn) {
      return;
    }
    let cursor = range.startOn;
    while (cursor <= range.endOn) {
      pushCalendarEvent(
        events,
        {
          id: `period-actual-${index}-${cursor}`,
          date: cursor,
          type: "period",
          label: "经期",
          title: periodStageLabel(cursor, range.startOn),
          detail: "已记录本次经期",
          tone: "period",
          tab: "period"
        },
        dateStart,
        dateEnd
      );
      cursor = addDays(cursor, 1);
    }
  });

  const latestStart = text(periodRecords.value[0] ?? {}, "start_on") || text(periodProfile.value, "last_period_start");
  if (!latestStart) {
    return events;
  }

  let predictedStart = latestStart;
  while (predictedStart >= addDays(dateStart, -safeCycleDays * 2)) {
    predictedStart = addDays(predictedStart, -safeCycleDays);
  }
  predictedStart = addDays(predictedStart, safeCycleDays);

  while (predictedStart <= addDays(dateEnd, safeCycleDays * 2)) {
    const predictedEnd = addDays(predictedStart, safePeriodDays - 1);
    const overlapsActual = actualRanges.some((range) => isDateBetween(predictedStart, range.startOn, range.endOn));
    if (!overlapsActual) {
      let cursor = predictedStart;
      while (cursor <= predictedEnd) {
        pushCalendarEvent(
          events,
          {
            id: `period-predicted-${predictedStart}-${cursor}`,
            date: cursor,
            type: "period",
            label: "预计经期",
            title: periodStageLabel(cursor, predictedStart),
            detail: `预测开始：${predictedStart}`,
            tone: "period-light",
            tab: "period"
          },
          dateStart,
          dateEnd
        );
        cursor = addDays(cursor, 1);
      }
      const ovulationOn = addDays(predictedStart, -14);
      pushCalendarEvent(
        events,
        {
          id: `period-ovulation-${predictedStart}`,
          date: ovulationOn,
          type: "period",
          label: "排卵日",
          title: "排卵日估算",
          detail: `易孕期：${addDays(ovulationOn, -5)} 至 ${addDays(ovulationOn, 1)}`,
          tone: "period-ovulation",
          tab: "period"
        },
        dateStart,
        dateEnd
      );
    }
    predictedStart = addDays(predictedStart, safeCycleDays);
  }

  return events;
}

function buildCalendarEvents(dateStart: string, dateEnd: string): CalendarEvent[] {
  const events: CalendarEvent[] = [];

  todoTasks.value.forEach((task) => {
    const dueAt = text(task, "due_at");
    const date = dueAt.slice(0, 10);
    if (!date) {
      return;
    }
    pushCalendarEvent(
      events,
      {
        id: `todo-${text(task, "id")}`,
        date,
        type: "todo",
        label: text(task, "status") === "DONE" ? "已完成待办" : "待办",
        title: text(task, "title"),
        detail: `${todoScopeLabel(text(task, "task_scope"))} · ${todoAssigneeLabel(task)}`,
        tone: text(task, "status") === "DONE" ? "todo-done" : "todo",
        tab: "todo"
      },
      dateStart,
      dateEnd
    );
  });

  pendingReminders.value.forEach((reminder) => {
    const dueAt = text(reminder, "due_at");
    const date = dueAt.slice(0, 10);
    if (!date) {
      return;
    }
    pushCalendarEvent(
      events,
      {
        id: `reminder-${text(reminder, "id")}`,
        date,
        type: "reminder",
        label: "提醒",
        title: text(reminder, "title"),
        detail: formatDateTime(dueAt),
        tone: "reminder",
        tab: "reminders"
      },
      dateStart,
      dateEnd
    );
  });

  allCareRecords.value.forEach((record) => {
    const careDate = text(record, "care_date");
    if (!careDate) {
      return;
    }
    pushCalendarEvent(
      events,
      {
        id: `care-${text(record, "id")}`,
        date: careDate,
        type: "care",
        label: "养护",
        title: `${text(record, "plant_name") || "花卉"} · ${careTypeLabel(text(record, "care_type"))}`,
        detail: text(record, "detail") || text(record, "raw_text") || "已记录养护",
        tone: "care",
        tab: "care"
      },
      dateStart,
      dateEnd
    );
  });

  albumPhotos.value.forEach((entry) => {
    const entryType = text(entry, "entry_type") || "PHOTO";
    const takenOn = text(entry, "taken_on");
    const createdAt = text(entry, "created_at").slice(0, 10);
    if (entryType === "ANNIVERSARY" || entryType === "BIRTHDAY") {
      const visibleYear = dateFromValue(visibleMonth.value)?.getFullYear() ?? new Date().getFullYear();
      const date = buildOccurrenceDate(takenOn, visibleYear);
      if (!date) {
        return;
      }
      pushCalendarEvent(
        events,
        {
          id: `memory-recurring-${text(entry, "id")}-${visibleYear}`,
          date,
          type: "memory",
          label: memoryEntryTypeLabel(entryType),
          title: text(entry, "title"),
          detail: memoryCountdownLabel(entry),
          tone: "memory",
          tab: "album"
        },
        dateStart,
        dateEnd
      );
      return;
    }
    const date = takenOn || createdAt;
    if (!date) {
      return;
    }
    pushCalendarEvent(
      events,
      {
        id: `memory-${text(entry, "id")}`,
        date,
        type: "memory",
        label: memoryEntryTypeLabel(entryType),
        title: text(entry, "title"),
        detail: text(entry, "description") || text(entry, "wish_text") || "已记录时刻",
        tone: "memory",
        tab: "album"
      },
      dateStart,
      dateEnd
    );
  });

  return [...events, ...buildPeriodEvents(dateStart, dateEnd)].sort((left, right) => {
    if (left.date !== right.date) {
      return left.date.localeCompare(right.date);
    }
    return left.label.localeCompare(right.label, "zh-Hans-CN");
  });
}

function buildCalendarCells(monthStart: string) {
  const firstDay = dateFromValue(monthStart) ?? new Date();
  const gridStart = new Date(firstDay);
  const offset = (firstDay.getDay() + 6) % 7;
  gridStart.setDate(firstDay.getDate() - offset);
  const events = buildCalendarEvents(toDateValue(gridStart), toDateValue(new Date(gridStart.getFullYear(), gridStart.getMonth(), gridStart.getDate() + 41, 12, 0, 0, 0)));
  const eventMap = new Map<string, CalendarEvent[]>();
  events.forEach((event) => {
    const list = eventMap.get(event.date) ?? [];
    list.push(event);
    eventMap.set(event.date, list);
  });
  return Array.from({ length: 42 }, (_, index) => {
    const date = new Date(gridStart);
    date.setDate(gridStart.getDate() + index);
    const dateValue = toDateValue(date);
    const dayEvents = eventMap.get(dateValue) ?? [];
    return {
      date: dateValue,
      day: date.getDate(),
      inMonth: date.getMonth() === firstDay.getMonth(),
      isToday: dateValue === getTodayDateValue(),
      isSelected: dateValue === selectedCalendarDate.value,
      events: dayEvents.slice(0, 3),
      eventCount: dayEvents.length
    };
  });
}

const visibleMonthLabel = computed(() => formatMonthLabel(visibleMonth.value));
const calendarCells = computed(() => buildCalendarCells(visibleMonth.value));
const selectedDayEvents = computed(() => buildCalendarEvents(selectedCalendarDate.value, selectedCalendarDate.value));

function selectCalendarDate(dateValue: string) {
  selectedCalendarDate.value = dateValue;
}

function openCalendarMonth(offset: number) {
  visibleMonth.value = shiftMonth(visibleMonth.value, offset);
  const firstVisibleDate = visibleMonth.value.slice(0, 7);
  if (!selectedCalendarDate.value.startsWith(firstVisibleDate)) {
    selectedCalendarDate.value = visibleMonth.value;
  }
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

function resetPlantDraft() {
  plantDraft.value = {
    name: "",
    flowerColor: "",
    location: "",
    carePreference: "",
    acquiredOn: "",
    status: "GROWING",
    coverUrl: ""
  };
  plantFile.value = null;
}

function resetCareDraft(plantId = "") {
  careDraft.value.plantId = plantId;
  careDraft.value.careType = "WATER";
  careDraft.value.detail = "";
  careDraft.value.nextCareAt = "";
}

function syncPlantEditDraft(plant: AnyRow) {
  plantEditDraft.value = {
    id: text(plant, "id"),
    name: text(plant, "name"),
    flowerColor: text(plant, "flower_color"),
    location: text(plant, "location"),
    carePreference: text(plant, "care_preference"),
    acquiredOn: text(plant, "acquired_on"),
    status: text(plant, "status") || "GROWING",
    coverUrl: text(plant, "cover_url")
  };
  plantEditFile.value = null;
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
    assigneeIds: [],
    taskType: "TEMPORARY",
    cycleRule: "",
    note: "",
    dueAt: selectedCalendarDate.value ? `${selectedCalendarDate.value}T18:00` : ""
  };
}

function syncTodoEditDraft(task: AnyRow) {
  todoEditDraft.value = {
    id: text(task, "id"),
    title: text(task, "title"),
    taskScope: text(task, "task_scope") || "PERSONAL",
    assigneeId: text(task, "assignee_id"),
    assigneeIds: splitIds(text(task, "assignee_ids")),
    taskType: text(task, "task_type") || "TEMPORARY",
    cycleRule: text(task, "cycle_rule"),
    note: text(task, "note"),
    dueAt: text(task, "due_at")
  };
}

function resetPrivateDraft() {
  privateDraft.value = {
    content: "",
    visibility: "TO_PARTNER",
    receiverMemberId: text(otherMembers.value[0] ?? {}, "id"),
    messageDate: selectedCalendarDate.value || getTodayDateValue()
  };
}

function syncPrivateEditDraft(messageItem: AnyRow) {
  privateEditDraft.value = {
    id: text(messageItem, "id"),
    content: text(messageItem, "content"),
    visibility: text(messageItem, "visibility") || "TO_PARTNER",
    receiverMemberId: text(messageItem, "receiver_member_id"),
    messageDate: text(messageItem, "message_date") || text(messageItem, "created_at").slice(0, 10)
  };
}

function syncPeriodProfileDraft() {
  periodProfileDraft.value = {
    cycleDays: String(numberValue(periodProfile.value, "cycle_days") || 28),
    periodDays: String(numberValue(periodProfile.value, "period_days") || 5),
    lastPeriodStart: text(periodProfile.value, "last_period_start"),
    reminderEnabled:
      text(periodProfile.value, "reminder_enabled") === "true" || text(periodProfile.value, "reminder_enabled") === "1",
    reminderTime: text(periodProfile.value, "reminder_time") || "09:00",
    note: text(periodProfile.value, "note")
  };
}

function resetPeriodRecordDraft() {
  periodRecordDraft.value = {
    startOn: selectedCalendarDate.value || getTodayDateValue(),
    endOn: "",
    note: ""
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

function decisionOptions(): string[] {
  return decisionOptionsText.value
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function clearDecisionHistory() {
  decisionHistory.value = [];
}

function makeDecision() {
  const options = decisionOptions();
  if (decisionRolling.value) {
    return;
  }
  if (options.length === 0) {
    decisionResult.value = "先写几个选项吧";
    showMessage("请至少填写一个选项", "error");
    return;
  }
  if (options.length === 1) {
    decisionResult.value = options[0];
    decisionHistory.value = [options[0], ...decisionHistory.value.filter((item) => item !== options[0])].slice(0, 5);
    return;
  }
  decisionRolling.value = true;
  decisionResult.value = "正在摇摆...";
  let count = 0;
  decisionTimer = window.setInterval(() => {
    decisionResult.value = options[Math.floor(Math.random() * options.length)];
    count += 1;
    if (count < 16) {
      return;
    }
    if (decisionTimer) {
      window.clearInterval(decisionTimer);
      decisionTimer = undefined;
    }
    const finalResult = options[Math.floor(Math.random() * options.length)];
    decisionResult.value = finalResult;
    decisionHistory.value = [finalResult, ...decisionHistory.value.filter((item) => item !== finalResult)].slice(0, 5);
    decisionRolling.value = false;
  }, 90);
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

function resetPetQuickActionDraft(action: PetQuickActionKey = "FEED") {
  petQuickActionDraft.value = {
    action,
    recordedAt: getNowDateTimeValue(),
    description: "",
    nextDueAt: ""
  };
}

function resetPetWeightDraft() {
  petWeightDraft.value = {
    weightKg: "",
    recordedOn: getTodayDateValue(),
    note: ""
  };
}

function triggerPlantAchievement(content: PlantAchievementContent) {
  plantAchievementKey.value += 1;
  plantAchievementTitle.value = content.title;
  plantAchievementSubtitle.value = content.subtitle;
  plantAchievementVisible.value = true;
  if (plantAchievementTimer) {
    window.clearTimeout(plantAchievementTimer);
  }
  plantAchievementTimer = window.setTimeout(() => {
    plantAchievementVisible.value = false;
  }, 1400);
}

function openTab(tab: TabKey) {
  activeTab.value = tab;
  closePlantCarePanel();
  petQuickActionOpen.value = false;
  petWeightOpen.value = false;
  if (tab === "todo") {
    todoView.value = "list";
  }
  if (tab === "finance") {
    financeView.value = "list";
  }
  if (tab === "private") {
    privateView.value = "list";
  }
  if (tab === "period") {
    periodView.value = "list";
  }
  if (tab === "plants") {
    plantView.value = "list";
  }
  if (tab === "care") {
    careView.value = "list";
  }
  if (tab === "shopping") {
    shoppingView.value = "list";
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
  if (tab === "votes") {
    voteView.value = "list";
  }
  if (tab === "inventory") {
    inventoryView.value = "list";
  }
  if (tab === "recipes") {
    recipeView.value = "list";
  }
  if (tab === "pets") {
    petView.value = "list";
  }
}

function goHome() {
  closePlantCarePanel();
  petQuickActionOpen.value = false;
  petWeightOpen.value = false;
  activeTab.value = "today";
}

function goBack() {
  if (activeTab.value === "today") {
    return;
  }
  if (activeTab.value === "todo") {
    if (todoView.value !== "list") {
      todoView.value = "list";
      return;
    }
    goHome();
    return;
  }
  if (activeTab.value === "finance") {
    if (financeView.value !== "list") {
      financeView.value = "list";
      return;
    }
    goHome();
    return;
  }
  if (activeTab.value === "private") {
    if (privateView.value !== "list") {
      privateView.value = "list";
      return;
    }
    goHome();
    return;
  }
  if (activeTab.value === "period") {
    if (periodView.value !== "list") {
      periodView.value = "list";
      return;
    }
    goHome();
    return;
  }
  if (activeTab.value === "plants") {
    if (plantView.value !== "list") {
      plantView.value = "list";
      return;
    }
    goHome();
    return;
  }
  if (activeTab.value === "care") {
    if (careView.value !== "list") {
      careView.value = "list";
      return;
    }
    goHome();
    return;
  }
  if (activeTab.value === "shopping") {
    if (shoppingView.value !== "list") {
      shoppingView.value = "list";
      return;
    }
    goHome();
    return;
  }
  if (activeTab.value === "reminders") {
    if (reminderView.value !== "list") {
      reminderView.value = "list";
      return;
    }
    goHome();
    return;
  }
  if (activeTab.value === "profile") {
    if (profileView.value !== "detail") {
      profileView.value = "detail";
      return;
    }
    goHome();
    return;
  }
  if (activeTab.value === "memberAdd" || activeTab.value === "memberEdit") {
    activeTab.value = "members";
    return;
  }
  if (activeTab.value === "members") {
    goHome();
    return;
  }
  if (activeTab.value === "album") {
    if (memoryView.value !== "list") {
      memoryView.value = "list";
      return;
    }
    goHome();
    return;
  }
  if (activeTab.value === "votes") {
    if (voteView.value !== "list") {
      voteView.value = "list";
      return;
    }
    goHome();
    return;
  }
  if (activeTab.value === "inventory") {
    if (inventoryView.value !== "list") {
      inventoryView.value = "list";
      return;
    }
    goHome();
    return;
  }
  if (activeTab.value === "recipes") {
    if (recipeView.value === "mealPlanCreate") {
      recipeView.value = "week";
      return;
    }
    if (recipeView.value !== "list") {
      recipeView.value = "list";
      return;
    }
    goHome();
    return;
  }
  if (activeTab.value === "pets") {
    if (petView.value === "photoCreate") {
      petView.value = "photos";
      return;
    }
    if (petView.value === "medicalCreate") {
      petView.value = "medical";
      return;
    }
    if (petView.value === "photos" || petView.value === "medical" || petView.value === "edit") {
      petView.value = "detail";
      return;
    }
    if (petView.value !== "list") {
      petView.value = "list";
      return;
    }
    goHome();
    return;
  }
  goHome();
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

function startCreatePlant() {
  resetPlantDraft();
  plantView.value = "create";
}

function startEditPlant(plant: AnyRow) {
  selectedPlantId.value = text(plant, "id");
  syncPlantEditDraft(plant);
  plantView.value = "edit";
}

function openPlantCarePanel(plant: AnyRow) {
  const plantId = text(plant, "id");
  if (!plantId) {
    return;
  }
  selectedPlantId.value = plantId;
  plantQuickCarePlantId.value = plantId;
  resetCareDraft(plantId);
}

function closePlantCarePanel() {
  plantQuickCarePlantId.value = "";
  resetCareDraft(selectedPlantId.value);
}

async function submitPlantCheckIn(plant: AnyRow) {
  const plantId = numberValue(plant, "id");
  const plantName = text(plant, "name") || "花花";
  if (!Number.isFinite(plantId) || plantId <= 0) {
    showMessage("请先补全花卉档案", "error");
    return;
  }
  if (hasPlantCheckInOnDate(String(plantId))) {
    showMessage(`${plantName} 今天已经记录过了`, "info");
    return;
  }
  await saveCareRecord(
    plantId,
    { careType: "WATER", detail: "", nextCareAt: "" },
    `plant-check-in-${plantId}`,
    `${plantName} 今日打卡成功`
  );
}

function startCreateShopping() {
  shoppingDraft.value = {
    name: "",
    quantity: "",
    category: "DAILY"
  };
  shoppingView.value = "create";
}

function startCreateReminder() {
  reminderView.value = "create";
}

function startCreateFinanceRecord() {
  financeDraft.value = {
    title: "",
    amount: "",
    category: "MEAL",
    occurredOn: selectedCalendarDate.value || ""
  };
  financeView.value = "create";
}

function startCreatePrivateMessage() {
  resetPrivateDraft();
  privateView.value = "create";
}

function startEditPrivateMessage(messageItem: AnyRow) {
  syncPrivateEditDraft(messageItem);
  privateView.value = "edit";
}

function startEditPeriodProfile() {
  syncPeriodProfileDraft();
  periodView.value = "profile";
}

function startCreatePeriodRecord() {
  resetPeriodRecordDraft();
  periodView.value = "record";
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

function openPetQuickAction(action: PetQuickActionKey) {
  if (!selectedPetId.value) {
    showMessage("请先选择宠物", "error");
    return;
  }
  resetPetQuickActionDraft(action);
  petWeightOpen.value = false;
  petQuickActionOpen.value = true;
}

function openPetWeightCreate() {
  if (!selectedPetId.value) {
    showMessage("请先选择宠物", "error");
    return;
  }
  resetPetWeightDraft();
  petQuickActionOpen.value = false;
  petWeightOpen.value = true;
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

async function setHomeViewMode(mode: HomeViewMode) {
  if (homeViewMode.value === mode) {
    return;
  }
  const previousMode = savedHomeViewMode.value;
  homeViewMode.value = mode;
  try {
    await api.updateHomeViewMode({
      viewMode: mode
    });
    savedHomeViewMode.value = mode;
  } catch (error) {
    homeViewMode.value = previousMode;
    handleRequestError(error, "首页模式保存失败");
  }
}

function beginHomeCardDrag(cardKey: DrawerEntryKey, event: PointerEvent) {
  if (typeof window === "undefined" || typeof document === "undefined") {
    return;
  }
  homeCardDraggingKey.value = cardKey;
  const handlePointerMove = (moveEvent: PointerEvent) => {
    const element = document.elementFromPoint(moveEvent.clientX, moveEvent.clientY);
    const targetKey = element?.closest("[data-drawer-entry-key]")?.getAttribute("data-drawer-entry-key") || "";
    if (isDrawerEntryKey(targetKey)) {
      moveDrawerEntry(cardKey, targetKey);
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
  if (plantAchievementTimer) {
    window.clearTimeout(plantAchievementTimer);
    plantAchievementTimer = undefined;
  }
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
  allCareRecords.value = [];
  periodSummary.value = {
    profile: {
      cycle_days: 28,
      period_days: 5,
      last_period_start: "",
      reminder_enabled: true,
      reminder_time: "09:00",
      note: "",
      has_profile: false
    },
    records: [],
    prediction: {}
  };
  privateMessages.value = [];
  albumPhotos.value = [];
  inventoryItems.value = [];
  familyVotes.value = [];
  recipes.value = [];
  mealPlans.value = [];
  pets.value = [];
  petPhotos.value = [];
  petMedicalRecords.value = [];
  petCareRecords.value = [];
  petWeightRecords.value = [];
  plantCheckInDates.value = [];
  plantCheckInEntries.value = [];
  selectedTodoId.value = "";
  selectedPlantId.value = "";
  plantQuickCarePlantId.value = "";
  selectedMemoryId.value = "";
  selectedCalendarDate.value = getTodayDateValue();
  selectedInventoryId.value = "";
  selectedRecipeId.value = "";
  selectedPetId.value = "";
  visibleMonth.value = getMonthStartValue();
  petQuickActionOpen.value = false;
  petWeightOpen.value = false;
  plantAchievementVisible.value = false;
  plantAchievementTitle.value = "守护天数 +1";
  plantAchievementSubtitle.value = "今天也把花花照顾得很好";
  resetPetQuickActionDraft();
  resetPetWeightDraft();
  resetCareDraft();
  if (decisionTimer) {
    window.clearInterval(decisionTimer);
    decisionTimer = undefined;
  }
  decisionOptionsText.value = "喝酒\n吃烧烤\n看电影\n看电视剧\n看综艺\n打游戏\n喝酒看电视剧/综艺一起\n打小谭";
  decisionResult.value = "";
  decisionHistory.value = [];
  decisionRolling.value = false;
  plantView.value = "list";
  careView.value = "list";
  todoView.value = "list";
  todoFilter.value = "all";
  financeView.value = "list";
  privateView.value = "list";
  periodView.value = "list";
  reminderView.value = "list";
  profileView.value = "detail";
  memoryView.value = "list";
  inventoryView.value = "list";
  voteView.value = "list";
  recipeView.value = "list";
  shoppingView.value = "list";
  homeViewMode.value = "cards";
  savedHomeViewMode.value = "cards";
  petView.value = "list";
  profilePasswordVisible.value = false;
  profilePasswordHintVisible.value = false;
  memberEditPasswordVisible.value = false;
  calendarTodoDraft.value = {
    title: "",
    time: "18:00"
  };
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
      periodData,
      privateData,
      albumData,
      inventoryData,
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
      api.periodMine(),
      api.privateMessages(),
      api.albumPhotos(),
      api.inventoryItems(),
      api.recipes(),
      api.mealPlans(mealPlanWeekStart.value),
      api.pets()
    ]);
    currentUser.value = meData;
    today.value = todayData;
    family.value = familyData;
    applyHomeCardOrder(readHomeCardOrder(familyData));
    applyHomeViewMode(readHomeViewMode(familyData));
    plantCheckInDates.value = readPlantCheckInDates(familyData);
    plantCheckInEntries.value = readPlantCheckInEntries(familyData);
    todoTasks.value = choreData;
    reminders.value = reminderData;
    plants.value = plantData;
    shoppingItems.value = shoppingData;
    financeRecords.value = financeData;
    financeOverview.value = financeOverviewData;
    financeCategories.value = categoryData;
    periodSummary.value = periodData;
    privateMessages.value = privateData;
    albumPhotos.value = albumData;
    inventoryItems.value = inventoryData;
    recipes.value = recipeData;
    mealPlans.value = mealPlanData;
    pets.value = petData;
    if (profileView.value !== "edit") {
      syncProfileDraft();
    }
    if (periodView.value !== "profile") {
      syncPeriodProfileDraft();
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
      selectedPlantId.value = "";
      plantQuickCarePlantId.value = "";
      careDraft.value.plantId = "";
    } else {
      if (!plantData.some((plant) => text(plant, "id") === selectedPlantId.value)) {
        selectedPlantId.value = text(plantData[0], "id");
      }
      if (plantQuickCarePlantId.value && !plantData.some((plant) => text(plant, "id") === plantQuickCarePlantId.value)) {
        closePlantCarePanel();
      } else if (plantQuickCarePlantId.value) {
        careDraft.value.plantId = plantQuickCarePlantId.value;
      } else if (!plantData.some((plant) => text(plant, "id") === careDraft.value.plantId)) {
        careDraft.value.plantId = selectedPlantId.value;
      }
    }

    if (!privateDraft.value.receiverMemberId && otherMembers.value.length > 0) {
      privateDraft.value.receiverMemberId = text(otherMembers.value[0], "id");
    } else if (
      privateDraft.value.receiverMemberId &&
      !otherMembers.value.some((member) => text(member, "id") === privateDraft.value.receiverMemberId)
    ) {
      privateDraft.value.receiverMemberId = text(otherMembers.value[0] ?? {}, "id");
    }

    await Promise.all([loadPetDetails(true), loadCareRecords(true), loadAllCareRecords(plantData, true)]);
  } finally {
    if (!silent) {
      loading.value = false;
    }
  }
}

async function loadAllCareRecords(plantData = plants.value, silent = false) {
  if (plantData.length === 0) {
    allCareRecords.value = [];
    return;
  }
  try {
    const result = await Promise.all(
      plantData.map(async (plant) => {
        const plantId = Number(text(plant, "id"));
        const recordsOfPlant = await api.careRecords(plantId);
        return recordsOfPlant.map((record) => ({
          ...record,
          plant_name: text(plant, "name")
        }));
      })
    );
    allCareRecords.value = result.flat();
  } catch (error) {
    if (silent) {
      throw error;
    }
    handleRequestError(error, "养护日历加载失败");
  }
}

async function loadCareRecords(silent = false) {
  const plantId = Number(selectedPlantId.value);
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
  assigneeIds: string[];
  taskType: string;
  cycleRule: string;
  note: string;
  dueAt: string;
}) {
  const assigneeIds =
    source.taskScope === "SHARED"
      ? source.assigneeIds
          .map((item) => Number(item))
          .filter((item) => Number.isFinite(item) && item > 0)
      : [];
  return {
    title: source.title,
    taskScope: source.taskScope,
    assigneeId: source.taskScope === "SHARED" && source.assigneeId ? Number(source.assigneeId) : undefined,
    assigneeIds: assigneeIds.length > 0 ? assigneeIds : undefined,
    taskType: source.taskType,
    cycleRule: source.cycleRule,
    note: source.note,
    dueAt: source.dueAt || undefined
  };
}

function toggleTodoAssignee(target: typeof todoDraft.value | typeof todoEditDraft.value, memberId: string) {
  if (!memberId) {
    return;
  }
  const exists = target.assigneeIds.includes(memberId);
  target.assigneeIds = exists ? target.assigneeIds.filter((item) => item !== memberId) : [...target.assigneeIds, memberId];
  if (!target.assigneeId || !target.assigneeIds.includes(target.assigneeId)) {
    target.assigneeId = target.assigneeIds[0] ?? "";
  }
}

function canClaimTask(task: AnyRow): boolean {
  if (text(task, "status") !== "TODO" || text(task, "task_scope") !== "SHARED") {
    return false;
  }
  return !hasTaskAssignee(task, String(currentUser.value.memberId ?? ""));
}

async function submitCalendarTodo() {
  if (!calendarTodoDraft.value.title.trim()) {
    showMessage("请先填写待办标题", "error");
    return;
  }
  const dueAt = selectedCalendarDate.value
    ? `${selectedCalendarDate.value}T${calendarTodoDraft.value.time || "18:00"}`
    : undefined;
  await executeAction("calendar-todo-create", "日历待办创建失败", async () => {
    await api.createTodo({
      title: calendarTodoDraft.value.title,
      taskScope: "PERSONAL",
      dueAt
    });
    calendarTodoDraft.value.title = "";
    await loadAll();
    showMessage("待办已加入日历");
  });
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
    showMessage("已加入认领名单");
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
    activeTab.value = "votes";
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

async function submitPlantCreate() {
  if (!plantDraft.value.name.trim()) {
    showMessage("请先填写花卉名称", "error");
    return;
  }
  await executeAction("plant-create", "新增花卉失败", async () => {
    const coverUrl = await uploadSelectedImage(plantFile.value, "PLANT", undefined, "花卉照片");
    await api.createPlant({
      name: plantDraft.value.name,
      flowerColor: plantDraft.value.flowerColor,
      location: plantDraft.value.location,
      carePreference: plantDraft.value.carePreference,
      acquiredOn: plantDraft.value.acquiredOn || undefined,
      status: plantDraft.value.status,
      coverUrl: coverUrl || plantDraft.value.coverUrl || undefined
    });
    resetPlantDraft();
    plantView.value = "list";
    await loadAll();
    showMessage("花卉档案已创建");
  });
}

async function submitPlantEdit() {
  const plantId = Number(plantEditDraft.value.id);
  if (!Number.isFinite(plantId) || plantId <= 0 || !plantEditDraft.value.name.trim()) {
    showMessage("请先完善花卉信息", "error");
    return;
  }
  await executeAction("plant-update", "更新花卉失败", async () => {
    const coverUrl = await uploadSelectedImage(plantEditFile.value, "PLANT", plantId, "花卉照片");
    await api.updatePlant(plantId, {
      name: plantEditDraft.value.name,
      flowerColor: plantEditDraft.value.flowerColor,
      location: plantEditDraft.value.location,
      carePreference: plantEditDraft.value.carePreference,
      acquiredOn: plantEditDraft.value.acquiredOn || undefined,
      status: plantEditDraft.value.status,
      coverUrl: coverUrl || plantEditDraft.value.coverUrl || undefined
    });
    plantEditFile.value = null;
    plantView.value = "list";
    await loadAll();
    showMessage("花卉档案已更新");
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

async function saveCareRecord(
  plantId: number,
  draft: { careType: string; detail: string; nextCareAt: string },
  actionName: string,
  successMessage: string
) {
  if (!Number.isFinite(plantId) || plantId <= 0) {
    showMessage("请先选择花卉", "error");
    return false;
  }
  const detail = draft.detail.trim();
  const detailText = detail || `${careTypeLabel(draft.careType)}已完成`;
  let created = false;
  await executeAction(actionName, "保存养护记录失败", async () => {
    const previousCareDays = plantCareDaysThisMonth.value;
    await api.createCareRecord(plantId, {
      careType: draft.careType,
      careDate: getTodayDateValue(),
      detail: detailText,
      rawText: detailText,
      nextCareAt: draft.nextCareAt || undefined
    });
    selectedPlantId.value = String(plantId);
    await loadAll();
    triggerPlantAchievement(resolvePlantAchievementContent(previousCareDays, plantCareDaysThisMonth.value));
    showMessage(successMessage);
    created = true;
  });
  return created;
}

async function submitPlantCareRecord() {
  const plantId = Number(plantQuickCarePlantId.value || careDraft.value.plantId);
  const plantName = text(quickCarePlant.value ?? {}, "name") || "花花";
  const saved = await saveCareRecord(plantId, careDraft.value, `care-create-${plantId}`, `${plantName} 养护已保存`);
  if (!saved) {
    return;
  }
  closePlantCarePanel();
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
    shoppingView.value = "list";
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
    financeView.value = "list";
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
    const receiverMemberId = Number(privateDraft.value.receiverMemberId);
    await api.createPrivateMessage({
      content: privateDraft.value.content,
      visibility: privateDraft.value.visibility,
      receiverMemberId:
        privateDraft.value.visibility === "TO_PARTNER" && Number.isFinite(receiverMemberId) && receiverMemberId > 0
          ? receiverMemberId
          : undefined,
      messageDate: privateDraft.value.messageDate || undefined
    });
    resetPrivateDraft();
    privateView.value = "list";
    await loadAll();
    showMessage("留言已保存");
  });
}

async function submitPrivateMessageEdit() {
  const id = Number(privateEditDraft.value.id);
  if (!Number.isFinite(id) || id <= 0 || !privateEditDraft.value.content.trim()) {
    showMessage("请先完善留言内容", "error");
    return;
  }
  await executeAction("private-update", "更新留言失败", async () => {
    const receiverMemberId = Number(privateEditDraft.value.receiverMemberId);
    await api.updatePrivateMessage(id, {
      content: privateEditDraft.value.content,
      visibility: privateEditDraft.value.visibility,
      receiverMemberId:
        privateEditDraft.value.visibility === "TO_PARTNER" && Number.isFinite(receiverMemberId) && receiverMemberId > 0
          ? receiverMemberId
          : undefined,
      messageDate: privateEditDraft.value.messageDate || undefined
    });
    privateView.value = "list";
    await loadAll();
    showMessage("留言已更新");
  });
}

async function markPrivateMessageRead(id: number) {
  await executeAction(`private-read-${id}`, "标记已读失败", async () => {
    await api.readPrivateMessage(id);
    await loadAll();
    showMessage("已标记为已读");
  });
}

async function submitPeriodProfile() {
  const cycleDays = Number(periodProfileDraft.value.cycleDays);
  const periodDays = Number(periodProfileDraft.value.periodDays);
  if (!Number.isFinite(cycleDays) || cycleDays <= 0 || !Number.isFinite(periodDays) || periodDays <= 0) {
    showMessage("请先填写正确的周期和经期天数", "error");
    return;
  }
  await executeAction("period-profile", "保存周期设置失败", async () => {
    await api.savePeriodProfile({
      cycleDays,
      periodDays,
      lastPeriodStart: periodProfileDraft.value.lastPeriodStart || undefined,
      reminderEnabled: periodProfileDraft.value.reminderEnabled,
      reminderTime: periodProfileDraft.value.reminderTime || undefined,
      note: periodProfileDraft.value.note || undefined
    });
    periodView.value = "list";
    await loadAll();
    showMessage("周期设置已保存");
  });
}

async function submitPeriodRecord() {
  if (!periodRecordDraft.value.startOn) {
    showMessage("请先选择经期开始日期", "error");
    return;
  }
  await executeAction("period-record", "保存经期记录失败", async () => {
    await api.createPeriodRecord({
      startOn: periodRecordDraft.value.startOn,
      endOn: periodRecordDraft.value.endOn || undefined,
      note: periodRecordDraft.value.note || undefined
    });
    periodView.value = "list";
    await loadAll();
    showMessage("经期记录已保存");
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
    const [, reminderData, todayData] = await Promise.all([loadPetDetails(true), api.reminders(), api.today()]);
    reminders.value = reminderData;
    today.value = todayData;
    showMessage("宠物医疗记录已保存");
  });
}

async function submitPetQuickAction() {
  const petId = Number(selectedPetId.value);
  if (!Number.isFinite(petId) || petId <= 0) {
    showMessage("请先选择宠物", "error");
    return;
  }
  const actionCode = petQuickActionDraft.value.action as PetQuickActionKey;
  const safeDescription = petQuickActionDraft.value.description.trim() || `${petCareTypeLabel(actionCode)}已完成`;
  await executeAction("pet-quick-action", "保存快捷护理失败", async () => {
    await api.createPetCareRecord(petId, {
      careType: actionCode,
      recordedAt: petQuickActionDraft.value.recordedAt || getNowDateTimeValue(),
      description: safeDescription,
      nextDueAt: petQuickActionDraft.value.nextDueAt || undefined
    });
    petQuickActionOpen.value = false;
    resetPetQuickActionDraft(actionCode);
    const [, reminderData, todayData] = await Promise.all([loadPetDetails(true), api.reminders(), api.today()]);
    reminders.value = reminderData;
    today.value = todayData;
    showMessage(`${petCareTypeLabel(actionCode)}已记录`);
  });
}

async function submitPetWeightRecord() {
  const petId = Number(selectedPetId.value);
  const weightKg = Number(petWeightDraft.value.weightKg);
  if (!Number.isFinite(petId) || petId <= 0) {
    showMessage("请先选择宠物", "error");
    return;
  }
  if (!Number.isFinite(weightKg) || weightKg <= 0) {
    showMessage("请输入正确的体重", "error");
    return;
  }
  await executeAction("pet-weight-create", "保存体重记录失败", async () => {
    await api.createPetWeightRecord(petId, {
      weightKg,
      recordedOn: petWeightDraft.value.recordedOn || getTodayDateValue(),
      note: petWeightDraft.value.note || undefined
    });
    petWeightOpen.value = false;
    resetPetWeightDraft();
    await loadPetDetails();
    showMessage("体重已记录");
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
    petCareRecords.value = [];
    petWeightRecords.value = [];
    return;
  }
  try {
    const [photoData, medicalData, careData, weightData] = await Promise.all([
      api.petPhotos(petId),
      api.petMedicalRecords(petId),
      api.petCareRecords(petId),
      api.petWeightRecords(petId)
    ]);
    petPhotos.value = photoData;
    petMedicalRecords.value = medicalData;
    petCareRecords.value = careData;
    petWeightRecords.value = weightData;
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
    const [, reminderData, todayData] = await Promise.all([loadPetDetails(true), api.reminders(), api.today()]);
    reminders.value = reminderData;
    today.value = todayData;
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
  if (plantAchievementTimer) {
    window.clearTimeout(plantAchievementTimer);
    plantAchievementTimer = undefined;
  }
  if (decisionTimer) {
    window.clearInterval(decisionTimer);
    decisionTimer = undefined;
  }
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
  () => selectedPlantId.value,
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
    <header :class="['topbar', { 'home-topbar': activeTab === 'today' }]">
      <div :class="['topbar-main', { 'home-topbar-main': activeTab === 'today' }]">
        <button
          v-if="activeTab !== 'today'"
          class="icon-button"
          type="button"
          aria-label="返回上一层"
          title="返回上一层"
          @click="goBack()"
        >
          <ArrowLeft :size="19" />
        </button>
        <div :class="{ 'home-topbar-copy': activeTab === 'today' }">
          <p class="eyebrow">{{ activeTab === "today" ? "Home Of Us" : "返回上一层" }}</p>
          <h1>{{ headerTitle }}</h1>
        </div>
      </div>
      <div class="top-actions">
        <button v-if="activeTab === 'today'" class="user-chip" type="button" @click="openTab('profile')">
          <span
            class="user-chip-avatar"
            :style="{ background: text(currentUser, 'avatarUrl') ? 'transparent' : text(currentUser, 'avatarColor') || '#2F6B4F' }"
          >
            <img
              v-if="text(currentUser, 'avatarUrl')"
              :src="text(currentUser, 'avatarUrl')"
              alt="头像"
              loading="lazy"
              decoding="async"
            />
            <template v-else>{{ (text(currentUser, "displayName") || "我").slice(0, 1) }}</template>
          </span>
          <span>{{ text(currentUser, "displayName") }}</span>
        </button>
        <button
          v-if="activeTab !== 'today'"
          class="icon-button"
          type="button"
          aria-label="返回首页"
          title="返回首页"
          @click="goHome()"
        >
          <Home :size="18" />
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

    <section v-if="activeTab === 'today'" class="view home-view">
      <div class="summary-strip home-summary-strip">
        <button class="mini-metric mini-metric-button" type="button" aria-label="打开待办页面" @click="openTab('todo')">
          <span>待办</span>
          <strong>{{ openTodoTasks.length }}</strong>
          <small>{{ myTodoTasks.length }} 条是我自己要处理的</small>
        </button>
        <button class="mini-metric mini-metric-button" type="button" aria-label="打开提醒页面" @click="openTab('reminders')">
          <span>提醒</span>
          <strong>{{ pendingReminders.length }}</strong>
          <small>{{ pendingPlantCareReminders.length }} 条和花花养护有关</small>
        </button>
        <button class="mini-metric mini-metric-button" type="button" aria-label="打开记账页面" @click="openTab('finance')">
          <span>本月支出</span>
          <strong>{{ numberValue(financeOverview, "monthExpense") }}</strong>
          <small>本周 {{ numberValue(financeOverview, "weekExpense") }}</small>
        </button>
      </div>

      <div class="home-view-switch">
        <button
          :class="['home-view-button', { active: homeViewMode === 'cards' }]"
          type="button"
          @click="setHomeViewMode('cards')"
        >
          <LayoutGrid :size="16" />
          <span>卡片模式</span>
        </button>
        <button
          :class="['home-view-button', { active: homeViewMode === 'calendar' }]"
          type="button"
          @click="setHomeViewMode('calendar')"
        >
          <CalendarDays :size="16" />
          <span>日历模式</span>
        </button>
      </div>

      <template v-if="homeViewMode === 'calendar'">
        <article class="list-card calendar-card calendar-card-compact">
          <div class="section-title">
            <button class="icon-button" type="button" aria-label="上个月" title="上个月" @click="openCalendarMonth(-1)">
              <ArrowLeft :size="17" />
            </button>
            <h2>{{ visibleMonthLabel }}</h2>
            <button class="icon-button" type="button" aria-label="下个月" title="下个月" @click="openCalendarMonth(1)">
              <ArrowLeft class="calendar-next-icon" :size="17" />
            </button>
          </div>
          <div class="calendar-grid calendar-weekdays compact-weekdays">
            <span v-for="label in weekDayLabels" :key="label">{{ label }}</span>
          </div>
          <div class="calendar-grid compact-calendar-grid">
            <button
              v-for="cell in calendarCells"
              :key="cell.date"
              :class="[
                'calendar-cell',
                'calendar-cell-compact',
                {
                  'is-outside': !cell.inMonth,
                  'is-today': cell.isToday,
                  'is-selected': cell.isSelected
                }
              ]"
              type="button"
              @click="selectCalendarDate(cell.date)"
            >
              <strong>{{ cell.day }}</strong>
              <div class="calendar-dots">
                <span
                  v-for="event in cell.events"
                  :key="event.id"
                  :class="['calendar-dot', `calendar-dot-${event.tone}`]"
                />
                <small v-if="cell.eventCount > 3">+{{ cell.eventCount - 3 }}</small>
              </div>
            </button>
          </div>
        </article>

        <article class="list-card quick-add-card">
          <div class="section-title">
            <h2>当日待办</h2>
            <span>{{ selectedCalendarDate }}</span>
          </div>
          <div class="inline-fields">
            <input v-model="calendarTodoDraft.title" placeholder="今天要做什么" />
            <input v-model="calendarTodoDraft.time" type="time" />
          </div>
          <button class="secondary-button" :disabled="isSubmitting('calendar-todo-create')" type="button" @click="submitCalendarTodo()">
            <LoaderCircle v-if="isSubmitting('calendar-todo-create')" class="spin" :size="17" />
            <Plus v-else :size="17" />
            <span>添加到这一天</span>
          </button>
        </article>

        <article class="list-card day-detail-card">
          <div class="section-title">
            <h2>当日详情</h2>
            <span>{{ selectedDayEvents.length }}</span>
          </div>
          <p v-if="selectedDayEvents.length === 0" class="empty">这一天暂时没有安排</p>
          <article v-for="event in selectedDayEvents" :key="event.id" class="calendar-event-row">
            <span :class="['calendar-event-tag', `calendar-event-tag-${event.tone}`]">{{ event.label }}</span>
            <div class="feed-main">
              <p>{{ event.title }}</p>
              <small>{{ event.detail }}</small>
            </div>
            <button class="text-button" type="button" @click="openTab(event.tab)">
              查看
            </button>
          </article>
        </article>
      </template>

      <template v-else>
        <button class="home-entry-card home-calendar-entry" type="button" @click="setHomeViewMode('calendar')">
          <span class="home-entry-icon">
            <CalendarDays :size="18" />
          </span>
          <div class="drawer-module-copy">
            <div class="drawer-module-head">
              <strong>日历</strong>
              <small>{{ visibleMonthLabel }}</small>
            </div>
            <span>
              <template v-if="selectedDayEvents[0]">
                {{ selectedCalendarDate }} · {{ selectedDayEvents[0].title }}
              </template>
              <template v-else>{{ selectedCalendarDate }} 暂时没有安排</template>
            </span>
          </div>
        </button>

        <div class="home-module-grid">
          <template v-for="entry in drawerEntries" :key="`home-${entry.key}`">
            <article
              v-if="entry.type === 'single'"
              :data-drawer-entry-key="entry.key"
              :class="['home-entry-shell', entry.tone, { dragging: homeCardDraggingKey === entry.key }]"
            >
              <button class="home-entry-card" type="button" @click="openTab(entry.children[0].key)">
                <span class="home-entry-icon">
                  <component :is="entry.icon" :size="18" />
                </span>
                <div class="drawer-module-copy home-card-copy">
                  <div class="drawer-module-head">
                    <strong>{{ entry.label }}</strong>
                    <small>{{ entry.value }}</small>
                  </div>
                  <span>{{ entry.description }}</span>
                </div>
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

            <article
              v-else
              :data-drawer-entry-key="entry.key"
              :class="['home-group-shell', entry.tone, { dragging: homeCardDraggingKey === entry.key }]"
            >
              <div class="home-group-card">
                <div class="home-group-toggle">
                  <span class="home-entry-icon">
                    <component :is="entry.icon" :size="18" />
                  </span>
                  <div class="drawer-module-copy home-card-copy">
                    <div class="drawer-module-head">
                      <strong>{{ entry.label }}</strong>
                      <small>{{ entry.value }}</small>
                    </div>
                    <span>{{ entry.description }}</span>
                  </div>
                </div>
                <div class="home-subcard-grid">
                  <button
                    v-for="child in entry.children"
                    :key="`${entry.key}-${child.key}`"
                    class="drawer-child-button home-subcard-button"
                    type="button"
                    @click="openTab(child.key)"
                  >
                    <span>{{ child.label }}</span>
                    <small>{{ child.value }}</small>
                  </button>
                </div>
              </div>
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
          </template>
        </div>
      </template>
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
            <small>{{ todoAssigneeLabel(task) }}</small>
          </div>
          <div class="row-actions">
            <button
              v-if="canClaimTask(task)"
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
          <select v-model="todoDraft.taskType">
            <option value="TEMPORARY">临时任务</option>
            <option value="ROUTINE">周期任务</option>
          </select>
          <input v-model="todoDraft.cycleRule" placeholder="周期规则，如每周六" />
        </div>
        <div v-if="todoDraft.taskScope === 'SHARED'" class="field-stack">
          <span class="field-label">认领成员（可多选，可留空）</span>
          <div class="option-chip-group">
            <button
              v-for="member in familyMembers"
              :key="`todo-create-${text(member, 'id')}`"
              :class="['option-chip', { active: todoDraft.assigneeIds.includes(text(member, 'id')) }]"
              type="button"
              @click="toggleTodoAssignee(todoDraft, text(member, 'id'))"
            >
              <span>{{ text(member, "display_name") }}</span>
            </button>
          </div>
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
          <select v-model="todoEditDraft.taskType">
            <option value="TEMPORARY">临时任务</option>
            <option value="ROUTINE">周期任务</option>
          </select>
          <input v-model="todoEditDraft.cycleRule" placeholder="周期规则，如每周六" />
        </div>
        <div v-if="todoEditDraft.taskScope === 'SHARED'" class="field-stack">
          <span class="field-label">认领成员（可多选，可留空）</span>
          <div class="option-chip-group">
            <button
              v-for="member in familyMembers"
              :key="`todo-edit-${text(member, 'id')}`"
              :class="['option-chip', { active: todoEditDraft.assigneeIds.includes(text(member, 'id')) }]"
              type="button"
              @click="toggleTodoAssignee(todoEditDraft, text(member, 'id'))"
            >
              <span>{{ text(member, "display_name") }}</span>
            </button>
          </div>
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
      <template v-if="plantView === 'list'">
        <article class="list-card plant-dashboard-card">
          <div class="section-title">
            <div class="plant-dashboard-copy">
              <h2>本月照顾天数 {{ plantCareDaysThisMonth }}/{{ plantCareMonthTotalDays }}</h2>
              <p>{{ plantOverviewSummary }}</p>
            </div>
            <div class="plant-dashboard-actions">
              <button class="secondary-button compact-button" type="button" @click="openTab('care')">
                <Sprout :size="17" />
                <span>养护历史</span>
              </button>
              <button class="secondary-button compact-button plant-add-button" type="button" @click="startCreatePlant()">
                <Plus :size="17" />
                <span>新增花卉</span>
              </button>
            </div>
          </div>
          <div class="summary-strip plant-summary-strip">
            <article class="mini-metric">
              <span>待养护</span>
              <strong>{{ pendingPlantCareReminders.length }}条</strong>
              <small>{{ todayDuePlantSummary }}</small>
            </article>
            <article class="mini-metric">
              <span>本月照顾</span>
              <strong>{{ plantCareDaysThisMonth }}/{{ plantCareMonthTotalDays }}</strong>
              <small>{{ plantCareDaysThisMonth > 0 ? "手动打卡或养护都会算一次照顾成功" : "今天开始照顾花花吧" }}</small>
            </article>
            <article class="mini-metric">
              <span>较上月</span>
              <strong>{{ plantCareDelta > 0 ? `+${plantCareDelta}` : plantCareDelta }}</strong>
              <small>{{ plantCareDeltaLabel }}</small>
            </article>
          </div>
          <article class="plant-trend-card">
            <div class="plant-trend-head">
              <div class="plant-trend-copy">
                <span>本月照顾趋势</span>
                <strong>{{ plantCareDaysThisMonth }}/{{ plantCareMonthTotalDays }}</strong>
                <small>{{ plantCareDeltaLabel }}</small>
              </div>
              <div class="plant-trend-compare">
                <span>上月 {{ previousPlantCareDays }} 天</span>
                <strong>本月 {{ plantCareDaysThisMonth }} 天</strong>
              </div>
            </div>
            <div class="plant-trend-dots" :style="{ '--plant-trend-columns': Math.min(plantCareMonthTotalDays, 8) }">
              <span
                v-for="day in plantTrendDays"
                :key="day.key"
                :class="['plant-trend-dot', { active: day.active }]"
                :title="`${day.label}日`"
              />
            </div>
            <div class="plant-badge-row">
              <span
                v-for="badge in plantCareBadges"
                :key="badge.key"
                :class="['plant-badge-chip', { active: badge.active }]"
              >
                <Leaf :size="13" />
                <span>{{ badge.label }}</span>
              </span>
            </div>
          </article>
          <p v-if="plants.length === 0" class="empty">还没有花卉档案，先把第一盆花花放进来吧。</p>
        </article>
        <article
          v-for="plant in plants"
          :key="text(plant, 'id')"
          :class="['plant-card', 'plant-photo-card', { active: text(plant, 'id') === selectedPlantId }]"
          @click="selectedPlantId = text(plant, 'id')"
        >
          <img
            v-if="text(plant, 'cover_url')"
            class="plant-cover"
            :src="text(plant, 'cover_url')"
            :alt="text(plant, 'name')"
            loading="lazy"
            decoding="async"
          />
          <span v-else class="plant-cover plant-cover-placeholder">
            <Leaf :size="18" />
          </span>
          <div class="plant-copy">
            <h3>{{ text(plant, "name") }}</h3>
            <div v-if="text(plant, 'flower_color') || text(plant, 'location')" class="plant-tag-row">
              <span v-if="text(plant, 'flower_color')" class="plant-meta-chip">{{ text(plant, "flower_color") }}</span>
              <span v-if="text(plant, 'location')" class="plant-meta-chip location">{{ text(plant, "location") }}</span>
            </div>
            <small v-if="text(plant, 'care_preference')">{{ text(plant, "care_preference") }}</small>
            <span
              v-if="plantStatusTag(plant)"
              :class="['plant-care-tag', `plant-care-tag-${plantStatusTag(plant)?.tone || 'due'}`]"
            >
              <component :is="plantStatusTag(plant)?.icon || Bell" :size="13" />
              <span>{{ plantStatusTag(plant)?.text }}</span>
            </span>
          </div>
          <div class="plant-side">
            <strong>{{ plantStatusLabel(text(plant, "status")) }}</strong>
            <div class="row-actions">
              <button class="icon-button" type="button" aria-label="编辑花卉" title="编辑花卉" @click.stop="startEditPlant(plant)">
                <Pencil :size="16" />
              </button>
              <button
                class="icon-button danger-icon-button"
                type="button"
                aria-label="删除花卉"
                title="删除花卉"
                @click.stop="deletePlant(numberValue(plant, 'id'))"
              >
                <Trash2 :size="16" />
              </button>
            </div>
          </div>
          <div class="plant-card-actions">
            <button class="secondary-button compact-button plant-card-action-button" type="button" @click.stop="openPlantCarePanel(plant)">
              <Sprout :size="16" />
              <span>养护</span>
            </button>
            <button
              :class="[
                'secondary-button',
                'compact-button',
                'plant-card-action-button',
                'plant-card-check-button',
                { 'is-complete': hasPlantCheckInOnDate(text(plant, 'id')) }
              ]"
              :disabled="
                !text(plant, 'id') ||
                hasPlantCheckInOnDate(text(plant, 'id')) ||
                isSubmitting(`plant-check-in-${text(plant, 'id')}`)
              "
              type="button"
              @click.stop="submitPlantCheckIn(plant)"
            >
              <LoaderCircle v-if="isSubmitting(`plant-check-in-${text(plant, 'id')}`)" class="spin" :size="16" />
              <Check v-else :size="16" />
              <span>{{ hasPlantCheckInOnDate(text(plant, "id")) ? "已打卡" : "打卡" }}</span>
            </button>
          </div>
        </article>
      </template>

      <article v-else-if="plantView === 'create'" class="form-card">
        <div class="section-title">
          <button class="icon-button" type="button" aria-label="返回花卉列表" title="返回花卉列表" @click="plantView = 'list'">
            <ArrowLeft :size="18" />
          </button>
          <h2>新增花卉</h2>
          <Leaf :size="18" />
        </div>
        <label class="file-pick">
          <ImagePlus :size="18" />
          <span>{{ plantFile ? plantFile.name : "上传花卉照片" }}</span>
          <input type="file" accept="image/*" @change="plantFile = selectedFile($event)" />
        </label>
        <input v-model="plantDraft.name" placeholder="名称，如月季" />
        <div class="inline-fields">
          <input v-model="plantDraft.flowerColor" placeholder="花色" />
          <input v-model="plantDraft.location" placeholder="也可以手动填写其他位置" />
        </div>
        <div class="field-stack">
          <span class="field-label">快捷位置</span>
          <div class="option-chip-group">
            <button
              v-for="option in plantLocationOptions"
              :key="`plant-location-${option.value}`"
              :class="['option-chip', { active: plantDraft.location === option.value }]"
              type="button"
              @click="togglePlantLocation(plantDraft, option.value)"
            >
              <component :is="option.icon" :size="15" />
              <span>{{ option.label }}</span>
            </button>
          </div>
        </div>
        <input v-model="plantDraft.carePreference" placeholder="养护偏好" />
        <div class="inline-fields">
          <input v-model="plantDraft.acquiredOn" type="date" />
          <select v-model="plantDraft.status">
            <option value="GROWING">成长期</option>
            <option value="BLOOMING">花期</option>
            <option value="RESTING">休眠期</option>
          </select>
        </div>
        <button class="secondary-button" :disabled="isSubmitting('plant-create')" type="button" @click="submitPlantCreate()">
          <LoaderCircle v-if="isSubmitting('plant-create')" class="spin" :size="17" />
          <Plus v-else :size="17" />
          <span>保存花卉</span>
        </button>
      </article>

      <article v-else class="form-card">
        <div class="section-title">
          <button class="icon-button" type="button" aria-label="返回花卉列表" title="返回花卉列表" @click="plantView = 'list'">
            <ArrowLeft :size="18" />
          </button>
          <h2>编辑花卉</h2>
          <Leaf :size="18" />
        </div>
        <label class="file-pick">
          <ImagePlus :size="18" />
          <span>{{ plantEditFile ? plantEditFile.name : "更换花卉照片" }}</span>
          <input type="file" accept="image/*" @change="plantEditFile = selectedFile($event)" />
        </label>
        <input v-model="plantEditDraft.name" placeholder="名称，如月季" />
        <div class="inline-fields">
          <input v-model="plantEditDraft.flowerColor" placeholder="花色" />
          <input v-model="plantEditDraft.location" placeholder="也可以手动填写其他位置" />
        </div>
        <div class="field-stack">
          <span class="field-label">快捷位置</span>
          <div class="option-chip-group">
            <button
              v-for="option in plantLocationOptions"
              :key="`plant-edit-location-${option.value}`"
              :class="['option-chip', { active: plantEditDraft.location === option.value }]"
              type="button"
              @click="togglePlantLocation(plantEditDraft, option.value)"
            >
              <component :is="option.icon" :size="15" />
              <span>{{ option.label }}</span>
            </button>
          </div>
        </div>
        <input v-model="plantEditDraft.carePreference" placeholder="养护偏好" />
        <div class="inline-fields">
          <input v-model="plantEditDraft.acquiredOn" type="date" />
          <select v-model="plantEditDraft.status">
            <option value="GROWING">成长期</option>
            <option value="BLOOMING">花期</option>
            <option value="RESTING">休眠期</option>
          </select>
        </div>
        <button class="secondary-button" :disabled="isSubmitting('plant-update')" type="button" @click="submitPlantEdit()">
          <LoaderCircle v-if="isSubmitting('plant-update')" class="spin" :size="17" />
          <Check v-else :size="17" />
          <span>更新花卉</span>
        </button>
      </article>
    </section>

    <section v-if="activeTab === 'care'" class="view">
      <article class="list-card care-overview-card">
        <div class="section-title">
          <h2>待养护</h2>
          <span>{{ pendingPlantCareReminders.length }}</span>
        </div>
        <p v-if="pendingPlantCareReminders.length === 0" class="empty">最近没有待养护项目</p>
        <template v-else>
          <div class="feed-item">
            <span>最近待处理</span>
            <p>{{ text(nextPendingPlantCareReminder ?? {}, "title") }}</p>
            <small>{{ formatDateTime(text(nextPendingPlantCareReminder ?? {}, "due_at")) }}</small>
          </div>
          <small class="muted">当前还有 {{ pendingPlantCareReminders.length }} 条待养护提醒，处理后会自动同步更新。</small>
        </template>
      </article>

      <article class="list-card">
        <div class="section-title">
          <h2>养护历史</h2>
          <span>{{ careRecords.length }}</span>
        </div>
        <select v-model="selectedPlantId">
          <option value="">选择花卉查看</option>
          <option v-for="plant in plants" :key="text(plant, 'id')" :value="text(plant, 'id')">
            {{ text(plant, "name") }}
          </option>
        </select>
        <div v-if="currentPlant" class="feed-item">
          <span>{{ plantStatusLabel(text(currentPlant ?? {}, "status")) }}</span>
          <p>{{ text(currentPlant ?? {}, "name") }}</p>
          <small>{{ text(currentPlant ?? {}, "location") || "未记录位置" }}</small>
        </div>
        <p v-if="plants.length === 0" class="empty">还没有花卉档案，先去新增一盆花花吧。</p>
        <p v-else-if="careRecords.length === 0" class="empty">当前花卉还没有养护历史。</p>
        <div v-for="record in careRecords" :key="text(record, 'id')" class="feed-item">
          <span>{{ careTypeLabel(text(record, "care_type")) }} · {{ text(record, "care_date") }}</span>
          <p>{{ text(record, "detail") || text(record, "raw_text") }}</p>
          <small v-if="text(record, 'next_care_at')">下次养护：{{ formatDateTime(text(record, "next_care_at")) }}</small>
        </div>
      </article>
    </section>

    <section v-if="activeTab === 'shopping'" class="view">
      <article v-if="shoppingView === 'list'" class="list-card">
        <div class="section-title">
          <h2>购物清单</h2>
          <button class="icon-button" type="button" aria-label="新增购物项" title="新增购物项" @click="startCreateShopping()">
            <Plus :size="18" />
          </button>
        </div>
        <p v-if="todoShoppingItems.length === 0" class="empty">还没有待买物品</p>
      </article>
      <article v-for="item in shoppingView === 'list' ? todoShoppingItems : []" :key="text(item, 'id')" class="shopping-card">
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

      <article v-if="shoppingView === 'list'" class="list-card">
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

      <article v-else class="form-card">
        <div class="section-title">
          <button class="icon-button" type="button" aria-label="返回购物清单" title="返回购物清单" @click="shoppingView = 'list'">
            <ArrowLeft :size="18" />
          </button>
          <h2>新增购物项</h2>
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
        <div v-else class="reminder-group-list">
          <section v-for="group in reminderModuleGroups" :key="group.key" class="subsection reminder-module-group">
            <div class="reminder-group-head">
              <div class="reminder-group-title">
                <span class="reminder-group-icon">
                  <component :is="group.icon" :size="16" />
                </span>
                <div class="reminder-group-copy">
                  <strong>{{ group.label }}</strong>
                  <small>{{ group.description }}</small>
                </div>
              </div>
              <span class="reminder-group-count">{{ group.reminders.length }}</span>
            </div>
            <article v-for="reminder in group.reminders" :key="text(reminder, 'id')" class="reminder-row">
              <div class="reminder-copy">
                <strong>{{ text(reminder, "title") }}</strong>
                <p>{{ formatDateTime(text(reminder, "due_at")) }} · {{ relativeDaysLabel(text(reminder, "due_at")) }}</p>
                <small v-if="text(reminder, 'description')">{{ text(reminder, "description") }}</small>
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
          </section>
        </div>
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

    <section v-if="activeTab === 'period'" class="view">
      <article v-if="periodView === 'list'" class="list-card">
        <div class="section-title">
          <h2>周期概览</h2>
          <div class="row-actions">
            <button class="icon-button" type="button" aria-label="记录经期" title="记录经期" @click="startCreatePeriodRecord()">
              <Plus :size="18" />
            </button>
            <button class="icon-button" type="button" aria-label="周期设置" title="周期设置" @click="startEditPeriodProfile()">
              <Pencil :size="18" />
            </button>
          </div>
        </div>
        <div class="summary-strip">
          <article class="mini-metric">
            <span>周期长度</span>
            <strong>{{ numberValue(periodProfile, "cycle_days") || 28 }}天</strong>
          </article>
          <article class="mini-metric">
            <span>经期长度</span>
            <strong>{{ numberValue(periodProfile, "period_days") || 5 }}天</strong>
          </article>
          <article class="mini-metric">
            <span>下次预计</span>
            <strong>{{ text(periodPrediction, "next_start") || "待记录" }}</strong>
          </article>
        </div>
        <div class="feed-item">
          <span>最近开始日期</span>
          <p>{{ text(periodProfile, "last_period_start") || "还没有记录" }}</p>
          <small v-if="text(periodPrediction, 'ovulation_on')">
            排卵日估算：{{ text(periodPrediction, "ovulation_on") }} · 易孕期：{{ text(periodPrediction, "fertile_start") }} 到
            {{ text(periodPrediction, "fertile_end") }}
          </small>
          <small v-if="text(periodProfile, 'note')">{{ text(periodProfile, "note") }}</small>
        </div>
      </article>

      <article v-if="periodView === 'list'" class="list-card">
        <div class="section-title">
          <h2>经期记录</h2>
          <span>{{ periodRecords.length }}</span>
        </div>
        <p v-if="periodRecords.length === 0" class="empty">还没有经期记录</p>
        <div v-for="record in periodRecords" :key="text(record, 'id')" class="feed-item">
          <span>{{ text(record, "start_on") }} <template v-if="text(record, 'end_on')">至 {{ text(record, "end_on") }}</template></span>
          <p>{{ text(record, "note") || "已记录本次经期" }}</p>
        </div>
      </article>

      <article v-else-if="periodView === 'profile'" class="form-card">
        <div class="section-title">
          <button class="icon-button" type="button" aria-label="返回周期概览" title="返回周期概览" @click="periodView = 'list'">
            <ArrowLeft :size="18" />
          </button>
          <h2>周期设置</h2>
          <Droplets :size="18" />
        </div>
        <div class="inline-fields">
          <input v-model="periodProfileDraft.cycleDays" inputmode="numeric" placeholder="周期天数" />
          <input v-model="periodProfileDraft.periodDays" inputmode="numeric" placeholder="经期天数" />
        </div>
        <input v-model="periodProfileDraft.lastPeriodStart" type="date" />
        <div class="inline-fields">
          <label class="check-row">
            <input v-model="periodProfileDraft.reminderEnabled" type="checkbox" />
            <span>开启提醒</span>
          </label>
          <input v-model="periodProfileDraft.reminderTime" type="time" />
        </div>
        <textarea v-model="periodProfileDraft.note" rows="3" placeholder="补充说明" />
        <button class="secondary-button" :disabled="isSubmitting('period-profile')" type="button" @click="submitPeriodProfile()">
          <LoaderCircle v-if="isSubmitting('period-profile')" class="spin" :size="17" />
          <Check v-else :size="17" />
          <span>保存周期设置</span>
        </button>
      </article>

      <article v-else class="form-card">
        <div class="section-title">
          <button class="icon-button" type="button" aria-label="返回周期概览" title="返回周期概览" @click="periodView = 'list'">
            <ArrowLeft :size="18" />
          </button>
          <h2>记录经期</h2>
          <Droplets :size="18" />
        </div>
        <div class="inline-fields">
          <input v-model="periodRecordDraft.startOn" type="date" />
          <input v-model="periodRecordDraft.endOn" type="date" />
        </div>
        <textarea v-model="periodRecordDraft.note" rows="3" placeholder="如经量、状态、备注" />
        <button class="secondary-button" :disabled="isSubmitting('period-record')" type="button" @click="submitPeriodRecord()">
          <LoaderCircle v-if="isSubmitting('period-record')" class="spin" :size="17" />
          <Plus v-else :size="17" />
          <span>保存经期记录</span>
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

      <article v-if="financeView === 'list'" class="list-card">
        <div class="section-title">
          <h2>支出记录</h2>
          <button class="icon-button" type="button" aria-label="新增记账" title="新增记账" @click="startCreateFinanceRecord()">
            <Plus :size="18" />
          </button>
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

      <article v-else class="form-card">
        <div class="section-title">
          <button class="icon-button" type="button" aria-label="返回账本列表" title="返回账本列表" @click="financeView = 'list'">
            <ArrowLeft :size="18" />
          </button>
          <h2>新增记账</h2>
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
    </section>

    <section v-if="activeTab === 'private'" class="view">
      <article v-if="privateView === 'list'" class="list-card">
        <div class="section-title">
          <h2>留言板</h2>
          <button class="icon-button" type="button" aria-label="新增留言" title="新增留言" @click="startCreatePrivateMessage()">
            <Plus :size="18" />
          </button>
        </div>
        <p v-if="privateMessages.length === 0" class="empty">这里还很安静</p>
        <div v-for="item in privateMessages" :key="text(item, 'id')" class="private-item">
          <span>
            {{ text(item, "message_date") || text(item, "created_at").slice(0, 10) }} · {{ visibilityLabel(text(item, "visibility")) }} ·
            {{ memberName(text(item, "sender_member_id")) }}
            <template v-if="text(item, 'receiver_member_id')"> 给 {{ memberName(text(item, "receiver_member_id")) }}</template>
          </span>
          <p>{{ text(item, "content") }}</p>
          <div class="row-actions">
            <button
              v-if="text(item, 'sender_member_id') === String(currentUser.memberId ?? '')"
              class="text-button"
              type="button"
              @click="startEditPrivateMessage(item)"
            >
              编辑
            </button>
            <button
              v-if="text(item, 'receiver_member_id') === String(currentUser.memberId ?? '') && !text(item, 'read_at')"
              class="text-button"
              type="button"
              @click="markPrivateMessageRead(numberValue(item, 'id'))"
            >
              标记已读
            </button>
          </div>
        </div>
      </article>

      <article v-else-if="privateView === 'create'" class="form-card">
        <div class="section-title">
          <button class="icon-button" type="button" aria-label="返回留言板" title="返回留言板" @click="privateView = 'list'">
            <ArrowLeft :size="18" />
          </button>
          <h2>新增留言</h2>
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
        <input v-model="privateDraft.messageDate" type="date" />
        <textarea v-model="privateDraft.content" rows="4" placeholder="写一句只属于这里的话" />
        <button class="secondary-button" :disabled="isSubmitting('private-create')" type="button" @click="submitPrivateMessage">
          <LoaderCircle v-if="isSubmitting('private-create')" class="spin" :size="17" />
          <Send v-else :size="17" />
          <span>保存留言</span>
        </button>
      </article>

      <article v-else class="form-card">
        <div class="section-title">
          <button class="icon-button" type="button" aria-label="返回留言板" title="返回留言板" @click="privateView = 'list'">
            <ArrowLeft :size="18" />
          </button>
          <h2>编辑留言</h2>
          <Pencil :size="18" />
        </div>
        <div class="inline-fields">
          <select v-model="privateEditDraft.visibility">
            <option value="TO_PARTNER">悄悄话</option>
            <option value="PRIVATE">只给自己</option>
            <option value="SHARED">共同可见</option>
          </select>
          <select v-model="privateEditDraft.receiverMemberId" :disabled="privateEditDraft.visibility !== 'TO_PARTNER'">
            <option v-for="member in otherMembers" :key="`edit-${text(member, 'id')}`" :value="text(member, 'id')">
              {{ text(member, "display_name") }}
            </option>
          </select>
        </div>
        <input v-model="privateEditDraft.messageDate" type="date" />
        <textarea v-model="privateEditDraft.content" rows="4" placeholder="写一句只属于这里的话" />
        <button class="secondary-button" :disabled="isSubmitting('private-update')" type="button" @click="submitPrivateMessageEdit">
          <LoaderCircle v-if="isSubmitting('private-update')" class="spin" :size="17" />
          <Check v-else :size="17" />
          <span>更新留言</span>
        </button>
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
          <div class="memory-thumb">
            <img
              v-if="text(entry, 'image_url')"
              :src="text(entry, 'image_url')"
              :alt="text(entry, 'title')"
              loading="lazy"
              decoding="async"
            />
            <span v-else class="memory-thumb-placeholder">{{ memoryEntryTypeLabel(text(entry, "entry_type")) }}</span>
          </div>
          <div class="memory-copy">
            <span>
              {{ memoryEntryTypeLabel(text(entry, "entry_type")) }} ·
              {{ formatDateTime(text(entry, "taken_on") || text(entry, "created_at")) }}
            </span>
            <strong>{{ text(entry, "title") }}</strong>
            <p v-if="text(entry, 'description')">{{ text(entry, "description") }}</p>
            <p v-if="text(entry, 'wish_text')" class="muted">愿望：{{ text(entry, "wish_text") }}</p>
            <p v-if="['ANNIVERSARY', 'BIRTHDAY'].includes(text(entry, 'entry_type'))" class="muted">
              {{ memoryDateLabel(text(entry, "entry_type")) }}：{{ formatDateValue(text(entry, "taken_on")) }}
            </p>
            <p v-if="memoryCountdownLabel(entry)" class="muted">{{ memoryCountdownLabel(entry) }}</p>
            <p v-if="memoryReminderLabel(entry)" class="muted">{{ memoryReminderLabel(entry) }}</p>
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
        <div class="field-stack">
          <span class="field-label">{{ memoryDateLabel(albumDraft.entryType) }}</span>
          <input v-model="albumDraft.takenOn" type="date" />
        </div>
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
        <div v-if="['ANNIVERSARY', 'BIRTHDAY'].includes(albumDraft.entryType)" class="field-stack">
          <span class="field-label">提前提醒天数</span>
          <input v-model="albumDraft.reminderDaysBefore" inputmode="numeric" placeholder="如 3" />
        </div>
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
        <div class="field-stack">
          <span class="field-label">{{ memoryDateLabel(albumEditDraft.entryType) }}</span>
          <input v-model="albumEditDraft.takenOn" type="date" />
        </div>
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
        <div v-if="['ANNIVERSARY', 'BIRTHDAY'].includes(albumEditDraft.entryType)" class="field-stack">
          <span class="field-label">提前提醒天数</span>
          <input v-model="albumEditDraft.reminderDaysBefore" inputmode="numeric" placeholder="如 3" />
        </div>
        <button class="secondary-button" :disabled="isSubmitting('memory-update')" type="button" @click="submitMemoryEdit()">
          <LoaderCircle v-if="isSubmitting('memory-update')" class="spin" :size="17" />
          <ImagePlus v-else :size="17" />
          <span>更新时刻</span>
        </button>
      </article>
    </section>

    <section v-if="activeTab === 'inventory'" class="view">
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
    </section>

    <section v-if="activeTab === 'votes'" class="view">
      <article class="decision-card">
        <div class="decision-hero">
          <div class="decision-mark"><Dices :size="26" /></div>
          <div>
            <p class="eyebrow">把纠结交给随机数</p>
            <h2>丹总专属决策器</h2>
            <p class="decision-subtitle">两个人不用表决，抽一个就出发。</p>
          </div>
        </div>
        <div class="decision-input-wrap">
          <div class="section-title">
            <label class="field-label" for="decision-options">候选选项</label>
            <span>{{ decisionOptions().length }} 个</span>
          </div>
          <textarea id="decision-options" v-model="decisionOptionsText" rows="8" placeholder="每行写一个选项" />
        </div>
        <button class="decision-button" :disabled="decisionRolling" type="button" @click="makeDecision()">
          <LoaderCircle v-if="decisionRolling" class="spin" :size="19" />
          <Dices v-else :size="19" />
          <span>{{ decisionRolling ? "正在抽取" : "帮我决定" }}</span>
        </button>
        <div class="decision-result" :class="{ rolling: decisionRolling, ready: decisionResult && !decisionRolling }">
          <span class="decision-result-label">今天就选</span>
          <strong>{{ decisionResult || "等你按下按钮" }}</strong>
        </div>
        <div v-if="decisionHistory.length > 0" class="decision-history">
          <div class="section-title">
            <span class="field-label">最近决定</span>
            <button class="icon-button" type="button" aria-label="清空历史决定" title="清空历史决定" @click="clearDecisionHistory()">
              <Trash2 :size="16" />
            </button>
          </div>
          <div class="decision-history-list">
            <span v-for="(item, index) in decisionHistory" :key="`${item}-${index}`">{{ item }}</span>
          </div>
        </div>
      </article>
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
          <img v-if="profileDraft.avatarUrl" :src="profileDraft.avatarUrl" alt="头像" loading="lazy" decoding="async" />
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
          <img
            v-if="text(member, 'avatar_url')"
            :src="text(member, 'avatar_url')"
            :alt="text(member, 'display_name')"
            loading="lazy"
            decoding="async"
          />
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
            <img
              v-if="text(pet, 'avatar_url')"
              :src="text(pet, 'avatar_url')"
              :alt="text(pet, 'name')"
              loading="lazy"
              decoding="async"
            />
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

      <template v-else-if="petView === 'detail' && currentPet">
        <article class="list-card pet-focus pet-detail-hero">
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
              loading="lazy"
              decoding="async"
            />
            <span v-else class="pet-placeholder pet-hero">
              <PawPrint :size="28" />
            </span>
            <div>
              <strong>{{ text(currentPet, "name") }}</strong>
              <p>{{ speciesLabel(text(currentPet, "species")) }} · {{ text(currentPet, "breed") || "未记录品种" }}</p>
              <p>{{ petGenderLabel(text(currentPet, "gender")) }} · {{ petAgeLabel(text(currentPet, "birthday")) || "未记录年龄" }}</p>
              <p v-if="text(currentPet, 'birthday')" class="muted">生日：{{ text(currentPet, "birthday") }}</p>
              <p>{{ text(currentPet, "note") || "还没有补充备注" }}</p>
            </div>
          </div>
          <div class="summary-strip pet-summary-strip">
            <article class="mini-metric">
              <span>照片</span>
              <strong>{{ petPhotos.length }}</strong>
              <small>随手记录日常瞬间</small>
            </article>
            <article class="mini-metric">
              <span>医疗记录</span>
              <strong>{{ petMedicalRecords.length }}</strong>
              <small>疫苗、体检和用药都在这里</small>
            </article>
            <article class="mini-metric">
              <span>待提醒</span>
              <strong>{{ petReminderCount }}条</strong>
              <small v-if="petNextReminderItem">
                下次{{ petReminderItemLabel(petNextReminderItem) }} {{ relativeDaysLabel(text(petNextReminderItem, "next_due_at")) }}
              </small>
              <small v-else>暂时没有新的时间提醒</small>
            </article>
          </div>
        </article>

        <article class="list-card">
          <div class="section-title">
            <h2>快捷操作栏</h2>
            <span>点一下就能记一条</span>
          </div>
          <div class="pet-quick-grid">
            <button class="secondary-button pet-quick-button" type="button" @click="openPetQuickAction('FEED')">
              <Heart :size="17" />
              <span>喂食</span>
            </button>
            <button class="secondary-button pet-quick-button" type="button" @click="openPetQuickAction('DEWORMING')">
              <Stethoscope :size="17" />
              <span>驱虫</span>
            </button>
            <button class="secondary-button pet-quick-button" type="button" @click="openPetQuickAction('BATH')">
              <Droplets :size="17" />
              <span>洗澡</span>
            </button>
          </div>
        </article>

        <article class="list-card">
          <div class="section-title">
            <h2>待办任务流</h2>
            <span>{{ petTaskFlowItems.length }}项</span>
          </div>
          <div class="pet-task-list">
            <article
              v-for="item in petTaskFlowItems"
              :key="item.key"
              :class="['pet-task-card', `pet-task-${item.tone}`]"
            >
              <div class="pet-task-head">
                <span class="pet-task-icon">
                  <component :is="petQuickActionDefinitions[item.key].icon" :size="16" />
                </span>
                <div>
                  <strong>{{ item.label }}</strong>
                  <small>{{ item.description }}</small>
                </div>
              </div>
              <p>{{ item.statusText }}</p>
              <small>{{ item.metaText }}</small>
            </article>
          </div>
        </article>

        <article class="list-card pet-weight-card">
          <div class="section-title">
            <h2>体重曲线</h2>
            <button class="icon-button" type="button" aria-label="记录体重" title="记录体重" @click="openPetWeightCreate()">
              <Plus :size="18" />
            </button>
          </div>
          <p v-if="!petWeightChartPoints.hasData" class="empty">还没有体重记录，建议每月补一次，健康变化会更直观。</p>
          <template v-else>
            <div class="pet-weight-summary">
              <div>
                <strong>{{ numberValue(latestPetWeightRecord ?? {}, "weight_kg").toFixed(1) }}kg</strong>
                <span>最近记录：{{ text(latestPetWeightRecord ?? {}, "recorded_on") }}</span>
              </div>
              <small>{{ petWeightChartPoints.minLabel }} - {{ petWeightChartPoints.maxLabel }}</small>
            </div>
            <div class="pet-weight-chart">
              <svg viewBox="0 0 280 132" role="img" aria-label="宠物体重曲线">
                <line x1="12" y1="116" x2="268" y2="116" class="pet-weight-baseline" />
                <polyline :points="petWeightChartPoints.polyline" class="pet-weight-line" />
                <g v-for="point in petWeightChartPoints.points" :key="`${point.date}-${point.value}`">
                  <circle :cx="point.x" :cy="point.y" r="4" class="pet-weight-point" />
                </g>
              </svg>
            </div>
            <div class="pet-weight-axis">
              <span>{{ petWeightChartPoints.firstLabel }}</span>
              <span>{{ petWeightChartPoints.lastLabel }}</span>
            </div>
            <div class="pet-weight-log">
              <span v-for="record in [...petWeightRecords].slice(-3).reverse()" :key="text(record, 'id')">
                {{ text(record, "recorded_on").slice(5) }} · {{ numberValue(record, "weight_kg").toFixed(1) }}kg
              </span>
            </div>
          </template>
        </article>

        <article class="list-card">
          <div class="section-title">
            <h2>更多资料</h2>
            <span>继续查看详情</span>
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
      </template>

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
            <img :src="text(photo, 'image_url')" :alt="text(photo, 'description') || '宠物照片'" loading="lazy" decoding="async" />
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
        <div
          v-for="record in petMedicalRecords"
          :key="text(record, 'id')"
          :class="[
            'feed-item',
            'feed-item-actions',
            text(record, 'next_due_at') ? `pet-task-${relativeDaysTone(text(record, 'next_due_at'), 10)}` : 'pet-task-calm'
          ]"
        >
          <span>{{ petRecordTypeLabel(text(record, "record_type")) }} · {{ text(record, "record_date") || "未记录日期" }}</span>
          <p>{{ text(record, "description") }}</p>
          <small v-if="text(record, 'hospital') || text(record, 'medicine')">
            {{ text(record, "hospital") || "护理地点待补充" }}
            <template v-if="text(record, 'medicine')"> · {{ text(record, "medicine") }}</template>
          </small>
          <span v-if="text(record, 'next_due_at')" :class="['pet-inline-countdown', `pet-inline-countdown-${relativeDaysTone(text(record, 'next_due_at'), 10)}`]">
            下次提醒：{{ formatDateTime(text(record, "next_due_at")) }} · {{ relativeDaysLabel(text(record, "next_due_at")) }}
          </span>
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
    <transition name="achievement-pop">
      <div v-if="plantAchievementVisible" :key="plantAchievementKey" class="plant-achievement-pop">
        <Leaf :size="18" />
        <strong>{{ plantAchievementTitle }}</strong>
        <span>{{ plantAchievementSubtitle }}</span>
      </div>
    </transition>
    <div
      v-if="plantQuickCarePlantId || petQuickActionOpen || petWeightOpen"
      class="drawer-backdrop"
      @click="
        closePlantCarePanel();
        petQuickActionOpen = false;
        petWeightOpen = false;
      "
    />
    <section v-if="plantQuickCarePlantId" class="quick-care-sheet plant-sheet">
      <div class="section-title">
        <div>
          <p class="eyebrow">花花快捷养护</p>
          <h2>{{ text(quickCarePlant ?? {}, "name") || "当前花花" }} · {{ careTypeLabel(careDraft.careType) }}</h2>
        </div>
        <button class="icon-button" type="button" aria-label="关闭快捷养护" title="关闭快捷养护" @click="closePlantCarePanel()">
          <ArrowLeft :size="18" />
        </button>
      </div>
      <div class="inline-fields">
        <div class="field-stack">
          <span class="field-label">养护类型</span>
          <select v-model="careDraft.careType">
            <option value="WATER">浇水</option>
            <option value="FERTILIZE">施肥</option>
            <option value="PRUNE">修剪</option>
            <option value="OBSERVE">观察</option>
          </select>
        </div>
        <div class="field-stack">
          <span class="field-label">下次养护时间</span>
          <input v-model="careDraft.nextCareAt" type="datetime-local" />
        </div>
      </div>
      <small class="muted">留空就只记录这次养护；保存后今天会自动算作已打卡。</small>
      <textarea v-model="careDraft.detail" rows="3" placeholder="补充状态、用量或备注，也可以留空" />
      <button
        class="secondary-button"
        :disabled="isSubmitting(`care-create-${plantQuickCarePlantId}`)"
        type="button"
        @click="submitPlantCareRecord"
      >
        <LoaderCircle v-if="isSubmitting(`care-create-${plantQuickCarePlantId}`)" class="spin" :size="17" />
        <Sprout v-else :size="17" />
        <span>保存养护</span>
      </button>
    </section>
    <section v-if="petQuickActionOpen" class="quick-care-sheet pet-sheet">
      <div class="section-title">
        <div>
          <p class="eyebrow">宠物快捷记录</p>
          <h2>{{ text(currentPet ?? {}, "name") }} · {{ petCareTypeLabel(petQuickActionDraft.action) }}</h2>
        </div>
        <button class="icon-button" type="button" aria-label="关闭快捷护理" title="关闭快捷护理" @click="petQuickActionOpen = false">
          <ArrowLeft :size="18" />
        </button>
      </div>
      <div class="inline-fields">
        <div class="field-stack">
          <span class="field-label">操作类型</span>
          <select v-model="petQuickActionDraft.action">
            <option value="FEED">喂食</option>
            <option value="DEWORMING">驱虫</option>
            <option value="BATH">洗澡</option>
          </select>
        </div>
        <div class="field-stack">
          <span class="field-label">记录时间</span>
          <input v-model="petQuickActionDraft.recordedAt" type="datetime-local" />
        </div>
      </div>
      <div class="field-stack">
        <span class="field-label">下次提醒</span>
        <input v-model="petQuickActionDraft.nextDueAt" type="datetime-local" />
      </div>
      <small class="muted">留空就只记录本次，不生成新的提醒时间。</small>
      <textarea
        v-model="petQuickActionDraft.description"
        rows="3"
        :placeholder="`补充${petCareTypeLabel(petQuickActionDraft.action)}细节，比如状态、剂量或反应`"
      />
      <button class="secondary-button" :disabled="isSubmitting('pet-quick-action')" type="button" @click="submitPetQuickAction">
        <LoaderCircle v-if="isSubmitting('pet-quick-action')" class="spin" :size="17" />
        <component v-else :is="petQuickActionIcon(petQuickActionDraft.action)" :size="17" />
        <span>保存快捷记录</span>
      </button>
    </section>
    <section v-if="petWeightOpen" class="quick-care-sheet pet-sheet">
      <div class="section-title">
        <div>
          <p class="eyebrow">健康记录</p>
          <h2>{{ text(currentPet ?? {}, "name") }} · 记录体重</h2>
        </div>
        <button class="icon-button" type="button" aria-label="关闭体重记录" title="关闭体重记录" @click="petWeightOpen = false">
          <ArrowLeft :size="18" />
        </button>
      </div>
      <div class="inline-fields">
        <input v-model="petWeightDraft.weightKg" inputmode="decimal" placeholder="体重（kg）" />
        <input v-model="petWeightDraft.recordedOn" type="date" />
      </div>
      <textarea v-model="petWeightDraft.note" rows="3" placeholder="比如空腹、饭后、最近状态" />
      <button class="secondary-button" :disabled="isSubmitting('pet-weight-create')" type="button" @click="submitPetWeightRecord">
        <LoaderCircle v-if="isSubmitting('pet-weight-create')" class="spin" :size="17" />
        <Plus v-else :size="17" />
        <span>保存体重记录</span>
      </button>
    </section>
    <datalist id="role-options">
      <option v-for="item in roleOptions" :key="item" :value="item" />
    </datalist>
  </main>
</template>
