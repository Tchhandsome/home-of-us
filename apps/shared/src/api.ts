export type ApiResponse<T> = {
  success: boolean;
  code: string;
  message: string;
  data: T;
  timestamp: string;
};

export type AnyRow = Record<string, unknown>;

export type TodaySummary = {
  pendingReminders: number;
  plantCount: number;
  shoppingTodoCount: number;
  choreTodoCount: number;
  inventoryLowCount: number;
  monthExpense: number | string;
};

export type CreateQuickRecordPayload = {
  rawText: string;
  recordType?: string;
  linkedType?: string;
  linkedId?: number;
  happenedOn?: string;
};

export type CreatePlantPayload = {
  name: string;
  variety?: string;
  flowerColor?: string;
  location?: string;
  status?: string;
  carePreference?: string;
  acquiredOn?: string;
  coverUrl?: string;
};

export type CreateCareRecordPayload = {
  careType: string;
  careDate?: string;
  detail?: string;
  rawText?: string;
  nextCareAt?: string;
};

export type CreateShoppingItemPayload = {
  name: string;
  category?: string;
  channel?: string;
  quantity?: string;
};

export type CompleteShoppingItemPayload = {
  actualAmount: number;
  category?: string;
  buyerId?: number;
};

export type CreateReminderPayload = {
  title: string;
  description?: string;
  sourceType?: string;
  sourceId?: number;
  dueAt: string;
  repeatRule?: string;
};

export type CreateFinanceRecordPayload = {
  title: string;
  amount: number;
  direction?: string;
  category?: string;
  ownerId?: number;
  occurredOn?: string;
};

export type LoginPayload = {
  username: string;
  password: string;
};

export type RequestError = Error & {
  code?: string;
  status?: number;
};

export type CreateFamilyMemberPayload = {
  displayName: string;
  roleCode?: string;
  avatarColor?: string;
  avatarUrl?: string;
  bio?: string;
  username?: string;
  password?: string;
};

export type UpdateFamilyMemberPayload = Partial<CreateFamilyMemberPayload>;

export type UpdateHomeCardOrderPayload = {
  cardKeys: string[];
};

export type CreatePrivateMessagePayload = {
  content: string;
  visibility?: string;
  receiverMemberId?: number;
  messageDate?: string;
};

export type UpdatePrivateMessagePayload = CreatePrivateMessagePayload;

export type CreateAlbumPhotoPayload = {
  title: string;
  imageUrl?: string;
  entryType?: string;
  description?: string;
  wishText?: string;
  reminderEnabled?: boolean;
  reminderDaysBefore?: number;
  takenOn?: string;
};

export type UpdateAlbumPhotoPayload = CreateAlbumPhotoPayload;

export type CreateTodoPayload = {
  title: string;
  taskScope?: string;
  assigneeId?: number;
  assigneeIds?: number[];
  taskType?: string;
  cycleRule?: string;
  note?: string;
  dueAt?: string;
};

export type UpdateTodoPayload = CreateTodoPayload;

export type CreateInventoryPayload = {
  name: string;
  itemType?: string;
  category?: string;
  quantity?: string | number;
  unit?: string;
  lowStockThreshold?: string | number;
  expiresOn?: string;
  reminderDaysBefore?: number;
  note?: string;
};

export type UpdateInventoryPayload = CreateInventoryPayload;

export type CreateFamilyVotePayload = {
  title: string;
  voteCategory?: string;
  options?: string[];
};

export type SubmitFamilyVotePayload = {
  optionId: number;
};

export type CreateRecipePayload = {
  title: string;
  mealType?: string;
  ingredientsText?: string;
  stepsText?: string;
  preferredMemberIds?: number[];
};

export type UpdateRecipePayload = CreateRecipePayload;

export type CreateMealPlanPayload = {
  weekStart?: string;
  plannedOn: string;
  mealSlot?: string;
  recipeId?: number;
  titleSnapshot?: string;
  remindAt?: string;
};

export type GenerateWeeklyMealPlanPayload = {
  weekStart?: string;
  enableReminder?: boolean;
  remindTime?: string;
};

export type CreatePetPayload = {
  name: string;
  species?: string;
  breed?: string;
  gender?: string;
  birthday?: string;
  avatarUrl?: string;
  note?: string;
};

export type CreatePetPhotoPayload = {
  imageUrl: string;
  description?: string;
  takenOn?: string;
};

