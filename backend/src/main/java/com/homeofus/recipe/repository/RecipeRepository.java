package com.homeofus.recipe.repository;

import com.homeofus.recipe.dto.CreateRecipeRequest;
import com.homeofus.recipe.dto.UpdateRecipeRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 菜谱与餐单数据访问。
 *
 * @author tanchaohong
 */
@Repository
public class RecipeRepository {

    private final JdbcTemplate jdbcTemplate;

    public RecipeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 新增菜谱。
     *
     * @param id 菜谱 ID
     * @param familyId 家庭 ID
     * @param request 创建请求
     * @param preferredMemberIds 偏好成员
     * @param operatorId 操作人
     * @param now 当前时间
     */
    public void insertRecipe(Long id, Long familyId, CreateRecipeRequest request, String preferredMemberIds,
            Long operatorId, LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO recipe (id, family_id, title, meal_type, ingredients_text, steps_text, preferred_member_ids, "
                        + "created_at, updated_at, created_by, updated_by, deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                id, familyId, request.getTitle(), request.getMealType(), request.getIngredientsText(),
                request.getStepsText(), preferredMemberIds, now, now, operatorId, operatorId);
    }

    /**
     * 查询菜谱列表。
     *
     * @param familyId 家庭 ID
     * @return 菜谱列表
     */
    public List<Map<String, Object>> findRecipes(Long familyId) {
        return jdbcTemplate.queryForList(
                "SELECT id, family_id, title, meal_type, ingredients_text, steps_text, preferred_member_ids, created_at "
                        + "FROM recipe WHERE family_id = ? AND deleted = 0 ORDER BY created_at DESC",
                familyId);
    }

    /**
     * 查询单个菜谱。
     *
     * @param recipeId 菜谱 ID
     * @param familyId 家庭 ID
     * @return 菜谱
     */
    public Optional<Map<String, Object>> findRecipe(Long recipeId, Long familyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, family_id, title, meal_type, ingredients_text, steps_text, preferred_member_ids "
                        + "FROM recipe WHERE id = ? AND family_id = ? AND deleted = 0",
                recipeId, familyId);
        return rows.stream().findFirst();
    }

    /**
     * 更新菜谱。
     *
     * @param recipeId 菜谱 ID
     * @param familyId 家庭 ID
     * @param request 更新请求
     * @param preferredMemberIds 偏好成员
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int updateRecipe(Long recipeId, Long familyId, UpdateRecipeRequest request, String preferredMemberIds,
            Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE recipe SET title = ?, meal_type = ?, ingredients_text = ?, steps_text = ?, "
                        + "preferred_member_ids = ?, updated_at = ?, updated_by = ? "
                        + "WHERE id = ? AND family_id = ? AND deleted = 0",
                request.getTitle(), request.getMealType(), request.getIngredientsText(), request.getStepsText(),
                preferredMemberIds, now, operatorId, recipeId, familyId);
    }

    /**
     * 删除菜谱。
     *
     * @param recipeId 菜谱 ID
     * @param familyId 家庭 ID
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int deleteRecipe(Long recipeId, Long familyId, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE recipe SET deleted = 1, updated_at = ?, updated_by = ? "
                        + "WHERE id = ? AND family_id = ? AND deleted = 0",
                now, operatorId, recipeId, familyId);
    }

    /**
     * 新增餐单。
     *
     * @param id 餐单 ID
     * @param familyId 家庭 ID
     * @param weekStart 周起始日
     * @param plannedOn 计划日期
     * @param mealSlot 餐次
     * @param recipeId 菜谱 ID
     * @param titleSnapshot 标题快照
     * @param remindAt 提醒时间
     * @param operatorId 操作人
     * @param now 当前时间
     */
    public void insertMealPlan(Long id, Long familyId, LocalDate weekStart, LocalDate plannedOn, String mealSlot,
            Long recipeId, String titleSnapshot, LocalDateTime remindAt, Long operatorId, LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO meal_plan (id, family_id, week_start, planned_on, meal_slot, recipe_id, title_snapshot, "
                        + "remind_at, shopping_synced, created_at, updated_at, created_by, updated_by, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?, 0)",
                id, familyId, weekStart, plannedOn, mealSlot, recipeId, titleSnapshot, remindAt, now, now, operatorId,
                operatorId);
    }

    /**
     * 查询周餐单。
     *
     * @param familyId 家庭 ID
     * @param weekStart 周起始日
     * @return 餐单列表
     */
    public List<Map<String, Object>> findMealPlans(Long familyId, LocalDate weekStart) {
        return jdbcTemplate.queryForList(
                "SELECT id, family_id, week_start, planned_on, meal_slot, recipe_id, title_snapshot, remind_at, "
                        + "shopping_synced, created_at FROM meal_plan "
                        + "WHERE family_id = ? AND week_start = ? AND deleted = 0 ORDER BY planned_on ASC, meal_slot ASC",
                familyId, weekStart);
    }

    /**
     * 查询单个餐单。
     *
     * @param mealPlanId 餐单 ID
     * @param familyId 家庭 ID
     * @return 餐单
     */
    public Optional<Map<String, Object>> findMealPlan(Long mealPlanId, Long familyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, family_id, week_start, planned_on, meal_slot, recipe_id, title_snapshot, remind_at, "
                        + "shopping_synced FROM meal_plan WHERE id = ? AND family_id = ? AND deleted = 0",
                mealPlanId, familyId);
        return rows.stream().findFirst();
    }

    /**
     * 删除单个餐单。
     *
     * @param mealPlanId 餐单 ID
     * @param familyId 家庭 ID
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int deleteMealPlan(Long mealPlanId, Long familyId, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE meal_plan SET deleted = 1, updated_at = ?, updated_by = ? "
                        + "WHERE id = ? AND family_id = ? AND deleted = 0",
                now, operatorId, mealPlanId, familyId);
    }

    /**
     * 删除整周餐单。
     *
     * @param familyId 家庭 ID
     * @param weekStart 周起始日
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int deleteMealPlansByWeekStart(Long familyId, LocalDate weekStart, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE meal_plan SET deleted = 1, updated_at = ?, updated_by = ? "
                        + "WHERE family_id = ? AND week_start = ? AND deleted = 0",
                now, operatorId, familyId, weekStart);
    }

    /**
     * 标记整周餐单已同步购物清单。
     *
     * @param familyId 家庭 ID
     * @param weekStart 周起始日
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int markShoppingSynced(Long familyId, LocalDate weekStart, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE meal_plan SET shopping_synced = 1, updated_at = ?, updated_by = ? "
                        + "WHERE family_id = ? AND week_start = ? AND deleted = 0",
                now, operatorId, familyId, weekStart);
    }
}
