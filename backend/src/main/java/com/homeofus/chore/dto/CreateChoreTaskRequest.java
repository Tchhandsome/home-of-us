package com.homeofus.chore.dto;

import java.util.List;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建家务任务请求。
 *
 * @author tanchaohong
 */
@Getter
@Setter
public class CreateChoreTaskRequest {

    @NotBlank(message = "chore.title.required")
    private String title;

    private String taskScope;

    private Long assigneeId;

    private List<Long> assigneeIds;

    private String taskType;

    private String cycleRule;

    private String note;

    private String dueAt;
}
