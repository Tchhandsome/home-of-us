package com.homeofus.inventory.dto;

import java.math.BigDecimal;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建库存项请求。
 *
 * @author tanchaohong
 */
@Getter
@Setter
public class CreateInventoryItemRequest {

    @NotBlank(message = "inventory.name.required")
    private String name;

    private String category;

    private BigDecimal quantity;

    private String unit;

    private BigDecimal lowStockThreshold;
}

