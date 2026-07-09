package com.homeofus.period.dto;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 新增月经记录请求。
 *
 * @author tanchaohong
 */
@Getter
@Setter
public class CreatePeriodRecordRequest {

    @NotBlank(message = "period.startOn.required")
    private String startOn;

    private String endOn;

    private String note;
}
