package com.homeofus.pet.dto;

import java.math.BigDecimal;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建宠物体重记录请求。
 *
 * @author tanchaohong
 */
@Getter
@Setter
public class CreatePetWeightRecordRequest {

    @NotNull(message = "pet.weight.required")
    private BigDecimal weightKg;

    private String recordedOn;

    private String note;
}
