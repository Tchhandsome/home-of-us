package com.homeofus.chore.repository;

import com.homeofus.chore.dto.CreateChoreTaskRequest;
import com.homeofus.chore.dto.UpdateChoreTaskRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 待办任务数据访问。
 *
 * @author tanchaohong
 */
@Repository
public class ChoreRepository {

    private final JdbcTemplate jdbcTemplate;

    public ChoreRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 新增待办任务。
     *
     * @param id 主键
     * @param familyId 家庭 ID
     * @param request 创建请求
     * @param taskScope 任务范围
     * @param ownerMemberId 所属成员
     * @param assigneeId 认领成员
     * @param taskType 任务类型
     * @param dueAt 截止时间
     * @param operatorId 操作人
     * @param now 当前时间
     */
    public void insert(Long id, Long familyId, CreateChoreTaskRequest request, String taskScope, Long ownerMemberId,
            Long assigneeId, String taskType, LocalDateTime dueAt, Long operatorId, LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO chore_task (id, family_id, title, task_scope, owner_member_id, assignee_id, task_type, "
                        + "cycle_rule, note, status, due_at, completed_at, created_at, updated_at, created_by, "
                        + "updated_by, deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'TODO', ?, NULL, ?, ?, ?, ?, 0)",
                id, familyId, request.getTitle(), taskScope, ownerMemberId, assigneeId, taskType,
                request.getCycleRule(), request.getNote(), dueAt, now, now, operatorId, operatorId);
    }

    /**
     * 查询待办任务列表。
     *
     * @param familyId 家庭 ID
     * @return 待办任务列表
     */
    public List<Map<String, Object>> findTasks(Long familyId) {
        return jdbcTemplate.queryForList(
                "SELECT task.id, task.family_id, task.title, task.task_scope, task.owner_member_id, "
                        + "task.assignee_id, owner.display_name AS owner_name, assignee.display_name AS assignee_name, "
                        + "COALESCE(assignee_group.assignee_ids, '') AS assignee_ids, "
                        + "COALESCE(assignee_group.assignee_names, '') AS assignee_names, "
                        + "task.task_type, task.cycle_rule, task.note, task.status, task.due_at, task.completed_at, "
                        + "task.created_at FROM chore_task task "
                        + "LEFT JOIN family_member owner ON owner.id = task.owner_member_id AND owner.deleted = 0 "
                        + "LEFT JOIN family_member assignee ON assignee.id = task.assignee_id AND assignee.deleted = 0 "
                        + "LEFT JOIN (SELECT relation.task_id, "
                        + "GROUP_CONCAT(relation.member_id ORDER BY relation.created_at SEPARATOR ',') AS assignee_ids, "
                        + "GROUP_CONCAT(member.display_name ORDER BY relation.created_at SEPARATOR '、') AS assignee_names "
                        + "FROM chore_task_assignee relation "
                        + "LEFT JOIN family_member member ON member.id = relation.member_id AND member.deleted = 0 "
                        + "WHERE relation.family_id = ? AND relation.deleted = 0 GROUP BY relation.task_id) assignee_group "
                        + "ON assignee_group.task_id = task.id "
                        + "WHERE task.family_id = ? AND task.deleted = 0 "
                        + "ORDER BY CASE WHEN task.status = 'TODO' THEN 0 ELSE 1 END ASC, "
                        + "COALESCE(task.due_at, task.created_at) ASC, task.created_at DESC",
                familyId, familyId);
    }

