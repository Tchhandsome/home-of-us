package com.homeofus.plant.dto;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建花卉养护记录请求。
 *
 * @author tanchaohong
 */
@Getter
@Setter
public class CreatePlantCareRecordRequest {

    @NotBlank(message = "plant.careType.required")
    private String careType;

    private String careDate;

    private String detail;

    private String rawText;

    private String nextCareAt;
}

