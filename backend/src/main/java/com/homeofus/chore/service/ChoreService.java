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
import java.util.ArrayList;
import java.util.LinkedHashSet;
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
        List<Long> assigneeIds = resolveAssigneeIds(taskScope, request.getAssigneeId(), request.getAssigneeIds(),
                currentUser.getMemberId());
        Long assigneeId = firstAssigneeId(assigneeIds);
        LocalDateTime now = timeProvider.now();
        LocalDateTime dueAt = parseDueAt(request.getDueAt());
        choreRepository.insert(id, DefaultFamily.FAMILY_ID, request, taskScope, currentUser.getMemberId(), assigneeId,
                resolveTaskType(request.getTaskType()), dueAt, currentUser.getUserId(), now);
        syncAssignees(id, assigneeIds, currentUser, now);
        // 待办不再进入提醒中心，但仍兜底清掉旧来源，避免历史脏数据继续显示。
        clearReminder(id);
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
        List<Long> assigneeIds = resolveAssigneeIds(taskScope, request.getAssigneeId(), request.getAssigneeIds(),
                currentUser.getMemberId());
        Long assigneeId = firstAssigneeId(assigneeIds);
        LocalDateTime now = timeProvider.now();
        LocalDateTime dueAt = parseDueAt(request.getDueAt());
        int updated = choreRepository.update(DefaultFamily.FAMILY_ID, taskId, request, taskScope,
                currentUser.getMemberId(), assigneeId, resolveTaskType(request.getTaskType()), dueAt,
                currentUser.getUserId(), now);
        syncAssignees(taskId, assigneeIds, currentUser, now);
        clearReminder(taskId);
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
        LocalDateTime now = timeProvider.now();
        ensureTaskAssignee(taskId, currentUser.getMemberId(), currentUser.getUserId(), now);
        int updated = choreRepository.claim(DefaultFamily.FAMILY_ID, taskId, currentUser.getMemberId(),
                currentUser.getUserId(), now);
        return Map.of("updated", updated);
    }

    /**
     * 完成待办任务。
     *
     * @param taskId 任务 ID
     * @return 更新结果
     */
    public Map<String, Object> complete(Long taskId) {
        Map<String, Object> task = ensureTaskExists(taskId);
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        LocalDateTime now = timeProvider.now();
        if (StringUtils.equals(textValue(task, "task_scope"), "SHARED")) {
            ensureTaskAssignee(taskId, currentUser.getMemberId(), currentUser.getUserId(), now);
        }
        int updated = choreRepository.complete(DefaultFamily.FAMILY_ID, taskId, currentUser.getMemberId(),
                currentUser.getUserId(), now);
        clearReminder(taskId);
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
        clearReminder(taskId);
        int updated = choreRepository.delete(DefaultFamily.FAMILY_ID, taskId, currentUser.getUserId(),
                timeProvider.now());
        return Map.of("updated", updated);
    }

    private void clearReminder(Long taskId) {
        reminderService.deleteBySource(TODO_SOURCE_TYPE, taskId);
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

    private void syncAssignees(Long taskId, List<Long> assigneeIds, CurrentUser currentUser, LocalDateTime now) {
        choreRepository.clearAssignees(DefaultFamily.FAMILY_ID, taskId, currentUser.getUserId(), now);
        assigneeIds.forEach(memberId -> ensureTaskAssignee(taskId, memberId, currentUser.getUserId(), now));
        choreRepository.setPrimaryAssignee(DefaultFamily.FAMILY_ID, taskId, firstAssigneeId(assigneeIds),
                currentUser.getUserId(), now);
    }

    private void ensureTaskAssignee(Long taskId, Long memberId, Long operatorId, LocalDateTime now) {
        if (Objects.isNull(memberId)) {
            return;
        }
        int restored = choreRepository.restoreAssignee(DefaultFamily.FAMILY_ID, taskId, memberId, operatorId, now);
        if (restored == 0) {
            choreRepository.insertAssignee(idGenerator.nextId(), DefaultFamily.FAMILY_ID, taskId, memberId, operatorId,
                    now);
        }
    }

    private List<Long> resolveAssigneeIds(String taskScope, Long assigneeId, List<Long> assigneeIds,
            Long currentMemberId) {
        if (StringUtils.equals(taskScope, "PERSONAL")) {
            return List.of(currentMemberId);
        }
        LinkedHashSet<Long> normalized = new LinkedHashSet<>();
        if (Objects.nonNull(assigneeIds)) {
            assigneeIds.stream()
                    .filter(Objects::nonNull)
                    .filter(memberId -> memberId > 0)
                    .forEach(normalized::add);
        }
        if (Objects.nonNull(assigneeId) && assigneeId > 0) {
            normalized.add(assigneeId);
        }
        return new ArrayList<>(normalized);
    }

    private Long firstAssigneeId(List<Long> assigneeIds) {
        if (assigneeIds.isEmpty()) {
            return null;
        }
        return assigneeIds.get(0);
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
