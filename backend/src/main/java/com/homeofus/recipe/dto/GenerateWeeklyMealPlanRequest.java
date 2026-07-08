package com.homeofus.recipe.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 生成周餐单请求。
 *
 * @author tanchaohong
 */
@Getter
@Setter
public class GenerateWeeklyMealPlanRequest {

    private String weekStart;

    private Boolean enableReminder;

    private String remindTime;
}
