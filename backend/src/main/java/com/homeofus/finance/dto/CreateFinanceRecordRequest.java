package com.homeofus.finance.dto;

import java.math.BigDecimal;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建账本记录请求。
 *
 * @author tanchaohong
 */
@Getter
@Setter
public class CreateFinanceRecordRequest {

    @NotBlank(message = "finance.title.required")
    private String title;

    @NotNull(message = "finance.amount.required")
    private BigDecimal amount;

    private String direction;

    private String category;

    private Long ownerId;

    private String occurredOn;
}

