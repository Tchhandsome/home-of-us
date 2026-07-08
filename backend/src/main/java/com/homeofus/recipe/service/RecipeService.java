package com.homeofus.recipe.service;

import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.exception.BusinessException;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.common.web.CurrentUser;
import com.homeofus.common.web.CurrentUserProvider;
import com.homeofus.recipe.dto.CreateMealPlanRequest;
import com.homeofus.recipe.dto.CreateRecipeRequest;
import com.homeofus.recipe.dto.GenerateWeeklyMealPlanRequest;
import com.homeofus.recipe.dto.UpdateRecipeRequest;
import com.homeofus.recipe.repository.RecipeRepository;
import com.homeofus.reminder.service.ReminderService;
import com.homeofus.shopping.dto.CreateShoppingItemRequest;
import com.homeofus.shopping.repository.ShoppingRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 菜谱与餐单业务服务。
 *
 * @author tanchaohong
 */
@Service
public class RecipeService {

    private static final String MEAL_PLAN_SOURCE_TYPE = "MEAL_PLAN";

    private final RecipeRepository recipeRepository;

    private final ShoppingRepository shoppingRepository;

    private final ReminderService reminderService;

    private final CurrentUserProvider currentUserProvider;

    private final IdGenerator idGenerator;

    private final TimeProvider timeProvider;

    public RecipeService(RecipeRepository recipeRepository, ShoppingRepository shoppingRepository,
            ReminderService reminderService, CurrentUserProvider currentUserProvider, IdGenerator idGenerator,
            TimeProvider timeProvider) {
        this.recipeRepository = recipeRepository;
        this.shoppingRepository = shoppingRepository;
        this.reminderService = reminderService;
        this.currentUserProvider = currentUserProvider;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    /**
     * 创建菜谱。
     *
     * @param request 创建请求
     * @return 新菜谱 ID
     */
    public Map<String, Object> createRecipe(CreateRecipeRequest request) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        Long id = idGenerator.nextId();
        recipeRepository.insertRecipe(id, DefaultFamily.FAMILY_ID, request, joinMemberIds(request.getPreferredMemberIds()),
                currentUser.getUserId(), timeProvider.now());
        return Map.of("id", id);
    }

