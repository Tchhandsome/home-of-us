package com.homeofus.shopping.dto;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建购物项请求。
 *
 * @author tanchaohong
 */
@Getter
@Setter
public class CreateShoppingItemRequest {

    @NotBlank(message = "shopping.name.required")
    private String name;

    private String category;

    private String channel;

    private String quantity;
}