    /**
     * 查询单个待办任务。
     *
     * @param familyId 家庭 ID
     * @param taskId 任务 ID
     * @return 任务
     */
    public Optional<Map<String, Object>> findTask(Long familyId, Long taskId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, family_id, title, task_scope, owner_member_id, assignee_id, task_type, cycle_rule, note, "
                        + "status, due_at, completed_at FROM chore_task "
                        + "WHERE id = ? AND family_id = ? AND deleted = 0",
                taskId, familyId);
        return rows.stream().findFirst();
    }

    /**
     * 更新待办任务。
     *
     * @param familyId 家庭 ID
     * @param taskId 任务 ID
     * @param request 更新请求
     * @param taskScope 任务范围
     * @param ownerMemberId 所属成员
     * @param assigneeId 认领成员
     * @param taskType 任务类型
     * @param dueAt 截止时间
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int update(Long familyId, Long taskId, UpdateChoreTaskRequest request, String taskScope,
            Long ownerMemberId, Long assigneeId, String taskType, LocalDateTime dueAt, Long operatorId,
            LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE chore_task SET title = ?, task_scope = ?, owner_member_id = ?, assignee_id = ?, task_type = ?, "
                        + "cycle_rule = ?, note = ?, due_at = ?, updated_at = ?, updated_by = ? "
                        + "WHERE id = ? AND family_id = ? AND deleted = 0",
                request.getTitle(), taskScope, ownerMemberId, assigneeId, taskType, request.getCycleRule(),
                request.getNote(), dueAt, now, operatorId, taskId, familyId);
    }

    /**
     * 清空待办认领成员。
     *
     * @param familyId 家庭 ID
     * @param taskId 任务 ID
     * @param operatorId 操作人
     * @param now 当前时间
     */
    public void clearAssignees(Long familyId, Long taskId, Long operatorId, LocalDateTime now) {
        jdbcTemplate.update(
                "UPDATE chore_task_assignee SET deleted = 1, updated_at = ?, updated_by = ? "
                        + "WHERE family_id = ? AND task_id = ? AND deleted = 0",
                now, operatorId, familyId, taskId);
    }

    /**
     * 恢复已存在的认领成员关系。
     *
     * @param familyId 家庭 ID
     * @param taskId 任务 ID
     * @param memberId 成员 ID
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int restoreAssignee(Long familyId, Long taskId, Long memberId, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE chore_task_assignee SET deleted = 0, updated_at = ?, updated_by = ? "
                        + "WHERE family_id = ? AND task_id = ? AND member_id = ?",
                now, operatorId, familyId, taskId, memberId);
    }

    /**
     * 新增认领成员关系。
     *
     * @param id 主键
     * @param familyId 家庭 ID
     * @param taskId 任务 ID
     * @param memberId 成员 ID
     * @param operatorId 操作人
     * @param now 当前时间
     */
    public void insertAssignee(Long id, Long familyId, Long taskId, Long memberId, Long operatorId,
            LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO chore_task_assignee (id, family_id, task_id, member_id, created_at, updated_at, "
                        + "created_by, updated_by, deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)",
                id, familyId, taskId, memberId, now, now, operatorId, operatorId);
    }

    /**
     * 设置待办主认领人。
     *
     * @param familyId 家庭 ID
     * @param taskId 任务 ID
     * @param assigneeId 主认领人
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int setPrimaryAssignee(Long familyId, Long taskId, Long assigneeId, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE chore_task SET assignee_id = ?, updated_at = ?, updated_by = ? "
                        + "WHERE id = ? AND family_id = ? AND deleted = 0",
                assigneeId, now, operatorId, taskId, familyId);
    }

    /**
     * 认领待办任务。
     *
     * @param familyId 家庭 ID
     * @param taskId 任务 ID
     * @param assigneeId 认领成员
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int claim(Long familyId, Long taskId, Long assigneeId, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE chore_task SET assignee_id = ?, updated_at = ?, updated_by = ? "
                        + "WHERE id = ? AND family_id = ? AND deleted = 0 AND assignee_id IS NULL",
                assigneeId, now, operatorId, taskId, familyId);
    }

    /**
     * 完成待办任务。
     *
     * @param familyId 家庭 ID
     * @param taskId 任务 ID
     * @param assigneeId 完成人
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int complete(Long familyId, Long taskId, Long assigneeId, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE chore_task SET status = 'DONE', assignee_id = COALESCE(assignee_id, ?), completed_at = ?, "
                        + "updated_at = ?, updated_by = ? WHERE id = ? AND family_id = ? AND deleted = 0",
                assigneeId, now, now, operatorId, taskId, familyId);
    }

    /**
     * 删除待办任务。
     *
     * @param familyId 家庭 ID
     * @param taskId 任务 ID
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int delete(Long familyId, Long taskId, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE chore_task SET deleted = 1, updated_at = ?, updated_by = ? "
                        + "WHERE id = ? AND family_id = ? AND deleted = 0",
                now, operatorId, taskId, familyId);
    }
}
