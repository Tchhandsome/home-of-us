package com.homeofus.system.dashboard;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 首页摘要数据访问。
 *
 * @author tanchaohong
 */
@Repository
public class DashboardRepository {

    private final JdbcTemplate jdbcTemplate;

    public DashboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询今日摘要。
     *
     * @param familyId 家庭 ID
     * @param month 当前月份
     * @return 今日摘要
     */
    public Map<String, Object> loadTodaySummary(Long familyId, String month) {
        Map<String, Object> summary = new LinkedHashMap<>();
        // 首页只聚合关键数字，详细数据由各模块接口继续承载。
        summary.put("pendingReminders", countPendingReminders(familyId));
        summary.put("plantCount", countPlants(familyId));
        summary.put("shoppingTodoCount", countShoppingTodo(familyId));
        summary.put("choreTodoCount", countChoreTodo(familyId));
        summary.put("inventoryLowCount", countLowInventory(familyId));
        summary.put("monthExpense", sumMonthExpense(familyId, month));
        return summary;
    }

    private Integer countPendingReminders(Long familyId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM reminder WHERE family_id = ? AND status = 'PENDING' AND deleted = 0",
                Integer.class, familyId);
    }

    private Integer countPlants(Long familyId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(1) FROM plant WHERE family_id = ? AND deleted = 0",
                Integer.class, familyId);
    }

    private Integer countShoppingTodo(Long familyId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM shopping_item WHERE family_id = ? AND status = 'TODO' AND deleted = 0",
                Integer.class, familyId);
    }

    private Integer countChoreTodo(Long familyId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM chore_task WHERE family_id = ? AND status = 'TODO' AND deleted = 0",
                Integer.class, familyId);
    }

    private Integer countLowInventory(Long familyId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM inventory_item WHERE family_id = ? AND status = 'LOW' AND deleted = 0",
                Integer.class, familyId);
    }

    private Object sumMonthExpense(Long familyId, String month) {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM finance_record WHERE family_id = ? AND direction = 'EXPENSE' "
                        + "AND deleted = 0 AND DATE_FORMAT(occurred_on, '%Y-%m') = ?",
                Object.class, familyId, month);
    }
}

