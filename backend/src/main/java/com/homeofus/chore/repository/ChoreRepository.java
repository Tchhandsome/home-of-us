package com.homeofus.chore.repository;

import com.homeofus.chore.dto.CreateChoreTaskRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 家务任务数据访问。
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
     * 新增家务任务。
     *
     * @param id 主键
     * @param familyId 家庭 ID
     * @param request 创建请求
     * @param taskType 任务类型
     * @param dueAt 到期时间
     * @param now 当前时间
     */
    public void insert(Long id, Long familyId, CreateChoreTaskRequest request, String taskType, LocalDateTime dueAt,
            LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO chore_task (id, family_id, title, assignee_id, task_type, cycle_rule, status, due_at, "
                        + "completed_at, created_at, updated_at, created_by, updated_by, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, ?, 'TODO', ?, NULL, ?, ?, ?, ?, 0)",
                id, familyId, request.getTitle(), request.getAssigneeId(), taskType, request.getCycleRule(), dueAt,
                now, now, 1001L, 1001L);
    }

    /**
     * 查询家务任务。
     *
     * @param familyId 家庭 ID
     * @return 家务任务列表
     */
    public List<Map<String, Object>> findTasks(Long familyId) {
        return jdbcTemplate.queryForList(
                "SELECT id, family_id, title, assignee_id, task_type, cycle_rule, status, due_at, completed_at, "
                        + "created_at FROM chore_task WHERE family_id = ? AND deleted = 0 ORDER BY status DESC, due_at ASC",
                familyId);
    }
}

