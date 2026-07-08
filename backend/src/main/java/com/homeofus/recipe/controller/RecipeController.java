package com.homeofus.recipe.controller;

import com.homeofus.common.api.ApiResponse;
import com.homeofus.recipe.dto.CreateMealPlanRequest;
import com.homeofus.recipe.dto.CreateRecipeRequest;
import com.homeofus.recipe.dto.GenerateWeeklyMealPlanRequest;
import com.homeofus.recipe.dto.UpdateRecipeRequest;
import com.homeofus.recipe.service.RecipeService;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 菜谱与餐单接口。
 *
 * @author tanchaohong
 */
@RestController
@RequestMapping("/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    /**
     * 创建菜谱。
     *
     * @param request 创建请求
     * @return 新菜谱 ID
     */
    @PostMapping
    public ApiResponse<Map<String, Object>> createRecipe(@Valid @RequestBody CreateRecipeRequest request) {
        return ApiResponse.ok(recipeService.createRecipe(request));
    }

    /**
     * 查询菜谱列表。
     *
     * @return 菜谱列表
     */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> findRecipes() {
        return ApiResponse.ok(recipeService.findRecipes());
    }

    /**
     * 更新菜谱。
     *
     * @param recipeId 菜谱 ID
     * @param request 更新请求
     * @return 更新结果
     */
    @PatchMapping("/{recipeId}")
    public ApiResponse<Map<String, Object>> updateRecipe(@PathVariable Long recipeId,
            @Valid @RequestBody UpdateRecipeRequest request) {
        return ApiResponse.ok(recipeService.updateRecipe(recipeId, request));
    }

    /**
     * 删除菜谱。
     *
     * @param recipeId 菜谱 ID
     * @return 删除结果
     */
    @DeleteMapping("/{recipeId}")
    public ApiResponse<Map<String, Object>> deleteRecipe(@PathVariable Long recipeId) {
        return ApiResponse.ok(recipeService.deleteRecipe(recipeId));
    }

    /**
     * 创建单条餐单。
     *
     * @param request 创建请求
     * @return 新餐单 ID
     */
    @PostMapping("/meal-plans")
    public ApiResponse<Map<String, Object>> createMealPlan(@Valid @RequestBody CreateMealPlanRequest request) {
        return ApiResponse.ok(recipeService.createMealPlan(request));
    }

    /**
     * 查询周餐单。
     *
     * @param weekStart 周起始日
     * @return 餐单列表
     */
    @GetMapping("/meal-plans")
    public ApiResponse<List<Map<String, Object>>> findMealPlans(@RequestParam(required = false) String weekStart) {
        return ApiResponse.ok(recipeService.findMealPlans(weekStart));
    }

    /**
     * 自动生成周餐单。
     *
     * @param request 生成请求
     * @return 生成结果
     */
    @PostMapping("/meal-plans/generate-weekly")
    public ApiResponse<Map<String, Object>> generateWeeklyMealPlans(
            @RequestBody(required = false) GenerateWeeklyMealPlanRequest request) {
        return ApiResponse.ok(recipeService.generateWeeklyMealPlans(request));
    }

    /**
     * 同步周餐单到购物清单。
     *
     * @param weekStart 周起始日
     * @return 同步结果
     */
    @PostMapping("/meal-plans/{weekStart}/shopping-sync")
    public ApiResponse<Map<String, Object>> syncShoppingList(@PathVariable String weekStart) {
        return ApiResponse.ok(recipeService.syncShoppingList(weekStart));
    }

    /**
     * 删除单条餐单。
     *
     * @param mealPlanId 餐单 ID
     * @return 删除结果
     */
    @DeleteMapping("/meal-plans/{mealPlanId}")
    public ApiResponse<Map<String, Object>> deleteMealPlan(@PathVariable Long mealPlanId) {
        return ApiResponse.ok(recipeService.deleteMealPlan(mealPlanId));
    }
}
