package com.homeofus.record.dto;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建快速记录请求。
 *
 * @author tanchaohong
 */
@Getter
@Setter
public class CreateQuickRecordRequest {

    @NotBlank(message = "record.text.required")
    private String rawText;

    private String recordType;

    private String linkedType;

    private Long linkedId;

    private String happenedOn;
}