export type CreatePetMedicalRecordPayload = {
  recordType?: string;
  recordDate?: string;
  hospital?: string;
  medicine?: string;
  description: string;
  nextDueAt?: string;
};

export type SavePeriodProfilePayload = {
  cycleDays?: number;
  periodDays?: number;
  lastPeriodStart?: string;
  reminderEnabled?: boolean;
  reminderTime?: string;
  note?: string;
};

export type CreatePeriodRecordPayload = {
  startOn: string;
  endOn?: string;
  note?: string;
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

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "/api";
const AUTH_TOKEN_KEY = "homeOfUsToken";
const AUTH_REMEMBER_KEY = "homeOfUsRemember";

function getAuthToken(): string {
  if (typeof window === "undefined") {
    return "";
  }
  return window.sessionStorage.getItem(AUTH_TOKEN_KEY) ?? window.localStorage.getItem(AUTH_TOKEN_KEY) ?? "";
}

export function getRememberedLogin(): boolean {
  if (typeof window === "undefined") {
    return false;
  }
  return window.localStorage.getItem(AUTH_REMEMBER_KEY) === "1";
}

export function setAuthToken(token: string, remember = false) {
  if (typeof window === "undefined") {
    return;
  }
  window.sessionStorage.removeItem(AUTH_TOKEN_KEY);
  window.localStorage.removeItem(AUTH_TOKEN_KEY);
  if (remember) {
    window.localStorage.setItem(AUTH_TOKEN_KEY, token);
    window.localStorage.setItem(AUTH_REMEMBER_KEY, "1");
    return;
  }
  window.sessionStorage.setItem(AUTH_TOKEN_KEY, token);
  window.localStorage.removeItem(AUTH_REMEMBER_KEY);
}

export function clearAuthToken() {
  if (typeof window === "undefined") {
    return;
  }
  window.sessionStorage.removeItem(AUTH_TOKEN_KEY);
  window.localStorage.removeItem(AUTH_TOKEN_KEY);
  window.localStorage.removeItem(AUTH_REMEMBER_KEY);
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getAuthToken();
  const isFormData = options.body instanceof FormData;
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      ...(isFormData ? {} : { "Content-Type": "application/json" }),
      ...(token ? { "X-Home-Token": token } : {}),
      ...options.headers
    },
    ...options
  });
  const body = (await response.json()) as ApiResponse<T>;
  if (!response.ok || !body.success) {
    const error = new Error(body.message || "请求失败") as RequestError;
    error.code = body.code;
    error.status = response.status;
    throw error;
  }
  return body.data;
}

