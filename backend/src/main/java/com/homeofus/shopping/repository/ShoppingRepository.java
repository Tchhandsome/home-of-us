package com.homeofus.shopping.repository;

import com.homeofus.shopping.dto.CreateShoppingItemRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 购物清单数据访问。
 *
 * @author tanchaohong
 */
@Repository
public class ShoppingRepository {

    private final JdbcTemplate jdbcTemplate;

    public ShoppingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 新增购物项。
     *
     * @param id 主键
     * @param familyId 家庭 ID
     * @param request 创建请求
     * @param operatorId 操作人
     * @param now 当前时间
     */
    public void insert(Long id, Long familyId, CreateShoppingItemRequest request, Long operatorId, LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO shopping_item (id, family_id, name, category, channel, quantity, status, actual_amount, "
                        + "purchased_at, buyer_id, finance_record_id, checked_at, created_at, updated_at, created_by, "
                        + "updated_by, deleted) VALUES (?, ?, ?, ?, ?, ?, 'TODO', NULL, NULL, NULL, NULL, NULL, ?, ?, ?, ?, 0)",
                id, familyId, request.getName(), request.getCategory(), request.getChannel(), request.getQuantity(),
                now, now, operatorId, operatorId);
    }

    /**
     * 查询购物清单。
     *
     * @param familyId 家庭 ID
     * @return 购物项列表
     */
    public List<Map<String, Object>> findItems(Long familyId) {
        return jdbcTemplate.queryForList(
                "SELECT id, family_id, name, category, channel, quantity, status, actual_amount, purchased_at, "
                        + "buyer_id, finance_record_id, checked_at, created_at "
                        + "FROM shopping_item WHERE family_id = ? AND deleted = 0 ORDER BY status DESC, created_at DESC",
                familyId);
    }

    /**
     * 查询购物项。
     *
     * @param id 购物项 ID
     * @param familyId 家庭 ID
     * @return 购物项
     */
    public Optional<Map<String, Object>> findById(Long id, Long familyId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, family_id, name, category, status, finance_record_id FROM shopping_item "
                        + "WHERE id = ? AND family_id = ? AND deleted = 0 FOR UPDATE",
                id, familyId);
        return rows.stream().findFirst();
    }

    /**
     * 勾选购物项。
     *
     * @param id 购物项 ID
     * @param now 当前时间
     * @return 更新行数
     */
    public int complete(Long id, Long familyId, BigDecimal actualAmount, Long buyerId, Long financeRecordId,
            Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE shopping_item SET status = 'DONE', actual_amount = ?, purchased_at = ?, buyer_id = ?, "
                        + "finance_record_id = COALESCE(finance_record_id, ?), checked_at = ?, updated_at = ?, "
                        + "updated_by = ? WHERE id = ? AND family_id = ? AND deleted = 0",
                actualAmount, now, buyerId, financeRecordId, now, now, operatorId, id, familyId);
    }

    /**
     * 查询待买数量。
     *
     * @param familyId 家庭 ID
     * @return 待买数量
     */
    public Integer countTodo(Long familyId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM shopping_item WHERE family_id = ? AND status = 'TODO' AND deleted = 0",
                Integer.class, familyId);
    }

    /**
     * 删除购物项。
     *
     * @param id 购物项 ID
     * @param familyId 家庭 ID
     * @param operatorId 操作人
     * @param now 当前时间
     * @return 更新行数
     */
    public int delete(Long id, Long familyId, Long operatorId, LocalDateTime now) {
        return jdbcTemplate.update(
                "UPDATE shopping_item SET deleted = 1, updated_at = ?, updated_by = ? "
                        + "WHERE id = ? AND family_id = ? AND deleted = 0",
                now, operatorId, id, familyId);
    }
}
