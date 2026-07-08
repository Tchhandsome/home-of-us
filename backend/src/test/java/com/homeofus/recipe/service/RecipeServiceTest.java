package com.homeofus.recipe.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.common.web.CurrentUser;
import com.homeofus.common.web.CurrentUserProvider;
import com.homeofus.recipe.dto.GenerateWeeklyMealPlanRequest;
import com.homeofus.recipe.repository.RecipeRepository;
import com.homeofus.reminder.service.ReminderService;
import com.homeofus.shopping.repository.ShoppingRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 菜谱服务测试。
 *
 * @author tanchaohong
 */
@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private ShoppingRepository shoppingRepository;

    @Mock
    private ReminderService reminderService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private TimeProvider timeProvider;

    private RecipeService recipeService;

    @BeforeEach
    void setUp() {
        recipeService = new RecipeService(recipeRepository, shoppingRepository, reminderService, currentUserProvider,
                idGenerator, timeProvider);
    }

    @Test
    void shouldGenerateWeeklyMealPlansWithReminder() {
        CurrentUser currentUser = new CurrentUser(9001L, DefaultFamily.FAMILY_ID, 1001L, "xiaotan", "小谭");
        LocalDateTime now = LocalDateTime.of(2026, 7, 9, 14, 0);
        GenerateWeeklyMealPlanRequest request = new GenerateWeeklyMealPlanRequest();
        request.setWeekStart("2026-07-13");
        request.setEnableReminder(true);
        request.setRemindTime("18:00");

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(timeProvider.now()).thenReturn(now);
        when(timeProvider.today()).thenReturn(LocalDate.of(2026, 7, 9));
        when(recipeRepository.findRecipes(DefaultFamily.FAMILY_ID))
                .thenReturn(List.of(
                        Map.of("id", 1L, "title", "番茄牛腩"),
                        Map.of("id", 2L, "title", "土豆鸡块")));
        when(recipeRepository.findMealPlans(DefaultFamily.FAMILY_ID, LocalDate.of(2026, 7, 13))).thenReturn(List.of());
        when(idGenerator.nextId()).thenReturn(9401L, 9402L, 9403L, 9404L, 9405L, 9406L, 9407L);

        Map<String, Object> result = recipeService.generateWeeklyMealPlans(request);

        assertEquals(7, result.get("generatedCount"));
        verify(recipeRepository, times(7)).insertMealPlan(org.mockito.ArgumentMatchers.anyLong(),
                eq(DefaultFamily.FAMILY_ID), eq(LocalDate.of(2026, 7, 13)), org.mockito.ArgumentMatchers.any(LocalDate.class),
                eq("DINNER"), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class), eq(9001L), eq(now));
        verify(reminderService, times(7)).createFromSource(org.mockito.ArgumentMatchers.anyString(),
                eq("记得准备食材"), eq("MEAL_PLAN"), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class));
    }

    @Test
    void shouldSyncWeeklyMealPlansToShoppingListWithDeduplication() {
        CurrentUser currentUser = new CurrentUser(9001L, DefaultFamily.FAMILY_ID, 1001L, "xiaotan", "小谭");
        LocalDateTime now = LocalDateTime.of(2026, 7, 9, 14, 30);

        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(timeProvider.now()).thenReturn(now);
        when(timeProvider.today()).thenReturn(LocalDate.of(2026, 7, 9));
        when(recipeRepository.findMealPlans(DefaultFamily.FAMILY_ID, LocalDate.of(2026, 7, 13)))
                .thenReturn(List.of(
                        Map.of("id", 1L, "recipe_id", 101L),
                        Map.of("id", 2L, "recipe_id", 102L)));
        when(recipeRepository.findRecipes(DefaultFamily.FAMILY_ID))
                .thenReturn(List.of(
                        Map.of("id", 101L, "ingredients_text", "牛肉\n番茄\n土豆"),
                        Map.of("id", 102L, "ingredients_text", "土豆\n洋葱")));
        when(shoppingRepository.findItems(DefaultFamily.FAMILY_ID))
                .thenReturn(List.of(Map.of("name", "番茄", "status", "TODO")));
        when(idGenerator.nextId()).thenReturn(9501L, 9502L, 9503L);

        Map<String, Object> result = recipeService.syncShoppingList("2026-07-13");

        assertEquals(3, result.get("createdCount"));
        verify(shoppingRepository, times(3)).insert(org.mockito.ArgumentMatchers.anyLong(), eq(DefaultFamily.FAMILY_ID),
                org.mockito.ArgumentMatchers.any(), eq(9001L), eq(now));
        verify(recipeRepository).markShoppingSynced(DefaultFamily.FAMILY_ID, LocalDate.of(2026, 7, 13), 9001L, now);
    }
}
