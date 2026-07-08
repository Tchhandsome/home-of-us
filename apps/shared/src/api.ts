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

export type CreatePrivateMessagePayload = {
  content: string;
  visibility?: string;
  receiverMemberId?: number;
};

export type CreateAlbumPhotoPayload = {
  title: string;
  imageUrl: string;
  description?: string;
  takenOn?: string;
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

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "/api";
const AUTH_TOKEN_KEY = "homeOfUsToken";

function getAuthToken(): string {
  if (typeof localStorage === "undefined") {
    return "";
  }
  return localStorage.getItem(AUTH_TOKEN_KEY) ?? "";
}

export function setAuthToken(token: string) {
  if (typeof localStorage === "undefined") {
    return;
  }
  localStorage.setItem(AUTH_TOKEN_KEY, token);
}

export function clearAuthToken() {
  if (typeof localStorage === "undefined") {
    return;
  }
  localStorage.removeItem(AUTH_TOKEN_KEY);
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
    throw new Error(body.message || "请求失败");
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
  records: () => request<AnyRow[]>("/records?limit=20"),
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
  plants: () => request<AnyRow[]>("/plants"),
  createPlant: (payload: CreatePlantPayload) =>
    request<AnyRow>("/plants", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  createCareRecord: (plantId: number, payload: CreateCareRecordPayload) =>
    request<AnyRow>(`/plants/${plantId}/care-records`, {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  careRecords: (plantId: number) => request<AnyRow[]>(`/plants/${plantId}/care-records`),
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
  choreTasks: () => request<AnyRow[]>("/chore/tasks"),
  inventoryItems: () => request<AnyRow[]>("/inventory/items"),
  financeRecords: () => request<AnyRow[]>("/finance/records"),
  createFinanceRecord: (payload: CreateFinanceRecordPayload) =>
    request<AnyRow>("/finance/records", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  financeCategories: () => request<AnyRow[]>("/finance/categories"),
  financeSummary: () => request<AnyRow[]>("/finance/monthly-summary"),
  attachments: () => request<AnyRow[]>("/attachments"),
  privateMessages: () => request<AnyRow[]>("/private-messages"),
  createPrivateMessage: (payload: CreatePrivateMessagePayload) =>
    request<AnyRow>("/private-messages", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  readPrivateMessage: (id: number) =>
    request<AnyRow>(`/private-messages/${id}/read`, {
      method: "PATCH"
    }),
  albumPhotos: () => request<AnyRow[]>("/album/photos"),
  createAlbumPhoto: (payload: CreateAlbumPhotoPayload) =>
    request<AnyRow>("/album/photos", {
      method: "POST",
      body: JSON.stringify(payload)
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
  petPhotos: (petId: number) => request<AnyRow[]>(`/pets/${petId}/photos`),
  createPetPhoto: (petId: number, payload: CreatePetPhotoPayload) =>
    request<AnyRow>(`/pets/${petId}/photos`, {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  petMedicalRecords: (petId: number) => request<AnyRow[]>(`/pets/${petId}/medical-records`),
  createPetMedicalRecord: (petId: number, payload: CreatePetMedicalRecordPayload) =>
    request<AnyRow>(`/pets/${petId}/medical-records`, {
      method: "POST",
      body: JSON.stringify(payload)
    })
};
