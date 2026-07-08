package com.homeofus.chore.service;

import com.homeofus.chore.dto.CreateChoreTaskRequest;
import com.homeofus.chore.dto.UpdateChoreTaskRequest;
import com.homeofus.chore.repository.ChoreRepository;
import com.homeofus.common.domain.DefaultFamily;
import com.homeofus.common.exception.BusinessException;
import com.homeofus.common.jdbc.IdGenerator;
import com.homeofus.common.time.TimeProvider;
import com.homeofus.common.web.CurrentUser;
import com.homeofus.common.web.CurrentUserProvider;
import com.homeofus.reminder.service.ReminderService;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 共享待办业务服务。
 *
 * @author tanchaohong
 */
@Service
public class ChoreService {

    private static final String TODO_SOURCE_TYPE = "TODO_TASK";

    private final ChoreRepository choreRepository;

    private final ReminderService reminderService;

    private final CurrentUserProvider currentUserProvider;

    private final IdGenerator idGenerator;

    private final TimeProvider timeProvider;

    public ChoreService(ChoreRepository choreRepository, ReminderService reminderService,
            CurrentUserProvider currentUserProvider, IdGenerator idGenerator, TimeProvider timeProvider) {
        this.choreRepository = choreRepository;
        this.reminderService = reminderService;
        this.currentUserProvider = currentUserProvider;
        this.idGenerator = idGenerator;
        this.timeProvider = timeProvider;
    }

    /**
     * 创建待办任务。
     *
     * @param request 创建请求
     * @return 新任务 ID
     */
    public Map<String, Object> create(CreateChoreTaskRequest request) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        Long id = idGenerator.nextId();
        String taskScope = resolveTaskScope(request.getTaskScope());
        Long assigneeId = resolveAssigneeId(taskScope, request.getAssigneeId(), currentUser.getMemberId());
        LocalDateTime dueAt = parseDueAt(request.getDueAt());
        choreRepository.insert(id, DefaultFamily.FAMILY_ID, request, taskScope, currentUser.getMemberId(), assigneeId,
                resolveTaskType(request.getTaskType()), dueAt, currentUser.getUserId(), timeProvider.now());
        // 待办与提醒中心共用来源标识，更新时统一回收重建，避免重复提醒。
        syncReminder(id, request.getTitle(), request.getNote(), dueAt);
        return Map.of("id", id);
    }

    /**
     * 查询待办任务。
     *
     * @return 待办任务列表
     */
    public List<Map<String, Object>> findTasks() {
        return choreRepository.findTasks(DefaultFamily.FAMILY_ID);
    }

    /**
     * 更新待办任务。
     *
     * @param taskId 任务 ID
     * @param request 更新请求
     * @return 更新结果
     */
    public Map<String, Object> update(Long taskId, UpdateChoreTaskRequest request) {
        ensureTaskExists(taskId);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        String taskScope = resolveTaskScope(request.getTaskScope());
        Long assigneeId = resolveAssigneeId(taskScope, request.getAssigneeId(), currentUser.getMemberId());
        LocalDateTime dueAt = parseDueAt(request.getDueAt());
        int updated = choreRepository.update(DefaultFamily.FAMILY_ID, taskId, request, taskScope,
                currentUser.getMemberId(), assigneeId, resolveTaskType(request.getTaskType()), dueAt,
                currentUser.getUserId(), timeProvider.now());
        syncReminder(taskId, request.getTitle(), request.getNote(), dueAt);
        return Map.of("updated", updated);
    }

    /**
     * 认领共享任务。
     *
     * @param taskId 任务 ID
     * @return 更新结果
     */
    public Map<String, Object> claim(Long taskId) {
        Map<String, Object> task = ensureTaskExists(taskId);
        if (!StringUtils.equals(textValue(task, "task_scope"), "SHARED")) {
            throw new BusinessException("CHORE_TASK_SCOPE_INVALID", "只有共享任务可以认领");
        }
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        int updated = choreRepository.claim(DefaultFamily.FAMILY_ID, taskId, currentUser.getMemberId(),
                currentUser.getUserId(), timeProvider.now());
        return Map.of("updated", updated);
    }

    /**
     * 完成待办任务。
     *
     * @param taskId 任务 ID
     * @return 更新结果
     */
    public Map<String, Object> complete(Long taskId) {
        ensureTaskExists(taskId);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        int updated = choreRepository.complete(DefaultFamily.FAMILY_ID, taskId, currentUser.getMemberId(),
                currentUser.getUserId(), timeProvider.now());
        reminderService.deleteBySource(TODO_SOURCE_TYPE, taskId);
        return Map.of("updated", updated);
    }

    /**
     * 删除待办任务。
     *
     * @param taskId 任务 ID
     * @return 删除结果
     */
    public Map<String, Object> delete(Long taskId) {
        ensureTaskExists(taskId);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        reminderService.deleteBySource(TODO_SOURCE_TYPE, taskId);
        int updated = choreRepository.delete(DefaultFamily.FAMILY_ID, taskId, currentUser.getUserId(),
                timeProvider.now());
        return Map.of("updated", updated);
    }

    private void syncReminder(Long taskId, String title, String note, LocalDateTime dueAt) {
        reminderService.deleteBySource(TODO_SOURCE_TYPE, taskId);
        if (Objects.isNull(dueAt)) {
            return;
        }
        reminderService.createFromSource(title, StringUtils.defaultIfBlank(note, "家庭待办"), TODO_SOURCE_TYPE, taskId,
                dueAt);
    }

    private Map<String, Object> ensureTaskExists(Long taskId) {
        return choreRepository.findTask(DefaultFamily.FAMILY_ID, taskId)
                .orElseThrow(() -> new BusinessException("CHORE_TASK_NOT_FOUND", "待办不存在或已删除"));
    }

    private String resolveTaskScope(String taskScope) {
        if (StringUtils.equalsIgnoreCase(taskScope, "SHARED")) {
            return "SHARED";
        }
        return "PERSONAL";
    }

    private Long resolveAssigneeId(String taskScope, Long assigneeId, Long currentMemberId) {
        if (StringUtils.equals(taskScope, "PERSONAL")) {
            return currentMemberId;
        }
        return assigneeId;
    }

    private String resolveTaskType(String taskType) {
        return StringUtils.defaultIfBlank(taskType, "TEMPORARY");
    }

    private LocalDateTime parseDueAt(String dueAt) {
        if (StringUtils.isBlank(dueAt)) {
            return null;
        }
        try {
            return LocalDateTime.parse(dueAt);
        } catch (DateTimeParseException exception) {
            return timeProvider.now().plusDays(1);
        }
    }

    private String textValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (Objects.isNull(value)) {
            return "";
        }
        return String.valueOf(value);
    }
}
