package com.homeofus.inventory.repository;

import com.homeofus.inventory.dto.CreateInventoryItemRequest;
import com.homeofus.inventory.dto.UpdateInventoryItemRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 家庭库存数据访问。
 *
 * @author tanchaohong
 */
@Repository
public class InventoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public InventoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 新增库存项。
     *
     * @param id 主键
     * @param familyId 家庭 ID
     * @param request 创建请求
     * @param itemType 物资类型
     * @param status 库存状态
     * @param expiresOn 过期日期
     * @param reminderDaysBefore 提前提醒天数
     * @param operatorId 操作人
     * @param now 当前时间
     */
    public void insert(Long id, Long familyId, CreateInventoryItemRequest request, String itemType, String status,
            LocalDate expiresOn, Integer reminderDaysBefore, Long operatorId, LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO inventory_item (id, family_id, name, item_type, category, quantity, unit, "
                        + "low_stock_threshold, expires_on, reminder_days_before, note, status, created_at, updated_at, "
                        + "created_by, updated_by, deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                id, familyId, request.getName(), itemType, request.getCategory(), request.getQuantity(),
                request.getUnit(), request.getLowStockThreshold(), expiresOn, reminderDaysBefore, request.getNote(),
                status, now, now, operatorId, operatorId);
    }

    /**
     * 查询库存列表。
     *
     * @param familyId 家庭 ID
     * @return 库存列表
     */
    public List<Map<String, Object>> findItems(Long familyId) {
        return jdbcTemplate.queryForList(
                "SELECT id, family_id, name, item_type, category, quantity, unit, low_stock_threshold, expires_on, "
                        + "reminder_days_before, note, status, created_at FROM inventory_item "
                        + "WHERE family_id = ? AND deleted = 0 "
                        + "ORDER BY CASE WHEN status = 'LOW' THEN 0 ELSE 1 END ASC, created_at DESC",
                familyId);
    }

    /**
     * 查询单个库存项。
     *
     * @param id 库存项 ID
     * @param familyId 家庭 ID
     * @return 库存项
     */
    public Optional<Map<String, Object>> findItem(Long id, Long familyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, family_id, name, item_type, category, quantity, unit, low_stock_threshold, expires_on, "
                        + "reminder_days_before, note, status FROM inventory_item "
                        + "WHERE id = ? AND family_id = ? AND deleted = 0",
                id, familyId);
        return rows.stream().findFirst();
    }

    /**
     * 更新库存项。
     *
     * @param id 库存项 ID
     * @param familyId 家庭 ID
     * @param request 更新请求
     * @param itemType 物资类型
     * @param status 库存状态
     * @param expiresOn 过期日期
     * @param reminderDaysBefore 提前提醒天数
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int update(Long id, Long familyId, UpdateInventoryItemRequest request, String itemType, String status,
            LocalDate expiresOn, Integer reminderDaysBefore, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE inventory_item SET name = ?, item_type = ?, category = ?, quantity = ?, unit = ?, "
                        + "low_stock_threshold = ?, expires_on = ?, reminder_days_before = ?, note = ?, status = ?, "
                        + "updated_at = ?, updated_by = ? WHERE id = ? AND family_id = ? AND deleted = 0",
                request.getName(), itemType, request.getCategory(), request.getQuantity(), request.getUnit(),
                request.getLowStockThreshold(), expiresOn, reminderDaysBefore, request.getNote(), status, now,
                operatorId, id, familyId);
    }

    /**
     * 删除库存项。
     *
     * @param id 库存项 ID
     * @param familyId 家庭 ID
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int delete(Long id, Long familyId, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE inventory_item SET deleted = 1, updated_at = ?, updated_by = ? "
                        + "WHERE id = ? AND family_id = ? AND deleted = 0",
                now, operatorId, id, familyId);
    }
}
