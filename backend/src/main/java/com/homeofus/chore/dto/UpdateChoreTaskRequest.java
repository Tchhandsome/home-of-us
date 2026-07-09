package com.homeofus.chore.dto;

import java.util.List;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 更新待办任务请求。
 *
 * @author tanchaohong
 */
@Getter
@Setter
public class UpdateChoreTaskRequest {

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
