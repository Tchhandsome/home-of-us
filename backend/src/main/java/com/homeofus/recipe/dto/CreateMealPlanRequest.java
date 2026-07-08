package com.homeofus.recipe.dto;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建餐单请求。
 *
 * @author tanchaohong
 */
@Getter
@Setter
public class CreateMealPlanRequest {

    private String weekStart;

    @NotBlank(message = "meal.plan.date.required")
    private String plannedOn;

    private String mealSlot;

    private Long recipeId;

    private String titleSnapshot;

    private String remindAt;
}