    /**
     * 查询菜谱列表。
     *
     * @return 菜谱列表
     */
    public List<Map<String, Object>> findRecipes() {
        List<Map<String, Object>> recipes = recipeRepository.findRecipes(DefaultFamily.FAMILY_ID);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> recipe : recipes) {
            result.add(enrichRecipe(recipe));
        }
        return result;
    }

    /**
     * 更新菜谱。
     *
     * @param recipeId 菜谱 ID
     * @param request 更新请求
     * @return 更新结果
     */
    public Map<String, Object> updateRecipe(Long recipeId, UpdateRecipeRequest request) {
        ensureRecipeExists(recipeId);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        int updated = recipeRepository.updateRecipe(recipeId, DefaultFamily.FAMILY_ID, request,
                joinMemberIds(request.getPreferredMemberIds()), currentUser.getUserId(), timeProvider.now());
        return Map.of("updated", updated);
    }

    /**
     * 删除菜谱。
     *
     * @param recipeId 菜谱 ID
     * @return 删除结果
     */
    public Map<String, Object> deleteRecipe(Long recipeId) {
        ensureRecipeExists(recipeId);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        int updated = recipeRepository.deleteRecipe(recipeId, DefaultFamily.FAMILY_ID, currentUser.getUserId(),
                timeProvider.now());
        return Map.of("updated", updated);
    }

    /**
     * 创建单条餐单。
     *
     * @param request 创建请求
     * @return 新餐单 ID
     */
    public Map<String, Object> createMealPlan(CreateMealPlanRequest request) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        LocalDate plannedOn = parseDateOrToday(request.getPlannedOn());
        LocalDate weekStart = resolveWeekStart(request.getWeekStart(), plannedOn);
        Long recipeId = normalizeRecipeId(request.getRecipeId());
        String titleSnapshot = resolveMealPlanTitle(recipeId, request.getTitleSnapshot());
        LocalDateTime remindAt = parseDateTime(request.getRemindAt());
        Long mealPlanId = idGenerator.nextId();
        recipeRepository.insertMealPlan(mealPlanId, DefaultFamily.FAMILY_ID, weekStart, plannedOn,
                resolveMealSlot(request.getMealSlot()), recipeId, titleSnapshot, remindAt, currentUser.getUserId(),
                timeProvider.now());
        syncMealPlanReminder(mealPlanId, titleSnapshot, remindAt);
        return Map.of("id", mealPlanId);
    }

    /**
     * 查询周餐单。
     *
     * @param weekStart 周起始日
     * @return 餐单列表
     */
    public List<Map<String, Object>> findMealPlans(String weekStart) {
        LocalDate safeWeekStart = resolveWeekStart(weekStart, timeProvider.today());
        return recipeRepository.findMealPlans(DefaultFamily.FAMILY_ID, safeWeekStart);
    }

    /**
     * 自动生成周餐单。
     *
     * @param request 生成请求
     * @return 生成结果
     */
    public Map<String, Object> generateWeeklyMealPlans(GenerateWeeklyMealPlanRequest request) {
        LocalDate safeWeekStart = resolveWeekStart(Objects.isNull(request) ? null : request.getWeekStart(),
                timeProvider.today());
        List<Map<String, Object>> recipes = recipeRepository.findRecipes(DefaultFamily.FAMILY_ID);
        if (recipes.isEmpty()) {
            throw new BusinessException("RECIPE_NOT_FOUND", "还没有菜谱，暂时无法生成周餐单");
        }
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        List<Map<String, Object>> existingPlans = recipeRepository.findMealPlans(DefaultFamily.FAMILY_ID, safeWeekStart);
        for (Map<String, Object> existingPlan : existingPlans) {
            reminderService.deleteBySource(MEAL_PLAN_SOURCE_TYPE, numberValue(existingPlan, "id"));
        }
        recipeRepository.deleteMealPlansByWeekStart(DefaultFamily.FAMILY_ID, safeWeekStart, currentUser.getUserId(),
                timeProvider.now());
        boolean enableReminder = Objects.nonNull(request) && Boolean.TRUE.equals(request.getEnableReminder());
        LocalTime remindTime = parseTime(Objects.isNull(request) ? null : request.getRemindTime());
        List<Map<String, Object>> createdPlans = new ArrayList<>();
        for (int index = 0; index < 7; index++) {
            Map<String, Object> recipe = recipes.get(index % recipes.size());
            if (recipes.size() > 1 && index > 0
                    && numberValue(recipe, "id").equals(numberValue(createdPlans.get(index - 1), "recipe_id"))) {
                recipe = recipes.get((index + 1) % recipes.size());
            }
            LocalDate plannedOn = safeWeekStart.plusDays(index);
            LocalDateTime remindAt = enableReminder ? LocalDateTime.of(plannedOn, remindTime) : null;
            Long mealPlanId = idGenerator.nextId();
            String titleSnapshot = textValue(recipe, "title");
            recipeRepository.insertMealPlan(mealPlanId, DefaultFamily.FAMILY_ID, safeWeekStart, plannedOn, "DINNER",
                    numberValue(recipe, "id"), titleSnapshot, remindAt, currentUser.getUserId(), timeProvider.now());
            syncMealPlanReminder(mealPlanId, titleSnapshot, remindAt);
            Map<String, Object> createdPlan = new LinkedHashMap<>();
            createdPlan.put("id", mealPlanId);
            createdPlan.put("recipe_id", numberValue(recipe, "id"));
            createdPlan.put("title_snapshot", titleSnapshot);
            createdPlan.put("planned_on", plannedOn);
            createdPlan.put("meal_slot", "DINNER");
            createdPlan.put("remind_at", remindAt);
            createdPlans.add(createdPlan);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("weekStart", safeWeekStart);
        result.put("generatedCount", createdPlans.size());
        result.put("plans", createdPlans);
        return result;
    }

    /**
     * 同步周餐单到购物清单。
     *
     * @param weekStart 周起始日
     * @return 同步结果
     */
    public Map<String, Object> syncShoppingList(String weekStart) {
        LocalDate safeWeekStart = resolveWeekStart(weekStart, timeProvider.today());
        List<Map<String, Object>> mealPlans = recipeRepository.findMealPlans(DefaultFamily.FAMILY_ID, safeWeekStart);
        if (mealPlans.isEmpty()) {
            throw new BusinessException("MEAL_PLAN_NOT_FOUND", "本周还没有餐单");
        }
        List<Map<String, Object>> recipes = recipeRepository.findRecipes(DefaultFamily.FAMILY_ID);
        Map<Long, Map<String, Object>> recipeMap = new LinkedHashMap<>();
        for (Map<String, Object> recipe : recipes) {
            recipeMap.put(numberValue(recipe, "id"), recipe);
        }
        LinkedHashSet<String> ingredientNames = new LinkedHashSet<>();
        for (Map<String, Object> mealPlan : mealPlans) {
            Long recipeId = numberValue(mealPlan, "recipe_id");
            Map<String, Object> recipe = recipeMap.get(recipeId);
            if (Objects.isNull(recipe)) {
                continue;
            }
            addIngredients(ingredientNames, textValue(recipe, "ingredients_text"));
        }
        if (ingredientNames.isEmpty()) {
            throw new BusinessException("MEAL_PLAN_INGREDIENT_EMPTY", "菜谱还没有填写原料，无法同步购物清单");
        }
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        LinkedHashSet<String> existingTodoNames = new LinkedHashSet<>();
        for (Map<String, Object> item : shoppingRepository.findItems(DefaultFamily.FAMILY_ID)) {
            if (StringUtils.equals(textValue(item, "status"), "TODO")) {
                existingTodoNames.add(textValue(item, "name"));
            }
        }
        int createdCount = 0;
        for (String ingredientName : ingredientNames) {
            if (existingTodoNames.contains(ingredientName)) {
                continue;
            }
            CreateShoppingItemRequest request = new CreateShoppingItemRequest();
            request.setName(ingredientName);
            request.setCategory("MEAL");
            request.setChannel("餐单");
            request.setQuantity("");
            shoppingRepository.insert(idGenerator.nextId(), DefaultFamily.FAMILY_ID, request, currentUser.getUserId(),
                    timeProvider.now());
            createdCount++;
        }
        recipeRepository.markShoppingSynced(DefaultFamily.FAMILY_ID, safeWeekStart, currentUser.getUserId(),
                timeProvider.now());
        return Map.of("createdCount", createdCount, "weekStart", safeWeekStart);
    }

    /**
     * 删除餐单。
     *
     * @param mealPlanId 餐单 ID
     * @return 删除结果
     */
    public Map<String, Object> deleteMealPlan(Long mealPlanId) {
        ensureMealPlanExists(mealPlanId);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        reminderService.deleteBySource(MEAL_PLAN_SOURCE_TYPE, mealPlanId);
        int updated = recipeRepository.deleteMealPlan(mealPlanId, DefaultFamily.FAMILY_ID, currentUser.getUserId(),
                timeProvider.now());
        return Map.of("updated", updated);
    }

    private void addIngredients(LinkedHashSet<String> ingredientNames, String ingredientsText) {
        if (StringUtils.isBlank(ingredientsText)) {
            return;
        }
        String[] lines = ingredientsText.split("\\r?\\n");
        for (String line : lines) {
            String normalized = StringUtils.trim(line);
            if (StringUtils.isNotBlank(normalized)) {
                ingredientNames.add(normalized);
            }
        }
    }

    private void syncMealPlanReminder(Long mealPlanId, String titleSnapshot, LocalDateTime remindAt) {
        reminderService.deleteBySource(MEAL_PLAN_SOURCE_TYPE, mealPlanId);
        if (Objects.isNull(remindAt)) {
            return;
        }
        reminderService.createFromSource("今日餐单：" + titleSnapshot, "记得准备食材", MEAL_PLAN_SOURCE_TYPE,
                mealPlanId, remindAt);
    }

    private String resolveMealPlanTitle(Long recipeId, String titleSnapshot) {
        if (StringUtils.isNotBlank(titleSnapshot)) {
            return titleSnapshot;
        }
        if (Objects.nonNull(recipeId) && recipeId > 0) {
            return textValue(ensureRecipeExists(recipeId), "title");
        }
        throw new BusinessException("MEAL_PLAN_TITLE_REQUIRED", "请选择菜谱或填写餐单标题");
    }

    private Map<String, Object> ensureRecipeExists(Long recipeId) {
        return recipeRepository.findRecipe(recipeId, DefaultFamily.FAMILY_ID)
                .orElseThrow(() -> new BusinessException("RECIPE_NOT_FOUND", "菜谱不存在或已删除"));
    }

    private void ensureMealPlanExists(Long mealPlanId) {
        recipeRepository.findMealPlan(mealPlanId, DefaultFamily.FAMILY_ID)
                .orElseThrow(() -> new BusinessException("MEAL_PLAN_NOT_FOUND", "餐单不存在或已删除"));
    }

    private Map<String, Object> enrichRecipe(Map<String, Object> recipe) {
        Map<String, Object> recipeView = new LinkedHashMap<>(recipe);
        recipeView.put("preferredMemberIds", splitMemberIds(textValue(recipe, "preferred_member_ids")));
        return recipeView;
    }

    private LocalDate resolveWeekStart(String weekStart, LocalDate referenceDate) {
        if (StringUtils.isNotBlank(weekStart)) {
            try {
                return LocalDate.parse(weekStart).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            } catch (DateTimeParseException exception) {
                return referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            }
        }
        return referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private LocalDate parseDateOrToday(String value) {
        if (StringUtils.isBlank(value)) {
            return timeProvider.today();
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            return timeProvider.today();
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private LocalTime parseTime(String value) {
        if (StringUtils.isBlank(value)) {
            return LocalTime.of(17, 30);
        }
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException exception) {
            return LocalTime.of(17, 30);
        }
    }

    private String resolveMealSlot(String mealSlot) {
        return StringUtils.defaultIfBlank(mealSlot, "DINNER");
    }

    private Long normalizeRecipeId(Long recipeId) {
        if (Objects.isNull(recipeId) || recipeId <= 0) {
            return null;
        }
        return recipeId;
    }

    private String joinMemberIds(List<Long> preferredMemberIds) {
        if (Objects.isNull(preferredMemberIds) || preferredMemberIds.isEmpty()) {
            return "";
        }
        List<String> values = new ArrayList<>();
        for (Long preferredMemberId : preferredMemberIds) {
            if (Objects.nonNull(preferredMemberId)) {
                values.add(String.valueOf(preferredMemberId));
            }
        }
        return String.join(",", values);
    }

    private List<Long> splitMemberIds(String preferredMemberIds) {
        List<Long> values = new ArrayList<>();
        if (StringUtils.isBlank(preferredMemberIds)) {
            return values;
        }
        String[] parts = preferredMemberIds.split(",");
        for (String part : parts) {
            if (StringUtils.isBlank(part)) {
                continue;
            }
            try {
                values.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException exception) {
                continue;
            }
        }
        return values;
    }

    private Long numberValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    private String textValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (Objects.isNull(value)) {
            return "";
        }
        return String.valueOf(value);
    }
}
