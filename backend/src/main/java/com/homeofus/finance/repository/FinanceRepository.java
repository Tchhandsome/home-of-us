package com.homeofus.finance.repository;

import com.homeofus.finance.dto.CreateFinanceRecordRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 家庭账本数据访问。
 *
 * @author tanchaohong
 */
@Repository
public class FinanceRepository {

    private final JdbcTemplate jdbcTemplate;

    public FinanceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 新增账本记录。
     *
     * @param id 主键
     * @param familyId 家庭 ID
     * @param request 创建请求
     * @param direction 收支方向
     * @param ownerId 所属成员 ID
     * @param operatorId 操作人
     * @param occurredOn 发生日期
     * @param now 当前时间
     */
    public void insert(Long id, Long familyId, CreateFinanceRecordRequest request, String direction,
            Long ownerId, Long operatorId, LocalDate occurredOn, LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO finance_record (id, family_id, title, amount, direction, category, owner_id, occurred_on, "
                        + "created_at, updated_at, created_by, updated_by, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                id, familyId, request.getTitle(), request.getAmount(), direction, request.getCategory(),
                ownerId, occurredOn, now, now, operatorId, operatorId);
    }

    /**
     * 新增系统生成的账本记录。
     *
     * @param id 主键
     * @param familyId 家庭 ID
     * @param title 标题
     * @param amount 金额
     * @param direction 收支方向
     * @param category 分类
     * @param ownerId 所属成员 ID
     * @param operatorId 操作人
     * @param occurredOn 发生日期
     * @param now 当前时间
     */
    public void insertDirect(Long id, Long familyId, String title, BigDecimal amount, String direction, String category,
            Long ownerId, Long operatorId, LocalDate occurredOn, LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO finance_record (id, family_id, title, amount, direction, category, owner_id, occurred_on, "
                        + "created_at, updated_at, created_by, updated_by, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                id, familyId, title, amount, direction, category, ownerId, occurredOn, now, now, operatorId,
                operatorId);
    }

    /**
     * 查询账本记录。
     *
     * @param familyId 家庭 ID
     * @return 账本记录
     */
    public List<Map<String, Object>> findRecords(Long familyId) {
        return jdbcTemplate.queryForList(
                "SELECT id, family_id, title, amount, direction, category, owner_id, occurred_on, created_at "
                        + "FROM finance_record WHERE family_id = ? AND deleted = 0 ORDER BY occurred_on DESC, created_at DESC",
                familyId);
    }

    /**
     * 查询月度摘要。
     *
     * @param familyId 家庭 ID
     * @param monthPrefix 月份前缀
     * @return 摘要列表
     */
    public List<Map<String, Object>> summarizeMonth(Long familyId, String monthPrefix) {
        return jdbcTemplate.queryForList(
                "SELECT direction, COALESCE(SUM(amount), 0) AS amount FROM finance_record "
                        + "WHERE family_id = ? AND deleted = 0 AND DATE_FORMAT(occurred_on, '%Y-%m') = ? GROUP BY direction",
                familyId, monthPrefix);
    }

    /**
     * 查询账本摘要。
     *
     * @param familyId 家庭 ID
     * @param monthPrefix 月份前缀
     * @param weekStart 周起始日期
     * @param weekEnd 周结束日期
     * @return 摘要
     */
    public Map<String, Object> loadSummary(Long familyId, String monthPrefix, LocalDate weekStart, LocalDate weekEnd) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalExpense", jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM finance_record WHERE family_id = ? "
                        + "AND direction = 'EXPENSE' AND deleted = 0",
                Object.class, familyId));
        summary.put("monthExpense", jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM finance_record WHERE family_id = ? "
                        + "AND direction = 'EXPENSE' AND deleted = 0 AND DATE_FORMAT(occurred_on, '%Y-%m') = ?",
                Object.class, familyId, monthPrefix));
        summary.put("weekExpense", jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM finance_record WHERE family_id = ? "
                        + "AND direction = 'EXPENSE' AND deleted = 0 AND occurred_on BETWEEN ? AND ?",
                Object.class, familyId, weekStart, weekEnd));
        return summary;
    }

    /**
     * 删除账本记录。
     *
     * @param id 记录 ID
     * @param familyId 家庭 ID
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int delete(Long id, Long familyId, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE finance_record SET deleted = 1, updated_at = ?, updated_by = ? "
                        + "WHERE id = ? AND family_id = ? AND deleted = 0",
                now, operatorId, id, familyId);
    }
}
