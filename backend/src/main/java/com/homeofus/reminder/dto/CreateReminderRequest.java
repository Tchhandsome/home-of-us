package com.homeofus.reminder.dto;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建提醒请求。
 *
 * @author tanchaohong
 */
@Getter
@Setter
public class CreateReminderRequest {

    @NotBlank(message = "reminder.title.required")
    private String title;

    private String description;

    private String sourceType;

    private Long sourceId;

    @NotBlank(message = "reminder.dueAt.required")
    private String dueAt;

    private String repeatRule;
}