export const api = {
  login: (payload: LoginPayload) =>
    request<AnyRow>("/auth/login", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  me: () => request<AnyRow>("/auth/me"),
  today: () => request<TodaySummary>("/dashboard/today"),
  family: () => request<AnyRow>("/families/default"),
  createFamilyMember: (payload: CreateFamilyMemberPayload) =>
    request<AnyRow>("/families/default/members", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  updateFamilyMember: (id: number, payload: UpdateFamilyMemberPayload) =>
    request<AnyRow>(`/families/default/members/${id}`, {
      method: "PATCH",
      body: JSON.stringify(payload)
    }),
  updateHomeCardOrder: (payload: UpdateHomeCardOrderPayload) =>
    request<AnyRow>("/families/default/preferences/home-card-order", {
      method: "PATCH",
      body: JSON.stringify(payload)
    }),
  records: () => request<AnyRow[]>("/records?limit=20"),
  deleteRecord: (id: number) =>
    request<AnyRow>(`/records/${id}`, {
      method: "DELETE"
    }),
  createRecord: (payload: CreateQuickRecordPayload) =>
    request<AnyRow>("/records", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  reminders: () => request<AnyRow[]>("/reminders?limit=50"),
  createReminder: (payload: CreateReminderPayload) =>
    request<AnyRow>("/reminders", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  completeReminder: (id: number) =>
    request<AnyRow>(`/reminders/${id}/complete`, {
      method: "PATCH"
    }),
  deleteReminder: (id: number) =>
    request<AnyRow>(`/reminders/${id}`, {
      method: "DELETE"
    }),
  plants: () => request<AnyRow[]>("/plants"),
  createPlant: (payload: CreatePlantPayload) =>
    request<AnyRow>("/plants", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  updatePlant: (id: number, payload: CreatePlantPayload) =>
    request<AnyRow>(`/plants/${id}`, {
      method: "PATCH",
      body: JSON.stringify(payload)
    }),
  deletePlant: (id: number) =>
    request<AnyRow>(`/plants/${id}`, {
      method: "DELETE"
    }),
  createCareRecord: (plantId: number, payload: CreateCareRecordPayload) =>
    request<AnyRow>(`/plants/${plantId}/care-records`, {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  careRecords: (plantId: number) => request<AnyRow[]>(`/plants/${plantId}/care-records`),
  deleteCareRecord: (plantId: number, recordId: number) =>
    request<AnyRow>(`/plants/${plantId}/care-records/${recordId}`, {
      method: "DELETE"
    }),
  shoppingItems: () => request<AnyRow[]>("/shopping/items"),
  createShoppingItem: (payload: CreateShoppingItemPayload) =>
    request<AnyRow>("/shopping/items", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  checkShoppingItem: (id: number, payload: CompleteShoppingItemPayload) =>
    request<AnyRow>(`/shopping/items/${id}/check`, {
      method: "PATCH",
      body: JSON.stringify(payload)
    }),
  deleteShoppingItem: (id: number) =>
    request<AnyRow>(`/shopping/items/${id}`, {
      method: "DELETE"
    }),
  choreTasks: () => request<AnyRow[]>("/chore/tasks"),
  createTodo: (payload: CreateTodoPayload) =>
    request<AnyRow>("/chore/tasks", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  updateTodo: (id: number, payload: UpdateTodoPayload) =>
    request<AnyRow>(`/chore/tasks/${id}`, {
      method: "PATCH",
      body: JSON.stringify(payload)
    }),
  claimTodo: (id: number) =>
    request<AnyRow>(`/chore/tasks/${id}/claim`, {
      method: "PATCH"
    }),
  completeTodo: (id: number) =>
    request<AnyRow>(`/chore/tasks/${id}/complete`, {
      method: "PATCH"
    }),
  deleteTodo: (id: number) =>
    request<AnyRow>(`/chore/tasks/${id}`, {
      method: "DELETE"
    }),
  inventoryItems: () => request<AnyRow[]>("/inventory/items"),
  createInventoryItem: (payload: CreateInventoryPayload) =>
    request<AnyRow>("/inventory/items", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  updateInventoryItem: (id: number, payload: UpdateInventoryPayload) =>
    request<AnyRow>(`/inventory/items/${id}`, {
      method: "PATCH",
      body: JSON.stringify(payload)
    }),
  deleteInventoryItem: (id: number) =>
    request<AnyRow>(`/inventory/items/${id}`, {
      method: "DELETE"
    }),
  financeRecords: () => request<AnyRow[]>("/finance/records"),
  createFinanceRecord: (payload: CreateFinanceRecordPayload) =>
    request<AnyRow>("/finance/records", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  deleteFinanceRecord: (id: number) =>
    request<AnyRow>(`/finance/records/${id}`, {
      method: "DELETE"
    }),
  financeOverview: () => request<AnyRow>("/finance/summary"),
  financeCategories: () => request<AnyRow[]>("/finance/categories"),
  financeSummary: () => request<AnyRow[]>("/finance/monthly-summary"),
  attachments: () => request<AnyRow[]>("/attachments"),
  privateMessages: () => request<AnyRow[]>("/private-messages"),
  createPrivateMessage: (payload: CreatePrivateMessagePayload) =>
    request<AnyRow>("/private-messages", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  updatePrivateMessage: (id: number, payload: UpdatePrivateMessagePayload) =>
    request<AnyRow>(`/private-messages/${id}`, {
      method: "PATCH",
      body: JSON.stringify(payload)
    }),
  readPrivateMessage: (id: number) =>
    request<AnyRow>(`/private-messages/${id}/read`, {
      method: "PATCH"
    }),
  periodMine: () => request<AnyRow>("/periods/me"),
  savePeriodProfile: (payload: SavePeriodProfilePayload) =>
    request<AnyRow>("/periods/me/profile", {
      method: "PATCH",
      body: JSON.stringify(payload)
    }),
  createPeriodRecord: (payload: CreatePeriodRecordPayload) =>
    request<AnyRow>("/periods/me/records", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  albumPhotos: () => request<AnyRow[]>("/album/photos"),
  updateAlbumPhoto: (id: number, payload: UpdateAlbumPhotoPayload) =>
    request<AnyRow>(`/album/photos/${id}`, {
      method: "PATCH",
      body: JSON.stringify(payload)
    }),
  createAlbumPhoto: (payload: CreateAlbumPhotoPayload) =>
    request<AnyRow>("/album/photos", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  deleteAlbumPhoto: (id: number) =>
    request<AnyRow>(`/album/photos/${id}`, {
      method: "DELETE"
    }),
  familyVotes: () => request<AnyRow[]>("/family-votes"),
  createFamilyVote: (payload: CreateFamilyVotePayload) =>
    request<AnyRow>("/family-votes", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  submitFamilyVote: (id: number, payload: SubmitFamilyVotePayload) =>
    request<AnyRow>(`/family-votes/${id}/vote`, {
      method: "PATCH",
      body: JSON.stringify(payload)
    }),
  decideFamilyVote: (id: number) =>
    request<AnyRow>(`/family-votes/${id}/decide`, {
      method: "PATCH"
    }),
  deleteFamilyVote: (id: number) =>
    request<AnyRow>(`/family-votes/${id}`, {
      method: "DELETE"
    }),
  recipes: () => request<AnyRow[]>("/recipes"),
  createRecipe: (payload: CreateRecipePayload) =>
    request<AnyRow>("/recipes", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  updateRecipe: (id: number, payload: UpdateRecipePayload) =>
    request<AnyRow>(`/recipes/${id}`, {
      method: "PATCH",
      body: JSON.stringify(payload)
    }),
  deleteRecipe: (id: number) =>
    request<AnyRow>(`/recipes/${id}`, {
      method: "DELETE"
    }),
  mealPlans: (weekStart?: string) =>
    request<AnyRow[]>(`/recipes/meal-plans${weekStart ? `?weekStart=${encodeURIComponent(weekStart)}` : ""}`),
  createMealPlan: (payload: CreateMealPlanPayload) =>
    request<AnyRow>("/recipes/meal-plans", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  generateWeeklyMealPlans: (payload: GenerateWeeklyMealPlanPayload = {}) =>
    request<AnyRow>("/recipes/meal-plans/generate-weekly", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  syncMealPlanShoppingList: (weekStart: string) =>
    request<AnyRow>(`/recipes/meal-plans/${encodeURIComponent(weekStart)}/shopping-sync`, {
      method: "POST"
    }),
  deleteMealPlan: (id: number) =>
    request<AnyRow>(`/recipes/meal-plans/${id}`, {
      method: "DELETE"
    }),
  uploadImage: (file: File, linkedType?: string, linkedId?: number) => {
    const form = new FormData();
    form.append("file", file);
    if (linkedType) {
      form.append("linkedType", linkedType);
    }
    if (linkedId) {
      form.append("linkedId", String(linkedId));
    }
    return request<AnyRow>("/attachments/images", {
      method: "POST",
      body: form
    });
  },
  pets: () => request<AnyRow[]>("/pets"),
  createPet: (payload: CreatePetPayload) =>
    request<AnyRow>("/pets", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  updatePet: (petId: number, payload: UpdatePetPayload) =>
    request<AnyRow>(`/pets/${petId}`, {
      method: "PATCH",
      body: JSON.stringify(payload)
    }),
  deletePet: (petId: number) =>
    request<AnyRow>(`/pets/${petId}`, {
      method: "DELETE"
    }),
  petPhotos: (petId: number) => request<AnyRow[]>(`/pets/${petId}/photos`),
  createPetPhoto: (petId: number, payload: CreatePetPhotoPayload) =>
    request<AnyRow>(`/pets/${petId}/photos`, {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  deletePetPhoto: (petId: number, photoId: number) =>
    request<AnyRow>(`/pets/${petId}/photos/${photoId}`, {
      method: "DELETE"
    }),
  petMedicalRecords: (petId: number) => request<AnyRow[]>(`/pets/${petId}/medical-records`),
  createPetMedicalRecord: (petId: number, payload: CreatePetMedicalRecordPayload) =>
    request<AnyRow>(`/pets/${petId}/medical-records`, {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  deletePetMedicalRecord: (petId: number, recordId: number) =>
    request<AnyRow>(`/pets/${petId}/medical-records/${recordId}`, {
      method: "DELETE"
    })
};
