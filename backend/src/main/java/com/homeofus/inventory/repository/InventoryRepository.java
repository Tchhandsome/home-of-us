package com.homeofus.inventory.repository;

import com.homeofus.inventory.dto.CreateInventoryItemRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
     * @param status 库存状态
     * @param now 当前时间
     */
    public void insert(Long id, Long familyId, CreateInventoryItemRequest request, String status, LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO inventory_item (id, family_id, name, category, quantity, unit, low_stock_threshold, "
                        + "status, created_at, updated_at, created_by, updated_by, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                id, familyId, request.getName(), request.getCategory(), request.getQuantity(), request.getUnit(),
                request.getLowStockThreshold(), status, now, now, 1001L, 1001L);
    }

    /**
     * 查询库存列表。
     *
     * @param familyId 家庭 ID
     * @return 库存列表
     */
    public List<Map<String, Object>> findItems(Long familyId) {
        return jdbcTemplate.queryForList(
                "SELECT id, family_id, name, category, quantity, unit, low_stock_threshold, status, created_at "
                        + "FROM inventory_item WHERE family_id = ? AND deleted = 0 ORDER BY status ASC, created_at DESC",
                familyId);
    }
}

