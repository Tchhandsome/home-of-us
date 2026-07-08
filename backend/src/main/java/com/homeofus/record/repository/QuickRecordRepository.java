package com.homeofus.record.repository;

import com.homeofus.record.dto.CreateQuickRecordRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 快速记录数据访问。
 *
 * @author tanchaohong
 */
@Repository
public class QuickRecordRepository {

    private final JdbcTemplate jdbcTemplate;

    public QuickRecordRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 新增快速记录。
     *
     * @param id 主键
     * @param familyId 家庭 ID
     * @param request 创建请求
     * @param recordType 记录类型
     * @param happenedOn 发生日期
     * @param operatorId 操作人
     * @param now 当前时间
     */
    public void insert(Long id, Long familyId, CreateQuickRecordRequest request, String recordType, LocalDate happenedOn,
            Long operatorId, LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO quick_record (id, family_id, raw_text, record_type, linked_type, linked_id, happened_on, "
                        + "created_at, updated_at, created_by, updated_by, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                id, familyId, request.getRawText(), recordType, request.getLinkedType(), request.getLinkedId(),
                happenedOn, now, now, operatorId, operatorId);
    }

    /**
     * 查询最近快速记录。
     *
     * @param familyId 家庭 ID
     * @param limit 查询数量
     * @return 记录列表
     */
    public List<Map<String, Object>> findRecent(Long familyId, int limit) {
        return jdbcTemplate.queryForList(
                "SELECT id, family_id, raw_text, record_type, linked_type, linked_id, happened_on, created_at "
                        + "FROM quick_record WHERE family_id = ? AND deleted = 0 ORDER BY created_at DESC LIMIT ?",
                familyId, limit);
    }

    /**
     * 删除快速记录。
     *
     * @param id 记录 ID
     * @param familyId 家庭 ID
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int delete(Long id, Long familyId, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE quick_record SET deleted = 1, updated_at = ?, updated_by = ? "
                        + "WHERE id = ? AND family_id = ? AND deleted = 0",
                now, operatorId, id, familyId);
    }
}
