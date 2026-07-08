package com.homeofus.reminder.repository;

import com.homeofus.reminder.dto.CreateReminderRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 提醒数据访问。
 *
 * @author tanchaohong
 */
@Repository
public class ReminderRepository {

    private final JdbcTemplate jdbcTemplate;

    public ReminderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 新增提醒。
     *
     * @param id 主键
     * @param familyId 家庭 ID
     * @param request 创建请求
     * @param sourceType 来源类型
     * @param dueAt 到期时间
     * @param now 当前时间
     */
    public void insert(Long id, Long familyId, CreateReminderRequest request, String sourceType, LocalDateTime dueAt,
            LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO reminder (id, family_id, title, description, source_type, source_id, due_at, repeat_rule, "
                        + "status, completed_at, created_at, updated_at, created_by, updated_by, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', NULL, ?, ?, ?, ?, 0)",
                id, familyId, request.getTitle(), request.getDescription(), sourceType, request.getSourceId(), dueAt,
                request.getRepeatRule(), now, now, 1001L, 1001L);
    }

    /**
     * 按来源新增提醒。
     *
     * @param id 主键
     * @param familyId 家庭 ID
     * @param title 标题
     * @param description 描述
     * @param sourceType 来源类型
     * @param sourceId 来源 ID
     * @param dueAt 到期时间
     * @param now 当前时间
     */
    public void insertFromSource(Long id, Long familyId, String title, String description, String sourceType,
            Long sourceId, LocalDateTime dueAt, LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO reminder (id, family_id, title, description, source_type, source_id, due_at, repeat_rule, "
                        + "status, completed_at, created_at, updated_at, created_by, updated_by, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, NULL, 'PENDING', NULL, ?, ?, ?, ?, 0)",
                id, familyId, title, description, sourceType, sourceId, dueAt, now, now, 1001L, 1001L);
    }

    /**
     * 查询提醒列表。
     *
     * @param familyId 家庭 ID
     * @param limit 查询数量
     * @return 提醒列表
     */
    public List<Map<String, Object>> findReminders(Long familyId, int limit) {
        return jdbcTemplate.queryForList(
                "SELECT id, family_id, title, description, source_type, source_id, due_at, repeat_rule, status, "
                        + "completed_at, created_at FROM reminder "
                        + "WHERE family_id = ? AND deleted = 0 ORDER BY status ASC, due_at ASC LIMIT ?",
                familyId, limit);
    }

    /**
     * 完成提醒。
     *
     * @param id 提醒 ID
     * @param now 当前时间
     * @return 更新行数
     */
    public int complete(Long id, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE reminder SET status = 'DONE', completed_at = ?, updated_at = ?, updated_by = ? "
                        + "WHERE id = ? AND deleted = 0",
                now, now, 1001L, id);
    }

    /**
     * 查询待处理提醒数量。
     *
     * @param familyId 家庭 ID
     * @return 待处理数量
     */
    public Integer countPending(Long familyId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM reminder WHERE family_id = ? AND status = 'PENDING' AND deleted = 0",
                Integer.class, familyId);
    }
}

