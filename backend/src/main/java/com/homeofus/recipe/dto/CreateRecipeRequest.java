package com.homeofus.recipe.dto;

import java.util.List;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建菜谱请求。
 *
 * @author tanchaohong
 */
@Getter
@Setter
public class CreateRecipeRequest {

    @NotBlank(message = "recipe.title.required")
    private String title;

    private String mealType;

    private String ingredientsText;

    private String stepsText;

    private List<Long> preferredMemberIds;
}
