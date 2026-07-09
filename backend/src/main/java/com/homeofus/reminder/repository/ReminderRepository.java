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
     * @param operatorId 操作人
     * @param now 当前时间
     */
    public void insert(Long id, Long familyId, CreateReminderRequest request, String sourceType, LocalDateTime dueAt,
            Long operatorId, LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO reminder (id, family_id, title, description, source_type, source_id, due_at, repeat_rule, "
                        + "status, completed_at, created_at, updated_at, created_by, updated_by, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', NULL, ?, ?, ?, ?, 0)",
                id, familyId, request.getTitle(), request.getDescription(), sourceType, request.getSourceId(), dueAt,
                request.getRepeatRule(), now, now, operatorId, operatorId);
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
     * @param operatorId 操作人
     * @param now 当前时间
     */
    public void insertFromSource(Long id, Long familyId, String title, String description, String sourceType,
            Long sourceId, LocalDateTime dueAt, Long operatorId, LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO reminder (id, family_id, title, description, source_type, source_id, due_at, repeat_rule, "
                        + "status, completed_at, created_at, updated_at, created_by, updated_by, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, NULL, 'PENDING', NULL, ?, ?, ?, ?, 0)",
                id, familyId, title, description, sourceType, sourceId, dueAt, now, now, operatorId, operatorId);
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
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int complete(Long id, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE reminder SET status = 'DONE', completed_at = ?, updated_at = ?, updated_by = ? "
                        + "WHERE id = ? AND deleted = 0",
                now, now, operatorId, id);
    }

    /**
     * 删除提醒。
     *
     * @param id 提醒 ID
     * @param familyId 家庭 ID
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int delete(Long id, Long familyId, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE reminder SET deleted = 1, updated_at = ?, updated_by = ? "
                        + "WHERE id = ? AND family_id = ? AND deleted = 0",
                now, operatorId, id, familyId);
    }

    /**
     * 按来源删除提醒。
     *
     * @param familyId 家庭 ID
     * @param sourceType 来源类型
     * @param sourceId 来源 ID
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int deleteBySource(Long familyId, String sourceType, Long sourceId, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE reminder SET deleted = 1, updated_at = ?, updated_by = ? "
                        + "WHERE family_id = ? AND source_type = ? AND source_id = ? AND deleted = 0",
                now, operatorId, familyId, sourceType, sourceId);
    }

    /**
     * 按来源更新时间。
     *
     * @param familyId 家庭 ID
     * @param sourceType 来源类型
     * @param sourceId 来源 ID
     * @param oldDueAt 原到期时间
     * @param newDueAt 新到期时间
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int updateDueAtBySource(Long familyId, String sourceType, Long sourceId, LocalDateTime oldDueAt,
            LocalDateTime newDueAt, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE reminder SET due_at = ?, updated_at = ?, updated_by = ? "
                        + "WHERE family_id = ? AND source_type = ? AND source_id = ? AND due_at = ? "
                        + "AND status = 'PENDING' AND deleted = 0",
                newDueAt, now, operatorId, familyId, sourceType, sourceId, oldDueAt);
    }

    /**
     * 按来源逻辑删除提醒。
     *
     * @param familyId 家庭 ID
     * @param sourceType 来源类型
     * @param sourceId 来源 ID
     * @param oldDueAt 原到期时间
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int deleteBySourceAndDueAt(Long familyId, String sourceType, Long sourceId, LocalDateTime oldDueAt,
            Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE reminder SET deleted = 1, updated_at = ?, updated_by = ? "
                        + "WHERE family_id = ? AND source_type = ? AND source_id = ? AND due_at = ? "
                        + "AND status = 'PENDING' AND deleted = 0",
                now, operatorId, familyId, sourceType, sourceId, oldDueAt);
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
