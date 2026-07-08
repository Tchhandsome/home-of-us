package com.homeofus.shopping.dto;

import java.math.BigDecimal;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 完成购物项请求。
 *
 * @author tanchaohong
 */
@Getter
@Setter
public class CompleteShoppingItemRequest {

    @NotNull(message = "shopping.actualAmount.required")
    @DecimalMin(value = "0.01", message = "shopping.actualAmount.min")
    private BigDecimal actualAmount;

    private String category;

    private Long buyerId;
}
